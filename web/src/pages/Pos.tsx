import { useEffect, useState } from "react";
import { api } from "../api";
import type { Product, SaleResponse } from "../api";

interface CartLine { product: Product; cantidad: number; }

export function Pos() {
  const [catalog, setCatalog] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [result, setResult] = useState<SaleResponse | null>(null);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => { api.products().then((r) => setCatalog(r.content)); }, []);

  function add(p: Product) {
    setCart((c) => {
      const found = c.find((l) => l.product.id === p.id);
      if (found) return c.map((l) => l.product.id === p.id ? { ...l, cantidad: l.cantidad + 1 } : l);
      return [...c, { product: p, cantidad: 1 }];
    });
    setResult(null); setErr(null);
  }
  function remove(id: number) { setCart((c) => c.filter((l) => l.product.id !== id)); }
  function setQty(id: number, cantidad: number) {
    if (cantidad < 1) return remove(id);
    setCart((c) => c.map((l) => l.product.id === id ? { ...l, cantidad } : l));
  }

  const total = cart.reduce((s, l) => s + l.product.precio * l.cantidad, 0);

  async function confirm() {
    setErr(null);
    try {
      const resp = await api.createSale(cart.map((l) => ({ productId: l.product.id, cantidad: l.cantidad })));
      setResult(resp);
      setCart([]);
      // Refrescar catálogo (stock cambió)
      const r = await api.products();
      setCatalog(r.content);
    } catch (e: any) {
      setErr(e.message || "Error registrando venta");
    }
  }

  return (
    <div>
      <h1>Punto de venta</h1>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
        <div>
          <h2>Catálogo</h2>
          <table>
            <thead><tr><th>SKU</th><th>Producto</th><th>Stock</th><th>Precio</th><th></th></tr></thead>
            <tbody>
              {catalog.map((p) => (
                <tr key={p.id}>
                  <td>{p.sku}</td>
                  <td>{p.nombre}</td>
                  <td className={p.stockActual === 0 ? "low" : ""}>{p.stockActual}</td>
                  <td>S/ {Number(p.precio).toFixed(2)}</td>
                  <td><button onClick={() => add(p)} disabled={p.stockActual === 0}>+</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div>
          <h2>Carrito</h2>
          {cart.length === 0 ? <p className="muted">Vacío. Hacé clic en + para agregar.</p> : (
            <>
              <table>
                <thead><tr><th>Producto</th><th>Cant.</th><th>Subtotal</th><th></th></tr></thead>
                <tbody>
                  {cart.map((l) => (
                    <tr key={l.product.id}>
                      <td>{l.product.nombre}</td>
                      <td>
                        <input type="number" min={1} value={l.cantidad} style={{ width: 60 }}
                          onChange={(e) => setQty(l.product.id, +e.target.value)} />
                      </td>
                      <td>S/ {(l.product.precio * l.cantidad).toFixed(2)}</td>
                      <td><button onClick={() => remove(l.product.id)} style={{ background: "#dc2626", borderColor: "#dc2626" }}>x</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <h2 style={{ marginTop: "1rem" }}>Total: S/ {total.toFixed(2)}</h2>
              <button onClick={confirm} style={{ width: "100%" }}>Cobrar</button>
            </>
          )}
          {err && <div className="error">{err}</div>}
          {result && (
            <div className="success">
              ✓ Venta #{result.id} registrada · Total S/ {Number(result.total).toFixed(2)}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
