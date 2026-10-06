package com.example.EduSprint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ImageNormalizer {

    private static final Logger log = LoggerFactory.getLogger(ImageNormalizer.class);
    private static final int MAX_IMAGE_SIDE = 1568;
    private static final float JPEG_QUALITY = 0.85f;

    private ImageNormalizer() {
    }

    /** Normalizira sliku i vraća je kao data URI spreman za OpenRouter image_url. */
    public static String toJpegDataUri(byte[] bytes) {
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(normalizeImage(bytes));
    }

    /**
     * Dekodira sliku po sadržaju (ne po ekstenziji), stavlja je na bijelu podlogu, smanjuje na
     * MAX_IMAGE_SIDE i ponovno kodira kao JPEG, tako da se MIME i sadržaj uvijek slažu.
     */
    public static byte[] normalizeImage(byte[] bytes) {
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new ImageUnreadableException(e);
        }
        if (src == null) {
            throw new ImageUnreadableException(null);
        }

        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, (double) MAX_IMAGE_SIDE / Math.max(w, h));
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));

        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tw, th);
            g.drawImage(src, 0, 0, tw, th, null);
        } finally {
            g.dispose();
        }

        byte[] jpeg = encodeJpeg(out);
        log.info("AI image: original {} B {}x{} -> sent {} B {}x{}", bytes.length, w, h, jpeg.length, tw, th);
        return jpeg;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode image as JPEG", e);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    public static class ImageUnreadableException extends RuntimeException {
        public ImageUnreadableException(Throwable cause) {
            super("Slika se ne može pročitati (podržani formati: PNG, JPG).", cause);
        }
    }

}
