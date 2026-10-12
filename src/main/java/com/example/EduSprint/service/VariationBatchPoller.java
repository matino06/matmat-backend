package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

// Provjerava batcheve prvih varijacija. Svaki uspješan odgovor postaje posao task_variation s nacrtom
// (provjere i spremanje su tamo); lekcija bez odgovora, ili cijeli batch koji je pao ili istekao, ide na obični Sol.
@Component
public class VariationBatchPoller {

    private static final Logger log = LoggerFactory.getLogger(VariationBatchPoller.class);
    static final Set<String> TERMINAL = Set.of("completed", "failed", "expired", "cancelled");

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final BuildJobQueue queue;
    private final PlanSupport plan;
    private final boolean enabled;
    private final ObjectMapper mapper = new ObjectMapper();

    public VariationBatchPoller(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, BuildJobQueue queue, PlanSupport plan,
                                @Value("${own-courses.build.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.queue = queue;
        this.plan = plan;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelay = 3 * 60 * 1000, initialDelay = 60 * 1000)
    public void poll() {
        if (!enabled) return;
        List<String> open = jdbc.queryForList("SELECT batch_id FROM variation_batch WHERE status = 'u_tijeku'",
                new MapSqlParameterSource(), String.class);
        for (String batchId : open) {
            try {
                check(batchId);
            } catch (Exception e) {
                log.warn("Batch {} nije provjeren: {}", batchId, e.getMessage());
            }
        }
    }

    void check(String batchId) throws Exception {
        JsonNode batch = llm.getBatch(batchId);
        String status = batch.path("status").asText();
        if (!TERMINAL.contains(status)) return;
        MapSqlParameterSource b = new MapSqlParameterSource("b", batchId);
        // samo jedna instanca obrađuje gotov batch
        if (jdbc.update("UPDATE variation_batch SET status = 'obrada' WHERE batch_id = :b AND status = 'u_tijeku'", b) == 0) return;
        Map<String, Object> row = jdbc.queryForMap("SELECT course_id, account_id, job_id, objective_ids FROM variation_batch WHERE batch_id = :b", b);
        long courseId = ((Number) row.get("course_id")).longValue();
        long accountId = ((Number) row.get("account_id")).longValue();
        Long jobId = row.get("job_id") == null ? null : ((Number) row.get("job_id")).longValue();

        JsonNode cost = batch.path("usage").path("cost");
        if (cost.isNumber()) {
            queue.recordCost(new BuildJob(jobId == null ? 0 : jobId, courseId, accountId, VariationBatchStep.KIND, batchId,
                    BuildJobQueue.PRIORITY_BACKGROUND, false, 1, null), "batch", cost.decimalValue());
        }
        Map<Long, JsonNode> drafts = "completed".equals(status) ? drafts(batch.path("results")) : Map.of();
        for (JsonNode id : mapper.readTree((String) row.get("objective_ids"))) {
            long o = id.asLong();
            ObjectNode input = (ObjectNode) mapper.readTree(TaskVariationStep.input(o, 1));
            if (drafts.containsKey(o)) input.set("draft", drafts.get(o));
            queue.enqueue(courseId, accountId, TaskVariationStep.KIND, "v1 " + o, BuildJobQueue.PRIORITY_BACKGROUND, false, input.toString());
        }
        jdbc.update("UPDATE variation_batch SET status = :s, finished_at = now() WHERE batch_id = :b",
                b.addValue("s", "completed".equals(status) ? "gotov" : "neuspio"));
    }

    // custom_id „o<lekcija>“ → JSON zadatka iz odgovora modela; neuspjeli i neispravni odgovori se preskaču.
    Map<Long, JsonNode> drafts(JsonNode results) {
        Map<Long, JsonNode> out = new HashMap<>();
        for (JsonNode r : results) {
            String id = r.path("custom_id").asText("");
            JsonNode response = r.path("response");
            if (!id.startsWith("o") || response.path("status_code").asInt() != 200) continue;
            try {
                String content = response.path("body").path("choices").path(0).path("message").path("content").asText("");
                out.put(Long.parseLong(id.substring(1)), plan.json(content));
            } catch (RuntimeException ignored) {
                // lekcija ide na obični Sol
            }
        }
        return out;
    }

}
