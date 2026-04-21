import { useState, useRef } from 'react';
import { crearSolicitudEmpresa, urlPlantillaCarta, extractErrorMessage } from '../api/api';
import './SolicitudEmpresa.css';

const TIPOS_DOCUMENTO = ['CC', 'CE', 'PAS', 'TI'];
const TIPOS_VALIDACION = [
  { value: 'DEGREE', label: 'Verificación de título (DEGREE)' },
  { value: 'ENROLLMENT', label: 'Verificación de matrícula (ENROLLMENT)' },
];
const MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

const INITIAL_FORM = {
  nombreEmpresa: '',
  nitEmpresa: '',
  nombreContacto: '',
  cargoContacto: '',
  correoContacto: '',
  telefonoContacto: '',
  tipoDocumentoEstudiante: 'CC',
  documentoEstudiante: '',
  nombreEstudiante: '',
  programaConsultado: '',
  tipoValidacion: 'DEGREE',
  observaciones: '',
  aceptaTerminos: false,
};

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function validateForm(form, carta) {
  const errors = {};
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const nitRe = /^[0-9]{6,10}(-[0-9])?$/;

  if (!form.nombreEmpresa.trim()) errors.nombreEmpresa = 'Campo obligatorio';
  if (!form.nitEmpresa.trim()) errors.nitEmpresa = 'Campo obligatorio';
  else if (!nitRe.test(form.nitEmpresa.trim())) errors.nitEmpresa = 'Formato inválido. Ej: 890123456-1';
  if (!form.nombreContacto.trim()) errors.nombreContacto = 'Campo obligatorio';
  if (!form.cargoContacto.trim()) errors.cargoContacto = 'Campo obligatorio';
  if (!form.correoContacto.trim()) errors.correoContacto = 'Campo obligatorio';
  else if (!emailRe.test(form.correoContacto.trim())) errors.correoContacto = 'Correo electrónico inválido';
  if (!form.documentoEstudiante.trim()) errors.documentoEstudiante = 'Campo obligatorio';
  if (!form.nombreEstudiante.trim()) errors.nombreEstudiante = 'Campo obligatorio';
  if (!carta) errors.carta = 'Debes adjuntar la carta de autorización en PDF';
  if (!form.aceptaTerminos) errors.aceptaTerminos = 'Debes aceptar los términos para continuar';
  return errors;
}

export default function SolicitudEmpresa() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [carta, setCarta] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [resultado, setResultado] = useState(null);
  const fileInputRef = useRef(null);

  const clearFieldError = (name) => {
    if (fieldErrors[name]) {
      setFieldErrors((prev) => { const next = { ...prev }; delete next[name]; return next; });
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    clearFieldError(name);
    setError(null);
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) { setCarta(null); return; }

    if (!file.name.toLowerCase().endsWith('.pdf') || file.type !== 'application/pdf') {
      setFieldErrors((prev) => ({ ...prev, carta: 'Solo se aceptan archivos PDF (.pdf)' }));
      setCarta(null);
      e.target.value = '';
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      setFieldErrors((prev) => ({ ...prev, carta: `El archivo supera el límite de 10 MB (tamaño: ${formatBytes(file.size)})` }));
      setCarta(null);
      e.target.value = '';
      return;
    }
    clearFieldError('carta');
    setCarta(file);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validateForm(form, carta);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      // Scroll al primer error
      const firstErrorKey = Object.keys(errors)[0];
      document.getElementById(`field-${firstErrorKey}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const datos = {
        nombreEmpresa: form.nombreEmpresa.trim(),
        nitEmpresa: form.nitEmpresa.trim(),
        nombreContacto: form.nombreContacto.trim(),
        cargoContacto: form.cargoContacto.trim(),
        correoContacto: form.correoContacto.trim(),
        telefonoContacto: form.telefonoContacto.trim() || null,
        tipoDocumentoEstudiante: form.tipoDocumentoEstudiante,
        documentoEstudiante: form.documentoEstudiante.trim(),
        nombreEstudiante: form.nombreEstudiante.trim(),
        programaConsultado: form.programaConsultado.trim() || null,
        tipoValidacion: form.tipoValidacion,
        observaciones: form.observaciones.trim() || null,
        aceptaTerminos: form.aceptaTerminos,
      };

      const formData = new FormData();
      formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
      formData.append('carta', carta);

      const res = await crearSolicitudEmpresa(formData);
      setResultado(res.data);
    } catch (err) {
      const msg = extractErrorMessage(err, 'Error al enviar la solicitud. Intenta de nuevo.');
      // Detalle de errores de validación del backend
      const details = err?.response?.data?.details;
      setError(details?.length ? `${msg}: ${details.join(' | ')}` : msg);
    } finally {
      setLoading(false);
    }
  };

  const handleNuevaSolicitud = () => {
    setForm(INITIAL_FORM);
    setCarta(null);
    setResultado(null);
    setError(null);
    setFieldErrors({});
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // ── Panel de éxito ──────────────────────────────────────────────────────

  if (resultado) {
    return (
      <div className="page fade-in">
        <div className="page-header">
          <h1>Solicitud de Verificación Empresarial</h1>
          <p>Universidad de Medellín — Sistema de Validación Académica</p>
        </div>
        <div className="card" style={{ maxWidth: 560, margin: '0 auto' }}>
          <div className="success-panel">
            <div className="success-icon">✅</div>
            <h2>Solicitud registrada</h2>
            <p>
              Tu solicitud fue recibida correctamente. Guarda el número de radicado
              para hacer seguimiento.
            </p>
            <div className="radicado-box">
              <div className="radicado-label">Número de radicado</div>
              <div className="radicado-number">{resultado.numeroSolicitud}</div>
              <div className="radicado-estado">Estado: {resultado.estado}</div>
            </div>
            <div className="alert alert-success" style={{ textAlign: 'left' }}>
              El equipo de validación académica de la Universidad de Medellín revisará
              tu solicitud y se comunicará con <strong>{resultado.correoContacto}</strong>.
            </div>
            <button
              className="btn btn-primary btn-full"
              style={{ marginTop: 16 }}
              onClick={handleNuevaSolicitud}
            >
              ↩ Nueva solicitud
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── Formulario ──────────────────────────────────────────────────────────

  return (
    <div className="page fade-in">
      <div className="page-header">
        <h1>Solicitud de Verificación Empresarial</h1>
        <p>
          Diligencia el formulario para solicitar la verificación académica de un estudiante
          o egresado de la Universidad de Medellín. Adjunta la carta de autorización firmada.
        </p>
      </div>

      <div className="card" style={{ maxWidth: 720 }}>
        <form onSubmit={handleSubmit} noValidate>

          {/* ── Sección 1: Datos de la empresa ──────────────────────────── */}
          <div className="se-section">
            <div className="se-section-title">1. Datos de la empresa solicitante</div>

            <div className="grid-2">
              <div className="form-group" id="field-nombreEmpresa">
                <label className="form-label">Nombre de la empresa *</label>
                <input
                  className={`form-input${fieldErrors.nombreEmpresa ? ' has-error' : ''}`}
                  name="nombreEmpresa"
                  placeholder="Ej: Empresa S.A.S."
                  value={form.nombreEmpresa}
                  onChange={handleChange}
                  maxLength={200}
                />
                {fieldErrors.nombreEmpresa && <span className="field-error">{fieldErrors.nombreEmpresa}</span>}
              </div>

              <div className="form-group" id="field-nitEmpresa">
                <label className="form-label">NIT de la empresa *</label>
                <input
                  className={`form-input${fieldErrors.nitEmpresa ? ' has-error' : ''}`}
                  name="nitEmpresa"
                  placeholder="Ej: 890123456-1"
                  value={form.nitEmpresa}
                  onChange={handleChange}
                  maxLength={20}
                />
                {fieldErrors.nitEmpresa && <span className="field-error">{fieldErrors.nitEmpresa}</span>}
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group" id="field-nombreContacto">
                <label className="form-label">Nombre del contacto *</label>
                <input
                  className={`form-input${fieldErrors.nombreContacto ? ' has-error' : ''}`}
                  name="nombreContacto"
                  placeholder="Nombre completo"
                  value={form.nombreContacto}
                  onChange={handleChange}
                  maxLength={150}
                />
                {fieldErrors.nombreContacto && <span className="field-error">{fieldErrors.nombreContacto}</span>}
              </div>

              <div className="form-group" id="field-cargoContacto">
                <label className="form-label">Cargo *</label>
                <input
                  className={`form-input${fieldErrors.cargoContacto ? ' has-error' : ''}`}
                  name="cargoContacto"
                  placeholder="Ej: Jefe de Talento Humano"
                  value={form.cargoContacto}
                  onChange={handleChange}
                  maxLength={100}
                />
                {fieldErrors.cargoContacto && <span className="field-error">{fieldErrors.cargoContacto}</span>}
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group" id="field-correoContacto">
                <label className="form-label">Correo electrónico *</label>
                <input
                  className={`form-input${fieldErrors.correoContacto ? ' has-error' : ''}`}
                  name="correoContacto"
                  type="email"
                  placeholder="contacto@empresa.com"
                  value={form.correoContacto}
                  onChange={handleChange}
                  maxLength={254}
                />
                {fieldErrors.correoContacto && <span className="field-error">{fieldErrors.correoContacto}</span>}
              </div>

              <div className="form-group">
                <label className="form-label">Teléfono de contacto</label>
                <input
                  className="form-input"
                  name="telefonoContacto"
                  placeholder="Ej: +57 604 3456789"
                  value={form.telefonoContacto}
                  onChange={handleChange}
                  maxLength={20}
                />
              </div>
            </div>
          </div>

          {/* ── Sección 2: Datos del estudiante ─────────────────────────── */}
          <div className="se-section">
            <div className="se-section-title">2. Datos del estudiante a verificar</div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Tipo de documento *</label>
                <select
                  className="form-select"
                  name="tipoDocumentoEstudiante"
                  value={form.tipoDocumentoEstudiante}
                  onChange={handleChange}
                >
                  {TIPOS_DOCUMENTO.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>

              <div className="form-group" id="field-documentoEstudiante">
                <label className="form-label">Número de documento *</label>
                <input
                  className={`form-input${fieldErrors.documentoEstudiante ? ' has-error' : ''}`}
                  name="documentoEstudiante"
                  placeholder="Número de documento"
                  value={form.documentoEstudiante}
                  onChange={handleChange}
                  maxLength={20}
                />
                {fieldErrors.documentoEstudiante && <span className="field-error">{fieldErrors.documentoEstudiante}</span>}
              </div>
            </div>

            <div className="form-group" id="field-nombreEstudiante">
              <label className="form-label">Nombre completo del estudiante *</label>
              <input
                className={`form-input${fieldErrors.nombreEstudiante ? ' has-error' : ''}`}
                name="nombreEstudiante"
                placeholder="Nombre y apellidos del estudiante o egresado"
                value={form.nombreEstudiante}
                onChange={handleChange}
                maxLength={150}
              />
              {fieldErrors.nombreEstudiante && <span className="field-error">{fieldErrors.nombreEstudiante}</span>}
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Tipo de verificación *</label>
                <select
                  className="form-select"
                  name="tipoValidacion"
                  value={form.tipoValidacion}
                  onChange={handleChange}
                >
                  {TIPOS_VALIDACION.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Programa consultado</label>
                <input
                  className="form-input"
                  name="programaConsultado"
                  placeholder="Ej: Ingeniería de Sistemas"
                  value={form.programaConsultado}
                  onChange={handleChange}
                  maxLength={200}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Observaciones adicionales</label>
              <textarea
                className="form-input"
                name="observaciones"
                placeholder="Información adicional relevante para la verificación (opcional)"
                value={form.observaciones}
                onChange={handleChange}
                maxLength={500}
                rows={3}
                style={{ resize: 'vertical', minHeight: 72 }}
              />
            </div>
          </div>

          {/* ── Sección 3: Carta y consentimiento ───────────────────────── */}
          <div className="se-section">
            <div className="se-section-title">3. Carta de autorización y consentimiento</div>

            <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 16, lineHeight: 1.6 }}>
              Adjunta la carta de autorización firmada por el representante legal de la empresa
              en formato PDF (máx. 10 MB). Puedes descargar la plantilla modelo a continuación.
            </p>

            <a
              href={urlPlantillaCarta()}
              download
              className="plantilla-link"
              style={{ display: 'inline-flex', marginBottom: 20 }}
            >
              📄 Descargar plantilla de carta de autorización (.docx)
            </a>

            <div className="form-group" id="field-carta">
              <label className="form-label">Carta de autorización (PDF) *</label>
              <label
                className={`upload-zone${carta ? ' has-file' : ''}${fieldErrors.carta ? ' has-error' : ''}`}
                htmlFor="carta-input"
              >
                <div className="upload-icon">{carta ? '📎' : '☁️'}</div>
                {carta ? (
                  <>
                    <div className="upload-file-name">{carta.name}</div>
                    <div className="upload-file-size">{formatBytes(carta.size)}</div>
                    <div className="upload-hint" style={{ marginTop: 8 }}>Haz clic para cambiar el archivo</div>
                  </>
                ) : (
                  <>
                    <div className="upload-hint">Haz clic para seleccionar el PDF</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Solo archivos .pdf · Máximo 10 MB</div>
                  </>
                )}
                <input
                  id="carta-input"
                  type="file"
                  accept=".pdf,application/pdf"
                  onChange={handleFileChange}
                  ref={fileInputRef}
                />
              </label>
              {fieldErrors.carta && <span className="field-error">{fieldErrors.carta}</span>}
            </div>

            <div className="form-group" id="field-aceptaTerminos">
              <label
                className={`checkbox-group${fieldErrors.aceptaTerminos ? ' has-error' : ''}`}
                htmlFor="terminos-input"
              >
                <input
                  id="terminos-input"
                  type="checkbox"
                  name="aceptaTerminos"
                  checked={form.aceptaTerminos}
                  onChange={handleChange}
                />
                <span className="checkbox-label">
                  Declaro que la información suministrada es verídica y que la empresa autoriza
                  el tratamiento de datos personales conforme a la{' '}
                  <a
                    href="https://www.udemedellin.edu.co/politica-privacidad"
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                  >
                    política de privacidad de la Universidad de Medellín
                  </a>
                  . *
                </span>
              </label>
              {fieldErrors.aceptaTerminos && <span className="field-error">{fieldErrors.aceptaTerminos}</span>}
            </div>
          </div>

          {/* ── Errores globales y submit ────────────────────────────────── */}
          {error && <div className="alert alert-error">⚠️ {error}</div>}

          <button
            type="submit"
            className="btn btn-primary btn-full"
            disabled={loading}
            style={{ marginTop: 8 }}
          >
            {loading ? <><span className="spinner" /> Enviando solicitud...</> : '✦ Enviar solicitud'}
          </button>
        </form>
      </div>
    </div>
  );
}
