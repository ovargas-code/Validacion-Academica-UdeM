import { useState } from 'react';
import { verificarCertificado, descargarCertificadoPDF, extractErrorMessage } from '../api/api';

export default function Verificar() {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!code.trim()) return;
    setLoading(true);
    setResult(null);
    setError(null);
    try {
      const res = await verificarCertificado(code.trim());
      setResult(res.data);
    } catch (err) {
      setError(
        err.response?.status === 404
          ? 'No se encontró ningún certificado con ese código.'
          : extractErrorMessage(err, 'Error al verificar. Intenta de nuevo.')
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page fade-in">
      <div className="page-header">
        <h1>🔍 Verificar Certificado</h1>
        <p>Ingresa el código único del certificado para comprobar su autenticidad.</p>
      </div>

      <div className="card" style={{ maxWidth: 560 }}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10 }}>
          <input
            className="form-input"
            placeholder="Código de verificación (ej: UDEM-A1B2C3)"
            value={code}
            onChange={(e) => { setCode(e.target.value); setError(null); }}
            style={{ flex: 1 }}
          />
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || !code.trim()}
          >
            {loading ? <span className="spinner" /> : '🔍 Buscar'}
          </button>
        </form>

        {error && (
          <div className="alert alert-error" style={{ marginTop: 16 }}>
            ⚠️ {error}
          </div>
        )}

        {result && (
          <div className="result-card fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <strong style={{ fontFamily: 'var(--font-display)', fontSize: '1rem' }}>Certificado encontrado</strong>
              <span className={`badge ${result.status === 'VALID' ? 'badge-valid' : 'badge-invalid'}`}>
                {result.status === 'VALID' ? '✔ VÁLIDO' : '✘ INVÁLIDO'}
              </span>
            </div>

            {result.studentName && (
              <div className="result-row">
                <span className="result-label">Estudiante</span>
                <span className="result-value">{result.studentName}</span>
              </div>
            )}
            {result.studentDocument && (
              <div className="result-row">
                <span className="result-label">Documento</span>
                <span className="result-value">{result.studentDocument}</span>
              </div>
            )}
            {result.validationType && (
              <div className="result-row">
                <span className="result-label">Tipo de validación</span>
                <span className="result-value">{result.validationType}</span>
              </div>
            )}
            {result.verificationCode && (
              <div className="result-row">
                <span className="result-label">Código</span>
                <span className="result-value" style={{ fontFamily: 'monospace', color: 'var(--accent)' }}>
                  {result.verificationCode}
                </span>
              </div>
            )}
            {result.issuedAt && (
              <div className="result-row">
                <span className="result-label">Emitido</span>
                <span className="result-value">{new Date(result.issuedAt).toLocaleDateString('es-CO')}</span>
              </div>
            )}

            <a
              href={descargarCertificadoPDF(code.trim())}
              target="_blank"
              rel="noreferrer"
              className="btn btn-success btn-full"
              style={{ marginTop: 16 }}
            >
              ⬇ Descargar PDF
            </a>
          </div>
        )}
      </div>
    </div>
  );
}
