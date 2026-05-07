-- =========================================================
-- DYVAT DATABASE SCHEMA
-- Android Kotlin + Supabase
-- Best-practice baseline for small grocery inventory app
-- =========================================================

-- Required for gen_random_uuid()
create extension if not exists pgcrypto;

-- =========================================================
-- 1. ENUM TYPES
-- =========================================================

do $$
begin
    if not exists (select 1 from pg_type where typname = 'product_status') then
        create type product_status as enum ('active', 'discontinued');
    end if;

    if not exists (select 1 from pg_type where typname = 'ticket_status') then
        create type ticket_status as enum ('active', 'cancelled');
    end if;
end $$;

-- =========================================================
-- 2. COMMON FUNCTIONS
-- =========================================================

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create or replace function public.generate_dyvat_code()
returns text
language plpgsql
as $$
declare
    letter text;
    digits text;
begin
    letter := chr(65 + floor(random() * 26)::int);
    digits := lpad(floor(random() * 1000000)::int::text, 6, '0');
    return letter || digits;
end;
$$;

-- =========================================================
-- 3. MASTER DATA TABLES
-- =========================================================

create table if not exists public.categories (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    name text not null,
    is_active boolean not null default true,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint categories_name_not_blank check (length(trim(name)) > 0),
    unique (owner_id, id)
);

create unique index if not exists ux_categories_owner_lower_name
on public.categories (owner_id, lower(name));

create table if not exists public.units (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    name text not null,
    is_active boolean not null default true,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint units_name_not_blank check (length(trim(name)) > 0),
    unique (owner_id, id)
);

create unique index if not exists ux_units_owner_lower_name
on public.units (owner_id, lower(name));

create table if not exists public.suppliers (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    name text not null,
    phone text,
    is_active boolean not null default true,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint suppliers_name_not_blank check (length(trim(name)) > 0),
    unique (owner_id, id)
);

create unique index if not exists ux_suppliers_owner_lower_name
on public.suppliers (owner_id, lower(name));

create unique index if not exists ux_suppliers_owner_phone
on public.suppliers (owner_id, phone)
where phone is not null and length(trim(phone)) > 0;

-- =========================================================
-- 4. PRODUCTS
-- =========================================================

create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    code text not null default public.generate_dyvat_code(),
    name text not null,

    category_id uuid not null,
    unit_id uuid not null,
    supplier_id uuid not null,

    default_purchase_price_vnd bigint not null default 0,
    default_sale_price_vnd bigint not null default 0,

    status product_status not null default 'active',

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint products_name_not_blank check (length(trim(name)) > 0),
    constraint products_purchase_price_non_negative check (default_purchase_price_vnd >= 0),
    constraint products_sale_price_non_negative check (default_sale_price_vnd >= 0),

    constraint fk_products_category
        foreign key (owner_id, category_id)
        references public.categories(owner_id, id),

    constraint fk_products_unit
        foreign key (owner_id, unit_id)
        references public.units(owner_id, id),

    constraint fk_products_supplier
        foreign key (owner_id, supplier_id)
        references public.suppliers(owner_id, id),

    unique (owner_id, id),
    unique (owner_id, code)
);

create unique index if not exists ux_products_owner_lower_name
on public.products (owner_id, lower(name));

create index if not exists ix_products_owner_status_name
on public.products (owner_id, status, name);

create index if not exists ix_products_owner_category
on public.products (owner_id, category_id);

create index if not exists ix_products_owner_supplier
on public.products (owner_id, supplier_id);

-- =========================================================
-- 5. PURCHASE TICKETS
-- One purchase ticket = one inventory lot header
-- =========================================================

create table if not exists public.purchase_tickets (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    code text not null default public.generate_dyvat_code(),
    purchase_date date not null default current_date,

    status ticket_status not null default 'active',
    cancelled_at timestamptz,
    cancel_reason text,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint purchase_tickets_cancelled_at_required
        check (
            (status = 'active' and cancelled_at is null)
            or
            (status = 'cancelled' and cancelled_at is not null)
        ),

    unique (owner_id, id),
    unique (owner_id, code)
);

create index if not exists ix_purchase_tickets_owner_date_desc
on public.purchase_tickets (owner_id, purchase_date desc);

create index if not exists ix_purchase_tickets_owner_status_date
on public.purchase_tickets (owner_id, status, purchase_date desc);

-- =========================================================
-- 6. PURCHASE ITEMS
-- Each row is also an inventory lot line
-- =========================================================

create table if not exists public.purchase_items (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    purchase_ticket_id uuid not null,
    product_id uuid not null,
    supplier_id uuid not null,
    unit_id uuid not null,

    expiry_date date,

    quantity_purchased integer not null,
    quantity_remaining integer,

    purchase_price_vnd bigint not null,

    line_total_vnd bigint generated always as
        (quantity_purchased * purchase_price_vnd) stored,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint purchase_items_quantity_positive check (quantity_purchased > 0),
    constraint purchase_items_remaining_non_negative check (quantity_remaining >= 0),
    constraint purchase_items_remaining_not_over_purchased check (quantity_remaining <= quantity_purchased),
    constraint purchase_items_price_non_negative check (purchase_price_vnd >= 0),

    constraint fk_purchase_items_ticket
        foreign key (owner_id, purchase_ticket_id)
        references public.purchase_tickets(owner_id, id)
        on delete restrict,

    constraint fk_purchase_items_product
        foreign key (owner_id, product_id)
        references public.products(owner_id, id),

    constraint fk_purchase_items_supplier
        foreign key (owner_id, supplier_id)
        references public.suppliers(owner_id, id),

    constraint fk_purchase_items_unit
        foreign key (owner_id, unit_id)
        references public.units(owner_id, id),

    unique (owner_id, id)
);

create index if not exists ix_purchase_items_owner_ticket
on public.purchase_items (owner_id, purchase_ticket_id);

create index if not exists ix_purchase_items_owner_product_remaining
on public.purchase_items (owner_id, product_id, quantity_remaining)
where quantity_remaining > 0;

create index if not exists ix_purchase_items_owner_product_expiry
on public.purchase_items (owner_id, product_id, expiry_date)
where quantity_remaining > 0;

create index if not exists ix_purchase_items_owner_supplier
on public.purchase_items (owner_id, supplier_id);

-- =========================================================
-- 7. SALE TICKETS
-- =========================================================

create table if not exists public.sale_tickets (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    code text not null default public.generate_dyvat_code(),
    sale_date date not null default current_date,

    status ticket_status not null default 'active',
    cancelled_at timestamptz,
    cancel_reason text,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint sale_tickets_cancelled_at_required
        check (
            (status = 'active' and cancelled_at is null)
            or
            (status = 'cancelled' and cancelled_at is not null)
        ),

    unique (owner_id, id),
    unique (owner_id, code)
);

create index if not exists ix_sale_tickets_owner_date_desc
on public.sale_tickets (owner_id, sale_date desc);

create index if not exists ix_sale_tickets_owner_status_date
on public.sale_tickets (owner_id, status, sale_date desc);

-- =========================================================
-- 8. SALE ITEMS
-- Each sale item points to the exact purchase item / lot line
-- This preserves historical import price for profit calculation
-- =========================================================

create table if not exists public.sale_items (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,

    sale_ticket_id uuid not null,
    product_id uuid not null,
    purchase_item_id uuid not null,
    unit_id uuid not null,

    quantity_sold integer not null,
    sale_price_vnd bigint not null,

    -- Cost at selling time. Filled from purchase_items.purchase_price_vnd.
    unit_cost_vnd bigint,

    line_revenue_vnd bigint generated always as
        (quantity_sold * sale_price_vnd) stored,

    line_cost_vnd bigint generated always as
        (quantity_sold * unit_cost_vnd) stored,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint sale_items_quantity_positive check (quantity_sold > 0),
    constraint sale_items_sale_price_non_negative check (sale_price_vnd >= 0),
    constraint sale_items_unit_cost_non_negative check (unit_cost_vnd >= 0),

    constraint fk_sale_items_ticket
        foreign key (owner_id, sale_ticket_id)
        references public.sale_tickets(owner_id, id)
        on delete restrict,

    constraint fk_sale_items_product
        foreign key (owner_id, product_id)
        references public.products(owner_id, id),

    constraint fk_sale_items_purchase_item
        foreign key (owner_id, purchase_item_id)
        references public.purchase_items(owner_id, id),

    constraint fk_sale_items_unit
        foreign key (owner_id, unit_id)
        references public.units(owner_id, id),

    unique (owner_id, id)
);

create index if not exists ix_sale_items_owner_ticket
on public.sale_items (owner_id, sale_ticket_id);

create index if not exists ix_sale_items_owner_product
on public.sale_items (owner_id, product_id);

create index if not exists ix_sale_items_owner_purchase_item
on public.sale_items (owner_id, purchase_item_id);

-- =========================================================
-- 9. TRIGGERS FOR DEFAULT VALUES AND STOCK SAFETY
-- =========================================================

create or replace function public.set_purchase_item_remaining()
returns trigger
language plpgsql
as $$
begin
    if new.quantity_remaining is null then
        new.quantity_remaining := new.quantity_purchased;
    end if;

    return new;
end;
$$;

create trigger trg_purchase_items_set_remaining
before insert on public.purchase_items
for each row
execute function public.set_purchase_item_remaining();

create or replace function public.prevent_purchase_cancel_if_sold()
returns trigger
language plpgsql
as $$
begin
    if old.status = 'active' and new.status = 'cancelled' then
        if new.cancelled_at is null then
            new.cancelled_at := now();
        end if;

        if exists (
            select 1
            from public.sale_items si
            join public.sale_tickets st
                on st.owner_id = si.owner_id
               and st.id = si.sale_ticket_id
            where si.owner_id = new.owner_id
              and si.purchase_item_id in (
                  select pi.id
                  from public.purchase_items pi
                  where pi.owner_id = new.owner_id
                    and pi.purchase_ticket_id = new.id
              )
        ) then
            raise exception
                'Cannot cancel purchase ticket because related sale items already exist.';
        end if;

        update public.purchase_items
        set quantity_remaining = 0,
            updated_at = now()
        where owner_id = new.owner_id
          and purchase_ticket_id = new.id;
    end if;

    return new;
end;
$$;

create trigger trg_purchase_tickets_cancel
before update of status on public.purchase_tickets
for each row
execute function public.prevent_purchase_cancel_if_sold();

create or replace function public.handle_sale_item_stock()
returns trigger
language plpgsql
as $$
declare
    v_cost bigint;
    v_ticket_status ticket_status;
begin
    if tg_op in ('INSERT', 'UPDATE') then
        select status
        into v_ticket_status
        from public.sale_tickets
        where owner_id = new.owner_id
          and id = new.sale_ticket_id;

        if v_ticket_status is distinct from 'active' then
            raise exception 'Cannot modify sale items of a non-active sale ticket.';
        end if;
    end if;

    if tg_op = 'DELETE' then
        select status
        into v_ticket_status
        from public.sale_tickets
        where owner_id = old.owner_id
          and id = old.sale_ticket_id;

        if v_ticket_status is distinct from 'active' then
            raise exception 'Cannot delete sale items of a non-active sale ticket.';
        end if;

        update public.purchase_items
        set quantity_remaining = quantity_remaining + old.quantity_sold,
            updated_at = now()
        where owner_id = old.owner_id
          and id = old.purchase_item_id;

        return old;
    end if;

    if tg_op = 'UPDATE' then
        update public.purchase_items
        set quantity_remaining = quantity_remaining + old.quantity_sold,
            updated_at = now()
        where owner_id = old.owner_id
          and id = old.purchase_item_id;
    end if;

    select purchase_price_vnd
    into v_cost
    from public.purchase_items
    where owner_id = new.owner_id
      and id = new.purchase_item_id;

    if v_cost is null then
        raise exception 'Purchase item / inventory lot not found.';
    end if;

    new.unit_cost_vnd := v_cost;

    update public.purchase_items
    set quantity_remaining = quantity_remaining - new.quantity_sold,
        updated_at = now()
    where owner_id = new.owner_id
      and id = new.purchase_item_id
      and quantity_remaining >= new.quantity_sold;

    if not found then
        raise exception 'Not enough stock in selected lot.';
    end if;

    return new;
end;
$$;

create trigger trg_sale_items_stock_insert
before insert on public.sale_items
for each row
execute function public.handle_sale_item_stock();

create trigger trg_sale_items_stock_update
before update on public.sale_items
for each row
execute function public.handle_sale_item_stock();

create trigger trg_sale_items_stock_delete
before delete on public.sale_items
for each row
execute function public.handle_sale_item_stock();

create or replace function public.restore_stock_when_sale_cancelled()
returns trigger
language plpgsql
as $$
begin
    if old.status = 'active' and new.status = 'cancelled' then
        if new.cancelled_at is null then
            new.cancelled_at := now();
        end if;

        update public.purchase_items pi
        set quantity_remaining = pi.quantity_remaining + si.quantity_sold,
            updated_at = now()
        from public.sale_items si
        where si.owner_id = new.owner_id
          and si.sale_ticket_id = new.id
          and pi.owner_id = si.owner_id
          and pi.id = si.purchase_item_id;
    end if;

    return new;
end;
$$;

create trigger trg_sale_tickets_cancel_restore_stock
before update of status on public.sale_tickets
for each row
execute function public.restore_stock_when_sale_cancelled();

-- updated_at triggers
create trigger trg_categories_updated_at
before update on public.categories
for each row execute function public.set_updated_at();

create trigger trg_units_updated_at
before update on public.units
for each row execute function public.set_updated_at();

create trigger trg_suppliers_updated_at
before update on public.suppliers
for each row execute function public.set_updated_at();

create trigger trg_products_updated_at
before update on public.products
for each row execute function public.set_updated_at();

create trigger trg_purchase_tickets_updated_at
before update on public.purchase_tickets
for each row execute function public.set_updated_at();

create trigger trg_purchase_items_updated_at
before update on public.purchase_items
for each row execute function public.set_updated_at();

create trigger trg_sale_tickets_updated_at
before update on public.sale_tickets
for each row execute function public.set_updated_at();

create trigger trg_sale_items_updated_at
before update on public.sale_items
for each row execute function public.set_updated_at();

-- =========================================================
-- 10. VIEWS FOR UI CARDS AND STATISTICS
-- security_invoker = true makes views respect RLS of base tables
-- =========================================================

create or replace view public.v_purchase_ticket_cards
with (security_invoker = true)
as
select
    pt.owner_id,
    pt.id,
    pt.code,
    pt.purchase_date,
    pt.status,
    pt.cancelled_at,
    coalesce(sum(pi.line_total_vnd), 0)::bigint as total_purchase_amount_vnd,
    count(pi.id)::int as item_count
from public.purchase_tickets pt
left join public.purchase_items pi
    on pi.owner_id = pt.owner_id
   and pi.purchase_ticket_id = pt.id
group by pt.owner_id, pt.id, pt.code, pt.purchase_date, pt.status, pt.cancelled_at;

create or replace view public.v_sale_ticket_cards
with (security_invoker = true)
as
select
    st.owner_id,
    st.id,
    st.code,
    st.sale_date,
    st.status,
    st.cancelled_at,
    coalesce(sum(si.line_revenue_vnd), 0)::bigint as total_sale_amount_vnd,
    coalesce(sum(si.line_cost_vnd), 0)::bigint as total_cost_amount_vnd,
    coalesce(sum(si.line_revenue_vnd - si.line_cost_vnd), 0)::bigint as profit_vnd,
    count(si.id)::int as item_count
from public.sale_tickets st
left join public.sale_items si
    on si.owner_id = st.owner_id
   and si.sale_ticket_id = st.id
group by st.owner_id, st.id, st.code, st.sale_date, st.status, st.cancelled_at;

create or replace view public.v_inventory_lot_cards
with (security_invoker = true)
as
select
    pt.owner_id,
    pt.id as purchase_ticket_id,
    pt.code as lot_code,
    pt.purchase_date,
    case
        when pt.status = 'cancelled' then 'cancelled'
        when coalesce(sum(pi.quantity_remaining), 0) = 0 then 'out_of_stock'
        when min(pi.expiry_date) is not null and min(pi.expiry_date) < current_date then 'has_expired_item'
        else 'in_stock'
    end as lot_status,
    coalesce(sum(pi.quantity_remaining * pi.purchase_price_vnd), 0)::bigint as total_inventory_value_vnd,
    coalesce(sum(pi.quantity_remaining), 0)::int as total_remaining_quantity
from public.purchase_tickets pt
left join public.purchase_items pi
    on pi.owner_id = pt.owner_id
   and pi.purchase_ticket_id = pt.id
group by pt.owner_id, pt.id, pt.code, pt.purchase_date, pt.status;

create or replace view public.v_inventory_lot_details
with (security_invoker = true)
as
select
    pi.owner_id,
    pt.id as purchase_ticket_id,
    pt.code as lot_code,
    pt.purchase_date,
    pi.id as purchase_item_id,
    p.code as product_code,
    p.name as product_name,
    u.name as unit_name,
    s.name as supplier_name,
    pi.expiry_date,
    pi.quantity_purchased,
    pi.quantity_remaining,
    pi.purchase_price_vnd,
    (pi.quantity_remaining * pi.purchase_price_vnd)::bigint as remaining_value_vnd
from public.purchase_items pi
join public.purchase_tickets pt
    on pt.owner_id = pi.owner_id
   and pt.id = pi.purchase_ticket_id
join public.products p
    on p.owner_id = pi.owner_id
   and p.id = pi.product_id
join public.units u
    on u.owner_id = pi.owner_id
   and u.id = pi.unit_id
join public.suppliers s
    on s.owner_id = pi.owner_id
   and s.id = pi.supplier_id;

create or replace view public.v_daily_business_summary
with (security_invoker = true)
as
with purchase_daily as (
    select
        pt.owner_id,
        pt.purchase_date as business_date,
        sum(pi.line_total_vnd)::bigint as total_purchase_vnd,
        count(distinct pt.id)::int as purchase_ticket_count
    from public.purchase_tickets pt
    join public.purchase_items pi
        on pi.owner_id = pt.owner_id
       and pi.purchase_ticket_id = pt.id
    where pt.status = 'active'
    group by pt.owner_id, pt.purchase_date
),
sale_daily as (
    select
        st.owner_id,
        st.sale_date as business_date,
        sum(si.line_revenue_vnd)::bigint as total_sale_vnd,
        sum(si.line_cost_vnd)::bigint as total_cost_vnd,
        sum(si.line_revenue_vnd - si.line_cost_vnd)::bigint as profit_vnd,
        count(distinct st.id)::int as sale_ticket_count
    from public.sale_tickets st
    join public.sale_items si
        on si.owner_id = st.owner_id
       and si.sale_ticket_id = st.id
    where st.status = 'active'
    group by st.owner_id, st.sale_date
)
select
    coalesce(p.owner_id, s.owner_id) as owner_id,
    coalesce(p.business_date, s.business_date) as business_date,
    coalesce(p.total_purchase_vnd, 0)::bigint as total_purchase_vnd,
    coalesce(s.total_sale_vnd, 0)::bigint as total_sale_vnd,
    coalesce(s.total_cost_vnd, 0)::bigint as total_cost_vnd,
    coalesce(s.profit_vnd, 0)::bigint as profit_vnd,
    coalesce(p.purchase_ticket_count, 0)::int as purchase_ticket_count,
    coalesce(s.sale_ticket_count, 0)::int as sale_ticket_count
from purchase_daily p
full outer join sale_daily s
    on s.owner_id = p.owner_id
   and s.business_date = p.business_date;

-- =========================================================
-- 11. RLS POLICIES
-- Supabase recommends RLS for protecting data at database level.
-- =========================================================

alter table public.categories enable row level security;
alter table public.units enable row level security;
alter table public.suppliers enable row level security;
alter table public.products enable row level security;
alter table public.purchase_tickets enable row level security;
alter table public.purchase_items enable row level security;
alter table public.sale_tickets enable row level security;
alter table public.sale_items enable row level security;

-- Drop old policies if rerunning script
drop policy if exists "categories_owner_all" on public.categories;
drop policy if exists "units_owner_all" on public.units;
drop policy if exists "suppliers_owner_all" on public.suppliers;
drop policy if exists "products_owner_all" on public.products;
drop policy if exists "purchase_tickets_owner_all" on public.purchase_tickets;
drop policy if exists "purchase_items_owner_all" on public.purchase_items;
drop policy if exists "sale_tickets_owner_all" on public.sale_tickets;
drop policy if exists "sale_items_owner_all" on public.sale_items;

create policy "categories_owner_all"
on public.categories
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "units_owner_all"
on public.units
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "suppliers_owner_all"
on public.suppliers
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "products_owner_all"
on public.products
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "purchase_tickets_owner_all"
on public.purchase_tickets
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "purchase_items_owner_all"
on public.purchase_items
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "sale_tickets_owner_all"
on public.sale_tickets
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "sale_items_owner_all"
on public.sale_items
for all
to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

-- Grants for Supabase authenticated role
grant usage on schema public to authenticated;

grant select, insert, update, delete on
    public.categories,
    public.units,
    public.suppliers,
    public.products,
    public.purchase_tickets,
    public.purchase_items,
    public.sale_tickets,
    public.sale_items
to authenticated;

grant select on
    public.v_purchase_ticket_cards,
    public.v_sale_ticket_cards,
    public.v_inventory_lot_cards,
    public.v_inventory_lot_details,
    public.v_daily_business_summary
to authenticated;