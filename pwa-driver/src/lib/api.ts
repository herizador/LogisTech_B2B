import { supabase } from './supabase'

const API = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080'

export const API_BASE = API

async function authHeaders(): Promise<Record<string, string>> {
  const { data } = await supabase.auth.getSession()
  const token = data.session?.access_token
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${API}${path}`, { headers: await authHeaders() })
  if (res.status === 401 || res.status === 403) throw new Error('Sin autorización: entra de nuevo')
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json() as Promise<T>
}

async function patch<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${API}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...(await authHeaders()) },
    body: JSON.stringify(body),
  })
  if (res.status === 401 || res.status === 403) throw new Error('Sin autorización: entra de nuevo')
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${text || res.statusText}`)
  }
  return res.json() as Promise<T>
}

export interface Company {
  id: string
  name: string
}

export interface Driver {
  id: string
  fullName: string
  vehicleCapacityKg: number | null
}

export interface RouteStop {
  seq: number
  orderId: string
  customerName: string
  address: string
  lat: number | null
  lng: number | null
  eta: string | null
  deliveredAt: string | null
}

export interface DriverRoute {
  routeId: string
  date: string
  driverName: string
  totalKm: number
  stops: RouteStop[]
}

export const getCompanies = () => get<Company[]>('/api/companies')
export const getDrivers = (companyId: string) => get<Driver[]>(`/api/companies/${companyId}/drivers`)
export const getDriverRoutes = (companyId: string, driverId: string, date: string) =>
  get<DriverRoute[]>(`/api/companies/${companyId}/drivers/${driverId}/routes?date=${date}`)
export const markDelivered = (companyId: string, orderId: string) =>
  patch<{ id: string; status: string }>(`/api/companies/${companyId}/orders/${orderId}`, {
    status: 'delivered',
  })

// --- Cola offline: entregas sin red se guardan y se reintentan al volver ---
export interface OutItem {
  companyId: string
  orderId: string
  ts: number
}

const OUTBOX_KEY = 'logistech-outbox-v1'

export function loadOutbox(): OutItem[] {
  try {
    return JSON.parse(localStorage.getItem(OUTBOX_KEY) ?? '[]') as OutItem[]
  } catch {
    return []
  }
}

function saveOutbox(items: OutItem[]): void {
  localStorage.setItem(OUTBOX_KEY, JSON.stringify(items))
}

export function queueDelivery(companyId: string, orderId: string): void {
  const items = loadOutbox()
  if (!items.some((i) => i.orderId === orderId)) {
    items.push({ companyId, orderId, ts: Date.now() })
    saveOutbox(items)
  }
}

/** Intenta enviar la cola; devuelve cuántas quedan pendientes. */
export async function flushOutbox(): Promise<number> {
  const remaining: OutItem[] = []
  for (const item of loadOutbox()) {
    try {
      await markDelivered(item.companyId, item.orderId)
    } catch {
      remaining.push(item)
    }
  }
  saveOutbox(remaining)
  return remaining.length
}
