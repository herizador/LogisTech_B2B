# Arquitectura LogisTech B2B v2

## Flujo

Dashboard (Vercel) ↔ Backend API (Render/Fly, Spring Boot) ↔ Supabase (Postgres+PostGIS+Auth+Realtime+Storage)
Repartidor PWA ↔ Supabase Realtime + cola offline (Dexie) → sync a API

## Optimización de rutas (Fase 2)

1. Pedidos del día desde Postgres (lat/lng, ventanas, peso).
2. Matriz distancias reales: OSRM público o Valhalla self-hosted.
3. Timefold Solver (ya en pom) resuelve VRP: minimiza km/tiempo con capacidad y horarios.
4. Guarda ruta ordenada + ETA. Dashboard la pinta con MapLibre. PWA la consume offline.

## Multi-tenant

`companies(id, name)` → resto de tablas con `company_id` + RLS Supabase por `auth.jwt()`.
Roles: admin_empresa, dispatcher, driver.

## Despliegue

- Frontend: Vercel (gratis, CDN)
- Backend: Render/Fly con Dockerfile
- DB: Supabase Cloud (PostGIS: `create extension postgis`)
- CI: GitHub Actions (build + test en push)
