import { Routes, Route, Link } from 'react-router-dom'
import MapView from './components/MapView'

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
  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="lg:col-span-2 bg-white rounded-xl shadow p-4">
        <h2 className="font-semibold mb-2">Flota en tiempo real (MapLibre + OSM)</h2>
        <MapView />
      </div>
      <div className="bg-white rounded-xl shadow p-4">
        <h2 className="font-semibold mb-2">Próximos pasos</h2>
        <ol className="list-decimal ml-5 text-sm space-y-1">
          <li>Crear proyecto en Supabase y copiar URL + anon key a .env</li>
          <li>Ejecutar <code>docs/02-modelo-datos.sql</code> en Supabase SQL Editor</li>
          <li>Arrancar backend: <code>cd backend; ./mvnw spring-boot:run</code></li>
          <li>Probar <code>GET http://localhost:8080/api/health</code></li>
        </ol>
      </div>
    </div>
  )
}

function Rutas() {
  return <p className="text-sm">Aquí irá el optimizador VRP (Timefold + OSRM). Pendiente de backend.</p>
}

function Health() {
  return <p className="text-sm">Backend health: http://localhost:8080/api/health</p>
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
