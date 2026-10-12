package com.example.EduSprint.controller;

import com.example.EduSprint.service.EmailUnsubscribeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Public (no login) unsubscribe link from reminder emails.
@RestController
@RequestMapping("/email/unsubscribe/{accountId}/{token}")
public class EmailController {

    private final EmailUnsubscribeService emailUnsubscribeService;

    public EmailController(EmailUnsubscribeService emailUnsubscribeService) {
        this.emailUnsubscribeService = emailUnsubscribeService;
    }

    // Opening the link only shows a button: mail scanners follow links with GET, and that must not unsubscribe anyone.
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmPage(@PathVariable Long accountId, @PathVariable String token) {
        if (!emailUnsubscribeService.isValid(accountId, token)) {
            return invalidLink();
        }
        return ResponseEntity.ok(page(
                "Odjava od podsjetnika",
                "Više nećeš dobivati podsjetnike za učenje na mail. Uvijek ih možeš ponovno uključiti u postavkama.",
                """
                <form method="post">
                  <button type="submit" style="background:#ff2056;color:#fff;border:0;border-radius:999px;padding:14px 32px;font-size:16px;font-weight:800;cursor:pointer;">Odjavi me</button>
                </form>
                """));
    }

    // Used by the button above and by the mail client's own unsubscribe button (RFC 8058 one-click).
    @PostMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@PathVariable Long accountId, @PathVariable String token) {
        if (!emailUnsubscribeService.unsubscribe(accountId, token)) {
            return invalidLink();
        }
        return ResponseEntity.ok(page(
                "Odjavljen/a si",
                "Više ti nećemo slati podsjetnike za učenje. Ako se predomisliš, uključi ih u postavkama na MatMatu.",
                """
                <a href="https://matmat.online/settings" style="color:#ff2056;font-weight:700;">Otvori postavke</a>
                """));
    }

    private static ResponseEntity<String> invalidLink() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(page(
                "Link ne vrijedi",
                "Ovaj link za odjavu nije ispravan. Podsjetnike možeš isključiti u postavkama na MatMatu.",
                """
                <a href="https://matmat.online/settings" style="color:#ff2056;font-weight:700;">Otvori postavke</a>
                """));
    }

    private static String page(String title, String text, String action) {
        return """
            <!doctype html>
            <html lang="hr">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>TITLE · MatMat</title>
              </head>
              <body style="margin:0;padding:48px 16px;background:#f4f4f5;font-family:'Segoe UI',Arial,sans-serif;">
                <div style="max-width:440px;margin:0 auto;background:#fff;border-radius:20px;padding:40px;text-align:center;box-shadow:0 4px 32px rgba(0,0,0,0.08);">
                  <p style="margin:0 0 12px;font-size:13px;font-weight:800;color:#ff2056;letter-spacing:2px;text-transform:uppercase;">MatMat</p>
                  <h1 style="margin:0 0 12px;font-size:24px;color:#09090b;">TITLE</h1>
                  <p style="margin:0 0 28px;font-size:15px;color:#52525b;line-height:1.6;">TEXT</p>
                  ACTION
                </div>
              </body>
            </html>
            """
                .replace("TITLE", title)
                .replace("TEXT", text)
                .replace("ACTION", action);
    }
}
