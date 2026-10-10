const API = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080'

export const API_BASE = API

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
  const res = await fetch(`${API}${path}`)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText} en ${path}`)
  return res.json() as Promise<T>
}

export const getCompanies = () => get<Company[]>('/api/companies')
export const getOrders = (companyId: string) => get<Order[]>(`/api/companies/${companyId}/orders`)

export interface RouteStopDto {
  seq: number
  orderId: string
  customerName: string
  address: string
  eta: string | null
}

export interface RouteDto {
  routeId: string
  driverName: string
  totalKm: number
  drivingMinutes: number
  stops: RouteStopDto[]
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
  const res = await fetch(`${API}/api/companies/${companyId}/routes/reset`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ date }),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${text || res.statusText}`)
  }
  return res.json() as Promise<ResetDayResult>
}

export async function optimizeRoutes(  companyId: string,
  body: { date?: string; depotLat?: number; depotLng?: number },
): Promise<OptimizeResult> {
  const res = await fetch(`${API}/api/companies/${companyId}/routes/optimize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${text || res.statusText}`)
  }
  return res.json() as Promise<OptimizeResult>
}

export interface NewOrder {
  customerName: string
  address: string
  lat?: number
  lng?: number
  weightKg?: number
}

export async function createOrder(companyId: string, body: NewOrder): Promise<Order> {
  const res = await fetch(`${API}/api/companies/${companyId}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${text || res.statusText} en POST /api/companies/${companyId}/orders`)
  }
  return res.json() as Promise<Order>
}
