package com.example.EduSprint.dto;

// Napredak izrade vlastitog predmeta za karticu na /moji-predmeti/[id].
// phase: citanje | plan | pregled; planStep: 0–5 (opseg, stavke, preduvjeti, lekcije, završetak; 5 = gotovo)
// state: radi | ceka_kvotu | neuspjelo | pauzirano | ceka_kreatora | prevelik
public record BuildProgressDTO(String courseStatus, String phase, String state, int done, int total,
                               int percent, int failed, int planStep) {
}
