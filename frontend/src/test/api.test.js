import { describe, it, expect } from 'vitest';
import { extractErrorMessage } from '../api/api';

describe('extractErrorMessage', () => {
  it('retorna el mensaje amigable del interceptor si existe', () => {
    const err = { friendlyMessage: 'Demasiadas solicitudes. Espera 30 segundos.' };
    expect(extractErrorMessage(err)).toBe('Demasiadas solicitudes. Espera 30 segundos.');
  });

  it('retorna error de conexión si no hay respuesta del servidor', () => {
    const err = {};
    expect(extractErrorMessage(err)).toBe('No se pudo conectar al servidor. Verifica tu conexión.');
  });

  it('retorna message del backend si existe en response.data', () => {
    const err = { response: { data: { message: 'Documento ya registrado' } } };
    expect(extractErrorMessage(err)).toBe('Documento ya registrado');
  });

  it('retorna el fallback si response.data no tiene message', () => {
    const err = { response: { data: {} } };
    expect(extractErrorMessage(err, 'Fallback personalizado')).toBe('Fallback personalizado');
  });

  it('usa el fallback por defecto si no se proporciona uno', () => {
    const err = { response: { data: {} } };
    expect(extractErrorMessage(err)).toBe('Error al procesar la solicitud. Inténtalo de nuevo.');
  });
});
