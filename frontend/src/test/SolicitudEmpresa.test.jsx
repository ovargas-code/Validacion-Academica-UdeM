import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SolicitudEmpresa from '../pages/SolicitudEmpresa';
import { crearSolicitudEmpresa } from '../api/api';

vi.mock('../api/api', () => ({
  crearSolicitudEmpresa: vi.fn(),
  descargarCertificadoPDF: vi.fn((code) => `http://localhost:8080/api/v1/verificaciones/${code}/pdf`),
  extractErrorMessage: vi.fn((err, fallback) => fallback || 'Error'),
  resolverUrlBackend: vi.fn((pathOrUrl) =>
    pathOrUrl?.startsWith('http') ? pathOrUrl : `http://localhost:8080${pathOrUrl}`
  ),
}));

async function fillRequiredTextFields(user, { email = 'contacto@empresa.com', observaciones = 'Validacion laboral' } = {}) {
  await user.type(screen.getByPlaceholderText('Ej: Empresa S.A.S.'), 'Empresa S.A.S.');
  await user.type(screen.getByPlaceholderText('Ej: 890123456-1'), '890123456-1');
  await user.type(screen.getByPlaceholderText('Nombre completo'), 'Ana Gomez');
  await user.type(screen.getByPlaceholderText('Ej: Jefe de Talento Humano'), 'Talento Humano');
  await user.type(screen.getByPlaceholderText('contacto@empresa.com'), email);
  await user.type(screen.getByPlaceholderText('Ej: +57 604 3456789'), '+57 604 3456789');
  await user.type(screen.getByPlaceholderText('Número de documento'), '123456789');
  await user.type(screen.getByPlaceholderText('Nombre y apellidos del estudiante o egresado'), 'Juan Perez');
  await user.type(screen.getByPlaceholderText('Ej: Ingeniería de Sistemas'), 'Ingenieria de Sistemas');
  if (observaciones) {
    await user.type(screen.getByPlaceholderText('Información adicional relevante para la verificación'), observaciones);
  }
}

async function acceptPolicy(user) {
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
    render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user, { email: 'correo-invalido' });
    await acceptPolicy(user);

    expect(screen.getByRole('button', { name: /enviar solicitud/i })).toBeDisabled();
  });

  it('mantiene deshabilitado el envio sin aceptar tratamiento de datos', async () => {
    const user = userEvent.setup();
    render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user);

    expect(screen.getByRole('button', { name: /enviar solicitud/i })).toBeDisabled();
  });

  it('habilita y envia cuando todos los campos visibles estan completos', async () => {
    const user = userEvent.setup();
    render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user);
    await acceptPolicy(user);

    const submit = screen.getByRole('button', { name: /enviar solicitud/i });
    expect(submit).toBeEnabled();

    await user.click(submit);

    expect(crearSolicitudEmpresa).toHaveBeenCalledTimes(1);
    const formData = crearSolicitudEmpresa.mock.calls[0][0];
    expect(formData.has('datos')).toBe(true);
    expect(formData.has('carta')).toBe(false);
  });

  it('permite enviar con observaciones adicionales vacias', async () => {
    const user = userEvent.setup();
    render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user, { observaciones: '' });
    await acceptPolicy(user);

    const submit = screen.getByRole('button', { name: /enviar solicitud/i });
    expect(submit).toBeEnabled();

    await user.click(submit);

    expect(crearSolicitudEmpresa).toHaveBeenCalledTimes(1);
  });

  it('muestra descarga del certificado final cuando el backend devuelve codigo', async () => {
    crearSolicitudEmpresa.mockResolvedValueOnce({
      data: {
        numeroSolicitud: 'SOL-001',
        estado: 'APROBADA',
        correoContacto: 'contacto@empresa.com',
        codigoVerificacion: 'UDEM-ABC123',
      },
    });
    const user = userEvent.setup();
    render(<SolicitudEmpresa />);

    await fillRequiredTextFields(user);
    await acceptPolicy(user);
    await user.click(screen.getByRole('button', { name: /enviar solicitud/i }));

    const link = await screen.findByRole('link', { name: /descargar certificado/i });
    expect(link).toHaveAttribute('href', 'http://localhost:8080/api/v1/verificaciones/UDEM-ABC123/pdf');
  });
});
