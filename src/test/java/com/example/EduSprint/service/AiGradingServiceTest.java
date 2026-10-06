package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiGradingServiceTest {

    @Test
    void largeImageIsDownscaledToMaxSideAndEncodedAsJpeg() throws Exception {
        byte[] out = AiGradingService.normalizeImage(encode(new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB), "jpg"));
        // JPEG SOI marker
        assertEquals((byte) 0xFF, out[0]);
        assertEquals((byte) 0xD8, out[1]);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(out));
        assertEquals(1568, decoded.getWidth());
        assertEquals(1176, decoded.getHeight());
    }

    @Test
    void transparentPngIsFlattenedOntoWhite() throws Exception {
        BufferedImage transparent = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        byte[] out = AiGradingService.normalizeImage(encode(transparent, "png"));
        int rgb = ImageIO.read(new ByteArrayInputStream(out)).getRGB(50, 50) & 0xFFFFFF;
        assertTrue((rgb & 0xFF) > 240 && ((rgb >> 8) & 0xFF) > 240 && ((rgb >> 16) & 0xFF) > 240);
    }

    @Test
    void unreadableBytesThrow() {
        assertThrows(AiGradingService.ImageUnreadableException.class,
                () -> AiGradingService.normalizeImage(new byte[]{1, 2, 3, 4}));
    }

    private static byte[] encode(BufferedImage img, String format) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }
}
