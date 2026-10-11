import { Suspense, lazy, useEffect, useState } from 'react'
import { Routes, Route, Link, Navigate, useNavigate } from 'react-router-dom'
import OrderForm from './components/OrderForm'
import Rutas from './components/Rutas'
import Conductores from './components/Conductores'
import Login from './components/Login'
import { useAuth } from './lib/auth'
import { API_BASE, deleteOrder, getCompanies, getOrders, updateOrderStatus, type Company, type Order } from './lib/api'

const MapView = lazy(() => import('./components/MapView'))

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <p className="p-6 text-sm">Cargando sesión…</p>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function Layout({ children }: { children: React.ReactNode }) {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <header className="bg-slate-900 text-white px-6 py-3 flex items-center justify-between">
        <Link to="/" className="font-bold text-lg">LogisTech B2B</Link>
        <nav className="flex gap-4 text-sm items-center">
          <Link to="/" className="hover:underline">Flota</Link>
          <Link to="/rutas" className="hover:underline">Rutas</Link>
          <Link to="/conductores" className="hover:underline">Conductores</Link>
          <Link to="/health" className="hover:underline">API Status</Link>
          {user && (
            <>
              <span className="text-slate-300 text-xs">{user.email}</span>
              <button
                className="hover:underline text-xs"
                onClick={() => signOut().then(() => navigate('/login'))}
              >
                Salir
              </button>
            </>
          )}
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

  async function deliver(orderId: string) {
    try {
      await updateOrderStatus(companyId, orderId, 'delivered')
      loadOrders(companyId)
    } catch (e) {
      setError(`Error marcando entrega: ${e instanceof Error ? e.message : e}`)
    }
  }

  async function remove(orderId: string, name: string) {
    if (!window.confirm(`¿Borrar el pedido de ${name}?`)) return
    try {
      await deleteOrder(companyId, orderId)
      loadOrders(companyId)
    } catch (e) {
      setError(`Error borrando: ${e instanceof Error ? e.message : e}`)
    }
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
        {loading ? (
          <p className="text-sm">Cargando…</p>
        ) : (
          <Suspense fallback={<p className="text-sm">Cargando mapa…</p>}>
            <MapView points={points} />
          </Suspense>
        )}
      </div>
      <div className="bg-white rounded-xl shadow p-4">
        <h2 className="font-semibold mb-2">Pedidos ({orders.length})</h2>
        {orders.length === 0 && !loading && (
          <p className="text-sm text-slate-500">Sin pedidos. Crea uno con POST /api/companies/{companyId}/orders</p>
        )}
        <ul className="space-y-2 text-sm">
          {orders.map((o) => (
            <li key={o.id} className="border rounded p-2">
              <div className="flex items-center justify-between">
                <p className="font-medium">{o.customerName}</p>
                <span className={`text-xs px-1.5 py-0.5 rounded ${
                  o.status === 'delivered' ? 'bg-green-100 text-green-700'
                  : o.status === 'assigned' ? 'bg-blue-100 text-blue-700'
                  : 'bg-slate-100 text-slate-500'}`}
                >
                  {o.status}
                </span>
              </div>
              <p className="text-slate-500">{o.address}</p>
              <p className="text-xs text-slate-400">
                {o.lat != null && o.lng != null
                  ? `${o.lat.toFixed(5)}, ${o.lng.toFixed(5)}`
                  : 'sin coordenadas'}
                {o.weightKg != null ? ` · ${o.weightKg} kg` : ''}
              </p>
              <div className="flex gap-2 mt-1">
                {o.status !== 'delivered' && (
                  <button className="text-xs text-green-700 hover:underline" onClick={() => deliver(o.id)}>
                    Marcar entregada
                  </button>
                )}
                <button className="text-xs text-red-600 hover:underline" onClick={() => remove(o.id, o.customerName)}>
                  Borrar
                </button>
              </div>
            </li>
          ))}
        </ul>
        {companyId && <OrderForm companyId={companyId} onCreated={() => loadOrders(companyId)} />}
      </div>
    </div>
  )
}

function Health() {
  return <p className="text-sm">Backend health: {(import.meta.env.VITE_API_URL as string) ?? 'http://localhost:8080'}/api/health</p>
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<RequireAuth><Dashboard /></RequireAuth>} />
        <Route path="/rutas" element={<RequireAuth><Rutas /></RequireAuth>} />
        <Route path="/conductores" element={<RequireAuth><Conductores /></RequireAuth>} />
        <Route path="/health" element={<Health />} />
      </Routes>
    </Layout>
  )
}
