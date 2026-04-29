import axios from 'axios';

const BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Adjunta el token JWT a todas las peticiones que lo necesiten
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor de respuesta global
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response, config } = error;

    // Sin respuesta del servidor — error de red o timeout
    if (!response) {
      error.friendlyMessage = 'No se pudo conectar al servidor. Verifica tu conexión o que el backend esté activo.';
      return Promise.reject(error);
    }

    // 401 con sesión activa → sesión expirada, redirigir al login
    if (response.status === 401 && localStorage.getItem('token')) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // 429 → demasiadas solicitudes, adjuntar mensaje con tiempo de espera
    if (response.status === 429) {
      const retry = response.headers?.['x-rate-limit-retry-after-seconds'];
      error.friendlyMessage = retry
        ? `Demasiadas solicitudes. Espera ${retry} segundos antes de intentarlo de nuevo.`
        : 'Demasiadas solicitudes. Espera un momento antes de intentarlo de nuevo.';
      return Promise.reject(error);
    }

    // 5xx → error interno del servidor
    if (response.status >= 500) {
      error.friendlyMessage = 'Error interno del servidor. Intenta más tarde.';
      return Promise.reject(error);
    }

    // Respuestas blob con error (ej: /confirm con OTP incorrecto) — dejar que el componente las maneje
    if (config?.responseType === 'blob') {
      return Promise.reject(error);
    }

    return Promise.reject(error);
  }
);

/**
 * Extrae un mensaje de error legible a partir de cualquier error de Axios.
 * Prioriza: mensaje amigable del interceptor → mensaje del backend → fallback.
 */
export function extractErrorMessage(err, fallback = 'Error al procesar la solicitud. Inténtalo de nuevo.') {
  if (err?.friendlyMessage) return err.friendlyMessage;
  if (!err?.response) return 'No se pudo conectar al servidor. Verifica tu conexión.';
  return err.response?.data?.message || fallback;
}

// Autenticación
export const login = (username, password) =>
  api.post('/api/auth/login', { username, password });

// Validaciones (públicas — rate limited)
export const verificarYGenerarCertificado = (data) =>
  api.post('/api/validations/verify', data);

// Verificaciones (públicas)
export const verificarCertificado = (code) =>
  api.get(`/api/v1/verificaciones/${code}`);

export const descargarCertificadoPDF = (code) =>
  `${BASE_URL}/api/v1/verificaciones/${code}/pdf`;

export const resolverUrlBackend = (pathOrUrl) => {
  if (!pathOrUrl) return null;
  if (/^https?:\/\//i.test(pathOrUrl) || pathOrUrl.startsWith('data:')) return pathOrUrl;
  const path = pathOrUrl.startsWith('/') ? pathOrUrl : `/${pathOrUrl}`;
  return `${BASE_URL}${path}`;
};

// Estudiantes (requieren ROLE_ADMIN + JWT)
export const listarEstudiantes = (page = 0, size = 20) =>
  api.get('/api/v1/students', { params: { page, size } });

export const registrarEstudiante = (data) =>
  api.post('/api/v1/students', data);

export const buscarEstudiantePorDocumento = (document) =>
  api.get(`/api/v1/students/${document}`);

// Flujo de verificación por correo (nuevo)
export const iniciarValidacion = (data) =>
  api.post('/api/validations/initiate', data);

export const confirmarVerificacion = (token, code) =>
  api.post('/api/validations/confirm', { token, code }, { responseType: 'blob' });

// Solicitudes de empresa (públicas — rate limited)

/**
 * Envía el formulario de solicitud de verificación académica por empresa.
 * El argumento debe ser un FormData con la parte "datos" como Blob JSON.
 *
 * Se pasa Content-Type undefined para que axios no sobreescriba el
 * multipart/form-data con boundary que el navegador construye automáticamente.
 */
export const crearSolicitudEmpresa = (formData) =>
  api.post('/api/v1/solicitudes-empresa', formData, {
    headers: { 'Content-Type': undefined },
  });

export const consultarSolicitud = (numero) =>
  api.get(`/api/v1/solicitudes-empresa/${numero}`);

/** URL directa para descarga de la plantilla .docx (enlace <a>, no llamada axios). */
export const urlPlantillaCarta = () =>
  `${BASE_URL}/api/v1/solicitudes-empresa/recursos/plantilla-carta`;

// Solicitudes empresa — Admin (requieren ROLE_ADMIN + JWT)

export const listarSolicitudesAdmin = (page = 0, size = 20, estado = null) => {
  const params = { page, size };
  if (estado) params.estado = estado;
  return api.get('/api/v1/admin/solicitudes-empresa', { params });
};

export const descargarCertificadoFinalSolicitud = (numero) =>
  api.get(`/api/v1/admin/solicitudes-empresa/${numero}/certificado-final`, {
    responseType: 'blob',
  });

export const marcarEnRevision = (numero, comentarioAdmin = null) =>
  api.post(
    `/api/v1/admin/solicitudes-empresa/${numero}/marcar-en-revision`,
    { comentarioAdmin }
  );

export const aprobarSolicitud = (numero, comentarioAdmin = null) =>
  api.post(
    `/api/v1/admin/solicitudes-empresa/${numero}/aprobar`,
    { comentarioAdmin }
  );

export const rechazarSolicitud = (numero, comentarioAdmin) =>
  api.post(
    `/api/v1/admin/solicitudes-empresa/${numero}/rechazar`,
    { comentarioAdmin }
  );
