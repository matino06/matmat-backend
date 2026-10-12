package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;

// Jedna vrsta koraka izrade (čitanje stranice, stavke ishoda, varijacija …). Izvršitelj bira
// korak po build_job.kind; korak vraća izlaz koji se sprema u build_job.output i može dodati nove poslove.
public interface BuildStep {

    String kind();

    String run(BuildJob job) throws Exception;

    // Nakon što je posao označen gotovim: ovdje korak pokreće iduću fazu kad je cijela trenutna faza gotova.
    default void afterComplete(BuildJob job) {
    }
}
