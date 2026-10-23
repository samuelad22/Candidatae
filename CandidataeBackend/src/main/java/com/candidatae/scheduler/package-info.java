/**
 * Paquete reservado para las tareas programadas de Candidatae.
 *
 * <p>Aquí vivirán las clases anotadas con {@code @Scheduled} que leen periódicamente
 * los buzones conectados y los convierten en candidaturas. Concretamente, la
 * <strong>Fase 3</strong> del proyecto ("Integración con Gmail") trae:</p>
 *
 * <ul>
 *   <li>Una tarea que recorre las {@code cuenta_correo} en estado {@code ACTIVA} y
 *       llama a la lectura incremental de la API de Gmail a partir del
 *       {@code cursor_sincronizacion} de cada cuenta.</li>
 *   <li>El registro de cada ejecución en la tabla {@code sincronizacion} (con sus
 *       contadores de correos leídos, candidaturas creadas y errores).</li>
 * </ul>
 *
 * <p>Mientras tanto, el paquete queda vacío a propósito: no se crean clases
 * "de relleno" para una funcionalidad que aún no está implementada.</p>
 */
package com.candidatae.scheduler;
