import axios from 'axios';

const BASE_URL = '';

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
