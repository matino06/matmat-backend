package com.example.EduSprint.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

// Word, PowerPoint i OpenDocument materijal pretvara u PDF preko Gotenberga (LibreOffice) koji radi kao zaseban
// servis na Railwayu, dostupan samo preko interne mreže. Servis spava kad ga nitko ne koristi; prvi zahtjev ga
// budi (izmjereno ~2,5 s), a može vratiti 502, pa se zahtjev ponavlja.
@Component
public class OfficeConverter {

    private final String url;
    private final long[] retryDelaysMs;
    private final String authorization;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    @Autowired
    public OfficeConverter(@Value("${gotenberg.url:http://gotenberg.railway.internal:3000}") String url,
                           @Value("${gotenberg.username:matmat}") String username,
                           @Value("${gotenberg.password:}") String password) {
        this(url, username, password, new long[]{2_000, 5_000});
    }

    OfficeConverter(String url, String username, String password, long[] retryDelaysMs) {
        this.retryDelaysMs = retryDelaysMs;
        this.url = url.replaceAll("/+$", "") + "/forms/libreoffice/convert";
        this.authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    // Vraća privremenu PDF datoteku; pozivatelj je briše. Ime treba pravi nastavak jer po njemu LibreOffice bira filtar.
    public File toPdf(File source, String extension) {
        String boundary = "----matmat" + UUID.randomUUID();
        String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"materijal." + extension
                + "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        for (int attempt = 0; ; attempt++) {
            File pdf = null;
            try {
                pdf = Files.createTempFile("own-course-", ".pdf").toFile();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofMinutes(2))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Authorization", authorization)
                        .POST(HttpRequest.BodyPublishers.concat(
                                HttpRequest.BodyPublishers.ofString(head),
                                HttpRequest.BodyPublishers.ofFile(source.toPath()),
                                HttpRequest.BodyPublishers.ofString(tail)))
                        .build();
                HttpResponse<java.nio.file.Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(pdf.toPath()));
                int status = response.statusCode();
                if (status == 200) return pdf;
                pdf.delete();
                // 4xx: Gotenberg ne može otvoriti datoteku (oštećena, s lozinkom, kriv format); ponavljanje ne pomaže.
                if (status >= 400 && status < 500) throw unreadable();
                if (attempt >= retryDelaysMs.length) throw unavailable();
            } catch (IOException e) {
                if (pdf != null) pdf.delete();
                if (attempt >= retryDelaysMs.length) throw unavailable();
            } catch (InterruptedException e) {
                if (pdf != null) pdf.delete();
                Thread.currentThread().interrupt();
                throw unavailable();
            }
            try {
                Thread.sleep(retryDelaysMs[attempt]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw unavailable();
            }
        }
    }

    static ResponseStatusException unreadable() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Datoteka se ne može pretvoriti u PDF. Je li ispravna i bez lozinke? Možeš je i sam spremiti kao PDF.");
    }

    static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Pretvorba u PDF trenutno ne radi. Pokušaj ponovno za minutu ili datoteku spremi kao PDF.");
    }
}
