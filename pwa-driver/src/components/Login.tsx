import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'

export default function Login() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const message = await signIn(email, password)
    setBusy(false)
    if (message) {
      setError(message)
      return
    }
    navigate('/')
  }

  return (
    <main>
      <div className="card">
        <h2>Entrar</h2>
        <p className="muted">Usa tu cuenta de repartidor. Tu jefe la crea en el panel.</p>
        <form onSubmit={submit}>
          <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <input
            type="password"
            placeholder="Contraseña"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {error && <p className="err">{error}</p>}
          <button type="submit" className="primary" disabled={busy}>
            {busy ? 'Un momento…' : 'Entrar'}
          </button>
        </form>
      </div>
    </main>
  )
}
