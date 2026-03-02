package com.example.EduSprint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.core.exception.ResendException;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String RESEND_API_KEY;

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
}