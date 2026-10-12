package com.example.EduSprint.service;

import com.example.EduSprint.dto.*;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import com.example.EduSprint.storage.MaterialStorage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Materijali vlastitog predmeta: upload preko presigned URL-a, provjera ograničenja,
// procjena kvote, pokretanje izrade i napredak čitanja.
@Service
public class OwnCourseMaterialService {

    static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 300L * 1024 * 1024;
    static final int MAX_PAGES = 300;
    static final Map<String, String> TYPES = Map.of("application/pdf", "pdf", "image/jpeg", "jpg", "image/png", "png");
    // Procjena troška izrade po stranici materijala (čitanje + plan + provjere), iz prototipa fizike:
    // ~4 $ za 252 stranice. Služi samo za upozorenje prije pokretanja.
    static final BigDecimal ESTIMATED_USD_PER_PAGE = new BigDecimal("0.016");

    public static final String STATUS_BUILDING = "izrada";

    private final CourseRepository courseRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final MaterialStorage storage;
    private final BuildJobQueue queue;
    private final BuildQuotaService quota;

    public OwnCourseMaterialService(CourseRepository courseRepository, NamedParameterJdbcTemplate jdbc,
                                    MaterialStorage storage, BuildJobQueue queue, BuildQuotaService quota) {
        this.courseRepository = courseRepository;
        this.jdbc = jdbc;
        this.storage = storage;
        this.queue = queue;
        this.quota = quota;
    }

    public MaterialUploadUrlDTO uploadUrl(Account account, long courseId, MaterialUploadRequest request) {
        Course course = editable(account, courseId);
        String type = request == null ? null : request.contentType();
        if (type == null || !TYPES.containsKey(type)) {
            throw bad("Dopušteni su samo PDF, JPG i PNG.");
        }
        long size = request.sizeBytes() == null ? 0 : request.sizeBytes();
        checkFileSize(size);
        if (totalBytes(courseId) + size > MAX_TOTAL_BYTES) {
            throw bad("Materijali predmeta mogu imati najviše 300 MB ukupno.");
        }
        String key = materialsPrefix(course) + UUID.randomUUID() + "." + TYPES.get(type);
        return new MaterialUploadUrlDTO(storage.presignPut(key, type, Duration.ofMinutes(15)), key);
    }

    @Transactional
    public MaterialDTO register(Account account, long courseId, MaterialRegisterRequest request) {
        Course course = editable(account, courseId);
        String key = request == null ? null : request.key();
        if (key == null || !key.startsWith(materialsPrefix(course)) || !storage.exists(key)) {
            throw bad("Datoteka nije uploadana.");
        }
        String extension = key.substring(key.lastIndexOf('.') + 1);
        String type = TYPES.entrySet().stream().filter(e -> e.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> bad("Dopušteni su samo PDF, JPG i PNG."));
        long size = storage.size(key);
        try {
            checkFileSize(size);
            if (totalBytes(courseId) + size > MAX_TOTAL_BYTES) {
                throw bad("Materijali predmeta mogu imati najviše 300 MB ukupno.");
            }
            int pages = "application/pdf".equals(type) ? pdfPages(key) : 1;
            int total = totalPages(courseId) + pages;
            if (total > MAX_PAGES) {
                throw bad(tooManyPages(pages, total));
            }
            String name = request.originalName() == null || request.originalName().isBlank()
                    ? "materijal." + extension : request.originalName().strip();
            if (name.length() > 300) name = name.substring(0, 300);
            boolean onExam = Boolean.TRUE.equals(request.availableOnExam());
            Long id = jdbc.queryForObject("""
                    INSERT INTO course_material (course_id, s3_key, original_name, content_type, size_bytes, page_count, available_on_exam)
                    VALUES (:c, :k, :n, :t, :s, :p, :e) RETURNING material_id""",
                    new MapSqlParameterSource(Map.of("c", courseId, "k", key, "n", name, "t", type, "s", size,
                            "p", pages, "e", onExam)), Long.class);
            return new MaterialDTO(id, name, type, size, pages, onExam);
        } catch (ResponseStatusException e) {
            storage.delete(key); // odbijeni upload ne ostaje na S3
            throw e;
        }
    }

    public MaterialsSummaryDTO summary(Account account, long courseId) {
        own(account, courseId);
        List<MaterialDTO> materials = jdbc.query("""
                SELECT material_id, original_name, content_type, size_bytes, page_count, available_on_exam
                FROM course_material WHERE course_id = :c ORDER BY material_id""",
                new MapSqlParameterSource("c", courseId),
                (rs, i) -> new MaterialDTO(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                        rs.getInt(5), rs.getBoolean(6)));
        int pages = materials.stream().mapToInt(MaterialDTO::pageCount).sum();
        long bytes = materials.stream().mapToLong(MaterialDTO::sizeBytes).sum();
        int used = quota.usage(account).usedPercent();
        return new MaterialsSummaryDTO(materials, pages, MAX_PAGES, bytes, MAX_TOTAL_BYTES, MAX_FILE_BYTES,
                estimatedPercent(pages, account.getMonthlyQuotaUsd()), Math.max(0, 100 - used));
    }

    @Transactional
    public void remove(Account account, long courseId, long materialId) {
        editable(account, courseId);
        String key = materialKey(courseId, materialId);
        jdbc.update("DELETE FROM course_material WHERE material_id = :m", new MapSqlParameterSource("m", materialId));
        storage.delete(key);
    }

    public void setAvailableOnExam(Account account, long courseId, long materialId, boolean onExam) {
        editable(account, courseId);
        materialKey(courseId, materialId);
        jdbc.update("UPDATE course_material SET available_on_exam = :e WHERE material_id = :m",
                new MapSqlParameterSource("m", materialId).addValue("e", onExam));
    }

    // Pokreće izradu: za svaki materijal posao koji ga razdijeli na stranice, a on dodaje posao čitanja po stranici.
    @Transactional
    public void startBuild(Account account, long courseId) {
        Course course = editable(account, courseId);
        List<Long> materials = jdbc.queryForList("SELECT material_id FROM course_material WHERE course_id = :c ORDER BY material_id",
                new MapSqlParameterSource("c", courseId), Long.class);
        if (materials.isEmpty()) {
            throw bad("Dodaj barem jedan materijal.");
        }
        course.setStatus(STATUS_BUILDING);
        courseRepository.save(course);
        for (Long materialId : materials) {
            queue.enqueue(courseId, account.getAccountId(), SplitMaterialStep.KIND, "materijal " + materialId,
                    BuildJobQueue.PRIORITY_BACKGROUND, true, "{\"materialId\":" + materialId + "}");
        }
    }

    public BuildProgressDTO progress(Account account, long courseId) {
        Course course = own(account, courseId);
        Map<String, Integer> counts = new java.util.HashMap<>();
        jdbc.query("SELECT kind, status, count(*) FROM build_job WHERE course_id = :c GROUP BY kind, status",
                new MapSqlParameterSource("c", courseId),
                rs -> { counts.put(rs.getString(1) + "/" + rs.getString(2), rs.getInt(3)); });
        int total = totalPages(courseId);
        int done = counts.getOrDefault(ReadPageStep.KIND + "/" + BuildJobQueue.DONE, 0);
        String state = readingState(counts, done, total);
        int percent = total == 0 ? 0 : (int) Math.floor(25.0 * done / total);
        int failed = counts.entrySet().stream().filter(e -> e.getKey().endsWith("/" + BuildJobQueue.FAILED))
                .mapToInt(Map.Entry::getValue).sum();
        return new BuildProgressDTO(course.getStatus(), "citanje", state, done, total, percent, failed);
    }

    static String readingState(Map<String, Integer> counts, int done, int total) {
        if (hasStatus(counts, BuildJobQueue.FAILED)) return "neuspjelo";
        if (hasStatus(counts, BuildJobQueue.WAITING_QUOTA)) return "ceka_kvotu";
        if (hasStatus(counts, BuildJobQueue.PAUSED)) return "pauzirano";
        if (total > 0 && done >= total) return "procitano";
        return "radi";
    }

    private static boolean hasStatus(Map<String, Integer> counts, String status) {
        return counts.entrySet().stream().anyMatch(e -> e.getKey().endsWith("/" + status) && e.getValue() > 0);
    }

    static int estimatedPercent(int pages, BigDecimal monthlyQuota) {
        if (monthlyQuota == null || monthlyQuota.signum() <= 0) return 100;
        BigDecimal cost = ESTIMATED_USD_PER_PAGE.multiply(BigDecimal.valueOf(pages));
        return cost.multiply(BigDecimal.valueOf(100)).divide(monthlyQuota, 0, RoundingMode.UP).intValue();
    }

    static String tooManyPages(int pages, int total) {
        return "Ovaj materijal ima " + pages + " str., pa bi predmet imao " + total
                + " stranica; dopušteno je najviše " + MAX_PAGES + " (" + (total - MAX_PAGES) + " previše).";
    }

    static void checkFileSize(long size) {
        if (size <= 0) throw bad("Datoteka je prazna.");
        if (size > MAX_FILE_BYTES) throw bad("Datoteka može imati najviše 100 MB.");
    }

    private int pdfPages(String key) {
        File file = null;
        try {
            file = storage.download(key);
            try (PDDocument pdf = Loader.loadPDF(file)) {
                return pdf.getNumberOfPages();
            }
        } catch (Exception e) {
            throw bad("PDF se ne može otvoriti. Je li datoteka ispravna i bez lozinke?");
        } finally {
            if (file != null) file.delete();
        }
    }

    private int totalPages(long courseId) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(SUM(page_count), 0) FROM course_material WHERE course_id = :c",
                new MapSqlParameterSource("c", courseId), Integer.class);
        return n == null ? 0 : n;
    }

    private long totalBytes(long courseId) {
        Long n = jdbc.queryForObject("SELECT COALESCE(SUM(size_bytes), 0) FROM course_material WHERE course_id = :c",
                new MapSqlParameterSource("c", courseId), Long.class);
        return n == null ? 0 : n;
    }

    private String materialKey(long courseId, long materialId) {
        List<String> keys = jdbc.queryForList("SELECT s3_key FROM course_material WHERE material_id = :m AND course_id = :c",
                new MapSqlParameterSource("m", materialId).addValue("c", courseId), String.class);
        if (keys.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return keys.get(0);
    }

    private static String materialsPrefix(Course course) {
        return MaterialStorage.coursePrefix(course.getOwnerAccountId(), course.getCourseId()) + "materials/";
    }

    private Course own(Account account, long courseId) {
        OwnCourseService.requireCreator(account);
        return courseRepository.findCourseByCourseId(courseId)
                .filter(c -> c.isOwnedBy(account.getAccountId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // Materijali se mijenjaju samo dok izrada nije pokrenuta.
    private Course editable(Account account, long courseId) {
        Course course = own(account, courseId);
        if (!OwnCourseService.STATUS_NEW.equals(course.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Izrada je već pokrenuta.");
        }
        return course;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
