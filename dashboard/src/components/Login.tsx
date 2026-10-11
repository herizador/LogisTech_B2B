import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'

export default function Login() {
  const { signIn, signUp } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState<'in' | 'up'>('in')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setNotice(null)
    const message = mode === 'in' ? await signIn(email, password) : await signUp(email, password)
    setBusy(false)
    if (message) {
      setError(message)
      return
    }
    if (mode === 'up') {
      setNotice('Cuenta creada. Si pide confirmar el email, revísalo o desactiva la confirmación en Supabase.')
    }
    navigate('/')
  }

  const input = 'border rounded px-2 py-1 text-sm w-full'

  return (
    <div className="max-w-sm mx-auto bg-white rounded-xl shadow p-6 mt-10">
      <h2 className="font-semibold text-lg mb-4">{mode === 'in' ? 'Entrar' : 'Crear cuenta'}</h2>
      <form onSubmit={submit} className="space-y-2">
        <input
          className={input}
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          className={input}
          type="password"
          placeholder="Contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="text-xs text-red-600">{error}</p>}
        {notice && <p className="text-xs text-green-700">{notice}</p>}
        <button
          type="submit"
          disabled={busy}
          className="bg-slate-900 text-white text-sm rounded px-3 py-1.5 w-full disabled:opacity-50"
        >
          {busy ? 'Un momento…' : mode === 'in' ? 'Entrar' : 'Registrarme'}
        </button>
      </form>
      <button
        className="text-xs text-slate-500 underline mt-3"
        onClick={() => setMode(mode === 'in' ? 'up' : 'in')}
      >
        {mode === 'in' ? '¿Sin cuenta? Regístrate' : '¿Ya tienes cuenta? Entra'}
      </button>
    </div>
  )
}
