import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminSolicitudes from '../pages/AdminSolicitudes';

vi.mock('../api/api', () => ({
  listarSolicitudesAdmin: vi.fn(),
  descargarCartaSolicitud: vi.fn(),
  marcarEnRevision: vi.fn(),
  aprobarSolicitud: vi.fn(),
  rechazarSolicitud: vi.fn(),
  extractErrorMessage: vi.fn((err, fallback) => fallback || 'Error'),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});

import { listarSolicitudesAdmin } from '../api/api';

const EMPTY_PAGE = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
  pageSize: 20,
};

function renderPage() {
  return render(
    <MemoryRouter>
      <AdminSolicitudes />
    </MemoryRouter>
  );
}

describe('AdminSolicitudes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('redirige a /login si no hay token', () => {
    listarSolicitudesAdmin.mockResolvedValueOnce({ data: EMPTY_PAGE });
    renderPage();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('muestra estado vacío cuando la API devuelve lista vacía', async () => {
    localStorage.setItem('token', 'tok');
    listarSolicitudesAdmin.mockResolvedValueOnce({ data: EMPTY_PAGE });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/no hay solicitudes/i)).toBeInTheDocument();
    });
  });
});
