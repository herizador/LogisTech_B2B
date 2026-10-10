import { useEffect, useRef } from 'react'
import { Map, Marker } from 'maplibre-gl'

export default function MapView() {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!ref.current) return
    const map = new Map({
      container: ref.current,
      style: 'https://demotiles.maplibre.org/style.json',
      center: [-0.877, 41.648], // Zaragoza (Los Enlaces)
      zoom: 11,
    })
    new Marker().setLngLat([-0.877, 41.648]).addTo(map)
    return () => map.remove()
  }, [])

  return <div ref={ref} className="map-container" />
}
