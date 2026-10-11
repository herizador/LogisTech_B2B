-- LogisTech B2B: índices para las consultas habituales (ejecutar una vez).
create index if not exists idx_orders_company_status on orders (company_id, status);
create index if not exists idx_drivers_company on drivers (company_id);
create index if not exists idx_routes_company_date on routes (company_id, date desc);
create index if not exists idx_route_stops_route_seq on route_stops (route_id, seq);
