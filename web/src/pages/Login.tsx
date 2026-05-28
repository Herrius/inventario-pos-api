import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, setToken } from "../api";

export function Login() {
  const [email, setEmail] = useState("admin@demo.com");
  const [password, setPassword] = useState("admin123");
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const nav = useNavigate();

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      const resp = await api.login(email, password);
      setToken(resp.accessToken);
      nav("/");
    } catch (e: any) {
      setErr(e.message || "Login fallido");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="login" onSubmit={submit}>
      <h1>Inventario / POS</h1>
      <p className="hint">
        Demo: <code>admin@demo.com</code> / <code>admin123</code><br />
        o <code>cajero@demo.com</code> / <code>cajero123</code>
      </p>
      {err && <div className="error">{err}</div>}
      <div className="row">
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="email" />
      </div>
      <div className="row">
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="password" />
      </div>
      <button type="submit" disabled={loading} style={{ width: "100%" }}>
        {loading ? "Entrando..." : "Entrar"}
      </button>
    </form>
  );
}
