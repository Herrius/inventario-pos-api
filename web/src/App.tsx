import { BrowserRouter, Routes, Route, Navigate, Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { api, isAuthed, setToken } from "./api";
import type { User } from "./api";
import { Login } from "./pages/Login";
import { Products } from "./pages/Products";
import { Pos } from "./pages/Pos";
import { Daily } from "./pages/Daily";
import { Movements } from "./pages/Movements";
import { Admin } from "./pages/Admin";
import "./App.css";

function Layout({ children }: { children: React.ReactNode }) {
  const [me, setMe] = useState<User | null>(null);
  const nav = useNavigate();
  useEffect(() => {
    if (isAuthed()) api.me().then(setMe).catch(() => logout());
  }, []);
  function logout() {
    setToken(null);
    nav("/login");
  }
  return (
    <div>
      <nav className="navbar">
        <span className="brand">Inventario · POS</span>
        <Link to="/">Productos</Link>
        <Link to="/pos">POS</Link>
        <Link to="/daily">Resumen del día</Link>
        <Link to="/movements">Movimientos</Link>
        {me?.role === "ADMIN" && <Link to="/admin">Admin</Link>}
        <span className="spacer" />
        {me && <span className="user">{me.email} · {me.role}</span>}
        <button className="logout" onClick={logout}>Salir</button>
      </nav>
      <main className="main">{children}</main>
    </div>
  );
}

function RequireAuth({ children }: { children: React.ReactNode }) {
  return isAuthed() ? <Layout>{children}</Layout> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/"          element={<RequireAuth><Products  /></RequireAuth>} />
        <Route path="/pos"       element={<RequireAuth><Pos       /></RequireAuth>} />
        <Route path="/daily"     element={<RequireAuth><Daily     /></RequireAuth>} />
        <Route path="/movements" element={<RequireAuth><Movements /></RequireAuth>} />
        <Route path="/admin"     element={<RequireAuth><Admin     /></RequireAuth>} />
      </Routes>
    </BrowserRouter>
  );
}
