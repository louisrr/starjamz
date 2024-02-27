package com.play.stream.Starjams.PaymentService.services;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    public void sendEmail(String toEmail, String subject, String content) throws Exception {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        Mail mail = new Mail(new Email("your_from_email@example.com"), subject, new Email(toEmail), new Content("text/plain", content));

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);
        // Optionally, log the response status and body, or handle errors as needed
        System.out.println(response.getStatusCode());
        System.out.println(response.getBody());
    }
}
