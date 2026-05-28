import { useEffect, useState } from "react";
import { api } from "../api";
import type { Product } from "../api";

export function Products() {
  const [products, setProducts] = useState<Product[]>([]);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const resp = await api.products(q || undefined);
      setProducts(resp.content);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => { load(); }, []);

  return (
    <div>
      <h1>Catálogo</h1>
      <div className="row">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && load()}
          placeholder="Buscar por nombre o SKU..."
        />
        <button onClick={load}>Buscar</button>
      </div>
      {loading ? <p className="muted">Cargando...</p> : (
        <table>
          <thead>
            <tr><th>SKU</th><th>Nombre</th><th>Categoría</th><th>Precio</th><th>Stock</th><th>Min</th></tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>{p.sku}</td>
                <td>{p.nombre}</td>
                <td className="muted">{p.categoryNombre}</td>
                <td>S/ {Number(p.precio).toFixed(2)}</td>
                <td className={p.stockActual < p.minStock ? "low" : ""}>{p.stockActual}</td>
                <td className="muted">{p.minStock}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
