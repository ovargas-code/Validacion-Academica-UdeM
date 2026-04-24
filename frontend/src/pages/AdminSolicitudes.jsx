import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  listarSolicitudesAdmin,
  descargarCartaSolicitud,
  marcarEnRevision,
  aprobarSolicitud,
  rechazarSolicitud,
  extractErrorMessage,
} from '../api/api';
import './AdminSolicitudes.css';

const PAGE_SIZE = 20;
const ESTADOS = ['PENDIENTE', 'EN_REVISION', 'APROBADA', 'RECHAZADA'];

function badgeClass(estado) {
  switch (estado) {
    case 'PENDIENTE':   return 'badge badge-pending';
    case 'EN_REVISION': return 'badge badge-review';
    case 'APROBADA':    return 'badge badge-valid';
    case 'RECHAZADA':   return 'badge badge-invalid';
    default:            return 'badge';
  }
}

function fmt(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('es-CO', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  });
}

function Field({ label, value }) {
  return (
    <div className="modal-field">
      <span className="modal-field-label">{label}</span>
      <span className="modal-field-value">{value || '—'}</span>
    </div>
  );
}

export default function AdminSolicitudes() {
  const navigate = useNavigate();

  // ── Lista ──────────────────────────────────────────────────────────
  const [page, setPage]                   = useState(0);
  const [pageData, setPageData]           = useState(null);
  const [loading, setLoading]             = useState(true);
  const [estadoFiltro, setEstadoFiltro]   = useState('');

  // ── Toast ──────────────────────────────────────────────────────────
  const [toast, setToast]                 = useState(null); // { type: 'success'|'error', msg }
  const toastTimer                        = useRef(null);

  // ── Modal ──────────────────────────────────────────────────────────
  const [selected, setSelected]           = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  // ── Rechazo (sub-formulario dentro del modal) ──────────────────────
  const [showRechazar, setShowRechazar]   = useState(false);
  const [motivoRechazo, setMotivoRechazo] = useState('');

  // ── Guard de token ─────────────────────────────────────────────────
  useEffect(() => {
    if (!localStorage.getItem('token')) navigate('/login');
  }, [navigate]);

  // ── Carga paginada con filtro (useCallback con estadoFiltro en deps) ─
  const loadPage = useCallback((pageNum) => {
    setLoading(true);
    listarSolicitudesAdmin(pageNum, PAGE_SIZE, estadoFiltro || null)
      .then((res) => {
        setPageData(res.data);
        setPage(pageNum);
      })
      .catch((err) => {
        if (!err.response || err.response.status !== 401) {
          showToast('error', extractErrorMessage(err, 'No se pudo cargar las solicitudes.'));
        }
      })
      .finally(() => setLoading(false));
  }, [estadoFiltro]);

  // Se dispara en el montaje y cada vez que cambia estadoFiltro
  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  // ── ESC cierra el modal ────────────────────────────────────────────
  useEffect(() => {
    if (!selected) return;
    const onKey = (e) => {
      if (e.key === 'Escape' && !actionLoading) {
        setSelected(null);
        setShowRechazar(false);
        setMotivoRechazo('');
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [selected, actionLoading]);

  // ── Helpers ────────────────────────────────────────────────────────
  function showToast(type, msg) {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToast({ type, msg });
    toastTimer.current = setTimeout(() => setToast(null), 4000);
  }

  function closeModal() {
    setSelected(null);
    setShowRechazar(false);
    setMotivoRechazo('');
  }

  function openModal(sol) {
    setSelected(sol);
    setShowRechazar(false);
    setMotivoRechazo('');
  }

  // ── Descarga de carta ──────────────────────────────────────────────
  async function handleDescarga() {
    setActionLoading(true);
    try {
      const res = await descargarCartaSolicitud(selected.numeroSolicitud);
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Carta_${selected.numeroSolicitud}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      showToast('error', extractErrorMessage(err, 'No se pudo descargar la carta.'));
    } finally {
      setActionLoading(false);
    }
  }

  // ── Acción de transición de estado (marcar en revisión / aprobar) ──
  async function handleAction(apiFn, successMsg) {
    setActionLoading(true);
    try {
      await apiFn();
      showToast('success', successMsg);
      closeModal();
      loadPage(page);
    } catch (err) {
      showToast('error', extractErrorMessage(err, 'Error al procesar la acción.'));
    } finally {
      setActionLoading(false);
    }
  }

  // ── Rechazar ────────────────────────────────────────────────────────
  async function handleRechazar() {
    await handleAction(
      () => rechazarSolicitud(selected.numeroSolicitud, motivoRechazo.trim()),
      'Solicitud rechazada correctamente.'
    );
  }

  // ── Derivados ─────────────────────────────────────────────────────
  const solicitudes   = pageData?.content ?? [];
  const totalPages    = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;
  const from          = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const to            = Math.min(page * PAGE_SIZE + solicitudes.length, totalElements);

  const esEstadoFinal = selected?.estado === 'APROBADA' || selected?.estado === 'RECHAZADA';

  // ── Render ─────────────────────────────────────────────────────────
  return (
    <div className="page fade-in">

      {/* Header */}
      <div className="page-header">
        <h1>Solicitudes de Empresa</h1>
        <p>Revisa, aprueba o rechaza las solicitudes de verificación académica enviadas por empresas.</p>
      </div>

      {/* Toast */}
      {toast && (
        <div className={`as-toast alert alert-${toast.type === 'success' ? 'success' : 'error'}`}>
          {toast.type === 'success' ? '✓' : '⚠️'} {toast.msg}
        </div>
      )}

      {/* Card: filtro + tabla + paginación */}
      <div className="card">

        <div className="as-toolbar">
          <div className="as-filter">
            <label htmlFor="filtro-estado">Estado</label>
            <select
              id="filtro-estado"
              className="form-select"
              style={{ width: 'auto', minWidth: 160 }}
              value={estadoFiltro}
              onChange={(e) => setEstadoFiltro(e.target.value)}
            >
              <option value="">Todos</option>
              {ESTADOS.map((e) => (
                <option key={e} value={e}>{e.replace('_', ' ')}</option>
              ))}
            </select>
          </div>
          {!loading && pageData && (
            <span className="badge badge-pending">
              {totalElements > 0 ? `${from}–${to} de ${totalElements}` : '0 solicitudes'}
            </span>
          )}
        </div>

        {loading && (
          <div className="empty">
            <div className="spinner" style={{ width: 32, height: 32, borderWidth: 3 }} />
            <p style={{ marginTop: 16 }}>Cargando...</p>
          </div>
        )}

        {!loading && solicitudes.length === 0 && (
          <div className="empty">
            <div className="empty-icon">📭</div>
            <p>
              No hay solicitudes
              {estadoFiltro ? ` con estado ${estadoFiltro.replace('_', ' ')}` : ''} aún.
            </p>
          </div>
        )}

        {!loading && solicitudes.length > 0 && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>N.° Radicado</th>
                    <th>Empresa</th>
                    <th>Estudiante</th>
                    <th>Tipo</th>
                    <th>Estado</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {solicitudes.map((s) => (
                    <tr key={s.id} className="row-clickable" onClick={() => openModal(s)}>
                      <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{s.numeroSolicitud}</td>
                      <td>{s.nombreEmpresa}</td>
                      <td>{s.nombreEstudiante}</td>
                      <td>{s.tipoValidacion}</td>
                      <td>
                        <span className={badgeClass(s.estado)}>
                          {s.estado.replace('_', ' ')}
                        </span>
                      </td>
                      <td style={{ fontSize: 13, color: 'var(--text-muted)' }}>{fmt(s.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 20 }}>
                <button
                  className="btn btn-outline"
                  onClick={() => loadPage(0)}
                  disabled={page === 0}
                  style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                >
                  «
                </button>
                <button
                  className="btn btn-outline"
                  onClick={() => loadPage(page - 1)}
                  disabled={page === 0}
                  style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                >
                  ‹ Anterior
                </button>
                <span style={{ fontSize: '0.9rem', color: '#555', padding: '0 8px' }}>
                  Página <strong>{page + 1}</strong> de <strong>{totalPages}</strong>
                </span>
                <button
                  className="btn btn-outline"
                  onClick={() => loadPage(page + 1)}
                  disabled={page >= totalPages - 1}
                  style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                >
                  Siguiente ›
                </button>
                <button
                  className="btn btn-outline"
                  onClick={() => loadPage(totalPages - 1)}
                  disabled={page >= totalPages - 1}
                  style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                >
                  »
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Modal de detalle */}
      {selected && (
        <div
          className="modal-overlay"
          onClick={(e) => { if (e.target === e.currentTarget && !actionLoading) closeModal(); }}
          role="dialog"
          aria-modal="true"
          aria-label={`Detalle solicitud ${selected.numeroSolicitud}`}
        >
          <div className="modal-box">

            {/* Header del modal */}
            <div className="modal-header">
              <div className="modal-header-left">
                <span className="modal-numero">{selected.numeroSolicitud}</span>
                <span className={badgeClass(selected.estado)}>
                  {selected.estado.replace('_', ' ')}
                </span>
              </div>
              <button
                className="modal-close"
                onClick={closeModal}
                disabled={actionLoading}
                aria-label="Cerrar"
              >
                ✕
              </button>
            </div>

            {/* Cuerpo del modal */}
            <div className="modal-body">

              <p className="modal-section-title">Empresa</p>
              <div className="modal-grid">
                <Field label="Nombre empresa" value={selected.nombreEmpresa} />
                <Field label="NIT" value={selected.nitEmpresa} />
                <Field label="Contacto" value={selected.nombreContacto} />
                <Field label="Cargo" value={selected.cargoContacto} />
                <Field label="Correo contacto" value={selected.correoContacto} />
              </div>

              <p className="modal-section-title">Estudiante</p>
              <div className="modal-grid">
                <Field label="Nombre" value={selected.nombreEstudiante} />
                <Field
                  label={`Documento (${selected.tipoDocumentoEstudiante})`}
                  value={selected.documentoEstudiante}
                />
                <Field label="Tipo validación" value={selected.tipoValidacion} />
              </div>

              <p className="modal-section-title">Revisión</p>
              <div className="modal-grid">
                <Field label="Fecha solicitud" value={fmt(selected.createdAt)} />
                <Field label="Fecha revisión" value={fmt(selected.fechaRevision)} />
                <Field label="Admin responsable" value={selected.adminResponsable} />
                {selected.comentarioAdmin && (
                  <div className="modal-field" style={{ gridColumn: '1 / -1' }}>
                    <span className="modal-field-label">Comentario admin</span>
                    <span className="modal-field-value">{selected.comentarioAdmin}</span>
                  </div>
                )}
              </div>

              <div style={{ marginTop: 20 }}>
                <button
                  className="btn-download"
                  onClick={handleDescarga}
                  disabled={actionLoading}
                >
                  {actionLoading
                    ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                    : '⬇'
                  }
                  Descargar carta PDF
                </button>
              </div>
            </div>

            {/* Footer: botones contextuales según estado */}
            {!esEstadoFinal && (
              <div className="modal-footer">

                {!showRechazar && (
                  <div className="modal-actions">
                    {selected.estado === 'PENDIENTE' && (
                      <button
                        className="btn btn-review"
                        disabled={actionLoading}
                        onClick={() =>
                          handleAction(
                            () => marcarEnRevision(selected.numeroSolicitud),
                            'Solicitud marcada en revisión.'
                          )
                        }
                      >
                        Marcar en revisión
                      </button>
                    )}
                    <button
                      className="btn btn-success"
                      disabled={actionLoading}
                      onClick={() =>
                        handleAction(
                          () => aprobarSolicitud(selected.numeroSolicitud),
                          'Solicitud aprobada correctamente.'
                        )
                      }
                    >
                      Aprobar
                    </button>
                    <button
                      className="btn btn-danger"
                      disabled={actionLoading}
                      onClick={() => setShowRechazar(true)}
                    >
                      Rechazar
                    </button>
                    {actionLoading && (
                      <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
                    )}
                  </div>
                )}

                {showRechazar && (
                  <div className="rechazo-form">
                    <textarea
                      placeholder="Motivo del rechazo (obligatorio)"
                      value={motivoRechazo}
                      onChange={(e) => setMotivoRechazo(e.target.value)}
                      disabled={actionLoading}
                    />
                    <div className="rechazo-actions">
                      <button
                        className="btn btn-outline"
                        onClick={() => { setShowRechazar(false); setMotivoRechazo(''); }}
                        disabled={actionLoading}
                      >
                        Cancelar
                      </button>
                      <button
                        className="btn btn-danger"
                        onClick={handleRechazar}
                        disabled={actionLoading || !motivoRechazo.trim()}
                      >
                        {actionLoading
                          ? <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                          : 'Confirmar rechazo'
                        }
                      </button>
                    </div>
                  </div>
                )}

              </div>
            )}

          </div>
        </div>
      )}

    </div>
  );
}
