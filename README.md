# Gestor de Ofertas de Trabajo — Documento Base del Proyecto

## 1. Visión General y Objetivo
Cuando buscamos empleo activamente, nos inscribimos en decenas de ofertas a través de distintas plataformas (LinkedIn, InfoJobs, portales de empresa, etc.). Llevar el seguimiento manual en un Excel o Notion acaba siendo tedioso y fácil de abandonar.

**El objetivo de este proyecto** es construir una aplicación web centralizada (Dashboard) que permita a los usuarios gestionar y visualizar todas sus candidaturas de forma automática, **extrayendo la información directamente de los correos electrónicos de confirmación** que reciben al inscribirse en una oferta o avanzar en un proceso de selección.

---

## 2. Alcance del MVP (Producto Mínimo Viable)

Para no bloquearnos y poder entregar valor de forma incremental, dividimos el desarrollo en fases claras:

### Fase 1: Base y Gestión Manual (Core)
* **Autenticación de usuarios:** Registro e inicio de sesión (JWT / OAuth2).
* **Dashboard de Candidaturas:** Vista principal (formato tabla y/o tablero Kanban) con el listado de ofertas y sus estados: *Inscrito, Leído/En revisión, Entrevista RRHH, Prueba Técnica, Oferta, Descartado*.
* **CRUD de Candidaturas:** Posibilidad de crear, editar o mover manualmente una candidatura (esto permite al equipo de Frontend avanzar sin tener que esperar a la todo el flujo de integración del correo del usuario).
* **Estadísticas básicas:** Contadores rápidos (total de inscripciones esta semana, procesos activos, tasa de respuesta).

### Fase 2: Automatización por Correo
* **Conexión con bandeja de entrada:** Vinculación con el correo del usuario (ej. vía API de Gmail con OAuth2 o lectura por IMAP).
* **Sincronización y Filtrado:** Detección de correos transaccionales de portales de empleo (ej. *"Tu solicitud se ha enviado a..."*, *"Gracias por inscribirte en..."*).
* **Extracción de Datos (Parser):** Obtención automática de:
  * Nombre de la empresa.
  * Puesto / Rol.
  * Plataforma de origen (LinkedIn, InfoJobs, etc.).
  * Fecha de inscripción.
* **Actualización automática:** Si entra un correo nuevo sobre una oferta ya existente (ej. citación a entrevista o descarte), se actualiza el estado o se añade al historial de esa candidatura.

---

## 3. Stack Tecnológico

* **Frontend:** React (SPA), llamadas a API REST.
* **Backend:** Java + Spring Boot (gestión de dependencias con Maven).
* **Base de Datos:** PostgreSQL (relacional, ideal para mantener la integridad entre usuarios, candidaturas e historial de correos).
* **Documentación de API:** Swagger / OpenAPI (para que Front y Back tengamos el contrato de los endpoints siempre a mano).
* **Infraestructura y Despliegue:** Docker y Docker Compose (para levantar Base de Datos, Back y Front con un solo comando en local).

---

## 4. Arquitectura del Backend: Monolito Modular por Capas

Optamos por un **Monolito Modular**. Esto nos permite tener la sencillez de despliegue de un solo proyecto en Spring Boot, pero organizando el código por **módulos de dominio** para no mezclarlo todo y poder trabajar varios en paralelo sin conflictos constantes en Git.

Dentro de cada módulo, mantendremos una **arquitectura por capas** (`controller`, `service`, `repository`, `model/entity`, `dto`).

### Estructura de paquetes propuesta:
```text
com.equipo.jobtracker
│
├── common / shared          # Configuración global (CORS, Swagger), Seguridad base, Excepciones (@ControllerAdvice)
│
├── user                     # Módulo de Usuarios y Autenticación
│   ├── controller
│   ├── service
│   ├── repository
│   ├── model
│   └── dto
│
├── application              # Módulo Core: Gestión de Candidaturas, Estados y Dashboard
│   ├── controller
│   ├── service
│   ├── repository
│   ├── model
│   └── dto
│
└── emailingestion           # Módulo de Integración de Correo y Extracción de Datos
    ├── controller           # Endpoints para forzar sincronización o vincular cuenta
    ├── service              # Lógica de conexión al correo y parseo del contenido
    ├── repository           # Registro de correos ya procesados (para no duplicar)
    └── dto
