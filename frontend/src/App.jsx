import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Validar from './pages/Validar';
import Verificar from './pages/Verificar';
import Historial from './pages/Historial';
import Login from './pages/Login';
import './index.css';

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/validar" element={<Validar />} />
        <Route path="/verificar" element={<Verificar />} />
        <Route path="/historial" element={<Historial />} />
        <Route path="/login" element={<Login />} />
      </Routes>
    </BrowserRouter>
  );
}
