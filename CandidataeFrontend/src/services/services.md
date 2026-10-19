# services

Esta carpeta contiene las funciones que se comunican con APIs o servicios externos (fetch, axios, etc.).
Aquí van archivos como `paisesService.js` o `authService.js`, con funciones tipo `getPaises()` o `login()`.
Los componentes y hooks las llaman, pero no saben cómo se hace la petición.
Suele incluir la configuración base (URL, headers, manejo de errores) en un archivo común.