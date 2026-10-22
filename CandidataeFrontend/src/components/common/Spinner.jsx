export default function Spinner({ size = "md", label = "Cargando...", className = "" }) {
  return (
    <span
      role="status"
      aria-label={label}
      className={`spinner spinner-${size} ${className}`.trim()}
    />
  );
}