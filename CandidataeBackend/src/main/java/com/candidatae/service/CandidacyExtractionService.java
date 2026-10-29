package com.candidatae.service;

import com.candidatae.dto.EmailSyncDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic service that analyzes raw job-related emails to identify the
 * source job portal or ATS and classifies the candidacy status transition.
 *
 * <p>Tuned with extensive keywords and patterns from major Spanish and international
 * job platforms (LinkedIn, InfoJobs, Indeed, Tecnoempleo, Manfred, Glassdoor, Ticjob,
 * Domestika, Welcome to the Jungle, JobToday, etc.) and Applicant Tracking Systems
 * (Greenhouse, Lever, Workday, SmartRecruiters, Teamtailor, Personio, Workable, Ashby).</p>
 */
@Service
@Slf4j
public class CandidacyExtractionService {

    @Autowired(required = false)
    private CandidaturaService candidaturaService;

    private static final ZoneId ZONE_MADRID = ZoneId.of("Europe/Madrid");

    /**
     * Keywords indicating that an application was rejected or closed.
     */
    private static final List<String> REJECTED_KEYWORDS = List.of(
            // Spanish
            "descartad",
            "no continuar",
            "no continuaremos",
            "no continuará",
            "no continuara",
            "no ha sido seleccionad",
            "no has sido seleccionad",
            "no seguirá en el proceso",
            "no seguiras en el proceso",
            "no seguirá adelante",
            "no seguir adelante",
            "otro candidato",
            "otros candidatos",
            "otro perfil",
            "otros perfiles",
            "lamentamos informarte",
            "lamentamos comunicarte",
            "sintiendo mucho",
            "el proceso de selección ha finalizado",
            "el proceso de seleccion ha finalizado",
            "la vacante ha sido cubierta",
            "el puesto ha sido cubierto",
            "decidido avanzar con otros",
            "avanzar con otra candidatura",
            "en esta ocasión no",
            "en esta ocasion no",
            "no encaja con el perfil",
            "rechazad",
            "candidatura no seleccionada",
            "candidatura desestimada",
            "postulación no seleccionada",
            "postulacion no seleccionada",
            "proceso cerrado",
            "candidatura cerrada",
            "proceso finalizado",
            "hemos cubierto la vacante",
            "hemos cubierto el puesto",
            "decidido optar por otro",
            "decidido optar por otros",
            "no podemos ofrecerte",
            "no serás considerado",
            "no seras considerado",

            // English
            "unfortunately",
            "not moving forward",
            "decided not to move forward",
            "will not be moving forward",
            "pursuing other candidates",
            "decided to pursue other candidates",
            "moved forward with another candidate",
            "after careful consideration",
            "we regret to inform you",
            "your application was not successful",
            "position has been filled",
            "role has been filled",
            "we have filled the position",
            "not selected for this role",
            "unsuccessful application",
            "application has been declined",
            "we will not be proceeding",
            "unable to offer you",
            "decided to proceed with another"
    );

    /**
     * Keywords indicating that the candidate received a job or salary offer.
     */
    private static final List<String> OFFER_KEYWORDS = List.of(
            // Spanish
            "oferta formal de empleo",
            "oferta formal",
            "propuesta de empleo",
            "propuesta laboral",
            "carta de oferta",
            "propuesta salarial",
            "propuesta económica",
            "propuesta economica",
            "nos complace hacerte una oferta",
            "nos complace ofrecerte",
            "queremos que te unas al equipo",
            "bienvenido a bordo",
            "bienvenido/a a bordo",
            "comunicación oficial de contratación",
            "documentación de contratación",
            "contrato de trabajo",
            "oferta de trabajo",
            "oferta recibida",

            // English
            "job offer",
            "offer letter",
            "formal offer",
            "offer of employment",
            "employment offer",
            "we are pleased to offer you",
            "delighted to offer you",
            "thrilled to offer you",
            "welcome to the team",
            "extend an offer"
    );

    /**
     * Keywords indicating that the candidate has been invited to an interview,
     * screening call, or technical challenge.
     */
    private static final List<String> INTERVIEW_KEYWORDS = List.of(
            // Spanish
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
            "entrevista con",
            "entrevista inicial",
            "entrevista final",
            "entrevista de selección",
            "entrevista de seleccion",
            "entrevista acordada",
            "entrevista confirmada",
            "entrevista agendada",
            "entrevista programada",
            "cita para entrevista",
            "cita de entrevista",
            "agendar entrevista",
            "agendar una entrevista",
            "coordinación de entrevista",
            "coordinacion de entrevista",
            "coordinar entrevista",
            "coordinar una entrevista",
            "concertar entrevista",
            "concertar una entrevista",
            "concertado una entrevista",
            "evaluación técnica",
            "evaluacion tecnica",
            "prueba técnica",
            "prueba tecnica",
            "prueba de selección",
            "prueba de seleccion",
            "reunión de selección",
            "reunion de seleccion",
            "reunión técnica",
            "reunion tecnica",
            "reunión con",
            "reunion con",
            "charla de selección",
            "charla de seleccion",
            "llamada de presentación",
            "llamada de presentacion",
            "videollamada",
            "video call",
            "hablar sobre tu candidatura",
            "hablar sobre tu perfil",
            "avanzar a la siguiente fase",
            "avanzar a la siguiente etapa",
            "siguiente paso del proceso",
            "siguiente fase del proceso",
            "technical test",
            "coding challenge",
            "take-home test",
            "take-home challenge",
            "queremos conocerte",
            "nos gustaría conocerte",
            "nos gustaria conocerte",
            "agendar una llamada",
            "agendar llamada",
            "agendar videollamada",
            "agendar una videollamada",
            "agendar una reunión",
            "agendar reunion",
            "calendly.com",
            "calendly",
            "cal.com",
            "google meet",
            "teams.microsoft.com",
            "zoom.us",
            "conoce al equipo",

            // English
            "interview",
            "interviews",
            "interview invitation",
            "invitation to interview",
            "schedule an interview",
            "schedule your interview",
            "scheduled an interview",
            "interview scheduled",
            "interview confirmed",
            "interview with",
            "technical interview",
            "technical assessment",
            "first round interview",
            "second round interview",
            "final interview",
            "phone screen",
            "screening call",
            "video interview",
            "meet the team",
            "next round of interviews",
            "next stage of the process",
            "next step in the process",
            "we'd like to invite you"
    );

    /**
     * Keywords indicating progress or profile reviews on the application.
     */
    private static final List<String> IN_PROGRESS_KEYWORDS = List.of(
            // Spanish
            "ha visto tu cv",
            "ha visto tu currículum",
            "ha visto tu curriculum",
            "ha leído tu cv",
            "ha leido tu cv",
            "revisando tu candidatura",
            "revisando tu cv",
            "estamos evaluando tu perfil",
            "tu perfil ha interesado",
            "has avanzado en el proceso",
            "avanzado en el proceso",
            "tu candidatura ha cambiado de estado",
            "actualización de tu candidatura",
            "actualizacion de tu candidatura",
            "actualización sobre tu candidatura",
            "actualizacion sobre tu candidatura",
            "en proceso de selección",
            "en proceso",
            "evaluando tu perfil",
            "candidatura en proceso",

            // English
            "viewed your cv",
            "viewed your application",
            "viewed your profile",
            "is being reviewed",
            "reviewing your application",
            "under review",
            "application update",
            "update on your application",
            "status of your application",
            "moved to the next round",
            "your profile has been forwarded"
    );

    /**
     * Keywords indicating that an application was submitted or confirmed.
     */
    private static final List<String> APPLIED_KEYWORDS = List.of(
            // Spanish
            "hemos recibido tu candidatura",
            "hemos recibido tu postulación",
            "hemos recibido tu postulacion",
            "hemos recibido tu solicitud",
            "hemos recibido tu cv",
            "tu candidatura se ha enviado",
            "tu candidatura ha sido recibida",
            "tu candidatura ha sido enviada",
            "tu solicitud ha sido enviada",
            "tu solicitud ha sido recibida",
            "confirmación de tu candidatura",
            "confirmacion de tu candidatura",
            "confirmación de tu solicitud",
            "confirmacion de tu solicitud",
            "confirmación de tu postulación",
            "confirmacion de tu postulacion",
            "has completado tu inscripción",
            "has completado tu inscripcion",
            "has enviado tu solicitud",
            "inscripción confirmada",
            "inscripcion confirmada",
            "inscripción completada",
            "inscripcion completada",
            "solicitud completada",
            "solicitud enviada con éxito",
            "solicitud enviada con exito",
            "gracias por inscribirte",
            "gracias por tu postulación",
            "gracias por tu postulacion",
            "gracias por tu candidatura",
            "gracias por tu solicitud",
            "gracias por postularte",
            "acuse de recibo",
            "recepción de tu currículum",
            "recepcion de tu curriculum",
            "te has inscrito",
            "te has postulado",
            "recibida tu candidatura",
            "recibida tu solicitud",
            "postulación enviada",
            "postulacion enviada",
            "candidatura enviada",
            "solicitud en indeed",
            "solicitud vía indeed",
            "solicitud via indeed",
            "solicitud en linkedin",
            "se ha enviado tu solicitud",
            "se ha enviado su solicitud",
            "se ha enviado la solicitud",
            "se envió tu solicitud",
            "se envio tu solicitud",
            "tu solicitud para el puesto",
            "su solicitud para el puesto",
            "solicitud para el puesto",
            "solicitud en infojobs",
            "solicitud en tecnoempleo",
            "solicitud enviada",
            "solicitud recibida",
            "solicitud registrada",
            "solicitud tramitada",
            "candidatura recibida",
            "candidatura registrada",
            "has solicitado el puesto",
            "has solicitado el empleo",
            "has solicitado la vacante",
            "inscripción realizada",
            "inscripcion realizada",
            "postulación registrada",
            "postulacion registrada",
            "recepción de candidatura",
            "recepcion de candidatura",
            "recepción de solicitud",
            "recepcion de solicitud",
            "tu candidatura ha sido registrada",
            "currículum ha sido enviado",
            "curriculum ha sido enviado",
            "cv ha sido enviado",
            "currículum enviado",
            "curriculum enviado",
            "cv enviado",
            "currículum recibido",
            "curriculum recibido",
            "cv recibido",
            "has aplicado a",
            "has postulado a",
            "applied to",
            "documents on file",

            // English
            "thank you for applying",
            "thanks for applying",
            "thank you for your application",
            "thanks for your application",
            "we've received your application",
            "we have received your application",
            "we received your application",
            "your application has been received",
            "your application was received",
            "your application has been submitted",
            "your application was submitted",
            "your application was sent",
            "your application has been sent",
            "your application was submitted to",
            "application confirmation",
            "application submitted",
            "application received",
            "successfully applied",
            "you applied to",
            "submission confirmation",
            "confirming your application",
            "received your resume",
            "received your cv",
            "application was successfully submitted"
    );

    /**
     * Keywords and subject phrases for marketing digests, job alerts, and social noise.
     */
    private static final List<String> NEWSLETTER_AND_ALERT_KEYWORDS = List.of(
            // Spanish
            "alerta de empleo",
            "alertas de empleo",
            "alerta de trabajo",
            "alertas de trabajo",
            "empleos recomendados",
            "empleos para ti",
            "ofertas recomendadas",
            "ofertas que coinciden",
            "nuevas vacantes",
            "resumen semanal",
            "boletín de empleo",
            "boletin de empleo",
            "oportunidades de empleo para ti",
            "nuevas ofertas de trabajo",
            "nuevas ofertas de empleo",
            "te pueden interesar",
            "que te pueden gustar",
            "sugerencias de empleo",
            "resumen de ofertas",
            "te felicitó por",
            "te felicito por",
            "quiere conectar contigo",
            "invitación para conectar",
            "invitacion para conectar",
            "nueva conexión",
            "nueva conexion",
            "ha publicado un nuevo",
            "compartió una publicación",
            "compartio una publicacion",

            // English
            "job alert",
            "job alerts",
            "recommended jobs",
            "jobs recommended for you",
            "jobs for you",
            "jobs you may be interested in",
            "jobs that match your profile",
            "top job picks",
            "weekly job digest",
            "daily job alert",
            "new job openings for you",
            "job opportunities matching",
            "newsletter",
            "wants to connect",
            "invitation to connect",
            "congratulate",
            "viewed your post",
            "shared a post",
            "new connection",

            // Billing, E-commerce, Purchases, and Shipping
            "invoice",
            "factura",
            "receipt",
            "recibo de pago",
            "billing statement",
            "pedido",
            "order confirmation",
            "tu compra",
            "your purchase",
            "shipping confirmation",
            "envío de pedido",
            "recibo de compra",
            "billete electrónico",
            "billete electronico",
            "reserva de hotel",
            "tarjeta de embarque",

            // Spam, Phishing, and Scams
            "bitcoin",
            "cripto",
            "crypto",
            "lotería",
            "loteria",
            "lottery",
            "pharmacy",
            "viagra",
            "unclaimed inheritance"
    );

    /**
     * Processes a batch of raw emails for a user and classifies new candidacies
     * versus status updates on existing candidacies.
     */
    public EmailSyncDto.ExtractionSummaryDto processEmails(Long userId, List<EmailSyncDto.RawEmailDto> emails) {
        if (emails == null || emails.isEmpty()) {
            return new EmailSyncDto.ExtractionSummaryDto(0, 0, List.of());
        }

        int created = 0;
        int updated = 0;
        List<EmailSyncDto.DetectedEmailSummaryDto> detectedList = new ArrayList<>();

        for (EmailSyncDto.RawEmailDto email : emails) {
            String fromLower = email.fromAddress() != null ? email.fromAddress().toLowerCase(Locale.ROOT) : "";
            if (fromLower.contains("newsletters-noreply@linkedin.com")
                    || fromLower.contains("messaging-digest-noreply@linkedin.com")
                    || fromLower.contains("invitations@linkedin.com")
                    || fromLower.contains("notifications-noreply@linkedin.com")) {
                continue;
            }

            String platform = detectPlatform(email.fromAddress(), email.subject());
            String status = detectCandidacyStatus(email.subject(), email.plainTextBody(), email.htmlBody());

            if (status == null || "IGNORED".equalsIgnoreCase(status)) {
                log.debug("Skipping non-candidacy email from {}: {}", email.fromAddress(), email.subject());
                continue;
            }

            if ("APPLIED".equals(status)) {
                created++;
            } else {
                updated++;
            }

            detectedList.add(new EmailSyncDto.DetectedEmailSummaryDto(
                    email.uid(),
                    email.fromAddress(),
                    email.subject(),
                    email.receivedAt(),
                    platform,
                    status
            ));

            if (candidaturaService != null && userId != null) {
                try {
                    String puesto = extractJobTitle(email.subject());
                    String empresa = extractCompanyName(email.fromAddress(), email.subject(), platform, email.plainTextBody());
                    LocalDate fecha = email.receivedAt() != null
                            ? email.receivedAt().atZone(ZONE_MADRID).toLocalDate()
                            : LocalDate.now(ZONE_MADRID);
                    candidaturaService.registrarOActualizarDesdeCorreo(
                            userId, platform, status, puesto, empresa, fecha, email.receivedAt()
                    );
                } catch (Exception e) {
                    log.warn("Could not persist extracted candidacy for user {}: {}", userId, e.getMessage());
                }
            }
        }

        detectedList.sort(Comparator.comparing(
                EmailSyncDto.DetectedEmailSummaryDto::receivedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        log.info("Processed {} job emails for user {}: {} new candidacies, {} status updates",
                emails.size(), userId, created, updated);

        return new EmailSyncDto.ExtractionSummaryDto(created, updated, detectedList);
    }

    /**
     * Extracts an intelligent job title from the email subject.
     */
    public String extractJobTitle(String subject) {
        if (subject == null || subject.isBlank()) {
            return "Puesto de empleo";
        }
        String s = subject.trim();
        String lower = s.toLowerCase(Locale.ROOT);

        if (lower.startsWith("entrevista para el puesto de ")) {
            String candidate = s.substring(29).trim();
            int pipeIdx = candidate.indexOf('|');
            if (pipeIdx > 0) candidate = candidate.substring(0, pipeIdx).trim();
            int enIdx = candidate.toLowerCase(Locale.ROOT).lastIndexOf(" en ");
            if (enIdx > 0 && isLocation(candidate.substring(enIdx + 4))) {
                candidate = candidate.substring(0, enIdx).trim();
            }
            return cleanTitle(candidate);
        }

        if (lower.startsWith("entrevista para ")) {
            String candidate = s.substring(16).trim();
            int pipeIdx = candidate.indexOf('|');
            if (pipeIdx > 0) candidate = candidate.substring(0, pipeIdx).trim();
            return cleanTitle(candidate);
        }

        if (lower.startsWith("entrevista ") && !lower.contains(" para ")) {
            return cleanTitle(s);
        }

        int forIdx = lower.indexOf(" para ");
        if (forIdx >= 0 && forIdx < s.length() - 6) {
            String candidate = s.substring(forIdx + 6).trim();
            if (candidate.toLowerCase(Locale.ROOT).startsWith("el puesto de ")) {
                candidate = candidate.substring(13).trim();
            }
            int pipeIdx = candidate.indexOf('|');
            if (pipeIdx > 0) {
                candidate = candidate.substring(0, pipeIdx).trim();
            } else {
                int enIdx = candidate.toLowerCase(Locale.ROOT).lastIndexOf(" en ");
                if (enIdx > 0 && isLocation(candidate.substring(enIdx + 4))) {
                    candidate = candidate.substring(0, enIdx).trim();
                }
            }
            if (candidate.length() >= 3 && candidate.length() <= 100) {
                return cleanTitle(candidate);
            }
        }

        int colonIdx = s.lastIndexOf(':');
        if (colonIdx >= 0 && colonIdx < s.length() - 3) {
            String after = s.substring(colonIdx + 1).trim();
            if (after.length() >= 3 && after.length() <= 100) {
                return cleanTitle(after);
            }
        }
        int dashIdx = s.lastIndexOf(" - ");
        if (dashIdx >= 0 && dashIdx < s.length() - 5) {
            String after = s.substring(dashIdx + 3).trim();
            if (after.length() >= 3 && after.length() <= 100) {
                return cleanTitle(after);
            }
        }
        int toIdx = lower.indexOf(" to ");
        if (toIdx >= 0 && toIdx < s.length() - 4) {
            return cleanTitle(s.substring(toIdx + 4).trim());
        }
        if (lower.contains("se ha enviado tu solicitud a ")) {
            int a = lower.indexOf("se ha enviado tu solicitud a ");
            String comp = s.substring(a + 29).trim();
            return "Candidatura en " + cleanTitle(comp);
        }
        if (lower.startsWith("your application at ")) {
            String comp = s.substring(20).trim();
            int dash = comp.indexOf('-');
            if (dash > 0) comp = comp.substring(0, dash).trim();
            return "Candidatura en " + cleanTitle(comp);
        }
        return s.length() <= 100 ? cleanTitle(s) : "Puesto de empleo";
    }

    /**
     * Extracts company name checking email body for phrases like 'enviado a ...' first,
     * falling back to subject and sender domain.
     */
    public String extractCompanyName(String fromAddress, String subject, String platform, String plainTextBody) {
        if (plainTextBody != null && !plainTextBody.isBlank()) {
            String bodyLower = plainTextBody.toLowerCase(Locale.ROOT);
            int idx = bodyLower.indexOf("enviado a ");
            if (idx < 0) idx = bodyLower.indexOf("enviados a ");
            if (idx >= 0) {
                int start = idx + (bodyLower.contains("enviados a ") ? 11 : 10);
                String rest = plainTextBody.substring(start).trim();
                int newline = rest.indexOf('\n');
                if (newline > 0) rest = rest.substring(0, newline).trim();
                int dash = rest.indexOf('-');
                if (dash > 0) rest = rest.substring(0, dash).trim();
                if (rest.length() >= 2 && rest.length() <= 80) {
                    return cleanTitle(rest);
                }
            }
        }
        return extractCompanyName(fromAddress, subject, platform);
    }

    /**
     * Extracts an intelligent company name from the email sender address and subject.
     */
    public String extractCompanyName(String fromAddress, String subject, String platform) {
        String s = subject != null ? subject : "";
        String lower = s.toLowerCase(Locale.ROOT);

        int lastPipe = s.lastIndexOf('|');
        if (lastPipe >= 0 && lastPipe < s.length() - 2) {
            String candidate = cleanTitle(s.substring(lastPipe + 1));
            String candidateLower = candidate.toLowerCase(Locale.ROOT);
            if (!isLocation(candidateLower) && !candidateLower.contains("mañanas") && !candidateLower.contains("tardes")
                    && candidate.length() >= 2 && candidate.length() <= 80) {
                return candidate;
            }
        }

        if (lower.startsWith("entrevista ") && !lower.contains(" para ") && !lower.contains(" de ")) {
            String candidate = cleanTitle(s.substring(11));
            if (!isLocation(candidate.toLowerCase(Locale.ROOT)) && candidate.length() >= 2 && candidate.length() <= 80) {
                return candidate;
            }
        }

        int atIdx = lower.lastIndexOf(" at ");
        if (atIdx >= 0 && atIdx < s.length() - 4) {
            String company = s.substring(atIdx + 4).trim();
            int dash = company.indexOf(" - ");
            if (dash > 0) company = company.substring(0, dash).trim();
            if (company.length() >= 2 && company.length() <= 80) {
                return cleanTitle(company);
            }
        }

        int aIdx = lower.lastIndexOf(" a ");
        if (aIdx >= 0 && aIdx < s.length() - 3 && lower.contains("solicitud a ")) {
            String candidate = s.substring(aIdx + 3).trim();
            if (candidate.length() >= 2 && candidate.length() <= 80) {
                return cleanTitle(candidate);
            }
        }

        int enIdx = lower.lastIndexOf(" en ");
        if (enIdx >= 0 && enIdx < s.length() - 4) {
            String candidate = s.substring(enIdx + 4).trim();
            int pipe = candidate.indexOf('|');
            if (pipe > 0) candidate = candidate.substring(0, pipe).trim();
            String candidateLower = candidate.toLowerCase(Locale.ROOT);
            boolean isGeneric = candidateLower.contains("oferta") || candidateLower.contains("empleo")
                    || candidateLower.contains("proceso") || candidateLower.contains("vacante")
                    || candidateLower.contains("ventas");
            if (!isGeneric && !isLocation(candidateLower) && !candidateLower.startsWith("indeed") && !candidateLower.startsWith("linkedin")
                    && !candidateLower.startsWith("infojobs") && !candidateLower.startsWith("tecnoempleo")) {
                if (candidate.length() >= 2 && candidate.length() <= 80) {
                    return cleanTitle(candidate);
                }
            }
        }

        if (fromAddress != null && fromAddress.contains("@")) {
            String domain = fromAddress.substring(fromAddress.indexOf('@') + 1).toLowerCase(Locale.ROOT);
            if (!domain.contains("gmail") && !domain.contains("outlook") && !domain.contains("hotmail")
                    && !domain.contains("yahoo") && !domain.contains("infojobs") && !domain.contains("linkedin")
                    && !domain.contains("indeed") && !domain.contains("tecnoempleo")) {
                String prefix = domain.split("\\.")[0];
                if (prefix.length() >= 2) {
                    return capitalize(prefix);
                }
            }
        }

        return platform != null && !"OTHER".equals(platform) ? capitalize(platform) : "Empresa Confidencial";
    }

    private boolean isLocation(String text) {
        if (text == null) return false;
        String l = text.trim().toLowerCase(Locale.ROOT);
        return l.equals("madrid") || l.equals("barcelona") || l.equals("albacete") || l.equals("asturias")
                || l.equals("oviedo") || l.equals("gijon") || l.equals("gijón") || l.equals("valencia")
                || l.equals("sevilla") || l.equals("remoto") || l.equals("remote") || l.equals("españa") || l.equals("espana");
    }

    private String cleanTitle(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("^[\\s\\-\\:\\|]+|[\\s\\-\\:\\|]+$", "").trim();
        return cleaned.length() > 255 ? cleaned.substring(0, 255).trim() : cleaned;
    }

    private String capitalize(String str) {
        if (str == null || str.isBlank()) return "";
        String lower = str.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * Identifies the job board or ATS platform from the email sender address and subject.
     */
    public String detectPlatform(String fromAddress, String subject) {
        String combined = ((fromAddress != null ? fromAddress : "") + " " + (subject != null ? subject : ""))
                .toLowerCase(Locale.ROOT);

        // Major Spanish & Global Job Boards
        if (combined.contains("linkedin")) {
            return "LINKEDIN";
        }
        if (combined.contains("infojobs")) {
            return "INFOJOBS";
        }
        if (combined.contains("indeed")) {
            return "INDEED";
        }
        if (combined.contains("tecnoempleo")) {
            return "TECNOEMPLEO";
        }
        if (combined.contains("glassdoor")) {
            return "GLASSDOOR";
        }
        if (combined.contains("manfred") || combined.contains("getmanfred")) {
            return "MANFRED";
        }
        if (combined.contains("ticjob")) {
            return "TICJOB";
        }
        if (combined.contains("domestika")) {
            return "DOMESTIKA";
        }
        if (combined.contains("welcometothejungle") || combined.contains("wttj")) {
            return "WELCOME_TO_THE_JUNGLE";
        }
        if (combined.contains("jobtoday")) {
            return "JOBTODAY";
        }
        if (combined.contains("jooble")) {
            return "JOOBLE";
        }
        if (combined.contains("talent.com") || combined.contains("neuvoo")) {
            return "TALENT_COM";
        }
        if (combined.contains("turijobs")) {
            return "TURIJOBS";
        }
        if (combined.contains("computrabajo")) {
            return "COMPUTRABAJO";
        }
        if (combined.contains("hays")) {
            return "HAYS";
        }
        if (combined.contains("michaelpage") || combined.contains("pagepersonnel")) {
            return "MICHAEL_PAGE";
        }
        if (combined.contains("randstad")) {
            return "RANDSTAD";
        }
        if (combined.contains("adecco")) {
            return "ADECCO";
        }

        // Corporate Applicant Tracking Systems (ATS)
        if (combined.contains("greenhouse") || combined.contains("ghhire")) {
            return "GREENHOUSE";
        }
        if (combined.contains("lever.co") || combined.contains("hire.lever")) {
            return "LEVER";
        }
        if (combined.contains("workday") || combined.contains("myworkday")) {
            return "WORKDAY";
        }
        if (combined.contains("smartrecruiters")) {
            return "SMARTRECRUITERS";
        }
        if (combined.contains("teamtailor")) {
            return "TEAMTAILOR";
        }
        if (combined.contains("personio")) {
            return "PERSONIO";
        }
        if (combined.contains("workable")) {
            return "WORKABLE";
        }
        if (combined.contains("ashby")) {
            return "ASHBY";
        }
        if (combined.contains("bamboohr")) {
            return "BAMBOOHR";
        }
        if (combined.contains("breezy")) {
            return "BREEZY_HR";
        }
        if (combined.contains("taleo")) {
            return "TALEO";
        }
        if (combined.contains("recruitee")) {
            return "RECRUITEE";
        }
        if (combined.contains("jobvite")) {
            return "JOBVITE";
        }
        if (combined.contains("factorial")) {
            return "FACTORIAL";
        }

        return "OTHER";
    }

    /**
     * Determines the candidacy status according to keyword precedence:
     * REJECTED > OFFER > INTERVIEW > IN_PROGRESS > APPLIED.
     */
    public String detectCandidacyStatus(String subject, String plainTextBody, String htmlBody) {
        String normalizedSubject = subject != null ? subject.toLowerCase(Locale.ROOT) : "";
        String text = buildSearchableContent(subject, plainTextBody, htmlBody);

        // Discard obvious newsletters, digests, job alerts, connection requests, and billing noise
        if (containsAny(normalizedSubject, NEWSLETTER_AND_ALERT_KEYWORDS)) {
            return null;
        }

        if (containsAny(text, REJECTED_KEYWORDS)) {
            return "REJECTED";
        }
        if (containsAny(text, OFFER_KEYWORDS)) {
            return "OFFER";
        }
        if (containsAny(text, INTERVIEW_KEYWORDS)) {
            return "INTERVIEW";
        }
        if (containsAny(text, IN_PROGRESS_KEYWORDS)) {
            return "IN_PROGRESS";
        }
        if (containsAny(text, APPLIED_KEYWORDS)) {
            return "APPLIED";
        }

        // Generic confirmation check for portal applications without explicit template keywords
        if (isGenericJobApplicationContext(normalizedSubject, text)) {
            return "APPLIED";
        }

        return null;
    }

    private boolean isGenericJobApplicationContext(String subject, String text) {
        boolean hasConfirmation = subject.contains("confirmation") || subject.contains("confirmación")
                || subject.contains("confirmacion") || subject.contains("acuse") || subject.contains("recibo");
        boolean hasJobContext = text.contains("candidatura") || text.contains("solicitud")
                || text.contains("inscripción") || text.contains("inscripcion") || text.contains("postulación")
                || text.contains("postulacion") || text.contains("application") || text.contains("applied")
                || text.contains("documents on file") || text.contains("curriculum") || text.contains("resume");
        return hasConfirmation && hasJobContext;
    }

    private String buildSearchableContent(String subject, String plainTextBody, String htmlBody) {
        StringBuilder sb = new StringBuilder();
        if (subject != null) {
            sb.append(subject).append(" ");
        }
        if (plainTextBody != null) {
            sb.append(plainTextBody).append(" ");
        } else if (htmlBody != null) {
            sb.append(htmlBody.replaceAll("<[^>]+>", " "));
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String content, List<String> keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
