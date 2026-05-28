// Cliente HTTP minimalista. La base URL se lee de Vite env (VITE_API_URL),
// default http://localhost:8080 para dev local. El token se guarda en
// localStorage; sí, es portfolio-grade — para prod usar httpOnly cookies.

const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

function token(): string | null {
  return localStorage.getItem("token");
}

export function setToken(t: string | null) {
  if (t) localStorage.setItem("token", t);
  else localStorage.removeItem("token");
}

export function isAuthed(): boolean {
  return token() !== null;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const t = token();
  if (t) headers["Authorization"] = `Bearer ${t}`;
  const resp = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!resp.ok) {
    let detail: any = null;
    try { detail = await resp.json(); } catch {}
    throw new ApiError(resp.status, detail?.message || resp.statusText, detail);
  }
  if (resp.status === 204) return undefined as T;
  return resp.json() as Promise<T>;
}

export class ApiError extends Error {
  status: number;
  body: any;
  constructor(status: number, msg: string, body: any) {
    super(msg);
    this.status = status;
    this.body = body;
  }
}

// ---- Endpoints tipados ----

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface User {
  id: number;
  email: string;
  role: "ADMIN" | "CAJERO";
  createdAt: string;
}

export interface Product {
  id: number;
  sku: string;
  nombre: string;
  precio: number;
  categoryId: number;
  categoryNombre: string;
  stockActual: number;
  minStock: number;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StockMovement {
  id: number;
  productId: number;
  productSku: string;
  productNombre: string;
  tipo: "ENTRADA" | "SALIDA" | "AJUSTE_POSITIVO" | "AJUSTE_NEGATIVO";
  cantidad: number;
  deltaConSigno: number;
  razon: string | null;
  saleId: number | null;
  userEmail: string;
  createdAt: string;
}

export interface SaleItemRequest { productId: number; cantidad: number; }
export interface SaleResponse {
  id: number;
  userEmail: string;
  total: number;
  status: "PAGADA" | "ANULADA";
  createdAt: string;
  items: {
    id: number;
    productId: number;
    productSku: string;
    productNombre: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
  }[];
}

export interface DailySummary {
  date: string;
  totalVentas: number;
  revenue: number;
  ticketPromedio: number;
}

export interface Category {
  id: number;
  nombre: string;
  createdAt: string;
}

export interface ProductCreateRequest {
  sku: string;
  nombre: string;
  precio: number;
  categoryId: number;
  minStock: number;
}

export interface StockEntryRequest {
  productId: number;
  cantidad: number;
  razon?: string;
}

export interface StockAdjustmentRequest {
  productId: number;
  direccion: "POSITIVO" | "NEGATIVO";
  cantidad: number;
  razon?: string;
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>("POST", "/v1/auth/login", { email, password }),
  me: () => request<User>("GET", "/v1/users/me"),
  products: (q?: string) =>
    request<PageResponse<Product>>("GET",
      `/v1/products?size=100${q ? `&q=${encodeURIComponent(q)}` : ""}`),
  productById: (id: number) => request<Product>("GET", `/v1/products/${id}`),
  movements: (productId?: number) =>
    request<PageResponse<StockMovement>>("GET",
      `/v1/inventory/movements?size=100${productId ? `&productId=${productId}` : ""}`),
  createSale: (items: SaleItemRequest[]) =>
    request<SaleResponse>("POST", "/v1/sales", { items }),
  dailySummary: (date: string) =>
    request<DailySummary>("GET", `/v1/reports/sales/daily?date=${date}`),

  // --- ADMIN ---
  categories: () => request<Category[]>("GET", "/v1/categories"),
  createCategory: (nombre: string) =>
    request<Category>("POST", "/v1/categories", { nombre }),
  createProduct: (req: ProductCreateRequest) =>
    request<Product>("POST", "/v1/products", req),
  updateProduct: (id: number, req: ProductCreateRequest) =>
    request<Product>("PUT", `/v1/products/${id}`, req),
  deleteProduct: (id: number) =>
    request<void>("DELETE", `/v1/products/${id}`),
  registerEntry: (req: StockEntryRequest) =>
    request<StockMovement>("POST", "/v1/inventory/entries", req),
  registerAdjustment: (req: StockAdjustmentRequest) =>
    request<StockMovement>("POST", "/v1/inventory/adjustments", req),
};
