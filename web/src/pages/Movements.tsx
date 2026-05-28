import { useEffect, useState } from "react";
import { api } from "../api";
import type { StockMovement } from "../api";

export function Movements() {
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    api.movements().then((r) => setMovements(r.content)).finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1>Movimientos de inventario</h1>
      <p className="muted">
        Log append-only: cada entrada, venta y ajuste deja un evento inmutable.
      </p>
      {loading ? <p className="muted">Cargando...</p> : (
        <table>
          <thead>
            <tr>
              <th>Fecha</th><th>Producto</th><th>Tipo</th><th>Delta</th><th>Razón</th><th>Usuario</th>
            </tr>
          </thead>
          <tbody>
            {movements.map((m) => (
              <tr key={m.id}>
                <td className="muted">{new Date(m.createdAt).toLocaleString()}</td>
                <td>{m.productSku} · {m.productNombre}</td>
                <td><span className={`tag ${m.tipo}`}>{m.tipo}</span></td>
                <td>{m.deltaConSigno > 0 ? `+${m.deltaConSigno}` : m.deltaConSigno}</td>
                <td className="muted">{m.razon || "—"}</td>
                <td className="muted">{m.userEmail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
