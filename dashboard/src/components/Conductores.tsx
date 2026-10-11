import { useEffect, useState } from 'react'
import { createDriver, getCompanies, getDrivers, type Company, type Driver } from '../lib/api'

export default function Conductores() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [companyId, setCompanyId] = useState('')
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [fullName, setFullName] = useState('')
  const [capacity, setCapacity] = useState('1000')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getCompanies()
      .then((cs) => {
        setCompanies(cs)
        if (cs.length > 0) setCompanyId(cs[0].id)
      })
      .catch((e) => setError(`No se pudo contactar con la API: ${e.message}`))
  }, [])

  function load(id: string) {
    getDrivers(id).then(setDrivers).catch((e) => setError(`Error cargando conductores: ${e.message}`))
  }

  useEffect(() => {
    if (companyId) load(companyId)
  }, [companyId])

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const cap = capacity.trim().replace(',', '.')
      await createDriver(companyId, {
        fullName,
        vehicleCapacityKg: cap === '' ? undefined : Number(cap),
      })
      setFullName('')
      load(companyId)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setSaving(false)
    }
  }

  const input = 'border rounded px-2 py-1 text-sm w-full'

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="bg-white rounded-xl shadow p-4">
        <div className="flex items-center justify-between mb-2">
          <h2 className="font-semibold">Conductores ({drivers.length})</h2>
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
        {error && <p className="text-xs text-red-600 mb-2">{error}</p>}
        <ul className="space-y-2 text-sm">
          {drivers.map((d) => (
            <li key={d.id} className="border rounded p-2">
              <p className="font-medium">{d.fullName}</p>
              <p className="text-xs text-slate-400">
                Capacidad: {d.vehicleCapacityKg != null ? `${d.vehicleCapacityKg} kg` : '1000 kg (defecto)'}
              </p>
            </li>
          ))}
          {drivers.length === 0 && <p className="text-sm text-slate-500">Sin conductores: el optimizador usa un vehículo genérico.</p>}
        </ul>
      </div>
      <form onSubmit={submit} className="bg-white rounded-xl shadow p-4 space-y-2 h-fit">
        <h2 className="font-semibold">Nuevo conductor</h2>
        <input className={input} placeholder="Nombre *" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        <input className={input} placeholder="Capacidad kg (defecto 1000)" inputMode="decimal" value={capacity} onChange={(e) => setCapacity(e.target.value)} />
        <button
          type="submit"
          disabled={saving || !companyId}
          className="bg-slate-900 text-white text-sm rounded px-3 py-1.5 disabled:opacity-50"
        >
          {saving ? 'Guardando…' : 'Crear conductor'}
        </button>
        <p className="text-xs text-slate-400">Cada conductor es un vehículo en la optimización, con su capacidad.</p>
      </form>
    </div>
  )
}
