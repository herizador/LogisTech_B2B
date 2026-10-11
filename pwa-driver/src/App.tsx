import { Routes, Route, Link, Navigate, useNavigate } from 'react-router-dom'
import Login from './components/Login'
import MyRoute from './components/MyRoute'
import { useAuth } from './lib/auth'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <main><p>Cargando sesión…</p></main>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function Header() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  return (
    <header>
      <Link to="/">Repartidor</Link>
      {user && (
        <button
          className="ghost"
          style={{ background: 'transparent', color: '#fff', width: 'auto' }}
          onClick={() => signOut().then(() => navigate('/login'))}
        >
          Salir
        </button>
      )}
    </header>
  )
}

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<RequireAuth><MyRoute /></RequireAuth>} />
      </Routes>
    </>
  )
}
