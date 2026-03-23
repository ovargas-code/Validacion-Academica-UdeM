import { useState } from 'react';
import { verificarYGenerarCertificado, descargarCertificadoPDF } from '../api/api';

const VALIDATION_TYPES = ['DEGREE', 'ENROLLMENT', 'GRADES', 'CONDUCT'];

export default function Validar() {
  const [form, setForm] = useState({
    requesterName: '',
    requesterEmail: '',
    studentDocument: '',
    validationType: 'DEGREE',
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    setError(null);
    try {
      const res = await verificarYGenerarCertificado(form);
      setResult(res.data);
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data ||
        'Error al procesar la solicitud. Verifica que el backend esté activo.'
      );
    } finally {
      setLoading(false);
    }
  };

  const isValid = form.requesterName && form.requesterEmail && form.studentDocument;

  return (
    <div className="page fade-in">
      <div className="page-header">
        <h1>📋 Solicitar Validación</h1>
        <p>Completa el formulario para generar el certificado académico del estudiante.</p>
      </div>

      <div className="card" style={{ maxWidth: 600 }}>
        <form onSubmit={handleSubmit}>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">Tu nombre</label>
              <input
                className="form-input"
                name="requesterName"
                placeholder="Nombre completo"
                value={form.requesterName}
                onChange={handleChange}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Tu correo electrónico</label>
              <input
                className="form-input"
                name="requesterEmail"
                type="email"
                placeholder="correo@ejemplo.com"
                value={form.requesterEmail}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">Documento del estudiante</label>
              <input
                className="form-input"
                name="studentDocument"
                placeholder="Número de documento"
                value={form.studentDocument}
                onChange={handleChange}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Tipo de validación</label>
              <select
                className="form-select"
                name="validationType"
                value={form.validationType}
                onChange={handleChange}
              >
                {VALIDATION_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
          </div>

          {error && (
            <div className="alert alert-error">
              ⚠️ {typeof error === 'string' ? error : JSON.stringify(error)}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-full"
            disabled={loading || !isValid}
            style={{ marginTop: 8 }}
          >
            {loading ? <><span className="spinner" /> Procesando...</> : '✦ Generar Certificado'}
          </button>
        </form>

        {result && (
          <div className="result-card fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <strong style={{ fontFamily: 'var(--font-display)', fontSize: '1rem' }}>Resultado</strong>
              <span className={`badge ${result.status === 'VALID' ? 'badge-valid' : 'badge-invalid'}`}>
                {result.status === 'VALID' ? '✔ VÁLIDO' : '✘ INVÁLIDO'}
              </span>
            </div>
            {result.verificationCode && (
              <div className="result-row">
                <span className="result-label">Código de verificación</span>
                <span className="result-value" style={{ fontFamily: 'monospace', color: 'var(--accent)' }}>
                  {result.verificationCode}
                </span>
              </div>
            )}
            {result.studentName && (
              <div className="result-row">
                <span className="result-label">Estudiante</span>
                <span className="result-value">{result.studentName}</span>
              </div>
            )}
            {result.validationType && (
              <div className="result-row">
                <span className="result-label">Tipo</span>
                <span className="result-value">{result.validationType}</span>
              </div>
            )}
            <div className="alert alert-success" style={{ marginTop: 16 }}>
              ✉️ El certificado fue enviado a <strong>{form.requesterEmail}</strong>
            </div>
            {result.verificationCode && (
              <a
                href={descargarCertificadoPDF(result.verificationCode)}
                target="_blank"
                rel="noreferrer"
                className="btn btn-success btn-full"
                style={{ marginTop: 12 }}
              >
                ⬇ Descargar PDF
              </a>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
