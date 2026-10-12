package com.example.EduSprint.controller;

import com.example.EduSprint.dto.*;
import com.example.EduSprint.dto.OwnCourseDTO;
import com.example.EduSprint.dto.OwnCourseDeleteDTO;
import com.example.EduSprint.dto.QuotaUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.OwnCourseMaterialService;
import com.example.EduSprint.service.OwnCoursePlanEditService;
import com.example.EduSprint.service.OwnCourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Vlastiti predmeti kreatora. Sve rute traže račun s is_creator; tuđi predmet je 404.
@RestController
@RequestMapping("/own-courses")
public class OwnCourseController {

    private final AccountService accountService;
    private final OwnCourseService ownCourseService;
    private final OwnCourseMaterialService materials;
    private final OwnCoursePlanEditService planEdit;

    public OwnCourseController(AccountService accountService, OwnCourseService ownCourseService,
                               OwnCourseMaterialService materials, OwnCoursePlanEditService planEdit) {
        this.accountService = accountService;
        this.ownCourseService = ownCourseService;
        this.materials = materials;
        this.planEdit = planEdit;
    }

    // --- uređivanje plana (dok je nacrt; preimenovanje i nakon potvrde) ---

    @PatchMapping("/{courseId}/lessons/{objectiveId}")
    public ResponseEntity<Void> rename(Authentication a, @PathVariable Long courseId, @PathVariable Long objectiveId,
                                       @RequestBody java.util.Map<String, String> body) {
        planEdit.rename(account(a), courseId, objectiveId, body.get("name"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/lessons/{objectiveId}/move")
    public ResponseEntity<Void> move(Authentication a, @PathVariable Long courseId, @PathVariable Long objectiveId,
                                     @RequestBody java.util.Map<String, Integer> body) {
        planEdit.move(account(a), courseId, objectiveId, body.getOrDefault("direction", 1));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/lessons/{objectiveId}/merge-next")
    public ResponseEntity<Void> mergeNext(Authentication a, @PathVariable Long courseId, @PathVariable Long objectiveId) {
        planEdit.mergeNext(account(a), courseId, objectiveId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}/lessons/{objectiveId}")
    public java.util.Map<String, Object> deleteLesson(Authentication a, @PathVariable Long courseId, @PathVariable Long objectiveId) {
        return java.util.Map.of("dependents", planEdit.delete(account(a), courseId, objectiveId));
    }

    @PostMapping("/{courseId}/lessons")
    public java.util.Map<String, Object> addLesson(Authentication a, @PathVariable Long courseId,
                                                   @RequestBody java.util.Map<String, Object> body) {
        Object after = body.get("afterObjectiveId");
        long id = planEdit.add(account(a), courseId, after == null ? null : Long.valueOf(after.toString()),
                (String) body.get("name"));
        return java.util.Map.of("objectiveId", id);
    }

    @PostMapping("/{courseId}/lessons/{objectiveId}/prerequisites")
    public ResponseEntity<Void> addPrerequisite(Authentication a, @PathVariable Long courseId, @PathVariable Long objectiveId,
                                                @RequestBody java.util.Map<String, Long> body) {
        planEdit.addPrerequisite(account(a), courseId, objectiveId, body.get("prerequisiteId"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}/lessons/{objectiveId}/prerequisites/{prerequisiteId}")
    public ResponseEntity<Void> removePrerequisite(Authentication a, @PathVariable Long courseId, @PathVariable Long objectiveId,
                                                   @PathVariable Long prerequisiteId) {
        planEdit.removePrerequisite(account(a), courseId, objectiveId, prerequisiteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/assumed-knowledge")
    public java.util.List<java.util.Map<String, Object>> assumed(Authentication a, @PathVariable Long courseId) {
        return planEdit.assumedKnowledge(account(a), courseId);
    }

    @PostMapping("/{courseId}/assumed-knowledge")
    public ResponseEntity<Void> addAssumed(Authentication a, @PathVariable Long courseId, @RequestBody java.util.Map<String, String> body) {
        planEdit.addAssumed(account(a), courseId, body.get("text"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}/assumed-knowledge/{assumedId}")
    public ResponseEntity<Void> removeAssumed(Authentication a, @PathVariable Long courseId, @PathVariable Long assumedId) {
        planEdit.removeAssumed(account(a), courseId, assumedId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/assumed-knowledge/{assumedId}/not-known")
    public java.util.Map<String, Object> notKnown(Authentication a, @PathVariable Long courseId, @PathVariable Long assumedId) {
        return java.util.Map.of("objectiveId", planEdit.notKnown(account(a), courseId, assumedId));
    }

    @PostMapping("/{courseId}/warnings/{warningId}/ignore")
    public ResponseEntity<Void> ignoreWarning(Authentication a, @PathVariable Long courseId, @PathVariable Long warningId) {
        planEdit.ignoreWarning(account(a), courseId, warningId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/confirm")
    public void confirm(Authentication a, @PathVariable Long courseId, @RequestBody(required = false) java.util.Map<String, Boolean> body) {
        planEdit.confirm(account(a), courseId, body != null && Boolean.TRUE.equals(body.get("seriousAccepted")));
    }

    @GetMapping("/{courseId}/learning-mode")
    public java.util.Map<String, Object> learningMode(Authentication a, @PathVariable Long courseId) {
        return planEdit.learningMode(account(a), courseId);
    }

    @PostMapping("/{courseId}/learning-mode")
    public java.util.Map<String, Object> chooseLearningMode(Authentication a, @PathVariable Long courseId,
                                                            @RequestBody java.util.Map<String, String> body) {
        String date = body.get("examDate");
        java.time.LocalDate examDate;
        try {
            examDate = date == null || date.isBlank() ? null : java.time.LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Neispravan datum.");
        }
        planEdit.chooseLearningMode(account(a), courseId, body.get("mode"), examDate);
        return planEdit.learningMode(account(a), courseId);
    }

    @PostMapping("/{courseId}/instructions")
    public java.util.Map<String, Object> instruct(Authentication a, @PathVariable Long courseId,
                                                  @RequestBody java.util.Map<String, String> body) {
        return java.util.Map.of("jobId", planEdit.instruct(account(a), courseId, body.get("text")));
    }

    @GetMapping("/{courseId}/instructions/{jobId}")
    public java.util.Map<String, Object> instruction(Authentication a, @PathVariable Long courseId, @PathVariable Long jobId) {
        return planEdit.instruction(account(a), courseId, jobId);
    }

    @PostMapping("/{courseId}/instructions/{jobId}/accept")
    public java.util.Map<String, Object> accept(Authentication a, @PathVariable Long courseId, @PathVariable Long jobId) {
        return java.util.Map.of("changed", planEdit.accept(account(a), courseId, jobId));
    }

    @PostMapping("/{courseId}/warnings/{warningId}/add-lesson")
    public java.util.Map<String, Object> addLessonForWarning(Authentication a, @PathVariable Long courseId, @PathVariable Long warningId) {
        return java.util.Map.of("jobId", planEdit.instructForWarning(account(a), courseId, warningId));
    }

    @PostMapping("/{courseId}/recheck")
    public ResponseEntity<Void> recheck(Authentication a, @PathVariable Long courseId,
                                        @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        planEdit.recheck(account(a), courseId, body.get("changed"));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{courseId}")
    public OwnCourseDTO get(Authentication authentication, @PathVariable Long courseId) {
        return ownCourseService.get(account(authentication), courseId);
    }

    @GetMapping("/{courseId}/materials")
    public MaterialsSummaryDTO materials(Authentication authentication, @PathVariable Long courseId) {
        return materials.summary(account(authentication), courseId);
    }

    @PostMapping("/{courseId}/materials/upload-url")
    public MaterialUploadUrlDTO uploadUrl(Authentication authentication, @PathVariable Long courseId,
                                          @RequestBody MaterialUploadRequest body) {
        return materials.uploadUrl(account(authentication), courseId, body);
    }

    @PostMapping("/{courseId}/materials")
    public ResponseEntity<MaterialDTO> register(Authentication authentication, @PathVariable Long courseId,
                                                @RequestBody MaterialRegisterRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materials.register(account(authentication), courseId, body));
    }

    @PatchMapping("/{courseId}/materials/{materialId}")
    public ResponseEntity<Void> setAvailableOnExam(Authentication authentication, @PathVariable Long courseId,
                                                   @PathVariable Long materialId, @RequestBody java.util.Map<String, Boolean> body) {
        materials.setAvailableOnExam(account(authentication), courseId, materialId, Boolean.TRUE.equals(body.get("availableOnExam")));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}/materials/{materialId}")
    public ResponseEntity<Void> removeMaterial(Authentication authentication, @PathVariable Long courseId,
                                               @PathVariable Long materialId) {
        materials.remove(account(authentication), courseId, materialId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/build")
    public ResponseEntity<Void> startBuild(Authentication authentication, @PathVariable Long courseId) {
        materials.startBuild(account(authentication), courseId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{courseId}/build")
    public BuildProgressDTO progress(Authentication authentication, @PathVariable Long courseId) {
        return materials.progress(account(authentication), courseId);
    }

    @GetMapping("/{courseId}/plan")
    public java.util.List<PlanLessonDTO> plan(Authentication authentication, @PathVariable Long courseId) {
        return materials.plan(account(authentication), courseId);
    }

    // Upozorenja plana i radi li još provjera (frontend tada prikazuje „provjera…“).
    @GetMapping("/{courseId}/warnings")
    public java.util.Map<String, Object> warnings(Authentication authentication, @PathVariable Long courseId) {
        Account account = account(authentication);
        java.util.List<PlanWarningDTO> list = materials.warningList(account, courseId);
        return java.util.Map.of("checking", planEdit.checking(courseId), "warnings", list);
    }

    @PostMapping("/{courseId}/cancel")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable Long courseId) {
        ownCourseService.cancelBuild(account(authentication), courseId);
        return ResponseEntity.noContent().build();
    }

    private Account account(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return accountService.getAccount(principal.getEmail());
    }

    @GetMapping
    public List<OwnCourseDTO> list(Authentication authentication) {
        return ownCourseService.list(account(authentication));
    }

    @GetMapping("/quota")
    public QuotaUsageDTO quota(Authentication authentication) {
        return ownCourseService.quota(account(authentication));
    }

    @PostMapping("/{courseId}/retry")
    public ResponseEntity<Void> retry(Authentication authentication, @PathVariable Long courseId) {
        ownCourseService.retryFailed(account(authentication), courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<OwnCourseDTO> create(Authentication authentication, @RequestBody OwnCourseCreateDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownCourseService.create(account(authentication), body));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long courseId,
                                       @RequestBody OwnCourseDeleteDTO body) {
        ownCourseService.delete(account(authentication), courseId, body == null ? null : body.confirmName());
        return ResponseEntity.noContent().build();
    }

    // Poruke iz servisa („Datoteka može imati najviše 100 MB.“) frontend prikazuje kreatoru.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, String>> error(ResponseStatusException e) {
        String message = e.getReason() == null ? "" : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", message));
    }
}
