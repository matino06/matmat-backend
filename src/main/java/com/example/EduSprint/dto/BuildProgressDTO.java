package com.example.EduSprint.dto;

// Napredak izrade vlastitog predmeta za karticu na /moji-predmeti/[id].
// state: radi | ceka_kvotu | neuspjelo | pauzirano | procitano
public record BuildProgressDTO(String courseStatus, String phase, String state, int done, int total,
                               int percent, int failed) {
}
