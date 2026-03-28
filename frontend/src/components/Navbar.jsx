import { NavLink, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import './Navbar.css';

export default function Navbar() {
  const navigate = useNavigate();
  const [isAdmin, setIsAdmin] = useState(!!localStorage.getItem('token'));

  useEffect(() => {
    const onStorage = () => setIsAdmin(!!localStorage.getItem('token'));
    window.addEventListener('storage', onStorage);
    // También detectar cambios dentro de la misma pestaña
    const interval = setInterval(() => setIsAdmin(!!localStorage.getItem('token')), 500);
    return () => { window.removeEventListener('storage', onStorage); clearInterval(interval); };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAdmin(false);
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/" className="navbar-brand">
          <img
            src="/logo_udem.png"
            alt="Universidad de Medellín"
            className="navbar-logo"
          />
        </NavLink>
        <div className="navbar-links">
          <NavLink to="/validar" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            Solicitar
          </NavLink>
          <NavLink to="/verificar" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            Verificar
          </NavLink>
          <NavLink to="/historial" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            Historial
          </NavLink>
          {isAdmin ? (
            <button className="nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }} onClick={handleLogout}>
              Salir
            </button>
          ) : (
            <NavLink to="/login" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
              Admin
            </NavLink>
          )}
        </div>
      </div>
    </nav>
  );
}
