import { useEffect, useState } from "react";
import { api } from "../api";
import type { DailySummary } from "../api";

export function Daily() {
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [summary, setSummary] = useState<DailySummary | null>(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try { setSummary(await api.dailySummary(date)); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, [date]);

  return (
    <div>
      <h1>Resumen del día</h1>
      <div className="row">
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>
      {loading || !summary ? <p className="muted">Cargando...</p> : (
        <div className="cards">
          <div className="card">
            <div className="label">Ventas</div>
            <div className="value">{summary.totalVentas}</div>
          </div>
          <div className="card">
            <div className="label">Facturado</div>
            <div className="value">S/ {Number(summary.revenue).toFixed(2)}</div>
          </div>
          <div className="card">
            <div className="label">Ticket promedio</div>
            <div className="value">S/ {Number(summary.ticketPromedio).toFixed(2)}</div>
          </div>
        </div>
      )}
    </div>
  );
}
