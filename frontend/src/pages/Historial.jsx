import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { listarEstudiantes, buscarEstudiantePorDocumento, extractErrorMessage } from '../api/api';

const PAGE_SIZE = 20;

export default function Historial() {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null); // { content, totalElements, totalPages, currentPage, pageSize }
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [search, setSearch] = useState('');
  const [searching, setSearching] = useState(false);
  const [searchResult, setSearchResult] = useState(null);
  const [searchError, setSearchError] = useState(null);

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const loadPage = useCallback((pageNum) => {
    setLoading(true);
    setError(null);
    listarEstudiantes(pageNum, PAGE_SIZE)
      .then((res) => { setPageData(res.data); setPage(pageNum); })
      .catch((err) => {
        if (!err.response || err.response.status !== 401) {
          setError(extractErrorMessage(err, 'No se pudo cargar el historial. Verifique que el servidor esté activo.'));
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!localStorage.getItem('token')) { navigate('/login'); return; }
    loadPage(0);
  }, []);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!search.trim()) return;
    setSearching(true);
    setSearchResult(null);
    setSearchError(null);
    try {
      const res = await buscarEstudiantePorDocumento(search.trim());
      setSearchResult(res.data);
    } catch (err) {
      if (err.response?.status === 404) {
        setSearchError('No se encontró estudiante con ese documento.');
      } else if (!err.response || err.response.status !== 401) {
        setSearchError(extractErrorMessage(err, 'Error al buscar. Verifique que el servidor esté activo.'));
      }
    } finally {
      setSearching(false);
    }
  };

  const clearSearch = () => { setSearch(''); setSearchResult(null); setSearchError(null); };

  const estudiantes = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;
  const from = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const to = Math.min(page * PAGE_SIZE + estudiantes.length, totalElements);

  return (
    <div className="page fade-in">
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1>📊 Historial de Estudiantes</h1>
          <p>Consulta todos los estudiantes registrados o busca por documento.</p>
        </div>
        <button className="btn btn-outline" onClick={handleLogout} style={{ marginTop: 4 }}>
          Cerrar sesión
        </button>
      </div>

      {/* BUSCAR */}
      <div className="card" style={{ marginBottom: 24 }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: 10 }}>
          <input
            className="form-input"
            placeholder="Buscar por número de documento..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setSearchError(null); }}
            style={{ flex: 1 }}
          />
          <button type="submit" className="btn btn-primary" disabled={searching || !search.trim()}>
            {searching ? <span className="spinner" /> : '🔍 Buscar'}
          </button>
          {(searchResult || searchError) && (
            <button type="button" className="btn btn-outline" onClick={clearSearch}>✕</button>
          )}
        </form>

        {searchError && (
          <div className="alert alert-error" style={{ marginTop: 12 }}>⚠️ {searchError}</div>
        )}

        {searchResult && (
          <div className="result-card fade-in" style={{ marginTop: 16 }}>
            <div style={{ marginBottom: 12 }}>
              <strong style={{ fontFamily: 'var(--font-display)' }}>Estudiante encontrado</strong>
            </div>
            {Object.entries(searchResult).map(([key, val]) =>
              val && (
                <div className="result-row" key={key}>
                  <span className="result-label">{key}</span>
                  <span className="result-value">{String(val)}</span>
                </div>
              )
            )}
          </div>
        )}
      </div>

      {/* TABLA */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <strong style={{ fontFamily: 'var(--font-display)', fontSize: '1rem' }}>
            Todos los estudiantes
          </strong>
          {!loading && pageData && (
            <span className="badge badge-pending">
              {totalElements > 0 ? `${from}–${to} de ${totalElements}` : '0 registros'}
            </span>
          )}
        </div>

        {loading && (
          <div className="empty">
            <div className="spinner" style={{ width: 32, height: 32, borderWidth: 3 }} />
            <p style={{ marginTop: 16 }}>Cargando...</p>
          </div>
        )}

        {error && <div className="alert alert-error">{error}</div>}

        {!loading && !error && estudiantes.length === 0 && (
          <div className="empty">
            <div className="empty-icon">📭</div>
            <p>No hay estudiantes registrados aún.</p>
          </div>
        )}

        {!loading && estudiantes.length > 0 && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Documento</th>
                    <th>Programa</th>
                    <th>Nivel</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {estudiantes.map((e, i) => (
                    <tr key={e.id || i}>
                      <td>{e.fullName || '—'}</td>
                      <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{e.document || '—'}</td>
                      <td>{e.program || '—'}</td>
                      <td>{e.academicLevel || '—'}</td>
                      <td>
                        <span className={`badge ${
                          e.status === 'ACTIVO' ? 'badge-valid' :
                          e.status === 'GRADUADO' ? 'badge-pending' :
                          'badge-invalid'
                        }`}>
                          {e.status || 'N/A'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* PAGINACIÓN */}
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
    </div>
  );
}
