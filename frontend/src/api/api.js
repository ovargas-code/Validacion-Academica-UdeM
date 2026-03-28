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
export const listarEstudiantes = () =>
  api.get('/api/v1/students');

export const registrarEstudiante = (data) =>
  api.post('/api/v1/students', data);

export const buscarEstudiantePorDocumento = (document) =>
  api.get(`/api/v1/students/${document}`);

// Flujo de verificación por correo (nuevo)
export const iniciarValidacion = (data) =>
  api.post('/api/validations/initiate', data);

export const confirmarVerificacion = (token, code) =>
  api.post('/api/validations/confirm', { token, code }, { responseType: 'blob' });
