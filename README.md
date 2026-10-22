# Candidatae â€” Gestor de Candidaturas

> Documento base del proyecto. La **Fuente de Verdad** funcional (modelo de datos, mÃ¡quina
> de estados, endpoints y fases) estÃ¡ en el enunciado del proyecto y se refleja aquÃ­.
> Este README explica **cÃ³mo estÃ¡ construido el backend** y **por quÃ© cada cosa vive donde vive**.

## 1. VisiÃ³n General y Objetivo

Cuando buscamos empleo activamente, nos inscribimos en decenas de ofertas a travÃ©s de distintas
plataformas (LinkedIn, InfoJobs, Indeed, Tecnoempleo, Glassdoor...). Llevar el seguimiento manual
en un Excel acaba siendo tedioso y fÃ¡cil de abandonar.

**El objetivo** es una aplicaciÃ³n web centralizada (Dashboard) que permita a cada usuario gestionar
y visualizar **todas sus candidaturas**, extrayendo la informaciÃ³n directamente de los correos
electrÃ³nicos de confirmaciÃ³n que recibe al inscribirse en una oferta o al avanzar en un proceso de
selecciÃ³n.

Cada plataforma notifica sus hitos por correo, asÃ­ que el correo es la fuente de datos
automÃ¡tica de la que se extraen las candidaturas.

---

## 2. Alcance del MVP (Producto MÃ­nimo Viable)

El desarrollo se organiza en fases. Cada fase es un entregable verificable.

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Repositorio, esqueleto Spring Boot + React, PostgreSQL, migraciones | âœ… |
| 1 | Usuarios y acceso: registro, login, JWT, aislamiento por usuario | âœ… (login social pendiente) |
| 2 | Candidaturas: modelo de datos, CRUD, filtros, historial, notas, fusiÃ³n | â³ |
| 3 | IntegraciÃ³n con Gmail: conexiÃ³n OAuth de solo lectura, lectura incremental | âœ… (lectura pendiente) |
| 4 | ExtracciÃ³n determinista e interfaz LLM opcional, deduplicaciÃ³n | â³ |
| 5 | Dashboard y recordatorios: embudo, estadÃ­sticas, "sin seguimiento" | â³ |
| 6 | Pruebas de integraciÃ³n, revisiÃ³n de seguridad, despliegue | â³ |

**Tarea en curso â€” CRUD de `cuenta_correo`**: se detalla en `docs/PLAN-CRUD-CUENTA-CORREO.md`.

---

## 3. Stack TecnolÃ³gico

| Capa | TecnologÃ­a |
|---|---|
| Backend | Java 25 LTS + Spring Boot 4 (monolito por capas) |
| Build | Gradle |
| Base de datos | PostgreSQL 16, esquema gestionado con **Flyway** (migraciones versionadas) |
| Frontend | React (SPA) que consume la API REST |
| Seguridad | Spring Security, JWT de corta duraciÃ³n, OAuth2 (Google y LinkedIn), BCrypt, tokens de Gmail cifrados (AES-256-GCM) |
| DocumentaciÃ³n | Swagger / OpenAPI (springdoc) |
| Infraestructura | Docker + Docker Compose |
| Pruebas | JUnit 5 + Mockito + `@WebMvcTest` + Testcontainers (PostgreSQL real) |

---

## 4. Arquitectura del Backend: monolito por capas

Candidatae es **un Ãºnico artefacto de Spring Boot** que expone la API REST sin estado. Dentro se
organiza el cÃ³digo **por capas** bajo un Ãºnico paquete raÃ­z: `com.candidatae`.

La regla es sencilla y es lo que hace que se entienda el "dÃ³nde" de cada fichero:

> **El paquete de una clase dice quÃ© hace, no en quÃ© pantalla sale.**
> `controller` expone, `service` decide, `repository` guarda, `domain` modela,
> `dto` transporta, `security` autentica, `integration` habla con el exterior.

```
com.candidatae
â”‚
â”œâ”€â”€ CandidataeApplication.java   # Punto de entrada. Vive en la raÃ­z: escanea todo lo demÃ¡s.
â”‚
â”œâ”€â”€ config/          # ConfiguraciÃ³n global de la aplicaciÃ³n
â”‚   â”œâ”€â”€ SecurityConfig.java      # Cadena de filtros, CORS, reglas de acceso por ruta
â”‚   â””â”€â”€ OpenApiConfig.java       # Contrato OpenAPI / Swagger UI
â”‚
â”œâ”€â”€ controller/      # ExposiciÃ³n de la API REST: valida la entrada y devuelve DTOs
â”‚   â”œâ”€â”€ AuthController.java
â”‚   â””â”€â”€ CuentaCorreoController.java
â”‚
â”œâ”€â”€ service/         # LÃ³gica de negocio transaccional
â”‚   â”œâ”€â”€ AuthService.java         # Registro y login
â”‚   â”œâ”€â”€ UsuarioQueryService.java # Resuelve el usuario de la sesiÃ³n (email â†’ id)
â”‚   â””â”€â”€ CuentaCorreoService.java # Cuentas de correo: alta, ediciÃ³n, desconexiÃ³n
â”‚
â”œâ”€â”€ repository/      # Acceso a datos (Spring Data JPA)
â”‚   â”œâ”€â”€ UsuarioRepository.java
â”‚   â””â”€â”€ CuentaCorreoRepository.java
â”‚
â”œâ”€â”€ domain/          # Entidades JPA y enumerados
â”‚   â”œâ”€â”€ Usuario.java             # Tabla "users"
â”‚   â”œâ”€â”€ CuentaCorreo.java        # Tabla "cuenta_correo"
â”‚   â”œâ”€â”€ ProveedorAuth.java       # LOCAL | GOOGLE | LINKEDIN
â”‚   â”œâ”€â”€ ProveedorCorreo.java     # GMAIL
â”‚   â””â”€â”€ EstadoConexion.java      # PENDIENTE | ACTIVA | EXPIRADA | CON_ERROR | DESCONECTADA
â”‚
â”œâ”€â”€ dto/             # Objetos de transferencia (Request / Response). Nunca entidades JPA.
â”‚   â”œâ”€â”€ ApiErrorResponse.java    # Formato Ãºnico de error de la API
â”‚   â”œâ”€â”€ AuthDto.java              # Register Â· Login Â· Response
â”‚   â””â”€â”€ CuentaCorreoDto.java      # Create Â· Update Â· Response Â· GmailAuthorize
â”‚
â”œâ”€â”€ security/        # AutenticaciÃ³n y autorizaciÃ³n
â”‚   â”œâ”€â”€ JwtService.java, JwtAuthenticationFilter.java
â”‚   â”œâ”€â”€ CurrentUserProvider.java # Identificador del usuario de la sesiÃ³n
â”‚   â”œâ”€â”€ JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java
â”‚   â””â”€â”€ TokenCipherService.java  # Cifrado AES-256-GCM de los tokens de Gmail
â”‚
â”œâ”€â”€ integration/     # Adaptadores hacia sistemas externos
â”‚   â”œâ”€â”€ gmail/       # OAuth2 de Google (solo lectura) + almacÃ©n de "state"
â”‚   â”‚   â”œâ”€â”€ GmailOAuthService.java
â”‚   â”‚   â””â”€â”€ OAuthStateStore.java
â”‚   â””â”€â”€ llm/         # (Fase 4) Interfaz abstracta del proveedor de extracciÃ³n
â”‚
â”œâ”€â”€ exception/       # TaxonomÃ­a de errores de negocio + manejador global
â”‚   â”œâ”€â”€ ApiException.java         # Ãšnico error de negocio, con su status HTTP
â”‚   â””â”€â”€ GlobalExceptionHandler.java  # Traduce cualquier error al JSON de la API
â”‚
â””â”€â”€ scheduler/       # (Fase 3) Tareas @Scheduled de sincronizaciÃ³n con Gmail
```

### Por quÃ© estas piezas estÃ¡n donde estÃ¡n

| DecisiÃ³n | Motivo |
|---|---|
| La clase principal estÃ¡ en la **raÃ­z** del paquete | `@SpringBootApplication` escanea automÃ¡ticamente todas las capas, sin `scanBasePackages` que mantener al dÃ­a. |
| `GlobalExceptionHandler` vive en `exception/`, no en `config/` | El manejo de errores completo (excepciones tipadas + traducciÃ³n a JSON) queda **en un solo sitio**, en vez de partirlo entre dos paquetes. |
| `TokenCipherService` en `security/` y no en `integration/gmail/` | Es una primitiva criptogrÃ¡fica reutilizable, no un detalle de Google. Si maÃ±ana se cifra otro secreto, no hay que tocar el paquete de la integraciÃ³n. |
| `GmailOAuthService` en `integration/gmail/` | Hablar con Google es una integraciÃ³n externa. El servicio de negocio (`CuentaCorreoService`) no sabe que existe Google: solo le pide "revoca este token". |
| `scheduler/` vacÃ­o hasta la Fase 3 | Se documenta **dÃ³nde** irÃ¡ la sincronizaciÃ³n periÃ³dica sin crear clases de relleno. |
| Los DTOs no son las entidades JPA | El `refresh_token_cifrado` de `CuentaCorreo` nunca debe salir por la API: laå®žä½“ es un detalle de persistencia y el DTO, el contrato. |
| `UsuarioQueryService` existe | La sesiÃ³n identifica al usuario por **email**, pero las tablas de negocio usan el **id** numÃ©rico. Esta clase es el Ãºnico punto que traduce de uno a otro. |
| Un fichero de DTO por funcionalidad, con varios `record` dentro | `AuthDto` y `CuentaCorreoDto` agrupan los DTOs de su funcionalidad, pero **cada uno sigue siendo un tipo distinto** (`CuentaCorreoDto.Create`, `.Update`, `.Response`, `.GmailAuthorize`). AsÃ­ se reducen los ficheros sin fundir contratos: el de entrada lleva validaciÃ³n y el de salida no, y un DTO Ãºnico los mezclarÃ­a. `ApiErrorResponse` va aparte porque es el contrato de error de toda la API, no de una funcionalidad. |
| Una sola excepciÃ³n de negocio (`ApiException`) | Cinco clases (`BadRequest`, `Conflict`, `Unauthorized`, `ResourceNotFound`, `OAuthFlow`) eran 95 % idÃ©nticas y el manejador global las traducÃ­a con el mismo cÃ³digo. Lo que distingue un error de otro es su **status HTTP**, no la clase: con una Ãºnica clase y factorÃ­as (`ApiException.notFound(...)`, `.conflict(...)`, `.badRequest(...)`, `.unauthorized(...)`, `.oauth(...)`) la taxonomÃ­a cabe en una pantalla y sigue siendo explÃ­cita en cada punto donde se lanza. |

---

## 5. Puesta en marcha

### 5.1 Requisitos

- Java 25 LTS
- Docker (para PostgreSQL)
- Node.js 20+ (frontend)

### 5.2 Base de datos

```bash
cd CandidataeBackend
docker compose up -d          # PostgreSQL en localhost:5432 (user: admin, pass: admin, db: candidatae)
```

El esquema se crea **solo con Flyway** (`spring.jpa.hibernate.ddl-auto=validate`).
Si necesitas empezar de cero:

```bash
docker compose down -v        # âš ï¸ borra el volumen y todos los datos
docker compose up -d
```

### 5.3 Backend

```bash
cd CandidataeBackend
./gradlew bootRun             # http://localhost:8080
```

DocumentaciÃ³n interactiva de la API: <http://localhost:8080/swagger-ui.html>

```bash
./gradlew test                # 69 tests (unitarios + web + integraciÃ³n con Testcontainers)
```

### 5.4 Variables de entorno

Todas tienen valor por defecto para desarrollo local, asÃ­ que la app arranca sin configurar
nada. **En producciÃ³n son obligatorias.**

En local puedes copiarlas a un fichero `.env` en la raÃ­z del repositorio (ya estÃ¡ en el
`.gitignore`) y exportar las que uses en tu terminal o en el IDE:

```bash
cp .env.example .env
```

| Variable | Por defecto (dev) | Para quÃ© sirve |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/candidatae` | ConexiÃ³n a PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | `admin` / `admin` | Credenciales de la base de datos |
| `JWT_SECRET` | clave de desarrollo | Clave de firma de los JWT (mÃ­n. 32 caracteres) |
| `JWT_EXPIRATION_MS` | `86400000` | DuraciÃ³n del token (24 h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | OrÃ­genes permitidos del frontend |
| `GMAIL_TOKEN_ENCRYPTION_KEY` | clave de desarrollo | Clave AES-256 en Base64 (32 bytes) con la que se cifran los refresh tokens de Gmail |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | *(vacÃ­o)* | Credenciales OAuth2 de Google, necesarias para vincular buzones |
| `GMAIL_REDIRECT_URI` | `http://localhost:8080/api/email-accounts/gmail/callback` | Redirect URI OAuth2 (debe coincidir **exactamente** con el de Google Cloud) |
| `GMAIL_SECURE_COOKIE` | `false` | `true` en producciÃ³n (requiere HTTPS) |
| `FRONTEND_BASE_URL` | `http://localhost:5173` | A dÃ³nde redirige el callback de OAuth2 |
| `JPA_SHOW_SQL` / `JPA_FORMAT_SQL` | `true` | Volcado del SQL a consola. **Poner a `false` en producciÃ³n**: ensucia el log y puede filtrar datos. |

Generar una clave de cifrado vÃ¡lida:

```bash
openssl rand -base64 32
```

---

## 6. API REST

Prefijo: `/api`. Los endpoints marcados con **JWT** exigen el token de sesiÃ³n
(`Authorization: Bearer <token>`).

### AutenticaciÃ³n

| MÃ©todo | Ruta | DescripciÃ³n |
|---|---|---|
| POST | `/api/auth/register` | Registro con email y contraseÃ±a. Devuelve el JWT. |
| POST | `/api/auth/login` | Inicio de sesiÃ³n. Devuelve el JWT. |

### Cuentas de correo

| MÃ©todo | Ruta | JWT | DescripciÃ³n |
|---|---|---|---|
| GET | `/api/email-accounts` | âœ” | Lista las cuentas de correo del usuario. |
| GET | `/api/email-accounts/{id}` | âœ” | Detalle de una cuenta. |
| POST | `/api/email-accounts` | âœ” | Alta manual (sin pasar por Google). |
| PATCH | `/api/email-accounts/{id}` | âœ” | Edita email o estado de la cuenta. |
| DELETE | `/api/email-accounts/{id}` | âœ” | Desconecta la cuenta y revoca el token en Google. |
| POST | `/api/email-accounts/gmail` | âœ” | Inicia la vinculaciÃ³n OAuth2: devuelve la `authorizationUrl` de Google. |
| GET | `/api/email-accounts/gmail/callback` | â€” | Callback de Google (lo llama Google, no el usuario). |

### Formato de error

Todas las respuestas de error comparten la misma forma, venga de donde venga:

```json
{
  "timestamp": "2026-10-20T10:15:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Cuenta de correo no encontrada",
  "path": "/api/email-accounts/9",
  "fieldErrors": null
}
```

---

## 7. Frontend

```bash
cd CandidataeFrontend
npm install
npm run dev                  # http://localhost:5173
```

---

## 8. Notas de arquitectura

- **La tabla de usuarios se llama `users`, no `usuario`.** DecisiÃ³n del equipo para no rehacer el
  esquema ya desplegado en los entornos de desarrollo. La clase Java sÃ­ se llama `Usuario`, y las
  columnas que la referencian se llaman `usuario_id` (p. ej. `cuenta_correo`). Cuando `user` era
  el nombre de la tabla, `user` era ademÃ¡s palabra reservada en PostgreSQL; esa razÃ³n ya no
  aplica, pero el nombre se mantiene por el criterio de no tocar el esquema.
- **El prefijo de las rutas es `/api`**, sin versiÃ³n, para ajustarse a la Fuente de Verdad del
  proyecto. Si en el futuro hace falta versionar la API, se aÃ±ade `/api/v2` y se migra en una
  sola pasada.
- **Los secretos se cifran en la base de datos**: el refresh token de Gmail se guarda con
  AES-256-GCM y nunca sale por la API ni aparece en los logs.
- **Aislamiento de datos**: todas las consultas a `cuenta_correo` filtran por `usuario_id`,
  y `CuentaCorreoRepository.findById(Long)` estÃ¡ bloqueado para evitar usos sin filtro.
- **El login social (Google/LinkedIn) sigue pendiente.** La columna `auth_provider` ya es un
  enumerado (`ProveedorAuth`) preparado para ello, y `POST /api/auth/refresh` (renovaciÃ³n de
  sesiÃ³n) estÃ¡ igualmente pendiente: hoy el JWT dura 24 h.
- **Detalle del CRUD de cuentas de correo**: `docs/PLAN-CRUD-CUENTA-CORREO.md`.
