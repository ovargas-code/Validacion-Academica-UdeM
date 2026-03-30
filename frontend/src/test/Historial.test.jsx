import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Historial from '../pages/Historial';

vi.mock('../api/api', () => ({
  listarEstudiantes: vi.fn(),
  buscarEstudiantePorDocumento: vi.fn(),
  extractErrorMessage: vi.fn((err, fallback) => fallback || 'Error'),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});

import { listarEstudiantes, buscarEstudiantePorDocumento } from '../api/api';

const PAGE_1 = {
  content: [
    { id: 1, document: '20240001', fullName: 'Laura Martínez', program: 'Ingeniería de Sistemas', academicLevel: 'PREGRADO', status: 'ACTIVO' },
    { id: 2, document: '20240003', fullName: 'Valentina Restrepo', program: 'Psicología', academicLevel: 'MAESTRIA', status: 'GRADUADO' },
    { id: 3, document: '20240005', fullName: 'Isabella Sánchez', program: 'Medicina', academicLevel: 'PREGRADO', status: 'INACTIVO' },
  ],
  totalElements: 3,
  totalPages: 1,
  currentPage: 0,
  pageSize: 20,
};

function renderHistorial() {
  return render(
    <MemoryRouter>
      <Historial />
    </MemoryRouter>
  );
}

describe('Historial', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('redirige a /login si no hay token en localStorage', () => {
    listarEstudiantes.mockResolvedValueOnce({ data: PAGE_1 });
    renderHistorial();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('muestra spinner mientras carga', () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockReturnValueOnce(new Promise(() => {})); // nunca resuelve
    renderHistorial();
    expect(screen.getByText(/cargando/i)).toBeInTheDocument();
  });

  it('renderiza la tabla con los estudiantes tras carga exitosa', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({ data: PAGE_1 });
    renderHistorial();

    await waitFor(() => {
      expect(screen.getByText('Laura Martínez')).toBeInTheDocument();
      expect(screen.getByText('Valentina Restrepo')).toBeInTheDocument();
      expect(screen.getByText('Isabella Sánchez')).toBeInTheDocument();
    });
  });

  it('muestra el estado de cada estudiante con badge correcto', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({ data: PAGE_1 });
    renderHistorial();

    await waitFor(() => screen.getByText('Laura Martínez'));

    const rows = screen.getAllByRole('row').slice(1); // omitir encabezado
    expect(within(rows[0]).getByText('ACTIVO')).toHaveClass('badge-valid');
    expect(within(rows[1]).getByText('GRADUADO')).toHaveClass('badge-pending');
    expect(within(rows[2]).getByText('INACTIVO')).toHaveClass('badge-invalid');
  });

  it('muestra el contador "1–3 de 3"', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({ data: PAGE_1 });
    renderHistorial();

    await waitFor(() => {
      expect(screen.getByText('1–3 de 3')).toBeInTheDocument();
    });
  });

  it('muestra estado vacío cuando no hay estudiantes', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({
      data: { content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20 },
    });
    renderHistorial();

    await waitFor(() => {
      expect(screen.getByText(/no hay estudiantes registrados/i)).toBeInTheDocument();
    });
  });

  it('muestra error cuando la API falla', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockRejectedValueOnce({ response: { status: 500 } });
    renderHistorial();

    await waitFor(() => {
      expect(screen.getByText(/no se pudo cargar el historial/i)).toBeInTheDocument();
    });
  });

  it('busca por documento y muestra el resultado', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({ data: PAGE_1 });
    buscarEstudiantePorDocumento.mockResolvedValueOnce({
      data: { document: '20240001', fullName: 'Laura Martínez', program: 'Ingeniería de Sistemas', academicLevel: 'PREGRADO', status: 'ACTIVO' },
    });

    const user = userEvent.setup();
    renderHistorial();
    await waitFor(() => screen.getByText('Laura Martínez'));

    await user.type(screen.getByPlaceholderText(/buscar por número de documento/i), '20240001');
    await user.click(screen.getByRole('button', { name: /buscar/i }));

    await waitFor(() => {
      expect(buscarEstudiantePorDocumento).toHaveBeenCalledWith('20240001');
      expect(screen.getByText('Estudiante encontrado')).toBeInTheDocument();
    });
  });

  it('muestra error 404 cuando no se encuentra el estudiante', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({ data: PAGE_1 });
    buscarEstudiantePorDocumento.mockRejectedValueOnce({ response: { status: 404 } });

    const user = userEvent.setup();
    renderHistorial();
    await waitFor(() => screen.getByText('Laura Martínez'));

    await user.type(screen.getByPlaceholderText(/buscar por número de documento/i), '99999');
    await user.click(screen.getByRole('button', { name: /buscar/i }));

    await waitFor(() => {
      expect(screen.getByText(/no se encontró estudiante/i)).toBeInTheDocument();
    });
  });

  it('renderiza controles de paginación en tablas multi-página', async () => {
    localStorage.setItem('token', 'tok');
    listarEstudiantes.mockResolvedValueOnce({
      data: { ...PAGE_1, totalElements: 45, totalPages: 3, pageSize: 20 },
    });
    renderHistorial();

    await waitFor(() => screen.getByText('Laura Martínez'));

    expect(screen.getByRole('button', { name: '«' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /siguiente/i })).toBeInTheDocument();
    expect(screen.getByText(/página/i)).toBeInTheDocument();
  });
});
