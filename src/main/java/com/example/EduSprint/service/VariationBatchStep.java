package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Plan s više od 50 lekcija: 1. varijacije svih lekcija osim prvih 5 šalju se kao jedan batch na OpenRouter
// (pola cijene, do 24 h). Rezultate preuzima VariationBatchPoller.
@Component
public class VariationBatchStep implements BuildStep {

    public static final String KIND = "task_batch";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final VariationPrompt prompt;
    private final TaskVariationStep variations;
    private final ObjectMapper mapper = new ObjectMapper();

    public VariationBatchStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, VariationPrompt prompt, TaskVariationStep variations) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.prompt = prompt;
        this.variations = variations;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) throws Exception {
        MapSqlParameterSource j = new MapSqlParameterSource("j", job.jobId());
        List<String> sent = jdbc.queryForList("SELECT batch_id FROM variation_batch WHERE job_id = :j", j, String.class);
        if (!sent.isEmpty()) return sent.get(0); // već poslan (posao je ponovljen nakon slanja)

        JsonNode ids = mapper.readTree(job.input()).path("objectiveIds");
        String system = prompt.system(job.courseId());
        Map<String, ObjectNode> requests = new LinkedHashMap<>();
        for (JsonNode id : ids) {
            requests.put("o" + id.asLong(), llm.body(variations.model(), system, prompt.user(id.asLong()), TaskVariationStep.options()));
        }
        String batchId = llm.createBatch(variations.model(), requests);
        jdbc.update("""
                INSERT INTO variation_batch (batch_id, course_id, account_id, job_id, objective_ids)
                VALUES (:b, :c, :a, :j, :o)""", j.addValue("b", batchId).addValue("c", job.courseId())
                .addValue("a", job.accountId()).addValue("o", ids.toString()));
        return batchId;
    }
}
