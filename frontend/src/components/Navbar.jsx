import { NavLink } from 'react-router-dom';
import './Navbar.css';

export default function Navbar() {
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
        </div>
      </div>
    </nav>
  );
}
