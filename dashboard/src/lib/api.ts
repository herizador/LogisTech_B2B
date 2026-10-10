const API = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080'

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
