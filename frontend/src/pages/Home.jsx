import { Link } from 'react-router-dom';
import './Home.css';

export default function Home() {
  return (
    <div className="home">
      <div className="home-hero">
        <div className="hero-cabezote">
          <img
            src="/cabezote_rojo.png"
            alt="Universidad de Medellín"
            className="cabezote-img"
          />
        </div>
        <div className="hero-content fade-in">
          <div className="hero-badge">Sistema de Certificación Académica</div>
          <h1 className="hero-title">
            Validación<br />
            <span className="hero-accent">Académica UdeM</span>
          </h1>
          <p className="hero-desc">
            Verifica la autenticidad de certificados académicos, solicita validaciones
            y descarga documentos oficiales en segundos.
          </p>
          <div className="hero-actions">
            <Link to="/validar" className="btn btn-primary">
              ✦ Solicitar Validación
            </Link>
            <Link to="/verificar" className="btn btn-outline">
              Verificar Certificado
            </Link>
          </div>
        </div>
      </div>

      <div className="home-cards page">
        <div className="feature-grid">
          <Link to="/validar" className="feature-card">
            <div className="feature-icon">📋</div>
            <h3>Solicitar Validación</h3>
            <p>Ingresa los datos del estudiante y recibe el certificado directamente en tu correo.</p>
            <span className="feature-link">Ir al formulario →</span>
          </Link>

          <Link to="/verificar" className="feature-card">
            <div className="feature-icon">🔍</div>
            <h3>Verificar Certificado</h3>
            <p>Consulta la autenticidad de un certificado usando su código único de verificación.</p>
            <span className="feature-link">Verificar ahora →</span>
          </Link>

          <Link to="/historial" className="feature-card">
            <div className="feature-icon">📊</div>
            <h3>Historial</h3>
            <p>Consulta todos los estudiantes registrados y el estado de sus validaciones.</p>
            <span className="feature-link">Ver historial →</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
