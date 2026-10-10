-- LogisTech B2B: exponer tablas a la Data API + RLS inicial (modo desarrollo).
-- Ejecutar DESPUÉS de docs/02-modelo-datos.sql, en Supabase SQL Editor.
-- TODO prod: sustituir las políticas permisivas por políticas por company_id + auth.jwt().

-- 1. Exponer tablas a los roles de la Data API (necesario si desactivas
--    "Automatically expose new tables", como recomienda Supabase)
grant select, insert, update, delete on companies to anon, authenticated;
grant select, insert, update, delete on drivers to anon, authenticated;
grant select, insert, update, delete on orders to anon, authenticated;
grant select, insert, update, delete on routes to anon, authenticated;
grant select, insert, update, delete on route_stops to anon, authenticated;

-- 2. Activar RLS (por si "Enable automatic RLS" no lo hizo)
alter table companies enable row level security;
alter table drivers enable row level security;
alter table orders enable row level security;
alter table routes enable row level security;
alter table route_stops enable row level security;

-- 3. Políticas de desarrollo: acceso total. Restringir antes de producción.
create policy "dev_all" on companies    for all using (true) with check (true);
create policy "dev_all" on drivers      for all using (true) with check (true);
create policy "dev_all" on orders       for all using (true) with check (true);
create policy "dev_all" on routes       for all using (true) with check (true);
create policy "dev_all" on route_stops  for all using (true) with check (true);
