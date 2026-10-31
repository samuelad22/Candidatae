package com.candidatae.service;

import com.candidatae.dto.EmailSyncDto;
import com.candidatae.integration.imap.MimeMessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CandidacyExtractionService & MimeMessageParser - portal detection and status classification")
class CandidacyExtractionServiceTest {

    private CandidacyExtractionService service;

    @BeforeEach
    void setUp() {
        service = new CandidacyExtractionService();
    }

    @Test
    @DisplayName("isJobRelatedEmail() matches job platforms and application subjects, rejects spam")
    void isJobRelatedEmailDetection() {
        // Known platforms
        assertTrue(MimeMessageParser.isJobRelatedEmail("notificaciones@infojobs.net", "Aviso importante"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("alert@indeed.com", "Mensaje nuevo"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("jobs-noreply@linkedin.com", "Actualizaciones"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("team@getmanfred.com", "Hola"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("no-reply@greenhouse.io", "Notification"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("talent@welcometothejungle.com", "Status"));

        // Application subject keywords
        assertTrue(MimeMessageParser.isJobRelatedEmail("hr@randomcompany.com", "Tu candidatura a Desarrollador Java"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("reclutamiento@startup.io", "Has completado tu inscripción con éxito"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("jobs@tech.co", "Thank you for applying to Senior Engineer"));
        assertTrue(MimeMessageParser.isJobRelatedEmail("hiring@corp.com", "Invitación a entrevista técnica"));

        // Unrelated emails
        assertFalse(MimeMessageParser.isJobRelatedEmail("newsletter@supermercado.es", "Ofertas en alimentación"));
        assertFalse(MimeMessageParser.isJobRelatedEmail("seguridad@banco.com", "Tu extracto mensual"));
    }

    @Test
    @DisplayName("detectPlatform() detects all major job boards and ATS platforms")
    void detectPlatformRecognition() {
        assertEquals("INFOJOBS", service.detectPlatform("notificaciones@infojobs.net", "Inscripción"));
        assertEquals("LINKEDIN", service.detectPlatform("jobs-listings@linkedin.com", "Application sent"));
        assertEquals("INDEED", service.detectPlatform("messages-noreply@indeedemail.com", "You applied"));
        assertEquals("TECNOEMPLEO", service.detectPlatform("avisos@tecnoempleo.com", "Tu CV"));
        assertEquals("MANFRED", service.detectPlatform("team@getmanfred.com", "Proceso abierto"));
        assertEquals("GLASSDOOR", service.detectPlatform("noreply@glassdoor.es", "Alerta"));
        assertEquals("TICJOB", service.detectPlatform("candidatos@ticjob.es", "Oferta Java"));
        assertEquals("DOMESTIKA", service.detectPlatform("jobs@domestika.org", "Diseño UX"));
        assertEquals("WELCOME_TO_THE_JUNGLE", service.detectPlatform("no-reply@welcometothejungle.com", "Status"));
        assertEquals("GREENHOUSE", service.detectPlatform("no-reply@greenhouse.io", "Thanks for applying"));
        assertEquals("LEVER", service.detectPlatform("candidatos@hire.lever.co", "Update"));
        assertEquals("WORKDAY", service.detectPlatform("careers@myworkday.com", "Application Received"));
        assertEquals("TEAMTAILOR", service.detectPlatform("jobs@teamtailor-mail.com", "Interview"));
        assertEquals("PERSONIO", service.detectPlatform("bewerbung@personio-mail.com", "Bewerbung"));
        assertEquals("OTHER", service.detectPlatform("hr@directcompany.com", "Tu candidatura"));
    }

    @Test
    @DisplayName("detectCandidacyStatus() correctly classifies REJECTED, OFFER, INTERVIEW, IN_PROGRESS and APPLIED")
    void detectCandidacyStatusClassification() {
        // Rejections (Spanish & English)
        assertEquals("REJECTED", service.detectCandidacyStatus("Candidatura descartada", "Hemos decidido no continuar con tu candidatura.", null));
        assertEquals("REJECTED", service.detectCandidacyStatus("Actualización sobre tu candidatura", "Lamentamos comunicarte que en esta ocasión hemos seleccionado a otro perfil.", null));
        assertEquals("REJECTED", service.detectCandidacyStatus("Your application to Acme", "Unfortunately, we have decided to pursue other candidates.", null));
        assertEquals("REJECTED", service.detectCandidacyStatus("Update regarding your application", "After careful consideration, the position has been filled.", null));

        // Offers (Spanish & English)
        assertEquals("OFFER", service.detectCandidacyStatus("Oferta formal de empleo", "Nos complace hacerte una oferta laboral y propuesta salarial.", null));
        assertEquals("OFFER", service.detectCandidacyStatus("Job offer: Software Engineer", "We are pleased to offer you the position. Welcome to the team!", null));

        // Interviews (Spanish & English)
        assertEquals("INTERVIEW", service.detectCandidacyStatus("Invitación a entrevista técnica", "Queremos agendar una entrevista técnica contigo por videollamada.", null));
        assertEquals("INTERVIEW", service.detectCandidacyStatus("Interview with Tech Lead", "We'd like to schedule an interview with the team via Calendly.", null));
        assertEquals("INTERVIEW", service.detectCandidacyStatus("Technical assessment", "Please complete this coding challenge within 5 days.", null));

        // In Progress (Spanish & English)
        assertEquals("IN_PROGRESS", service.detectCandidacyStatus("Actualización de tu candidatura", "La empresa ha visto tu CV y tu perfil ha interesado.", null));
        assertEquals("IN_PROGRESS", service.detectCandidacyStatus("Application update", "Your application is being reviewed and moved to the next round.", null));

        // Default Applied
        assertEquals("APPLIED", service.detectCandidacyStatus("Inscripción confirmada", "Hemos recibido tu solicitud para Backend Developer.", null));
        assertEquals("APPLIED", service.detectCandidacyStatus("Your application was sent to Acme", "Thank you for applying.", null));
    }

    @Test
    @DisplayName("processEmails() increments created and updated counts appropriately")
    void processEmailsBatchSummary() {
        List<EmailSyncDto.RawEmailDto> batch = List.of(
                new EmailSyncDto.RawEmailDto(1L, "<1>", "notificaciones@infojobs.net", "Inscripción confirmada", Instant.now(), "Recibida", null),
                new EmailSyncDto.RawEmailDto(2L, "<2>", "team@getmanfred.com", "Invitación a entrevista técnica", Instant.now(), "Agendemos entrevista", null),
                new EmailSyncDto.RawEmailDto(3L, "<3>", "noreply@greenhouse.io", "Update on your application", Instant.now(), "Unfortunately not moving forward", null)
        );

        EmailSyncDto.ExtractionSummaryDto summary = service.processEmails(99L, batch);

        assertEquals(1, summary.candidaciesCreated()); // APPLIED
        assertEquals(2, summary.candidaciesUpdated()); // INTERVIEW + REJECTED
        assertEquals(3, summary.detectedEmails().size());
        assertEquals("INFOJOBS", summary.detectedEmails().get(0).detectedPlatform());
        assertEquals("APPLIED", summary.detectedEmails().get(0).detectedStatus());
        assertEquals("MANFRED", summary.detectedEmails().get(1).detectedPlatform());
        assertEquals("INTERVIEW", summary.detectedEmails().get(1).detectedStatus());
        assertEquals("GREENHOUSE", summary.detectedEmails().get(2).detectedPlatform());
        assertEquals("REJECTED", summary.detectedEmails().get(2).detectedStatus());
    }
}

