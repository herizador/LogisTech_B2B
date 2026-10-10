import { useState } from 'react'
import { createOrder } from '../lib/api'

export default function OrderForm({ companyId, onCreated }: { companyId: string; onCreated: () => void }) {
  const [customerName, setCustomerName] = useState('')
  const [address, setAddress] = useState('')
  const [lat, setLat] = useState('41.648')
  const [lng, setLng] = useState('-0.877')
  const [weightKg, setWeightKg] = useState('25')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await createOrder(companyId, {
        customerName,
        address,
        lat: parseCoord(lat, 'Lat', -90, 90),
        lng: parseCoord(lng, 'Lng', -180, 180),
        weightKg: weightKg.trim() === '' ? undefined : Number(weightKg.trim().replace(',', '.')),
      })
      setCustomerName('')
      setAddress('')
      onCreated()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setSaving(false)
    }
  }

  const input = 'border rounded px-2 py-1 text-sm w-full'

  function parseCoord(raw: string, name: string, min: number, max: number): number | undefined {
    const t = raw.trim().replace(',', '.')
    if (t === '') return undefined
    const n = Number(t)
    if (!Number.isFinite(n)) throw new Error(`${name}: "${raw}" no es un número (ej. 41.648)`)
    if (n < min || n > max) throw new Error(`${name} debe estar entre ${min} y ${max}`)
    return n
  }

  return (
    <form onSubmit={submit} className="space-y-2 mt-4 border-t pt-3">
      <h3 className="font-semibold text-sm">Nuevo pedido</h3>
      <input className={input} placeholder="Cliente *" value={customerName} onChange={(e) => setCustomerName(e.target.value)} required />
      <input className={input} placeholder="Dirección *" value={address} onChange={(e) => setAddress(e.target.value)} required />
      <div className="flex gap-2">
        <input className={input} placeholder="Lat" inputMode="decimal" value={lat} onChange={(e) => setLat(e.target.value)} />
        <input className={input} placeholder="Lng" inputMode="decimal" value={lng} onChange={(e) => setLng(e.target.value)} />
      </div>
      <input className={input} placeholder="Peso kg" inputMode="decimal" value={weightKg} onChange={(e) => setWeightKg(e.target.value)} />
      {error && <p className="text-xs text-red-600">{error}</p>}
      <button
        type="submit"
        disabled={saving || !companyId}
        className="bg-slate-900 text-white text-sm rounded px-3 py-1.5 disabled:opacity-50"
      >
        {saving ? 'Guardando…' : 'Crear pedido'}
      </button>
    </form>
  )
}
