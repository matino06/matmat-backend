package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenRouterClientTest {

    @Test
    void blankReasoningEffortMeansModelDefault() {
        assertNull(OpenRouterClient.reasoningEffort(null));
        assertNull(OpenRouterClient.reasoningEffort(""));
        assertNull(OpenRouterClient.reasoningEffort("  "));
    }

    @Test
    void reasoningEffortIsNormalised() {
        assertEquals("high", OpenRouterClient.reasoningEffort(" High "));
        assertEquals("none", OpenRouterClient.reasoningEffort("none"));
    }

    @Test
    void unknownReasoningEffortIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> OpenRouterClient.reasoningEffort("maximum"));
    }

    @Test
    void everyCallAsksForProvidersThatDoNotCollectData() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode plain = mapper.createObjectNode().put("model", "anthropic/claude-haiku-5.5");
        assertEquals("deny", OpenRouterClient.denyDataCollection(plain).path("provider").path("data_collection").asText());

        ObjectNode routed = mapper.createObjectNode().put("model", "x");
        routed.putObject("provider").put("sort", "price").put("data_collection", "allow");
        ObjectNode out = OpenRouterClient.denyDataCollection(routed);
        assertEquals("deny", out.path("provider").path("data_collection").asText());
        assertEquals("price", out.path("provider").path("sort").asText());   // other routing is kept
    }
}
