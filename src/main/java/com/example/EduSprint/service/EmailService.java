package com.example.EduSprint.service;

import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.core.exception.ResendException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String RESEND_API_KEY;

    private final AccountService accountService;
    private final LearningObjectiveService learningObjectiveService;
    private final SolvedTaskService solvedTaskService;

    public EmailService(AccountService accountService, LearningObjectiveService learningObjectiveService, SolvedTaskService solvedTaskService) {
        this.accountService = accountService;
        this.learningObjectiveService = learningObjectiveService;
        this.solvedTaskService = solvedTaskService;
    }

    public void sendWelcomeEmail(String to) {
        if (to.equals("milicevic.matino@gmail.com")) {
            return;
        }

        Resend resend = new Resend(RESEND_API_KEY);

        CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                .from("MatMat <info@matmat.online>")
                .to(to)
                .subject("Dobrodošao na MatMat 🙂")
                .html("""
                    Bok,<br><br>
                    
                    dobrodošao na <strong>MatMat</strong>!<br><br>
                    
                    MatMat je osmišljen tako da ti kroz pametno ponavljanje i prilagođene zadatke pomogne da postupno i sigurno savladaš gradivo za maturu.<br><br>
                    
                    Za najbolji napredak preporučujem da:<br><br>
                    
                    • rješavaš zadatke redovito (i 10–15 minuta dnevno je dovoljno)<br>
                    • pratiš svoj dnevni cilj i niz dana<br>
                    • ne preskačeš dane u prvom tjednu — tada se algoritam najbrže prilagođava tebi<br><br>
                    
                    Ako budeš imao/la bilo kakvih pitanja, prijedloga ili problema, slobodno se javi.<br><br>
                    
                    Sretno s učenjem 😊<br><br>
                    
                    Lijep pozdrav
                """)
                .build();

        try {
            resend.emails().send(emailOptions);
        } catch (ResendException e) {
            System.err.println("Neuspješno slanje emaila: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 18 * * *", zone = "Europe/Zagreb")
    public void sendDailyReminders() {
        ZoneId zone = ZoneId.of("Europe/Zagreb");
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);

        // Start of day (00:00) and end of day (23:59:59.999999999) in the same zone
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        Instant yesterdayStart = yesterday.atStartOfDay(zone).toInstant();
        Instant yesterdayEnd = yesterday.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        // Fetch all accounts that have reminders enabled and no solved tasks today
        List<Account> accountsToRemind = accountService.getAccountForReminders(yesterdayStart, yesterdayEnd, startOfDay, endOfDay);

        for (Account account : accountsToRemind) {
            try {
                sendReminderEmail(account);
            } catch (Exception e) {
                System.err.println("Failed to send reminder to " + account.getEmail() + ": " + e.getMessage());
            }
        }
    }

//    @Scheduled(cron = "0 0 19 * * 4", zone = "Europe/Zagreb")
    public void sendWeeklyReEngagementEmails() {
        ZoneId zone = ZoneId.of("Europe/Zagreb");
        LocalDate today = LocalDate.now(zone);

        List<Account> accounts = accountService.getAllAccountsWithRemindersEnabled();

        for (Account account : accounts) {
            try {
                java.sql.Timestamp maxEndTime = solvedTaskService.getMaxEndTime(account.getAccountId());

                if (maxEndTime == null) {
                    long daysSinceReg = ChronoUnit.DAYS.between(account.getRegistrationDate(), today);
                    sendNeverStartedEmail(account, (int) daysSinceReg);
                } else {
                    LocalDate lastSessionDate = maxEndTime.toInstant().atZone(zone).toLocalDate();
                    long daysSince = ChronoUnit.DAYS.between(lastSessionDate, today);

                    if (daysSince <= 1) continue;

                    int napredak = learningObjectiveService.calculateExamProgress(account);

                    if (daysSince >= 15) {
                        int[] streaks = solvedTaskService.calculateStreaks(account.getAccountId(), account.getCurrentCourse().getCourseId());
                        sendLongPauseEmail(account, (int) daysSince, napredak, streaks[1]);
                    } else {
                        sendShortPauseEmail(account, (int) daysSince, napredak);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to send weekly email to " + account.getEmail() + ": " + e.getMessage());
            }
        }
    }

    private void sendNeverStartedEmail(Account account, int daysSinceRegistration) {
        String firstName = account.getName().split(" ")[0];
        String html = """
<!doctype html>
<html lang="hr">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>FIRSTNAME, MatMat te čeka — kad krećeš? 🎯</title>
    <style>
      body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
      table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-collapse: collapse; }
      img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }
      body { margin: 0; padding: 0; background-color: #f4f4f5; font-family: "Segoe UI", Arial, sans-serif; }
    </style>
  </head>
  <body style="margin: 0; padding: 0; background-color: #f4f4f5;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f5; padding: 32px 16px;">
      <tr>
        <td align="center">
          <table role="presentation" width="100%" style="max-width: 520px; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 4px 32px rgba(0,0,0,0.08);">
            <tr>
              <td style="background: #ff2056; padding: 10px 0; text-align: center;">
                <span style="font-size: 13px; font-weight: 800; color: #ffffff; letter-spacing: 2px; text-transform: uppercase;">MatMat · Priprema za maturu</span>
              </td>
            </tr>
            <tr>
              <td style="padding: 44px 44px 36px;">
                <p style="margin: 0 0 4px; font-size: 13px; color: #a1a1aa; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">Za tebe, FIRSTNAME</p>
                <p style="margin: 0 0 20px; font-size: 25px; font-weight: 900; color: #09090b; line-height: 1.25;">Registracija ✓ — zadaci čekaju.</p>
                <p style="margin: 0 0 32px; font-size: 15px; color: #52525b; line-height: 1.7;">
                  Prošlo je već <strong style="color: #09090b;">DAYS_SINCE_REG dana</strong> od registracije, ali još nisi otvorio/la ni jedan zadatak.<br /><br />
                  Nema veze — nije kasno, a ni teško za početi. MatMat te vodi korak po korak kroz gradivo za državnu maturu. Treba ti samo 5 minuta da vidiš gdje stoji tvoje znanje.
                </p>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 32px;">
                  <tr>
                    <td style="background: #09090b; border-radius: 16px; padding: 24px 28px;">
                      <p style="margin: 0 0 6px; font-size: 11px; font-weight: 800; color: #ff2056; letter-spacing: 2px; text-transform: uppercase;">Tvoj izazov za ovaj tjedan</p>
                      <p style="margin: 0 0 8px; font-size: 20px; font-weight: 900; color: #ffffff;">Jedan zadatak. To je sve. 🎯</p>
                      <p style="margin: 0; font-size: 13px; color: #a1a1aa; line-height: 1.55;">Jedan zadatak i odmah ćeš vidjeti gdje stoji tvoje znanje — i znat ćeš na čemu treba raditi.</p>
                    </td>
                  </tr>
                </table>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 12px;">
                  <tr>
                    <td align="center">
                      <a href="https://matmat.online/tasks" style="display: inline-block; background: #ff2056; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 900; padding: 16px 44px; border-radius: 999px;">Počni odmah →</a>
                    </td>
                  </tr>
                </table>
                <p style="margin: 0; text-align: center; font-size: 12px; color: #a1a1aa;">Treba ti samo 5 minuta za početak. 🕐</p>
              </td>
            </tr>
            <tr><td style="padding: 0 44px;"><div style="border-top: 1px solid #f4f4f5;"></div></td></tr>
            <tr>
              <td style="padding: 20px 44px 28px; text-align: center;">
                <p style="margin: 0 0 6px; font-size: 12px; color: #a1a1aa; line-height: 1.6;">Dobivaš ovaj podsjetnik jer si ga uključio/la u postavkama.</p>
                <p style="margin: 0; font-size: 12px; color: #a1a1aa;"><a href="https://matmat.online" style="color: #a1a1aa; text-decoration: underline;">Odjavi se</a></p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
"""
                .replace("FIRSTNAME", firstName)
                .replace("DAYS_SINCE_REG", String.valueOf(daysSinceRegistration));

        Resend resend = new Resend(RESEND_API_KEY);
        CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                .from("MatMat <info@matmat.online>")
                .to(account.getEmail())
                .subject(firstName + ", MatMat te čeka — kad krećeš? \uD83C\uDFAF")
                .html(html)
                .build();
        try {
            resend.emails().send(emailOptions);
        } catch (ResendException e) {
            throw new RuntimeException("Resend error", e);
        }
    }

    private void sendShortPauseEmail(Account account, int daysPause, int napredak) {
        String firstName = account.getName().split(" ")[0];
        int remaining = 100 - napredak;
        String html = """
<!doctype html>
<html lang="hr">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>FIRSTNAME, DAYS_PAUSE dana bez vježbe — matura se bliži ⏰</title>
    <style>
      body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
      table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-collapse: collapse; }
      img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }
      body { margin: 0; padding: 0; background-color: #f4f4f5; font-family: "Segoe UI", Arial, sans-serif; }
    </style>
  </head>
  <body style="margin: 0; padding: 0; background-color: #f4f4f5;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f5; padding: 32px 16px;">
      <tr>
        <td align="center">
          <table role="presentation" width="100%" style="max-width: 520px; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 4px 32px rgba(0,0,0,0.08);">
            <tr>
              <td style="background: #ff2056; padding: 10px 0; text-align: center;">
                <span style="font-size: 13px; font-weight: 800; color: #ffffff; letter-spacing: 2px; text-transform: uppercase;">MatMat · Priprema za maturu</span>
              </td>
            </tr>
            <tr>
              <td style="padding: 44px 44px 36px;">
                <p style="margin: 0 0 4px; font-size: 13px; color: #a1a1aa; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">Za tebe, FIRSTNAME</p>
                <p style="margin: 0 0 20px; font-size: 25px; font-weight: 900; color: #09090b; line-height: 1.25;">DAYS_PAUSE dana pauze.<br />Matura ne čeka.</p>
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin-bottom: 24px;">
                  <tr>
                    <td style="background: #fff7f9; border: 1px solid #ffe0e7; border-radius: 999px; padding: 8px 20px;">
                      <span style="font-size: 14px; font-weight: 800; color: #ff2056;">NAPREDAK% spremnosti</span>
                      <span style="font-size: 14px; color: #71717b;"> — nastavi graditi na tome</span>
                    </td>
                  </tr>
                </table>
                <p style="margin: 0 0 32px; font-size: 15px; color: #52525b; line-height: 1.7;">Tvoj napredak je sačuvan i čeka te točno tamo gdje si stalo/la. Svaki tjedan bez vježbe malo smanjuje sigurnost pred maturu — ali svaka sesija to vraća natrag.</p>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 32px;">
                  <tr>
                    <td style="background: #09090b; border-radius: 16px; padding: 24px 28px;">
                      <p style="margin: 0 0 6px; font-size: 11px; font-weight: 800; color: #ff2056; letter-spacing: 2px; text-transform: uppercase;">Tvoj izazov za ovaj tjedan</p>
                      <p style="margin: 0 0 8px; font-size: 20px; font-weight: 900; color: #ffffff;">5 zadataka — samo 20 minuta. 💪</p>
                      <p style="margin: 0; font-size: 13px; color: #a1a1aa; line-height: 1.55;">Dovoljno da se vratiš u ritam i osjećaš napredak. Ne treba više od toga.</p>
                    </td>
                  </tr>
                </table>
                <p style="margin: 0 0 8px; font-size: 13px; font-weight: 700; color: #09090b;">Tvoja trenutna spremnost</p>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 6px;">
                  <tr>
                    <td style="background: #f4f4f5; border-radius: 999px; height: 10px; overflow: hidden;">
                      <table role="presentation" width="NAPREDAK%" cellpadding="0" cellspacing="0">
                        <tr><td style="background: #ff2056; height: 10px; border-radius: 999px;"></td></tr>
                      </table>
                    </td>
                  </tr>
                </table>
                <p style="margin: 0 0 32px; font-size: 12px; color: #a1a1aa;">NAPREDAK% — još REMAINING% do potpune spremnosti 💪</p>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 12px;">
                  <tr>
                    <td align="center">
                      <a href="https://matmat.online/tasks" style="display: inline-block; background: #ff2056; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 900; padding: 16px 44px; border-radius: 999px;">Vrati se u ritam →</a>
                    </td>
                  </tr>
                </table>
                <p style="margin: 0; text-align: center; font-size: 12px; color: #a1a1aa;">Treba ti samo 5–10 minuta. 🕐</p>
              </td>
            </tr>
            <tr><td style="padding: 0 44px;"><div style="border-top: 1px solid #f4f4f5;"></div></td></tr>
            <tr>
              <td style="padding: 20px 44px 28px; text-align: center;">
                <p style="margin: 0 0 6px; font-size: 12px; color: #a1a1aa; line-height: 1.6;">Dobivaš ovaj podsjetnik jer si ga uključio/la u postavkama.</p>
                <p style="margin: 0; font-size: 12px; color: #a1a1aa;"><a href="https://matmat.online" style="color: #a1a1aa; text-decoration: underline;">Odjavi se</a></p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
"""
                .replace("FIRSTNAME", firstName)
                .replace("DAYS_PAUSE", String.valueOf(daysPause))
                .replace("NAPREDAK", String.valueOf(napredak))
                .replace("REMAINING", String.valueOf(remaining));

        Resend resend = new Resend(RESEND_API_KEY);
        CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                .from("MatMat <info@matmat.online>")
                .to(account.getEmail())
                .subject(firstName + ", " + daysPause + " dana bez vježbe — matura se bliži \u23F0")
                .html(html)
                .build();
        try {
            resend.emails().send(emailOptions);
        } catch (ResendException e) {
            throw new RuntimeException("Resend error", e);
        }
    }

    private void sendLongPauseEmail(Account account, int daysPause, int napredak, int longestStreak) {
        String firstName = account.getName().split(" ")[0];
        String html = """
<!doctype html>
<html lang="hr">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>FIRSTNAME, DAYS_PAUSE dana bez MatMata — al' nije kasno 🚀</title>
    <style>
      body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
      table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-collapse: collapse; }
      img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }
      body { margin: 0; padding: 0; background-color: #f4f4f5; font-family: "Segoe UI", Arial, sans-serif; }
    </style>
  </head>
  <body style="margin: 0; padding: 0; background-color: #f4f4f5;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f5; padding: 32px 16px;">
      <tr>
        <td align="center">
          <table role="presentation" width="100%" style="max-width: 520px; background: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 4px 32px rgba(0,0,0,0.08);">
            <tr>
              <td style="background: #ff2056; padding: 10px 0; text-align: center;">
                <span style="font-size: 13px; font-weight: 800; color: #ffffff; letter-spacing: 2px; text-transform: uppercase;">MatMat · Priprema za maturu</span>
              </td>
            </tr>
            <tr>
              <td style="padding: 44px 44px 36px;">
                <p style="margin: 0 0 4px; font-size: 13px; color: #a1a1aa; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">Za tebe, FIRSTNAME</p>
                <p style="margin: 0 0 20px; font-size: 25px; font-weight: 900; color: #09090b; line-height: 1.25;">Punih DAYS_PAUSE dana<br />bez zadatka.</p>
                <p style="margin: 0 0 28px; font-size: 15px; color: #52525b; line-height: 1.7;">
                  Imao/la si <strong style="color: #09090b;">NAPREDAK% spremnosti</strong> i niz od <strong style="color: #09090b;">LONGEST_STREAK dana</strong> zaredom. To je bio dobar temelj — i sav taj napredak i dalje čeka.<br /><br />
                  Matura se ne pomiče. Ali jedan tjedan može napraviti razliku ako kreneš danas.
                </p>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 32px;">
                  <tr>
                    <td width="48%" style="text-align: center; padding: 16px 8px; background: #fffbeb; border-radius: 12px; border: 1px solid #fde68a;">
                      <div style="font-size: 28px; font-weight: 900; color: #f59e0b;">LONGEST_STREAK</div>
                      <div style="font-size: 11px; font-weight: 700; color: #71717b; margin-top: 2px;">najduži niz 🏆</div>
                    </td>
                    <td width="4%"></td>
                    <td width="48%" style="text-align: center; padding: 16px 8px; background: #f0fdf4; border-radius: 12px; border: 1px solid #bbf7d0;">
                      <div style="font-size: 28px; font-weight: 900; color: #16a34a;">NAPREDAK%</div>
                      <div style="font-size: 11px; font-weight: 700; color: #71717b; margin-top: 2px;">spremnost 📈</div>
                    </td>
                  </tr>
                </table>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 32px;">
                  <tr>
                    <td style="background: #09090b; border-radius: 16px; padding: 24px 28px;">
                      <p style="margin: 0 0 6px; font-size: 11px; font-weight: 800; color: #ff2056; letter-spacing: 2px; text-transform: uppercase;">Tvoj izazov za ovaj tjedan</p>
                      <p style="margin: 0 0 8px; font-size: 20px; font-weight: 900; color: #ffffff;">Vrati se s jednom sesijom. 🔥</p>
                      <p style="margin: 0; font-size: 13px; color: #a1a1aa; line-height: 1.55;">Nije potrebno sve odjednom — samo se pojavi. Jedna sesija i odmah si opet u igri.</p>
                    </td>
                  </tr>
                </table>
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin-bottom: 12px;">
                  <tr>
                    <td align="center">
                      <a href="https://matmat.online/tasks" style="display: inline-block; background: #ff2056; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 900; padding: 16px 44px; border-radius: 999px;">Nastavi gdje si stalo/la →</a>
                    </td>
                  </tr>
                </table>
                <p style="margin: 0; text-align: center; font-size: 12px; color: #a1a1aa;">Treba ti samo 5–10 minuta. 🕐</p>
              </td>
            </tr>
            <tr><td style="padding: 0 44px;"><div style="border-top: 1px solid #f4f4f5;"></div></td></tr>
            <tr>
              <td style="padding: 20px 44px 28px; text-align: center;">
                <p style="margin: 0 0 6px; font-size: 12px; color: #a1a1aa; line-height: 1.6;">Dobivaš ovaj podsjetnik jer si ga uključio/la u postavkama.</p>
                <p style="margin: 0; font-size: 12px; color: #a1a1aa;"><a href="https://matmat.online" style="color: #a1a1aa; text-decoration: underline;">Odjavi se</a></p>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
"""
                .replace("FIRSTNAME", firstName)
                .replace("DAYS_PAUSE", String.valueOf(daysPause))
                .replace("NAPREDAK", String.valueOf(napredak))
                .replace("LONGEST_STREAK", String.valueOf(longestStreak));

        Resend resend = new Resend(RESEND_API_KEY);
        CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                .from("MatMat <info@matmat.online>")
                .to(account.getEmail())
                .subject(firstName + ", " + daysPause + " dana bez MatMata — al' nije kasno \uD83D\uDE80")
                .html(html)
                .build();
        try {
            resend.emails().send(emailOptions);
        } catch (ResendException e) {
            throw new RuntimeException("Resend error", e);
        }
    }

    public void sendReminderEmail(Account account) {
        Resend resend = new Resend(RESEND_API_KEY);

        int[] streaks = solvedTaskService.calculateStreaks(account.getAccountId(), account.getCurrentCourse().getCourseId());

        int currentStreak = streaks[0];
        int longestStreak = streaks[1];
        int progress = learningObjectiveService.calculateExamProgress(account);
        int remaining = 100 - progress;

        String htmlContent = String.format("""
        <!doctype html>
        <html lang="hr">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <meta http-equiv="X-UA-Compatible" content="IE=edge" />
            <title>Tvoji zadaci te čekaju! 🔥</title>
            <style>
              body, table, td, a { -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; }
              table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-collapse: collapse; }
              img { -ms-interpolation-mode: bicubic; border: 0; outline: none; text-decoration: none; }
              body { margin: 0; padding: 0; background-color: #f4f4f5; font-family: "Nunito", "Segoe UI", Arial, sans-serif; }
            </style>
          </head>
          <body style="margin: 0; padding: 0; background-color: #f4f4f5">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f5; padding: 32px 16px">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" style="max-width: 560px; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 24px rgba(0, 0, 0, 0.07);">
                    <!-- Header -->
                    <tr>
                      <td style="background: linear-gradient(135deg, #ff2056 0%%, #ff5078 100%%); padding: 36px 40px; text-align: center;">
                        <div style="font-size: 28px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px;">MatMat</div>
                        <div style="font-size: 13px; color: rgba(255, 255, 255, 0.75); margin-top: 4px; font-weight: 600;">Priprema za državnu maturu</div>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding: 36px 40px 24px;">
                        <p style="margin: 0 0 6px; font-size: 22px; font-weight: 800; color: #09090b;">Hej👋</p>
                        <p style="margin: 0 0 28px; font-size: 15px; color: #71717b; line-height: 1.6;">
                          Tvoja matura se bliži — svaki dan bez vježbe je propuštena šansa da postaneš sigurniji/a u gradivo.
                        </p>
                        <!-- Stats row -->
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 28px;">
                          <tr>
                            <td width="33%%" style="text-align: center; padding: 16px 8px; background: #fff7f9; border-radius: 12px; border: 1px solid #ffe0e7;">
                              <div style="font-size: 28px; font-weight: 800; color: #ff2056;">%d</div>
                              <div style="font-size: 11px; font-weight: 700; color: #71717b; margin-top: 2px;">dana niza 🔥</div>
                            </td>
                            <td width="4%%"></td>
                            <td width="33%%" style="text-align: center; padding: 16px 8px; background: #fffbeb; border-radius: 12px; border: 1px solid #fde68a;">
                              <div style="font-size: 28px; font-weight: 800; color: #f59e0b;">%d</div>
                              <div style="font-size: 11px; font-weight: 700; color: #71717b; margin-top: 2px;">najduži niz 🏆</div>
                            </td>
                            <td width="4%%"></td>
                            <td width="33%%" style="text-align: center; padding: 16px 8px; background: #f0fdf4; border-radius: 12px; border: 1px solid #bbf7d0;">
                              <div style="font-size: 28px; font-weight: 800; color: #16a34a;">%d%%</div>
                              <div style="font-size: 11px; font-weight: 700; color: #71717b; margin-top: 2px;">spremnost 📈</div>
                            </td>
                          </tr>
                        </table>
                        <!-- Progress bar -->
                        <p style="margin: 0 0 8px; font-size: 13px; font-weight: 700; color: #09090b;">Tvoja spremnost za maturu</p>
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 6px;">
                          <tr>
                            <td style="background: #f4f4f5; border-radius: 999px; height: 10px; overflow: hidden;">
                              <table role="presentation" width="%d%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="background: #ff2056; height: 10px; border-radius: 999px;"></td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                        <p style="margin: 0 0 28px; font-size: 12px; color: #71717b;">
                          %d%% — još %d%% do potpune spremnosti. Daj si šansu! 💪
                        </p>
                        <!-- Motivational message -->
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 28px;">
                          <tr>
                            <td style="background: #fff7f9; border-left: 4px solid #ff2056; border-radius: 0 12px 12px 0; padding: 16px 20px;">
                              <p style="margin: 0; font-size: 14px; font-weight: 700; color: #09090b;">🔥 %d dana zaredom! U ritmu si — nemoj ga prekinuti.</p>
                              <p style="margin: 8px 0 0; font-size: 13px; color: #71717b;">
                                Svaki novi dan čini zadatke lakšima i gradi sigurnost za maturu.
                              </p>
                            </td>
                          </tr>
                        </table>
                        <!-- CTA -->
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom: 12px;">
                          <tr>
                            <td align="center">
                              <a href="https://matmat.online/tasks" style="display: inline-block; background: #ff2056; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 800; padding: 14px 36px; border-radius: 999px;">
                                Riješi zadatke danas →
                              </a>
                            </td>
                          </tr>
                        </table>
                        <p style="margin: 0; text-align: center; font-size: 12px; color: #a1a1aa;">Treba ti samo 5–10 minuta. 🕐</p>
                      </td>
                    </tr>
                    <!-- Divider -->
                    <tr><td style="padding: 0 40px;"><div style="border-top: 1px solid #f4f4f5;"></div></td></tr>
                    <!-- Footer -->
                    <tr>
                      <td style="padding: 20px 40px 28px; text-align: center;">
                        <p style="margin: 0 0 6px; font-size: 12px; color: #a1a1aa;">
                          Poslali smo ti ovaj podsjetnik jer si ga uključila u postavkama.
                        </p>
                        <p style="margin: 0; font-size: 12px; color: #a1a1aa;">
                          <a href="https://matmat.online" style="color: #ff2056; text-decoration: none; font-weight: 700;">matmat.online</a>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """,
                currentStreak,
                longestStreak,
                progress,
                progress,
                progress,
                remaining,
                currentStreak
        );

        CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                .from("MatMat <info@matmat.online>")
                .to(account.getEmail())
                .subject("Ne prekidaj niz! Riješi zadatak danas \uD83C\uDFAF")
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(emailOptions);
        } catch (ResendException e) {
            // Log and rethrow as runtime exception to be caught in the loop
            throw new RuntimeException("Resend error", e);
        }
    }
}