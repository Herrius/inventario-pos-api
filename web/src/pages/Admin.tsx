import { useEffect, useState } from "react";
import { api } from "../api";
import type { Category, Product } from "../api";

type Tab = "productos" | "entradas" | "ajustes";

export function Admin() {
  const [tab, setTab] = useState<Tab>("productos");
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);

  async function refresh() {
    const [cats, prods] = await Promise.all([api.categories(), api.products()]);
    setCategories(cats);
    setProducts(prods.content);
  }
  useEffect(() => { refresh(); }, []);

  return (
    <div>
      <h1>Administración</h1>
      <div className="tabs">
        <button className={tab === "productos" ? "tab active" : "tab"} onClick={() => setTab("productos")}>Productos & Categorías</button>
        <button className={tab === "entradas" ? "tab active" : "tab"} onClick={() => setTab("entradas")}>Entradas de stock</button>
        <button className={tab === "ajustes" ? "tab active" : "tab"} onClick={() => setTab("ajustes")}>Ajustes</button>
      </div>
      {tab === "productos" && <ProductsAdmin categories={categories} products={products} onChange={refresh} />}
      {tab === "entradas" && <EntryAdmin products={products} onChange={refresh} />}
      {tab === "ajustes" && <AdjustmentAdmin products={products} onChange={refresh} />}
    </div>
  );
}

// ------------------------------- Productos & Categorías -------------------------------
function ProductsAdmin({ categories, products, onChange }: { categories: Category[]; products: Product[]; onChange: () => void }) {
  const [catName, setCatName] = useState("");
  const [sku, setSku] = useState("");
  const [nombre, setNombre] = useState("");
  const [precio, setPrecio] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [minStock, setMinStock] = useState("0");
  const [msg, setMsg] = useState<{ type: "ok" | "err"; text: string } | null>(null);

  async function addCategory(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      const c = await api.createCategory(catName.trim());
      setCatName("");
      setMsg({ type: "ok", text: `Categoría "${c.nombre}" creada (id ${c.id})` });
      onChange();
    } catch (e: any) {
      setMsg({ type: "err", text: e.message });
    }
  }
  async function addProduct(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      const p = await api.createProduct({
        sku: sku.trim(), nombre: nombre.trim(),
        precio: Number(precio), categoryId: Number(categoryId),
        minStock: Number(minStock),
      });
      setSku(""); setNombre(""); setPrecio(""); setCategoryId(""); setMinStock("0");
      setMsg({ type: "ok", text: `Producto ${p.sku} creado (id ${p.id}). Stock inicial: 0 — registralo en la pestaña Entradas.` });
      onChange();
    } catch (e: any) {
      setMsg({ type: "err", text: e.message });
    }
  }
  async function removeProduct(id: number, sku: string) {
    if (!confirm(`¿Eliminar producto ${sku}? (falla si tiene ventas o movimientos)`)) return;
    setMsg(null);
    try {
      await api.deleteProduct(id);
      setMsg({ type: "ok", text: `Producto ${sku} eliminado.` });
      onChange();
    } catch (e: any) {
      setMsg({ type: "err", text: e.message });
    }
  }

  return (
    <div>
      {msg && <div className={msg.type === "ok" ? "success" : "error"}>{msg.text}</div>}

      <div className="admin-grid">
        <section>
          <h2>Nueva categoría</h2>
          <form onSubmit={addCategory}>
            <div className="row">
              <input placeholder="Nombre de la categoría" value={catName} onChange={(e) => setCatName(e.target.value)} required minLength={2} />
              <button type="submit">Crear</button>
            </div>
          </form>

          <h2 style={{ marginTop: "1.5rem" }}>Categorías existentes</h2>
          <table>
            <thead><tr><th>id</th><th>Nombre</th></tr></thead>
            <tbody>
              {categories.map((c) => <tr key={c.id}><td>{c.id}</td><td>{c.nombre}</td></tr>)}
            </tbody>
          </table>
        </section>

        <section>
          <h2>Nuevo producto</h2>
          <form onSubmit={addProduct}>
            <div className="row">
              <input placeholder="SKU (ej: COCA-500)" value={sku} onChange={(e) => setSku(e.target.value)} required />
            </div>
            <div className="row">
              <input placeholder="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required minLength={2} />
            </div>
            <div className="row">
              <input type="number" step="0.01" min="0" placeholder="Precio (S/)" value={precio} onChange={(e) => setPrecio(e.target.value)} required />
              <input type="number" min="0" placeholder="Min stock" value={minStock} onChange={(e) => setMinStock(e.target.value)} required />
            </div>
            <div className="row">
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : "")} required>
                <option value="">— Categoría —</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
              <button type="submit">Crear producto</button>
            </div>
          </form>
        </section>
      </div>

      <h2>Productos existentes</h2>
      <table>
        <thead><tr><th>SKU</th><th>Nombre</th><th>Categoría</th><th>Precio</th><th>Stock</th><th>Min</th><th></th></tr></thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.sku}</td>
              <td>{p.nombre}</td>
              <td className="muted">{p.categoryNombre}</td>
              <td>S/ {Number(p.precio).toFixed(2)}</td>
              <td className={p.stockActual < p.minStock ? "low" : ""}>{p.stockActual}</td>
              <td className="muted">{p.minStock}</td>
              <td><button onClick={() => removeProduct(p.id, p.sku)} style={{ background: "#dc2626", borderColor: "#dc2626" }}>x</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ------------------------------- Entradas -------------------------------
function EntryAdmin({ products, onChange }: { products: Product[]; onChange: () => void }) {
  const [productId, setProductId] = useState<number | "">("");
  const [cantidad, setCantidad] = useState("");
  const [razon, setRazon] = useState("");
  const [msg, setMsg] = useState<{ type: "ok" | "err"; text: string } | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      const m = await api.registerEntry({
        productId: Number(productId),
        cantidad: Number(cantidad),
        razon: razon.trim() || undefined,
      });
      setCantidad(""); setRazon("");
      setMsg({ type: "ok", text: `Entrada registrada (+${m.cantidad} de ${m.productSku}). Nuevo stock visible en Productos.` });
      onChange();
    } catch (e: any) {
      setMsg({ type: "err", text: e.message });
    }
  }
  return (
    <section>
      <h2>Registrar entrada de stock</h2>
      <p className="muted">Compra a proveedor o ingreso inicial. Genera un <code>StockMovement</code> tipo ENTRADA.</p>
      {msg && <div className={msg.type === "ok" ? "success" : "error"}>{msg.text}</div>}
      <form onSubmit={submit}>
        <div className="row">
          <select value={productId} onChange={(e) => setProductId(e.target.value ? Number(e.target.value) : "")} required>
            <option value="">— Producto —</option>
            {products.map((p) => <option key={p.id} value={p.id}>{p.sku} · {p.nombre} (stock: {p.stockActual})</option>)}
          </select>
        </div>
        <div className="row">
          <input type="number" min="1" placeholder="Cantidad" value={cantidad} onChange={(e) => setCantidad(e.target.value)} required />
        </div>
        <div className="row">
          <input placeholder="Razón (opcional — ej: 'Compra a proveedor X')" value={razon} onChange={(e) => setRazon(e.target.value)} />
          <button type="submit">Registrar entrada</button>
        </div>
      </form>
    </section>
  );
}

// ------------------------------- Ajustes -------------------------------
function AdjustmentAdmin({ products, onChange }: { products: Product[]; onChange: () => void }) {
  const [productId, setProductId] = useState<number | "">("");
  const [direccion, setDireccion] = useState<"POSITIVO" | "NEGATIVO">("POSITIVO");
  const [cantidad, setCantidad] = useState("");
  const [razon, setRazon] = useState("");
  const [msg, setMsg] = useState<{ type: "ok" | "err"; text: string } | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      const m = await api.registerAdjustment({
        productId: Number(productId),
        direccion,
        cantidad: Number(cantidad),
        razon: razon.trim() || undefined,
      });
      setCantidad(""); setRazon("");
      setMsg({ type: "ok", text: `Ajuste ${direccion} registrado (${m.deltaConSigno > 0 ? "+" : ""}${m.deltaConSigno} de ${m.productSku}).` });
      onChange();
    } catch (e: any) {
      setMsg({ type: "err", text: e.message });
    }
  }
  return (
    <section>
      <h2>Ajuste manual de stock</h2>
      <p className="muted">POSITIVO: sobrante encontrado, devolución. NEGATIVO: merma, robo, vencimiento. Genera un <code>StockMovement</code> AJUSTE_POSITIVO/AJUSTE_NEGATIVO.</p>
      {msg && <div className={msg.type === "ok" ? "success" : "error"}>{msg.text}</div>}
      <form onSubmit={submit}>
        <div className="row">
          <select value={productId} onChange={(e) => setProductId(e.target.value ? Number(e.target.value) : "")} required>
            <option value="">— Producto —</option>
            {products.map((p) => <option key={p.id} value={p.id}>{p.sku} · {p.nombre} (stock: {p.stockActual})</option>)}
          </select>
        </div>
        <div className="row">
          <select value={direccion} onChange={(e) => setDireccion(e.target.value as any)} required>
            <option value="POSITIVO">POSITIVO (+)</option>
            <option value="NEGATIVO">NEGATIVO (−)</option>
          </select>
          <input type="number" min="1" placeholder="Cantidad" value={cantidad} onChange={(e) => setCantidad(e.target.value)} required />
        </div>
        <div className="row">
          <input placeholder="Razón (ej: 'Botellas rotas', 'Sobrante en conteo')" value={razon} onChange={(e) => setRazon(e.target.value)} />
          <button type="submit">Registrar ajuste</button>
        </div>
      </form>
    </section>
  );
}
