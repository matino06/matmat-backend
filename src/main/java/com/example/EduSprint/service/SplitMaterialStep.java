package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.example.EduSprint.storage.MaterialStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

// Prvi korak čitanja: materijal se jednom skine sa S3 i rastavi na stranice. PDF stranica se renderira
// u PNG (na S3) i sprema joj se tekstni sloj; fotografija je jedna stranica. Za svaku novu stranicu
// dodaje se posao čitanja. Ponovljeni korak ne dodaje iste stranice dvaput.
@Component
public class SplitMaterialStep implements BuildStep {

    public static final String KIND = "split_material";
    static final float DPI = 150;

    private final NamedParameterJdbcTemplate jdbc;
    private final MaterialStorage storage;
    private final BuildJobQueue queue;
    private final ObjectMapper mapper = new ObjectMapper();

    public SplitMaterialStep(NamedParameterJdbcTemplate jdbc, MaterialStorage storage, BuildJobQueue queue) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.queue = queue;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) throws Exception {
        long materialId = mapper.readTree(job.input()).path("materialId").asLong();
        Map<String, Object> m = jdbc.queryForMap("SELECT s3_key, content_type FROM course_material WHERE material_id = :m",
                new MapSqlParameterSource("m", materialId));
        String key = (String) m.get("s3_key");
        String type = (String) m.get("content_type");
        if (!"application/pdf".equals(type)) {
            addPage(job, materialId, 1, key, null);
            return "1 stranica";
        }
        String pagesPrefix = key.substring(0, key.indexOf("materials/")) + "pages/" + materialId + "/";
        File file = storage.download(key);
        try (PDDocument pdf = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            PDFTextStripper text = new PDFTextStripper();
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                int pageNo = i + 1;
                if (pageExists(materialId, pageNo)) continue;
                BufferedImage image = renderer.renderImageWithDPI(i, DPI, ImageType.RGB);
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(image, "png", png);
                String imageKey = pagesPrefix + pageNo + ".png";
                storage.put(imageKey, png.toByteArray(), "image/png");
                text.setStartPage(pageNo);
                text.setEndPage(pageNo);
                addPage(job, materialId, pageNo, imageKey, text.getText(pdf).strip());
            }
            return pdf.getNumberOfPages() + " stranica";
        } finally {
            file.delete();
        }
    }

    private boolean pageExists(long materialId, int pageNo) {
        return !jdbc.queryForList("SELECT 1 FROM material_page WHERE material_id = :m AND page_no = :p",
                new MapSqlParameterSource("m", materialId).addValue("p", pageNo), Integer.class).isEmpty();
    }

    private void addPage(BuildJob job, long materialId, int pageNo, String imageKey, String textLayer) {
        List<Long> ids = jdbc.queryForList("""
                INSERT INTO material_page (material_id, page_no, image_key, text_layer) VALUES (:m, :p, :k, :t)
                ON CONFLICT (material_id, page_no) DO NOTHING RETURNING page_id""",
                new MapSqlParameterSource(Map.of("m", materialId, "p", pageNo, "k", imageKey))
                        .addValue("t", textLayer == null || textLayer.isBlank() ? null : textLayer), Long.class);
        if (!ids.isEmpty()) {
            queue.enqueue(job.courseId(), job.accountId(), ReadPageStep.KIND, "stranica " + pageNo + " materijala " + materialId,
                    BuildJobQueue.PRIORITY_BACKGROUND, true, "{\"pageId\":" + ids.get(0) + "}");
        }
    }
}
