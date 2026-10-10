# LogisTech B2B — FMS y Optimización de Rutas

Proyecto propio (origen: propuesta DAW). Monorepo:

- `backend/` — Spring Boot 4 + Java 21 + Timefold Solver + PostGIS (Maven Wrapper incluido, no necesitas instalar Maven)
- `dashboard/` — React 19 + Vite 8 + Tailwind 4 + MapLibre GL + Supabase
- `pwa-driver/` — PWA repartidor (pendiente Fase 2: Workbox + Dexie + Background Sync)
- `docs/` — arquitectura y SQL inicial

## Requisitos

- Java 23 (target 21) ✔ ya lo tienes
- Node 22.12+ (tienes 22.11 — actualiza cuando puedas, funciona igual)
- Git ✔
- Cuenta gratis Supabase (https://supabase.com)
- Sin Docker por ahora (usamos Supabase Cloud como Postgres+PostGIS)

## Arranque rápido

1. Supabase: crea proyecto → copia URL + anon key.
2. Dashboard:
```powershell
cd dashboard
Copy-Item .env.example .env  # y rellena
npm run dev  # http://localhost:5173
```
3. Backend (nueva terminal):
```powershell
cd backend
.\mvnw.cmd spring-boot:run  # http://localhost:8080/api/health
```
4. DB: pega `docs/02-modelo-datos.sql` en Supabase → SQL Editor → Run.

Docs: `docs/01-arquitectura.md`
