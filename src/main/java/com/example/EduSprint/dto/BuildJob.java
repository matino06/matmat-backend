package com.example.EduSprint.dto;

// Jedan korak izrade vlastitog predmeta iz tablice build_job, kako ga vidi izvršitelj.
public record BuildJob(long jobId, long courseId, long accountId, String kind, String ref,
                       int priority, boolean countsQuota, int attempts, String input) {
}
