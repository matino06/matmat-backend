package com.example.EduSprint.controller;

import com.example.EduSprint.dto.*;
import com.example.EduSprint.dto.OwnCourseDTO;
import com.example.EduSprint.dto.OwnCourseDeleteDTO;
import com.example.EduSprint.dto.QuotaUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.OwnCourseMaterialService;
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

    public OwnCourseController(AccountService accountService, OwnCourseService ownCourseService,
                               OwnCourseMaterialService materials) {
        this.accountService = accountService;
        this.ownCourseService = ownCourseService;
        this.materials = materials;
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
