import axios from 'axios';

const BASE_URL = '';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Validaciones
export const verificarYGenerarCertificado = (data) =>
  api.post('/api/validations/verify', data);

// Verificaciones
export const verificarCertificado = (code) =>
  api.get(`/api/v1/verificaciones/${code}`);

export const descargarCertificadoPDF = (code) =>
  `${BASE_URL}/api/v1/verificaciones/${code}/pdf`;

// Estudiantes
export const listarEstudiantes = () =>
  api.get('/api/v1/students');

export const registrarEstudiante = (data) =>
  api.post('/api/v1/students', data);

export const buscarEstudiantePorDocumento = (document) =>
  api.get(`/api/v1/students/${document}`);
