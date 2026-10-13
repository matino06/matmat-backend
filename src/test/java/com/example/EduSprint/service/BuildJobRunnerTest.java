package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BuildJobRunnerTest {

    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final BuildQuotaService quota = mock(BuildQuotaService.class);
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    private static BuildJob job(String kind, boolean countsQuota) {
        return new BuildJob(1, 2, 3, kind, null, 1, countsQuota, 1, null);
    }

    private BuildJobRunner runner(BuildStep... steps) {
        return new BuildJobRunner(queue, quota, List.of(steps), jdbc, 2, "no-key");
    }

    private static BuildStep step(String kind, String output, RuntimeException error) {
        return new BuildStep() {
            public String kind() { return kind; }
            public String run(BuildJob job) {
                if (error != null) throw error;
                return output;
            }
        };
    }

    @Test
    void successfulStepCompletesTheJob() {
        BuildJob j = job("read_page", true);
        runner(step("read_page", "OUT", null)).run(j);
        verify(queue).complete(j, "OUT");
    }

    @Test
    void failingStepIsRetriedThroughTheQueue() {
        BuildJob j = job("read_page", true);
        runner(step("read_page", null, new RuntimeException("boom"))).run(j);
        verify(queue).fail(j, "boom");
        verify(queue, never()).complete(any(), any());
    }

    @Test
    void unknownKindFails() {
        BuildJob j = job("nepoznato", true);
        runner(step("read_page", "OUT", null)).run(j);
        verify(queue).fail(eq(j), anyString());
    }

    @Test
    void globalLimitPausesInsteadOfFailing() {
        BuildJob j = job("read_page", true);
        runner(step("read_page", null, new OwnCourseLlm.GlobalLimitException("OpenRouter 403 limit", null))).run(j);
        verify(queue).markPaused(j, "OpenRouter 403 limit");
        verify(queue, never()).fail(any(), any());
    }

    @Test
    void jobOfAccountWithoutQuotaWaitsForRenewal() {
        BuildJob j = job("read_page", true);
        when(queue.claimNext()).thenReturn(j, (BuildJob) null);
        when(quota.hasQuota(3)).thenReturn(false);
        runner(step("read_page", "OUT", null)).tick();
        verify(queue).markWaitingQuota(j);
        verify(queue, never()).complete(any(), any());
    }

    @Test
    void variationsIgnoreTheQuota() throws Exception {
        BuildJob j = job("read_page", false);
        when(queue.claimNext()).thenReturn(j, (BuildJob) null);
        runner(step("read_page", "OUT", null)).tick();
        verify(queue, timeout(2000)).complete(j, "OUT");
        verify(quota, never()).hasQuota(anyLong());
    }

    @Test
    void quotaIsRenewedForWaitingAccounts() {
        when(queue.accountsWaitingForQuota()).thenReturn(List.of(3L, 4L));
        when(quota.hasQuota(3)).thenReturn(true);
        when(quota.hasQuota(4)).thenReturn(false);
        runner(step("read_page", "OUT", null)).resumeWaitingQuota();
        verify(queue).resumeWaitingQuota(3);
        verify(queue, never()).resumeWaitingQuota(4);
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
