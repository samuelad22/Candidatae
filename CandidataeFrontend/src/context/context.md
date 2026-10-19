# context

Esta carpeta contiene los Context Providers de React, usados para compartir estado global sin pasar props manualmente.
Aquí van contextos como tema, autenticación, idioma o lista de favoritos.
Cada archivo suele exportar el contexto, su Provider y, opcionalmente, un hook para consumirlo (`useAuth`, `useTema`).
Los Providers se colocan normalmente en `main.jsx` o `App.jsx`, envolviendo a la aplicación.