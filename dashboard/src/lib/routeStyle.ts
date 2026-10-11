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

export const ROUTE_COLORS = ['#1d4ed8', '#dc2626', '#059669', '#d97706', '#7c3aed']
