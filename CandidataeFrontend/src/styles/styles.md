# styles

Esta carpeta contiene los estilos globales de la aplicación que no pertenecen a un componente concreto.
Aquí van el reset de CSS, variables (colores, tipografías, espaciados), fuentes y utilidades generales.
Los estilos específicos de cada componente se mantienen junto a él, no aquí.
Se importan normalmente una sola vez desde `main.jsx`.

# Archivos

- `tokens.css`: tipografías, espaciados, radios
- `theme.css`: colores del tema oscuro y claro (``data-theme`)
- `base.css`: reset y estilos base de `body`, títulos y párrafos
- `index.css`: importa todos los anteriores