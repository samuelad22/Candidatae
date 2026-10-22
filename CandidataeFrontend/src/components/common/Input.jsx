export default function Input({ label, error, className = "", ...props }) {
  return (
    <label className="field">
      {label && <span className="field-label">{label}</span>}
      <input className={`input ${className}`.trim()} {...props} />
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}