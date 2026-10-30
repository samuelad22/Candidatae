package com.candidatae.integration.imap;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Utility for filtering job-application emails by envelope metadata and
 * recursively extracting plain text and HTML content from MIME messages.
 *
 * <p>Contains extensive, scraped domain and subject keyword definitions covering
 * Spanish and international job portals (LinkedIn, InfoJobs, Indeed, Tecnoempleo,
 * Manfred, Glassdoor, Ticjob, Domestika, Welcome to the Jungle, JobToday, etc.)
 * and corporate Applicant Tracking Systems (Workday, Greenhouse, Lever, SmartRecruiters,
 * Teamtailor, Personio, Workable, Ashby, BambooHR, Breezy HR, Taleo, Recruitee, Factorial).</p>
 */
public final class MimeMessageParser {

    /**
     * Known domains for job portals and corporate ATS platforms.
     */
    private static final List<String> KNOWN_JOB_PORTAL_DOMAINS = List.of(
            // Major Spanish & Global Job Boards
            "linkedin.com",
            "infojobs.net",
            "infojobs.com",
            "indeed.com",
            "indeedemail.com",
            "indeedapply.com",
            "tecnoempleo.com",
            "glassdoor.com",
            "glassdoor.es",
            "getmanfred.com",
            "manfred.com",
            "ticjob.es",
            "ticjob.co",
            "ticjob.com",
            "domestika.org",
            "domestika.com",
            "welcometothejungle.com",
            "wttj.co",
            "jobtoday.com",
            "jooble.org",
            "jooble.com",
            "talent.com",
            "neuvoo.com",
            "turijobs.com",
            "computrabajo.com",
            "computrabajo.es",
            "trabajos.com",
            "monster.com",
            "monster.es",
            "stepstone.com",
            "stepstone.de",
            "hays.es",
            "hays.com",
            "michaelpage.es",
            "michaelpage.com",
            "pagepersonnel.es",
            "randstad.es",
            "randstad.com",
            "adecco.es",
            "adecco.com",

            // Corporate Applicant Tracking Systems (ATS)
            "greenhouse.io",
            "ghhire.com",
            "greenhouse-mail.io",
            "lever.co",
            "hire.lever.co",
            "myworkday.com",
            "workday.com",
            "smartrecruiters.com",
            "smartrecruiters-mail.com",
            "teamtailor.com",
            "teamtailor-mail.com",
            "personio.de",
            "personio.com",
            "personio-mail.com",
            "personio.es",
            "workable.com",
            "workablemail.com",
            "ashbyhq.com",
            "bamboohr.com",
            "bamboohr-mail.com",
            "breezy.hr",
            "breezymail.com",
            "taleo.net",
            "recruitee.com",
            "jobvite.com",
            "jazzhr.com",
            "applytojob.com",
            "pinpointhq.com",
            "factorialhr.com",
            "factorial.co",
            "join.com",
            "zohorecruit.com"
    );

    /**
     * Subject keywords and patterns typical of job applications, confirmations,
     * status updates, interview scheduling, and rejections.
     */
    private static final List<String> JOB_SUBJECT_KEYWORDS = List.of(
            // Spanish - Candidacy confirmation & application
            "candidatura",
            "inscripción",
            "inscripcion",
            "postulación",
            "postulacion",
            "proceso de selección",
            "proceso de seleccion",
            "solicitud de empleo",
            "oferta de empleo",
            "oferta de trabajo",
            "solicitud enviada",
            "solicitud recibida",
            "hemos recibido tu solicitud",
            "hemos recibido tu candidatura",
            "hemos recibido tu cv",
            "has completado tu inscripción",
            "has completado tu inscripcion",
            "has enviado tu solicitud",
            "tu candidatura se ha enviado",
            "tu candidatura ha sido recibida",
            "tu candidatura ha sido enviada",
            "tu candidatura para",
            "tu candidatura a",
            "acuse de recibo",
            "gracias por inscribirte",
            "gracias por tu postulación",
            "gracias por tu postulacion",
            "gracias por tu candidatura",
            "gracias por tu interés",
            "gracias por tu interes",
            "tu cv ha sido",

            // Spanish - Review & In Progress
            "ha visto tu cv",
            "ha visto tu currículum",
            "ha visto tu curriculum",
            "ha leído tu cv",
            "ha leido tu cv",
            "tu candidatura ha cambiado",
            "estado de tu candidatura",
            "actualización de tu candidatura",
            "actualizacion de tu candidatura",
            "actualización sobre tu candidatura",
            "actualizacion sobre tu candidatura",
            "en proceso de selección",

            // Spanish - Interview & Assessments
            "entrevista",
            "entrevistas",
            "invitación a entrevista",
            "invitacion a entrevista",
            "te invitamos a una entrevista",
            "entrevista técnica",
            "entrevista tecnica",
            "entrevista telefónica",
            "entrevista telefonica",
            "entrevista por videollamada",
            "entrevista acordada",
            "entrevista confirmada",
            "entrevista agendada",
            "entrevista programada",
            "cita para entrevista",
            "cita de entrevista",
            "coordinar entrevista",
            "concertar entrevista",
            "concertar una entrevista",
            "reunión de selección",
            "reunion de seleccion",
            "reunión técnica",
            "reunion tecnica",
            "reunión con",
            "reunion con",
            "charla de selección",
            "charla de seleccion",
            "videollamada",
            "video call",
            "calendly",
            "cal.com",
            "prueba técnica",
            "prueba tecnica",
            "evaluación técnica",
            "evaluacion tecnica",
            "agendar entrevista",
            "coordinación de entrevista",
            "coordinacion de entrevista",
            "queremos conocerte",
            "nos gustaría conocerte",
            "nos gustaria conocerte",

            // Spanish - Offers & Rejections
            "propuesta de empleo",
            "propuesta laboral",
            "oferta formal",
            "carta de oferta",
            "propuesta salarial",
            "propuesta económica",
            "propuesta economica",
            "candidatura descartada",
            "no continuaremos con tu candidatura",
            "no continuará en el proceso",
            "no continuara en el proceso",
            "no continuar con tu candidatura",

            // English - Applications & Confirmations
            "application",
            "applied",
            "your application",
            "application received",
            "application submitted",
            "application confirmation",
            "thanks for applying",
            "thank you for applying",
            "thanks for your application",
            "we've received your application",
            "we have received your application",
            "you applied to",
            "submission confirmation",

            // English - Review, Interview, Offers, Rejections
            "application update",
            "update on your application",
            "status of your application",
            "interview",
            "interviews",
            "interview invitation",
            "invitation to interview",
            "schedule an interview",
            "schedule your interview",
            "scheduled an interview",
            "interview scheduled",
            "interview confirmed",
            "meeting scheduled",
            "screening call",
            "technical interview",
            "technical assessment",
            "coding challenge",
            "take-home test",
            "job offer",
            "offer letter",
            "formal offer",
            "offer of employment",
            "not moving forward",
            "moving forward with your application"
    );

    private MimeMessageParser() {
    }

    /**
     * Fast envelope check to decide whether a message is related to a job candidacy
     * before downloading its full MIME body.
     */
    public static boolean isJobRelatedEmail(String fromAddress, String subject) {
        String normalizedFrom = fromAddress != null ? fromAddress.toLowerCase(Locale.ROOT) : "";
        String normalizedSubject = subject != null ? subject.toLowerCase(Locale.ROOT) : "";

        boolean matchesKeywords = JOB_SUBJECT_KEYWORDS.stream()
                .anyMatch(normalizedSubject::contains);
        if (matchesKeywords) {
            return true;
        }

        return KNOWN_JOB_PORTAL_DOMAINS.stream()
                .anyMatch(normalizedFrom::contains);
    }

    /**
     * Extracts plain text and HTML bodies from a Jakarta Mail {@link Part},
     * skipping file attachments to save memory.
     */
    public static ParsedContent extractContent(Part part) throws MessagingException, IOException {
        StringBuilder plainText = new StringBuilder();
        StringBuilder htmlText = new StringBuilder();
        parsePartRecursive(part, plainText, htmlText, 0);
        return new ParsedContent(
                plainText.isEmpty() ? null : plainText.toString().trim(),
                htmlText.isEmpty() ? null : htmlText.toString().trim()
        );
    }

    private static void parsePartRecursive(Part part, StringBuilder plainText, StringBuilder htmlText, int depth)
            throws MessagingException, IOException {
        if (depth > 10 || part == null) {
            return;
        }

        String disposition = part.getDisposition();
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
            return;
        }

        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            if (content instanceof String text) {
                if (!plainText.isEmpty()) {
                    plainText.append("\n");
                }
                plainText.append(text);
            }
        } else if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            if (content instanceof String html) {
                if (!htmlText.isEmpty()) {
                    htmlText.append("\n");
                }
                htmlText.append(html);
            }
        } else if (part.isMimeType("multipart/*")) {
            Object content = part.getContent();
            if (content instanceof Multipart multipart) {
                int count = multipart.getCount();
                for (int i = 0; i < count; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    parsePartRecursive(bodyPart, plainText, htmlText, depth + 1);
                }
            }
        }
    }

    public record ParsedContent(String plainText, String htmlText) {
    }
}
