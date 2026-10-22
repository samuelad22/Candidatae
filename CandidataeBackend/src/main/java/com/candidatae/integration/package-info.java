/**
 * Adaptadores hacia sistemas externos.
 *
 * <p>Este paquete (y sus subpaquetes) aísla las comunicaciones con sistemas externos
 * de la lógica de negocio: los controladores y los servicios no saben si un dato
 * viene de Google, de un modelo de lenguaje o de un fichero. Cada integración se
 * expone al resto de la aplicación detrás de una interfaz propia, de modo que se
 * pueda cambiar de proveedor sin tocar el dominio.</p>
 *
 * <p>Subpaquetes:</p>
 * <ul>
 *   <li>{@code gmail}: cliente de la API de Gmail en modo <strong>solo lectura</strong>
 *       (OAuth2 "authorization_code" manual, sin {@code oauth2Login()}) y almacén en
 *       memoria de los {@code state} de la vinculación. Es la Fase 3 del proyecto.</li>
 *   <li>{@code llm}: interfaz abstracta del proveedor de extracción opcional de datos
 *       a partir del texto del correo (Fase 4). Aún no existe.</li>
 * </ul>
 */
package com.candidatae.integration;
