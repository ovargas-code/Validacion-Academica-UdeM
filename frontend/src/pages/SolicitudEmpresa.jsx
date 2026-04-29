import { useState } from 'react';
import {
  crearSolicitudEmpresa,
  descargarCertificadoPDF,
  extractErrorMessage,
  resolverUrlBackend,
} from '../api/api';
import './SolicitudEmpresa.css';

const TIPOS_DOCUMENTO = ['CC', 'CE', 'PAS', 'TI'];
const TIPOS_VALIDACION = [
  { value: 'DEGREE', label: 'Verificación de título (DEGREE)' },
  { value: 'ENROLLMENT', label: 'Verificación de matrícula (ENROLLMENT)' },
];
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

const CERTIFICADO_URL_FIELDS = [
  'urlDescarga',
  'descargaUrl',
  'downloadUrl',
  'certificadoUrl',
  'urlCertificado',
  'pdfUrl',
  'cartaFinalUrl',
];
const CERTIFICADO_CODE_FIELDS = ['codigo', 'codigoVerificacion', 'verificationCode', 'code', 'token'];
const CERTIFICADO_PDF_FIELDS = ['pdf', 'certificado', 'certificadoPdf', 'pdfBase64'];

function firstStringValue(source, fields) {
  if (!source) return null;
  for (const field of fields) {
    const value = source[field];
    if (typeof value === 'string' && value.trim()) return value.trim();
  }
  return null;
}

function certificadoFinalDownload(resultado) {
  const directUrl = firstStringValue(resultado, CERTIFICADO_URL_FIELDS);
  if (directUrl) return resolverUrlBackend(directUrl);

  const code = firstStringValue(resultado, CERTIFICADO_CODE_FIELDS);
  if (code) return descargarCertificadoPDF(code);

  const pdf = firstStringValue(resultado, CERTIFICADO_PDF_FIELDS);
  if (pdf) {
    return pdf.startsWith('data:')
      ? pdf
      : `data:application/pdf;base64,${pdf.replace(/^data:application\/pdf;base64,/, '')}`;
  }

  return null;
}

function validateForm(form) {
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
  if (!form.telefonoContacto.trim()) errors.telefonoContacto = 'Campo obligatorio';
  if (!form.tipoDocumentoEstudiante) errors.tipoDocumentoEstudiante = 'Campo obligatorio';
  if (!form.documentoEstudiante.trim()) errors.documentoEstudiante = 'Campo obligatorio';
  if (!form.nombreEstudiante.trim()) errors.nombreEstudiante = 'Campo obligatorio';
  if (!form.tipoValidacion) errors.tipoValidacion = 'Campo obligatorio';
  if (!form.programaConsultado.trim()) errors.programaConsultado = 'Campo obligatorio';
  if (!form.aceptaTerminos) errors.aceptaTerminos = 'Debes aceptar los términos para continuar';
  return errors;
}

export default function SolicitudEmpresa() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [resultado, setResultado] = useState(null);
  const validationErrors = validateForm(form);
  const isFormComplete = Object.keys(validationErrors).length === 0;

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

  const handleBlur = (e) => {
    const { name } = e.target;
    const errors = validateForm(form);
    if (errors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: errors[name] }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validateForm(form);
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
        telefonoContacto: form.telefonoContacto.trim(),
        tipoDocumentoEstudiante: form.tipoDocumentoEstudiante,
        documentoEstudiante: form.documentoEstudiante.trim(),
        nombreEstudiante: form.nombreEstudiante.trim(),
        programaConsultado: form.programaConsultado.trim(),
        tipoValidacion: form.tipoValidacion,
        observaciones: form.observaciones.trim(),
        aceptaTerminos: form.aceptaTerminos,
      };

      const formData = new FormData();
      formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));

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
    setResultado(null);
    setError(null);
    setFieldErrors({});
  };

  // ── Panel de éxito ──────────────────────────────────────────────────────

  if (resultado) {
    const certificadoHref = certificadoFinalDownload(resultado);

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
            {certificadoHref && (
              <a
                className="btn btn-primary btn-full"
                style={{ marginTop: 16 }}
                href={certificadoHref}
                download={`Certificado_${resultado.numeroSolicitud || 'verificacion'}.pdf`}
                target={certificadoHref.startsWith('data:') ? undefined : '_blank'}
                rel={certificadoHref.startsWith('data:') ? undefined : 'noopener noreferrer'}
              >
                Descargar certificado
              </a>
            )}
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
          o egresado de la Universidad de Medellín.
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
                  onBlur={handleBlur}
                  required
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
                  onBlur={handleBlur}
                  required
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
                  onBlur={handleBlur}
                  required
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
                  onBlur={handleBlur}
                  required
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
                  onBlur={handleBlur}
                  required
                  maxLength={254}
                />
                {fieldErrors.correoContacto && <span className="field-error">{fieldErrors.correoContacto}</span>}
              </div>

              <div className="form-group" id="field-telefonoContacto">
                <label className="form-label">Teléfono de contacto *</label>
                <input
                  className={`form-input${fieldErrors.telefonoContacto ? ' has-error' : ''}`}
                  name="telefonoContacto"
                  placeholder="Ej: +57 604 3456789"
                  value={form.telefonoContacto}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  required
                  maxLength={20}
                />
                {fieldErrors.telefonoContacto && <span className="field-error">{fieldErrors.telefonoContacto}</span>}
              </div>
            </div>
          </div>

          {/* ── Sección 2: Datos del estudiante ─────────────────────────── */}
          <div className="se-section">
            <div className="se-section-title">2. Datos del estudiante a verificar</div>

            <div className="grid-2">
              <div className="form-group" id="field-tipoDocumentoEstudiante">
                <label className="form-label">Tipo de documento *</label>
                <select
                  className={`form-select${fieldErrors.tipoDocumentoEstudiante ? ' has-error' : ''}`}
                  name="tipoDocumentoEstudiante"
                  value={form.tipoDocumentoEstudiante}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  required
                >
                  {TIPOS_DOCUMENTO.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
                {fieldErrors.tipoDocumentoEstudiante && <span className="field-error">{fieldErrors.tipoDocumentoEstudiante}</span>}
              </div>

              <div className="form-group" id="field-documentoEstudiante">
                <label className="form-label">Número de documento *</label>
                <input
                  className={`form-input${fieldErrors.documentoEstudiante ? ' has-error' : ''}`}
                  name="documentoEstudiante"
                  placeholder="Número de documento"
                  value={form.documentoEstudiante}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  required
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
                onBlur={handleBlur}
                required
                maxLength={150}
              />
              {fieldErrors.nombreEstudiante && <span className="field-error">{fieldErrors.nombreEstudiante}</span>}
            </div>

            <div className="grid-2">
              <div className="form-group" id="field-tipoValidacion">
                <label className="form-label">Tipo de verificación *</label>
                <select
                  className={`form-select${fieldErrors.tipoValidacion ? ' has-error' : ''}`}
                  name="tipoValidacion"
                  value={form.tipoValidacion}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  required
                >
                  {TIPOS_VALIDACION.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
                {fieldErrors.tipoValidacion && <span className="field-error">{fieldErrors.tipoValidacion}</span>}
              </div>

              <div className="form-group" id="field-programaConsultado">
                <label className="form-label">Programa consultado *</label>
                <input
                  className={`form-input${fieldErrors.programaConsultado ? ' has-error' : ''}`}
                  name="programaConsultado"
                  placeholder="Ej: Ingeniería de Sistemas"
                  value={form.programaConsultado}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  required
                  maxLength={200}
                />
                {fieldErrors.programaConsultado && <span className="field-error">{fieldErrors.programaConsultado}</span>}
              </div>
            </div>

            <div className="form-group" id="field-observaciones">
              <label className="form-label">Observaciones adicionales</label>
              <textarea
                className={`form-input${fieldErrors.observaciones ? ' has-error' : ''}`}
                name="observaciones"
                placeholder="Información adicional relevante para la verificación"
                value={form.observaciones}
                onChange={handleChange}
                onBlur={handleBlur}
                maxLength={500}
                rows={3}
                style={{ resize: 'vertical', minHeight: 72 }}
              />
            </div>
          </div>

          {/* ── Sección 3: Autorización y consentimiento ────────────────── */}
          <div className="se-section">
            <div className="se-section-title">3. Autorización y consentimiento</div>

            <div className="legal-scroll-box" aria-label="Autorización de tratamiento de datos personales">
              <p>
                De conformidad con lo establecido en la ley 1581 de 2012, reglamentada por el Decreto
                1377 de 2013, así como las demás normas que la modifiquen, sustituyan o deroguen,
                manifiesto que autorizo de manera libre, previa, expresa, inequívoca y debidamente
                informada a la UNIVERSIDAD DE MEDELLÍN (Nit. 890.902.920-1) en calidad de responsable
                del tratamiento de mis datos personales, para recolectar, almacenar, transferir y
                transmitir a terceros aliados, circular y en general utilizar los datos personales
                suministrados en este formulario y los que a futuro suministre, para las siguientes
                finalidades: i) Realizar actividades promocionales y encuestas de información. ii)
                Invitación a conferencias, charlas y remisión de actividades de interés. iii) Para la
                presentación de quejas, denuncias o reportes ante las autoridades competentes. iv)
                Finalidades administrativas de seguimiento a la gestión de la Institución. v) Reportes
                de usabilidad de la plataforma y de gestión de la misma. vi) Las demás relacionadas con
                la información recolectada.
              </p>
              <p>
                Manifiesto que en calidad de titular de los datos personales suministrados, puedo
                solicitar la supresión, cancelación, corrección o actualización de la autorización que
                otorgo, en los términos dispuestos por la normatividad vigente en materia de protección
                de datos, y de acuerdo con lo previsto en la Política para el Manejo y Tratamiento de
                Datos Personales, que puede ser consultada en el siguiente link:{' '}
                <a
                  href="https://udemedellin.edu.co/politica-para-el-manejo-y-tratamiento-de-datos-personales/"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  https://udemedellin.edu.co/politica-para-el-manejo-y-tratamiento-de-datos-personales/
                </a>
              </p>
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
                  required
                />
                <span className="checkbox-label">
                  Declaro que la información suministrada es verídica y que la empresa autoriza
                  el tratamiento de datos personales conforme a la{' '}
                  <a
                    href="https://udemedellin.edu.co/politica-para-el-manejo-y-tratamiento-de-datos-personales/"
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
            disabled={loading || !isFormComplete}
            title={!isFormComplete ? 'Completa todos los campos obligatorios para enviar la solicitud' : undefined}
            style={{ marginTop: 8 }}
          >
            {loading ? <><span className="spinner" /> Enviando solicitud...</> : '✦ Enviar solicitud'}
          </button>
        </form>
      </div>
    </div>
  );
}
