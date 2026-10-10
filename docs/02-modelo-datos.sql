-- LogisTech B2B bootstrap: ejecutar en Supabase SQL Editor
create extension if not exists postgis;

create table if not exists companies (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  created_at timestamptz default now()
);

create table if not exists drivers (
  id uuid primary key default gen_random_uuid(),
  company_id uuid references companies(id) on delete cascade,
  full_name text not null,
  vehicle_capacity_kg numeric default 1000,
  created_at timestamptz default now()
);

create table if not exists orders (
  id uuid primary key default gen_random_uuid(),
  company_id uuid references companies(id) on delete cascade,
  customer_name text not null,
  address text not null,
  location geography(Point, 4326),
  weight_kg numeric default 0,
  window_start timestamptz,
  window_end timestamptz,
  status text default 'pending',
  created_at timestamptz default now()
);

create table if not exists routes (
  id uuid primary key default gen_random_uuid(),
  company_id uuid references companies(id) on delete cascade,
  driver_id uuid references drivers(id) on delete set null,
  date date not null,
  total_km numeric,
  created_at timestamptz default now()
);

create table if not exists route_stops (
  id uuid primary key default gen_random_uuid(),
  route_id uuid references routes(id) on delete cascade,
  order_id uuid references orders(id) on delete cascade,
  seq int not null,
  eta timestamptz,
  delivered_at timestamptz
);

-- Empresa demo
insert into companies (name) values ('Demo Mayorista') on conflict do nothing;
