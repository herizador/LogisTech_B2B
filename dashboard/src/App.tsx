import { useEffect, useState } from 'react'
import { Routes, Route, Link } from 'react-router-dom'
import MapView from './components/MapView'
import OrderForm from './components/OrderForm'
import { API_BASE, getCompanies, getOrders, type Company, type Order } from './lib/api'

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <header className="bg-slate-900 text-white px-6 py-3 flex items-center justify-between">
        <Link to="/" className="font-bold text-lg">LogisTech B2B</Link>
        <nav className="flex gap-4 text-sm">
          <Link to="/" className="hover:underline">Flota</Link>
          <Link to="/rutas" className="hover:underline">Rutas</Link>
          <Link to="/health" className="hover:underline">API Status</Link>
        </nav>
      </header>
      <main className="p-6">{children}</main>
    </div>
  )
}

function Dashboard() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [companyId, setCompanyId] = useState('')
  const [orders, setOrders] = useState<Order[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCompanies()
      .then((cs) => {
        setCompanies(cs)
        if (cs.length > 0) setCompanyId(cs[0].id)
        setLoading(false)
      })
      .catch((e) => {
        setError(`No se pudo contactar con la API (${API_BASE}): ${e.message}`)
        setLoading(false)
      })
  }, [])

  function loadOrders(id: string) {
    getOrders(id).then(setOrders).catch((e) => setError(`Error cargando pedidos: ${e.message}`))
  }

  useEffect(() => {
    if (!companyId) return
    loadOrders(companyId)
  }, [companyId])

  const points = orders
    .filter((o) => o.lat != null && o.lng != null)
    .map((o) => ({ lat: o.lat!, lng: o.lng!, label: o.customerName }))

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="lg:col-span-2 bg-white rounded-xl shadow p-4">
        <div className="flex items-center justify-between mb-2">
          <h2 className="font-semibold">Pedidos en mapa ({points.length})</h2>
          <select
            className="border rounded px-2 py-1 text-sm"
            value={companyId}
            onChange={(e) => setCompanyId(e.target.value)}
          >
            {companies.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>
        {error && <p className="text-sm text-red-600 mb-2">{error}</p>}
        {loading ? <p className="text-sm">Cargando…</p> : <MapView points={points} />}
      </div>
      <div className="bg-white rounded-xl shadow p-4">
        <h2 className="font-semibold mb-2">Pedidos ({orders.length})</h2>
        {orders.length === 0 && !loading && (
          <p className="text-sm text-slate-500">Sin pedidos. Crea uno con POST /api/companies/{companyId}/orders</p>
        )}
        <ul className="space-y-2 text-sm">
          {orders.map((o) => (
            <li key={o.id} className="border rounded p-2">
              <p className="font-medium">{o.customerName}</p>
              <p className="text-slate-500">{o.address}</p>
              <p className="text-xs text-slate-400">{o.status}{o.weightKg != null ? ` · ${o.weightKg} kg` : ''}</p>
            </li>
          ))}
        </ul>
        {companyId && <OrderForm companyId={companyId} onCreated={() => loadOrders(companyId)} />}
      </div>
    </div>
  )
}

function Rutas() {
  return <p className="text-sm">Aquí irá el optimizador VRP (Timefold + OSRM). Pendiente de backend.</p>
}

function Health() {
  return <p className="text-sm">Backend health: {(import.meta.env.VITE_API_URL as string) ?? 'http://localhost:8080'}/api/health</p>
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/rutas" element={<Rutas />} />
        <Route path="/health" element={<Health />} />
      </Routes>
    </Layout>
  )
}
