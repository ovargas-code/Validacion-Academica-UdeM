import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SolicitudEmpresa from '../pages/SolicitudEmpresa';
import { crearSolicitudEmpresa } from '../api/api';

vi.mock('../api/api', () => ({
  crearSolicitudEmpresa: vi.fn(),
  urlPlantillaCarta: vi.fn(() => '/plantilla-carta'),
  extractErrorMessage: vi.fn((err, fallback) => fallback || 'Error'),
}));

async function fillRequiredTextFields(user, { email = 'contacto@empresa.com' } = {}) {
  await user.type(screen.getByPlaceholderText('Ej: Empresa S.A.S.'), 'Empresa S.A.S.');
  await user.type(screen.getByPlaceholderText('Ej: 890123456-1'), '890123456-1');
  await user.type(screen.getByPlaceholderText('Nombre completo'), 'Ana Gomez');
  await user.type(screen.getByPlaceholderText('Ej: Jefe de Talento Humano'), 'Talento Humano');
  await user.type(screen.getByPlaceholderText('contacto@empresa.com'), email);
  await user.type(screen.getByPlaceholderText('Ej: +57 604 3456789'), '+57 604 3456789');
  await user.type(screen.getByPlaceholderText('Número de documento'), '123456789');
  await user.type(screen.getByPlaceholderText('Nombre y apellidos del estudiante o egresado'), 'Juan Perez');
  await user.type(screen.getByPlaceholderText('Ej: Ingeniería de Sistemas'), 'Ingenieria de Sistemas');
  await user.type(screen.getByPlaceholderText('Información adicional relevante para la verificación'), 'Validacion laboral');
}

async function uploadCartaAndAcceptPolicy(user, container) {
  const file = new File(['contenido'], 'carta.pdf', { type: 'application/pdf' });
  await user.upload(container.querySelector('#carta-input'), file);
  await user.click(screen.getByRole('checkbox'));
}

describe('SolicitudEmpresa', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    crearSolicitudEmpresa.mockResolvedValue({
      data: {
        numeroSolicitud: 'SOL-001',
        estado: 'PENDIENTE',
        correoContacto: 'contacto@empresa.com',
      },
    });
  });

  it('mantiene deshabilitado el envio cuando faltan campos obligatorios', () => {
    render(<SolicitudEmpresa />);

    expect(screen.getByRole('button', { name: /enviar solicitud/i })).toBeDisabled();
  });

  it('mantiene deshabilitado el envio con correo invalido', async () => {
    const user = userEvent.setup();
    const { container } = render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user, { email: 'correo-invalido' });
    await uploadCartaAndAcceptPolicy(user, container);

    expect(screen.getByRole('button', { name: /enviar solicitud/i })).toBeDisabled();
  });

  it('habilita y envia cuando todos los campos visibles estan completos', async () => {
    const user = userEvent.setup();
    const { container } = render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user);
    await uploadCartaAndAcceptPolicy(user, container);

    const submit = screen.getByRole('button', { name: /enviar solicitud/i });
    expect(submit).toBeEnabled();

    await user.click(submit);

    expect(crearSolicitudEmpresa).toHaveBeenCalledTimes(1);
  });
});
