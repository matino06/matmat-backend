package com.example.EduSprint.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OfficeConverterTest {

    private HttpServer server;
    private final List<File> files = new ArrayList<>();

    // Lažni Gotenberg: vraća redom zadane statuse; uz 200 šalje „PDF“ i pamti što je primio.
    private OfficeConverter gotenberg(AtomicInteger calls, StringBuilder received, int... statuses) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/forms/libreoffice/convert", ex -> {
            int status = statuses[Math.min(calls.getAndIncrement(), statuses.length - 1)];
            received.setLength(0);
            received.append(ex.getRequestHeaders().getFirst("Authorization")).append('\n')
                    .append(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = status == 200 ? "%PDF-1.7 probni".getBytes(StandardCharsets.UTF_8) : new byte[0];
            ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        return new OfficeConverter("http://127.0.0.1:" + server.getAddress().getPort() + "/", "matmat", "tajna", new long[]{1, 1});
    }

    private File source() throws Exception {
        File f = Files.createTempFile("izvor-", ".docx").toFile();
        Files.writeString(f.toPath(), "sadržaj worda");
        files.add(f);
        return f;
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
        files.forEach(File::delete);
    }

    @Test
    void sendsTheFileWithItsExtensionAndReturnsThePdf() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        StringBuilder received = new StringBuilder();
        File pdf = gotenberg(calls, received, 200).toPdf(source(), "pptx");
        files.add(pdf);
        assertEquals("%PDF-1.7 probni", Files.readString(pdf.toPath()));
        assertTrue(received.toString().startsWith("Basic bWF0bWF0OnRham5h\n"), "basic auth matmat:tajna");
        assertTrue(received.toString().contains("filename=\"materijal.pptx\""));
        assertTrue(received.toString().contains("sadržaj worda"));
    }

    @Test
    void retriesWhileTheSleepingServiceWakesUp() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        File pdf = gotenberg(calls, new StringBuilder(), 502, 502, 200).toPdf(source(), "docx");
        files.add(pdf);
        assertEquals(3, calls.get());
    }

    @Test
    void givesUpAfterTheRetries() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> gotenberg(calls, new StringBuilder(), 502).toPdf(source(), "docx"));
        assertEquals(503, e.getStatusCode().value());
        assertEquals(3, calls.get());
    }

    @Test
    void aBrokenFileIsNotRetried() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> gotenberg(calls, new StringBuilder(), 400).toPdf(source(), "docx"));
        assertEquals(400, e.getStatusCode().value());
        assertEquals(1, calls.get());
    }
}
