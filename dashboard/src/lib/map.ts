import { setWorkerUrl } from 'maplibre-gl'
// Vite emite el worker como asset versionado y devuelve su URL.
// Sin esto, MapLibre lo busca junto al bundle (/assets/...) donde no existe
// y Vercel devuelve index.html (MIME text/html) -> "Worker failed to load".
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?url'

setWorkerUrl(workerUrl)
