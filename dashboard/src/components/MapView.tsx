import { useEffect, useRef } from 'react'
import { Map, Marker, LngLatBounds } from 'maplibre-gl'

export interface MapPoint {
  lat: number
  lng: number
  label: string
  seq?: number
}

export interface RouteLine {
  points: MapPoint[]
  color: string
  markers?: MapPoint[]
}

const STYLE = 'https://basemaps.cartocdn.com/gl/positron-gl-style/style.json'
export const ROUTE_COLORS = ['#1d4ed8', '#dc2626', '#059669', '#d97706', '#7c3aed']

function numberedEl(seq: number, color: string): HTMLDivElement {
  const el = document.createElement('div')
  el.style.cssText = `background:${color};color:#fff;border-radius:9999px;width:24px;height:24px;`
    + 'display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:bold;'
    + 'border:2px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,.4)'
  el.textContent = String(seq)
  return el
}

export default function MapView({ points, routes = [] }: { points: MapPoint[]; routes?: RouteLine[] }) {
  const ref = useRef<HTMLDivElement>(null)
  const mapRef = useRef<Map | null>(null)

  useEffect(() => {
    if (!ref.current || mapRef.current) return
    const map = new Map({
      container: ref.current,
      style: STYLE,
      center: [-0.877, 41.648], // Zaragoza (Los Enlaces)
      zoom: 11,
    })
    mapRef.current = map
    return () => {
      map.remove()
      mapRef.current = null
    }
  }, [])

  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    let cancelled = false
    const markers: Marker[] = []

    const draw = () => {
      if (cancelled) return
      routes.forEach((r, i) => {
        const id = `route-line-${i}`
        map.addSource(id, {
          type: 'geojson',
          data: {
            type: 'Feature' as const,
            properties: {},
            geometry: { type: 'LineString' as const, coordinates: r.points.map((p) => [p.lng, p.lat]) },
          },
        })
        map.addLayer({
          id,
          type: 'line',
          source: id,
          paint: { 'line-color': r.color, 'line-width': 4, 'line-opacity': 0.85 },
        })
      })
      routes.forEach((r) => {
        (r.markers ?? r.points.filter((p) => p.seq != null)).forEach((p) => {
          if (p.seq == null) {
            markers.push(new Marker().setLngLat([p.lng, p.lat]).addTo(map))
          } else {
            markers.push(new Marker({ element: numberedEl(p.seq, r.color) }).setLngLat([p.lng, p.lat]).addTo(map))
          }
        })
      })
      points.forEach((p) => {
        markers.push(new Marker().setLngLat([p.lng, p.lat]).addTo(map))
      })
      const all = [...points, ...routes.flatMap((r) => r.points)]
      if (all.length > 1) {
        const bounds = new LngLatBounds()
        all.forEach((p) => bounds.extend([p.lng, p.lat]))
        map.fitBounds(bounds, { padding: 50 })
      } else if (all.length === 1) {
        map.setCenter([all[0].lng, all[0].lat])
        map.setZoom(13)
      }
    }

    if (map.isStyleLoaded()) draw()
    else map.once('load', draw)

    return () => {
      cancelled = true
      markers.forEach((m) => m.remove())
      try {
        routes.forEach((_, i) => {
          const id = `route-line-${i}`
          if (map.getLayer(id)) map.removeLayer(id)
          if (map.getSource(id)) map.removeSource(id)
        })
      } catch {
        /* mapa ya destruido */
      }
    }
  }, [points, routes])

  return <div ref={ref} className="map-container" />
}
