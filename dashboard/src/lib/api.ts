const API = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080'

export const API_BASE = API

async function authHeaders(): Promise<Record<string, string>> {
  const { supabase } = await import('./supabase')
  const { data } = await supabase.auth.getSession()
  const token = data.session?.access_token
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export interface Company {
  id: string
  name: string
}

export interface Order {
  id: string
  customerName: string
  address: string
  lat: number | null
  lng: number | null
  weightKg: number | null
  status: string
  windowStart: string | null
  windowEnd: string | null
  createdAt: string | null
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${API}${path}`, { headers: await authHeaders() })
  if (res.status === 401 || res.status === 403) {
    throw new Error(`${res.status} sin autorización (¿sesión caducada? entra de nuevo) en ${path}`)
  }
  if (!res.ok) throw new Error(`${res.status} ${res.statusText} en ${path}`)
  return res.json() as Promise<T>
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T | null> {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 401 || res.status === 403) {
    throw new Error(`${res.status} sin autorización (¿sesión caducada? entra de nuevo) en ${path}`)
  }
  if (res.status === 204) return null
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${text || res.statusText} en ${path}`)
  }
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const result = await request<T>('POST', path, body)
  if (result === null) throw new Error(`Respuesta vacía en ${path}`)
  return result
}

export const getCompanies = () => get<Company[]>('/api/companies')
export const getOrders = (companyId: string) => get<Order[]>(`/api/companies/${companyId}/orders`)

export interface RouteStopDto {
  seq: number
  orderId: string
  customerName: string
  address: string
  eta: string | null
  deliveredAt: string | null
}

export interface RouteDto {
  routeId: string
  driverName: string
  totalKm: number
  drivingMinutes: number
  stops: RouteStopDto[]
  geometry: number[][] | null
}

export interface SkippedDto {
  orderId: string
  customerName: string
  reason: string
}

export interface OptimizeResult {
  date: string
  totalKm: number
  totalDrivingMinutes: number
  routes: RouteDto[]
  skipped: SkippedDto[]
}

export interface ResetDayResult {
  date: string
  routesDeleted: number
  stopsDeleted: number
  ordersReset: number
}

export async function resetDay(companyId: string, date?: string): Promise<ResetDayResult> {
  return post<ResetDayResult>(`/api/companies/${companyId}/routes/reset`, { date })
}

export async function optimizeRoutes(  companyId: string,
  body: { date?: string; depotLat?: number; depotLng?: number },
): Promise<OptimizeResult> {
  return post<OptimizeResult>(`/api/companies/${companyId}/routes/optimize`, body)
}

export interface NewOrder {
  customerName: string
  address: string
  lat?: number
  lng?: number
  weightKg?: number
}

export interface Driver {
  id: string
  fullName: string
  vehicleCapacityKg: number | null
}

export const getDrivers = (companyId: string) => get<Driver[]>(`/api/companies/${companyId}/drivers`)

export async function createDriver(
  companyId: string,
  body: { fullName: string; vehicleCapacityKg?: number },
): Promise<Driver> {
  return post<Driver>(`/api/companies/${companyId}/drivers`, body)
}

export async function createOrder(companyId: string, body: NewOrder): Promise<Order> {
  return post<Order>(`/api/companies/${companyId}/orders`, body)
}

export async function updateOrderStatus(companyId: string, orderId: string, status: string): Promise<Order> {
  const result = await request<Order>('PATCH', `/api/companies/${companyId}/orders/${orderId}`, { status })
  if (result === null) throw new Error('Respuesta vacía al actualizar estado')
  return result
}

export async function deleteOrder(companyId: string, orderId: string): Promise<void> {
  await request<null>('DELETE', `/api/companies/${companyId}/orders/${orderId}`)
}

export interface RouteDetail {
  routeId: string
  date: string
  driverName: string
  totalKm: number
  stops: Array<RouteStopDto & { deliveredAt: string | null }>
}

export const getRouteDetail = (companyId: string, routeId: string) =>
  get<RouteDetail>(`/api/companies/${companyId}/routes/${routeId}`)

export interface RouteSummary {
  routeId: string
  date: string
  driverName: string
  totalKm: number
  stops: number
}

export const getRoutes = (companyId: string) => get<RouteSummary[]>(`/api/companies/${companyId}/routes`)
