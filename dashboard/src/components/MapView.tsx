import { useEffect, useRef } from 'react'
import { Map, Marker } from 'maplibre-gl'

export interface MapPoint {
  lat: number
  lng: number
  label: string
}

export default function MapView({ points }: { points: MapPoint[] }) {
  const ref = useRef<HTMLDivElement>(null)
  const mapRef = useRef<Map | null>(null)

  useEffect(() => {
    if (!ref.current || mapRef.current) return
    const map = new Map({
      container: ref.current,
      style: 'https://demotiles.maplibre.org/style.json',
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
    const markers = points.map((p) => new Marker().setLngLat([p.lng, p.lat]).addTo(map))
    return () => markers.forEach((m) => m.remove())
  }, [points])

  return <div ref={ref} className="map-container" />
}
