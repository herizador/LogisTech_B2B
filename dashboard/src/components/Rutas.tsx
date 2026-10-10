import { useEffect, useState } from 'react'
import { getCompanies, optimizeRoutes, type Company, type OptimizeResult } from '../lib/api'

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

function formatETA(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
}

export default function Rutas() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [companyId, setCompanyId] = useState('')
  const [date, setDate] = useState(todayISO())
  const [depotLat, setDepotLat] = useState('41.648')
  const [depotLng, setDepotLng] = useState('-0.877')
  const [result, setResult] = useState<OptimizeResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getCompanies()
      .then((cs) => {
        setCompanies(cs)
        if (cs.length > 0) setCompanyId(cs[0].id)
      })
      .catch((e) => setError(`No se pudo contactar con la API: ${e.message}`))
  }, [])

  async function run(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const r = await optimizeRoutes(companyId, {
        date,
        depotLat: depotLat === '' ? undefined : Number(depotLat),
        depotLng: depotLng === '' ? undefined : Number(depotLng),
      })
      setResult(r)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setLoading(false)
    }
  }

  const input = 'border rounded px-2 py-1 text-sm w-full'

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <form onSubmit={run} className="bg-white rounded-xl shadow p-4 space-y-2 h-fit">
        <h2 className="font-semibold">Optimizar rutas (Timefold)</h2>
        <label className="text-sm block">Empresa
          <select className={input} value={companyId} onChange={(e) => setCompanyId(e.target.value)}>
            {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label className="text-sm block">Fecha
          <input type="date" className={input} value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <div className="flex gap-2">
          <label className="text-sm block w-full">Base lat
            <input className={input} inputMode="decimal" value={depotLat} onChange={(e) => setDepotLat(e.target.value)} />
          </label>
          <label className="text-sm block w-full">Base lng
            <input className={input} inputMode="decimal" value={depotLng} onChange={(e) => setDepotLng(e.target.value)} />
          </label>
        </div>
        {error && <p className="text-xs text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={loading || !companyId}
          className="bg-slate-900 text-white text-sm rounded px-3 py-1.5 disabled:opacity-50"
        >
          {loading ? 'Optimizando… (hasta ~40s)' : 'Optimizar'}
        </button>
        <p className="text-xs text-slate-400">Optimiza pedidos pendientes con coordenadas. Respeta capacidad y ventanas horarias.</p>
      </form>

      <div className="lg:col-span-2 bg-white rounded-xl shadow p-4">
        <h2 className="font-semibold mb-2">Resultado</h2>
        {!result && !loading && <p className="text-sm text-slate-500">Sin resultado todavía.</p>}
        {loading && <p className="text-sm">Resolviendo VRP con Timefold…</p>}
        {result && (
          <div className="space-y-4 text-sm">
            <p>
              <strong>{result.date}</strong> · {result.totalKm.toFixed(1)} km · {result.totalDrivingMinutes} min
              · {result.routes.length} ruta(s)
            </p>
            {result.routes.map((r) => (
              <div key={r.routeId} className="border rounded p-3">
                <p className="font-medium">{r.driverName} · {r.totalKm.toFixed(1)} km · {r.drivingMinutes} min</p>
                <ol className="list-decimal ml-5 mt-1 space-y-1">
                  {r.stops.map((s) => (
                    <li key={s.orderId}>
                      {s.customerName} — {s.address} <span className="text-slate-400">ETA {formatETA(s.eta)}</span>
                    </li>
                  ))}
                </ol>
              </div>
            ))}
            {result.skipped.length > 0 && (
              <div className="border border-amber-300 bg-amber-50 rounded p-3">
                <p className="font-medium">Omitidos ({result.skipped.length})</p>
                <ul className="list-disc ml-5">
                  {result.skipped.map((s) => (
                    <li key={s.orderId}>{s.customerName} — {s.reason}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
