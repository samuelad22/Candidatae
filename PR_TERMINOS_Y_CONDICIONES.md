# PR: Términos y Condiciones, Cumplimiento RGPD y Derecho al Olvido

## 📋 Resumen
Esta PR corrige los fallos visuales y de renderizado del modal de **Términos y Condiciones**, actualiza su contenido a la normativa española y europea (**RGPD / LOPDGDD / AEPD**), y añade la persistencia del consentimiento y la supresión de cuenta (**Derecho al Olvido**, Art. 17 RGPD) en backend.

---

## 🛠️ Cambios Realizados

### 1. Frontend (Correcciones y UX)
- **Corrección de transparencia:** Se corrigió el diálogo del modal (`terminosCondiciones.css`) y se activó `data-bs-theme="dark"` en `index.html` para evitar solapamientos ilegibles.
- **Ruta de prueba:** Añadida la ruta pública `/terminos` (en `TerminosPage.jsx`) para previsualizar e interactuar con el modal.
- **Textos legales actualizados (`terminosCondiciones.js`):**
  - Consentimiento válido para mayores de **14 años** (Art. 7 LOPDGDD).
  - Plazos de conservación y transferencias internacionales (Google/Microsoft con DPF).
  - Canales para el ejercicio de derechos ARCO-POL y reclamación ante la **AEPD**.
- **Servicio API:** Creado `termsService.js` para sincronizar la aceptación/revocación con el backend.

### 2. Backend (Persistencia y Derecho al Olvido)
- **Base de datos (Flyway V5):** Añadidas columnas `terms_accepted`, `terms_accepted_at` y `terms_version` a la tabla `users`.
- **Consentimiento de términos:**
  - `GET /api/users/terms`: Consulta el estado de aceptación.
  - `POST /api/users/terms`: Registra la aceptación o revocación con versión y timestamp.
- **Derecho al Olvido (`DELETE /api/users/me`):**
  - Revoca previamente el token OAuth de Gmail en servidores de Google.
  - Elimina el usuario en base de datos; la regla `ON DELETE CASCADE` de PostgreSQL suprime en cadena todas sus candidaturas, correos, notas y contactos.

---

## 🧪 Cómo Probar

1. **Frontend:**
   - Navega a `http://localhost:5173/terminos`.
   - Comprueba que el modal tiene fondo oscuro sólido y desenfoque, sin solaparse con el fondo.
   - Lee o haz scroll en las cláusulas y prueba los botones «Aceptar» o «Denegar».

2. **Backend:**
   - Ejecuta `DELETE /api/users/me` con el token JWT de un usuario: debe devolver `204 No Content` y limpiar todos sus datos vinculados.
   - Ejecuta `POST /api/users/terms` con `{"accepted": true, "version": "1.0"}`: debe responder `200 OK` con el timestamp de auditoría.

---

## ✅ Checklist
- [x] Corrección visual de contraste y transparencia comprobada.
- [x] Textos adaptados a las directrices de la AEPD y LOPDGDD.
- [x] Tests unitarios de backend ejecutados y superados (`BUILD SUCCESSFUL`).
- [x] Build de frontend verificado sin errores (`npm run build`).
