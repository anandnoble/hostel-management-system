-- Student Self-Registration Table
-- Run this in your Supabase SQL Editor

create extension if not exists "uuid-ossp";

-- Create the pending registrations table
create table if not exists public.student_self_registrations (
    id uuid primary key default uuid_generate_v4(),
    hostel_id uuid references public.hostels(id) on delete set null,
    full_name text not null,
    email text not null,
    phone text,
    dob date,
    gender text,
    student_id_number text,
    course text,
    department text,
    academic_year text,
    parent_name text,
    parent_phone text,
    emergency_contact text,
    address text,
    status text not null default 'Pending',  -- Pending / Approved / Rejected
    notes text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Enable RLS
alter table public.student_self_registrations enable row level security;

-- Policy: Anyone (including unauthenticated) can INSERT a self-registration
drop policy if exists "Anyone can self-register" on public.student_self_registrations;
create policy "Anyone can self-register"
    on public.student_self_registrations
    for insert
    with check (true);

-- Policy: Authenticated hostel admins can read/update registrations for their hostel
drop policy if exists "Admins can manage registrations" on public.student_self_registrations;
create policy "Admins can manage registrations"
    on public.student_self_registrations
    for all
    using (
        -- SUPER_ADMIN sees all
        public.current_user_role() = 'SUPER_ADMIN'::public.user_role
        or
        -- Hostel admins see registrations belonging to their org's hostels
        exists (
            select 1 from public.hostels h
            where h.id = hostel_id
              and h.organization_id = public.current_user_org_id()
        )
    );

-- Performance index
create index if not exists idx_self_reg_hostel on public.student_self_registrations(hostel_id);
create index if not exists idx_self_reg_status on public.student_self_registrations(status);
create index if not exists idx_self_reg_email on public.student_self_registrations(email);
