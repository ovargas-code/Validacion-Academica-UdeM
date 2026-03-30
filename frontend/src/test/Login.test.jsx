import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Login from '../pages/Login';

// Mock del módulo de API
vi.mock('../api/api', () => ({
  login: vi.fn(),
  extractErrorMessage: vi.fn((err, fallback) => err?.friendlyMessage || fallback || 'Error'),
}));

// Mock de useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});

import { login } from '../api/api';

function renderLogin() {
  return render(
    <MemoryRouter>
      <Login />
    </MemoryRouter>
  );
}

describe('Login', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renderiza los campos de usuario, contraseña y botón de submit', () => {
    renderLogin();
    expect(screen.getByLabelText(/usuario/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contraseña/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument();
  });

  it('el botón está deshabilitado cuando los campos están vacíos', () => {
    renderLogin();
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeDisabled();
  });

  it('guarda el token y navega a /historial tras login exitoso', async () => {
    login.mockResolvedValueOnce({ data: { token: 'jwt-token-123' } });
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/usuario/i), 'admin');
    await user.type(screen.getByLabelText(/contraseña/i), 'clave123');
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(localStorage.getItem('token')).toBe('jwt-token-123');
      expect(mockNavigate).toHaveBeenCalledWith('/historial');
    });
  });

  it('muestra error de credenciales incorrectas ante respuesta 401', async () => {
    login.mockRejectedValueOnce({ response: { status: 401 } });
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/usuario/i), 'admin');
    await user.type(screen.getByLabelText(/contraseña/i), 'mal');
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/credenciales incorrectas/i)).toBeInTheDocument();
    });
  });

  it('muestra error de conexión cuando el servidor no responde', async () => {
    login.mockRejectedValueOnce({ friendlyMessage: 'No se pudo conectar al servidor. Verifica tu conexión o que el backend esté activo.' });
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/usuario/i), 'admin');
    await user.type(screen.getByLabelText(/contraseña/i), 'clave');
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/no se pudo conectar/i)).toBeInTheDocument();
    });
  });

  it('llama a login() con las credenciales ingresadas', async () => {
    login.mockResolvedValueOnce({ data: { token: 'tok' } });
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/usuario/i), 'admin');
    await user.type(screen.getByLabelText(/contraseña/i), 'secret');
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith('admin', 'secret');
    });
  });
});
