import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, extractErrorMessage } from '../api/api';

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await login(username, password);
      const token = res.data?.token || res.data?.accessToken || res.data;
      if (!token || typeof token !== 'string') throw new Error('Token inválido');
      localStorage.setItem('token', token);
      navigate('/historial');
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        setError('Credenciales incorrectas. Verifique usuario y contraseña.');
      } else if (err.message === 'Token inválido') {
        setError('Respuesta inesperada del servidor.');
      } else {
        setError(extractErrorMessage(err, 'No se pudo conectar al servidor. Verifique que esté activo.'));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page fade-in" style={{ maxWidth: 440 }}>
      <div className="page-header">
        <h1>Acceso Administrador</h1>
        <p>Ingrese sus credenciales para ver el historial de estudiantes.</p>
      </div>

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Usuario</label>
            <input
              className="form-input"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Contraseña</label>
            <input
              className="form-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>

          {error && (
            <div className="alert alert-error" style={{ marginBottom: 20 }}>
              ⚠️ {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%' }}
            disabled={loading || !username.trim() || !password.trim()}
          >
            {loading ? <span className="spinner" /> : 'Iniciar sesión'}
          </button>
        </form>
      </div>
    </div>
  );
}
