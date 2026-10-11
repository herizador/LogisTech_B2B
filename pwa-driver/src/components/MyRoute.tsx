import { useCallback, useEffect, useState } from 'react'
import {
  flushOutbox,
  getCompanies,
  getDriverRoutes,
  getDrivers,
  loadOutbox,
  markDelivered,
  queueDelivery,
  type Company,
  type Driver,
  type DriverRoute,
} from '../lib/api'

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

function formatETA(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
}

export default function MyRoute() {
  const [companies, setCompanies] = useState<Company[]>([])
  const [companyId, setCompanyId] = useState(() => localStorage.getItem('driver-company') ?? '')
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [driverId, setDriverId] = useState(() => localStorage.getItem('driver-id') ?? '')
  const [routes, setRoutes] = useState<DriverRoute[]>([])
  const [pending, setPending] = useState(0)
  const [online, setOnline] = useState(navigator.onLine)
  const [error, setError] = useState<string | null>(null)
  const [syncing, setSyncing] = useState(false)

  const load = useCallback(async () => {
    if (!companyId || !driverId) return
    setError(null)
    try {
      setRoutes(await getDriverRoutes(companyId, driverId, todayISO()))
      setPending(await flushOutbox())
    } catch (e) {
      // Sin red: muestra lo último en caché (la API usa NetworkFirst con fallback).
      setError(e instanceof Error ? e.message : String(e))
      setPending(loadOutbox().length)
    }
  }, [companyId, driverId])

  useEffect(() => {
    getCompanies()
      .then((cs) => {
        setCompanies(cs)
        if (!companyId && cs.length > 0) setCompanyId(cs[0].id)
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!companyId) return
    localStorage.setItem('driver-company', companyId)
    getDrivers(companyId)
      .then((ds) => {
        setDrivers(ds)
        if (!driverId && ds.length > 0) setDriverId(ds[0].id)
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [companyId])

  useEffect(() => {
    if (!driverId) return
    localStorage.setItem('driver-id', driverId)
    void load()
  }, [driverId, load])

  useEffect(() => {
    const onStatus = () => {
      setOnline(navigator.onLine)
      if (navigator.onLine) void load()
    }
    window.addEventListener('online', onStatus)
    window.addEventListener('offline', onStatus)
    return () => {
      window.removeEventListener('online', onStatus)
      window.removeEventListener('offline', onStatus)
    }
  }, [load])

  async function syncNow() {
    setSyncing(true)
    setPending(await flushOutbox())
    await load()
    setSyncing(false)
  }

  async function deliver(companyId_: string, orderId: string) {
    try {
      await markDelivered(companyId_, orderId)
    } catch {
      // Sin red u otro fallo: a la cola y se marca al sincronizar.
      queueDelivery(companyId_, orderId)
    }
    setPending(loadOutbox().length)
    await load()
  }

  return (
    <main>
      {!online && <div className="offbar">Sin conexión: verás la última ruta cargada. Las entregas se guardan y se envían solas.</div>}
      {pending > 0 && (
        <div className="card">
          <div className="row">
            <span>{pending} entrega(s) pendientes de enviar</span>
            <button className="ghost" onClick={syncNow} disabled={syncing || !online}>
              {syncing ? 'Enviando…' : 'Sincronizar'}
            </button>
          </div>
        </div>
      )}
      <div className="card">
        <h2>Mi ruta de hoy</h2>
        {error && <p className="err">{error}</p>}
        <select value={companyId} onChange={(e) => setCompanyId(e.target.value)}>
          <option value="">Empresa…</option>
          {companies.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <select value={driverId} onChange={(e) => setDriverId(e.target.value)}>
          <option value="">Soy…</option>
          {drivers.map((d) => (
            <option key={d.id} value={d.id}>{d.fullName}</option>
          ))}
        </select>
      </div>
      {routes.length === 0 && driverId && (
        <div className="card"><p className="muted">No tienes ruta asignada para hoy.</p></div>
      )}
      {routes.map((r) => (
        <div className="card" key={r.routeId}>
          <div className="row">
            <strong>{r.totalKm.toFixed(1)} km · {r.stops.length} paradas</strong>
          </div>
          {r.stops.map((s) => {
            const queued = loadOutbox().some((i) => i.orderId === s.orderId)
            const done = s.deliveredAt != null || queued
            return (
              <div className="stop" key={s.orderId}>
                <div className="row">
                  <h3>{s.seq}. {s.customerName}</h3>
                  <span className={`badge ${done ? 'delivered' : 'pending'}`}>
                    {s.deliveredAt ? 'entregada' : queued ? 'en cola' : 'pendiente'}
                  </span>
                </div>
                <p className="muted">{s.address} · ETA {formatETA(s.eta)}</p>
                <div className="actions">
                  {s.lat != null && s.lng != null && (
                    <a
                      href={`https://www.google.com/maps/dir/?api=1&destination=${s.lat},${s.lng}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Cómo llegar
                    </a>
                  )}
                  {!done && (
                    <button className="success" onClick={() => deliver(companyId, s.orderId)}>
                      Entregar
                    </button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      ))}
    </main>
  )
}
