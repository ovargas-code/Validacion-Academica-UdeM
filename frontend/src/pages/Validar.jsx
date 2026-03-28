import { useState } from 'react';
import { iniciarValidacion, confirmarVerificacion } from '../api/api';

const VALIDATION_TYPES = ['DEGREE', 'ENROLLMENT'];

export default function Validar() {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({
    requesterName: '',
    requesterEmail: '',
    emailConfirm: '',
    studentDocument: '',
    validationType: 'DEGREE',
  });
  const [otp, setOtp] = useState('');
  const [sessionToken, setSessionToken] = useState(null);
  const [maskedEmail, setMaskedEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [nonValidResult, setNonValidResult] = useState(null);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError(null);
    setNonValidResult(null);
  };

  const emailsMatch = form.requesterEmail && form.emailConfirm &&
    form.requesterEmail === form.emailConfirm;
  const isFormReady = form.requesterName && form.requesterEmail &&
    form.emailConfirm && emailsMatch && form.studentDocument;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!emailsMatch) { setError('Los correos electrónicos no coinciden.'); return; }
    setLoading(true);
    setError(null);
    setNonValidResult(null);
    try {
      const { requesterName, requesterEmail, studentDocument, validationType } = form;
      const res = await iniciarValidacion({ requesterName, requesterEmail, studentDocument, validationType });
      const data = res.data;
      if (data.status === 'VALID' && data.token) {
        setSessionToken(data.token);
        setMaskedEmail(data.maskedEmail || requesterEmail);
        setStep(2);
      } else {
        setNonValidResult(data);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error al procesar la solicitud. Verifica que el backend esté activo.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await confirmarVerificacion(sessionToken, otp);
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Certificado_UdeM.pdf');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setStep(3);
    } catch (err) {
      if (err.response?.data instanceof Blob) {
        try {
          const text = await err.response.data.text();
          const json = JSON.parse(text);
          if (json.code === 'TOKEN_EXPIRED') {
            setError('El código ha expirado. Vuelve al formulario para solicitar uno nuevo.');
          } else if (json.code === 'INVALID_CODE') {
            setError('Código incorrecto. Revisa tu correo e inténtalo de nuevo.');
          } else if (json.code === 'TOKEN_ALREADY_USED') {
            setError('Este código ya fue utilizado. Inicia una nueva solicitud.');
          } else {
            setError(json.message || 'Error al verificar el código.');
          }
        } catch {
          setError('Error al verificar el código.');
        }
      } else {
        setError(err.response?.data?.message || 'Error al verificar el código.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (step === 3) {
    return (
      <div className="page fade-in">
        <div className="page-header">
          <h1>✅ Verificación Completada</h1>
          <p>El proceso de validación finalizó exitosamente.</p>
        </div>
        <div className="card" style={{ maxWidth: 560 }}>
          <div className="alert alert-success">
            ✉️ El certificado fue descargado en tu dispositivo y enviado a <strong>{maskedEmail}</strong>.
          </div>
          <button
            className="btn btn-primary btn-full"
            style={{ marginTop: 16 }}
            onClick={() => {
              setStep(1);
              setForm({ requesterName: '', requesterEmail: '', emailConfirm: '', studentDocument: '', validationType: 'DEGREE' });
              setOtp(''); setSessionToken(null); setNonValidResult(null); setError(null);
            }}
          >
            ↩ Nueva solicitud
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page fade-in">
      <div className="page-header">
        <h1>📋 Solicitar Validación</h1>
        <p>Completa el formulario para generar el certificado académico del estudiante.</p>
      </div>

      {step === 1 && (
        <div className="card" style={{ maxWidth: 600 }}>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Tu nombre completo</label>
              <input className="form-input" name="requesterName" placeholder="Nombre completo"
                value={form.requesterName} onChange={handleChange} required />
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Correo electrónico</label>
                <input className="form-input" name="requesterEmail" type="email"
                  placeholder="correo@ejemplo.com" value={form.requesterEmail} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Confirmar correo</label>
                <input
                  className="form-input"
                  name="emailConfirm"
                  type="email"
                  placeholder="correo@ejemplo.com"
                  value={form.emailConfirm}
                  onChange={handleChange}
                  required
                  style={form.emailConfirm && !emailsMatch ? { borderColor: '#e53e3e' } : {}}
                />
                {form.emailConfirm && !emailsMatch && (
                  <span style={{ color: '#e53e3e', fontSize: '0.8rem', marginTop: 4, display: 'block' }}>
                    Los correos no coinciden
                  </span>
                )}
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Documento del estudiante</label>
                <input className="form-input" name="studentDocument" placeholder="Número de documento"
                  value={form.studentDocument} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Tipo de validación</label>
                <select className="form-select" name="validationType" value={form.validationType} onChange={handleChange}>
                  {VALIDATION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>

            {error && <div className="alert alert-error">⚠️ {error}</div>}

            {nonValidResult && (
              <div className="result-card fade-in" style={{ marginTop: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <strong>Resultado</strong>
                  <span className="badge badge-invalid">✘ {nonValidResult.status}</span>
                </div>
                <p style={{ margin: 0, color: '#666', fontSize: '0.9rem' }}>{nonValidResult.message}</p>
              </div>
            )}

            <button type="submit" className="btn btn-primary btn-full"
              disabled={loading || !isFormReady} style={{ marginTop: 12 }}>
              {loading ? <><span className="spinner" /> Procesando...</> : '✦ Continuar →'}
            </button>
          </form>
        </div>
      )}

      {step === 2 && (
        <div className="card" style={{ maxWidth: 500 }}>
          <div className="alert alert-success" style={{ marginBottom: 20 }}>
            ✉️ Enviamos un código de 6 dígitos a <strong>{maskedEmail}</strong>. Revisa tu bandeja de entrada.
          </div>

          <form onSubmit={handleConfirm}>
            <div className="form-group">
              <label className="form-label" style={{ textAlign: 'center', display: 'block' }}>
                Código de verificación
              </label>
              <input
                className="form-input"
                placeholder="000000"
                value={otp}
                onChange={e => { setOtp(e.target.value.replace(/\D/g, '').slice(0, 6)); setError(null); }}
                maxLength={6}
                inputMode="numeric"
                pattern="[0-9]{6}"
                required
                style={{ letterSpacing: '0.5em', fontSize: '1.6rem', textAlign: 'center', fontFamily: 'monospace' }}
                autoFocus
              />
              <span style={{ color: '#888', fontSize: '0.8rem', display: 'block', textAlign: 'center', marginTop: 4 }}>
                El código expira en 10 minutos
              </span>
            </div>

            {error && <div className="alert alert-error">⚠️ {error}</div>}

            <button type="submit" className="btn btn-primary btn-full"
              disabled={loading || otp.length !== 6} style={{ marginTop: 8 }}>
              {loading ? <><span className="spinner" /> Verificando...</> : '🔒 Verificar y descargar certificado'}
            </button>

            <button type="button" className="btn btn-secondary btn-full"
              style={{ marginTop: 8 }}
              onClick={() => { setStep(1); setOtp(''); setError(null); setSessionToken(null); }}>
              ← Volver y corregir datos
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
