-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- 1. Create Enums (Idempotent using DO blocks)
do $$ begin
    create type public.user_role as enum ('SUPER_ADMIN', 'HOSTEL_ADMIN', 'ACCOUNTANT', 'MAINTENANCE_STAFF', 'STUDENT');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.room_status as enum ('Available', 'Partially occupied', 'Full', 'Maintenance', 'Reserved', 'Inactive');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.bed_status as enum ('Available', 'Occupied', 'Maintenance', 'Inactive');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.hostel_status as enum ('Active', 'Vacated', 'Suspended', 'Archived');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.payment_status as enum ('Paid', 'Partially Paid', 'Pending', 'Overdue', 'Waived');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.payment_method as enum ('Cash', 'UPI', 'Bank Transfer', 'Card', 'Online', 'Other');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.complaint_category as enum ('Electrical', 'Plumbing', 'Furniture', 'Internet', 'Cleaning', 'AC', 'Fan', 'Water', 'Bathroom', 'Security', 'Other');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.complaint_priority as enum ('Low', 'Medium', 'High', 'Emergency');
exception when duplicate_object then null; end $$;

do $$ begin
    create type public.complaint_status as enum ('Submitted', 'Acknowledged', 'Assigned', 'In Progress', 'Waiting', 'Resolved', 'Closed', 'Reopened');
exception when duplicate_object then null; end $$;

-- 2. Create Tables (Idempotent using if not exists)

-- Organizations (Tenants)
create table if not exists public.organizations (
    id uuid primary key default uuid_generate_v4(),
    name text not null,
    domain text,
    status text not null default 'Active',
    subscription_plan text not null default 'Free',
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Profiles (extends auth.users)
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    organization_id uuid references public.organizations(id) on delete cascade,
    role public.user_role not null default 'STUDENT'::public.user_role,
    full_name text not null,
    email text not null unique,
    phone text,
    avatar_url text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Hostels
create table if not exists public.hostels (
    id uuid primary key default uuid_generate_v4(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    name text not null,
    address text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique (organization_id, name)
);

-- Buildings/Blocks
create table if not exists public.buildings (
    id uuid primary key default uuid_generate_v4(),
    hostel_id uuid not null references public.hostels(id) on delete cascade,
    name text not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique (hostel_id, name)
);

-- Floors
create table if not exists public.floors (
    id uuid primary key default uuid_generate_v4(),
    building_id uuid not null references public.buildings(id) on delete cascade,
    floor_number integer not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique (building_id, floor_number)
);

-- Rooms
create table if not exists public.rooms (
    id uuid primary key default uuid_generate_v4(),
    floor_id uuid not null references public.floors(id) on delete cascade,
    room_number text not null,
    capacity integer not null,
    room_type text not null,
    status public.room_status not null default 'Available'::public.room_status,
    notes text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique (floor_id, room_number)
);

-- Beds
create table if not exists public.beds (
    id uuid primary key default uuid_generate_v4(),
    room_id uuid not null references public.rooms(id) on delete cascade,
    bed_number text not null,
    status public.bed_status not null default 'Available'::public.bed_status,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique (room_id, bed_number)
);

-- Students
create table if not exists public.students (
    id uuid primary key references public.profiles(id) on delete cascade,
    student_id_number text not null,
    dob date,
    gender text,
    course text,
    department text,
    academic_year text,
    parent_name text,
    parent_phone text,
    emergency_contact text,
    address text,
    admission_date date default current_date not null,
    hostel_status public.hostel_status not null default 'Active'::public.hostel_status,
    joining_date date,
    leaving_date date,
    unique(id)
);

-- Room Allocations
create table if not exists public.room_allocations (
    id uuid primary key default uuid_generate_v4(),
    student_id uuid not null references public.students(id) on delete cascade,
    bed_id uuid not null references public.beds(id) on delete cascade,
    allocated_at timestamp with time zone default timezone('utc'::text, now()) not null,
    vacated_at timestamp with time zone,
    status text not null default 'Active',
    notes text,
    recorded_by uuid references public.profiles(id),
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Fee Invoices
create table if not exists public.fee_invoices (
    id uuid primary key default uuid_generate_v4(),
    student_id uuid not null references public.students(id) on delete cascade,
    billing_month text not null,
    amount numeric(10,2) not null,
    due_date date not null,
    discount numeric(10,2) not null default 0.00,
    late_fee numeric(10,2) not null default 0.00,
    amount_paid numeric(10,2) not null default 0.00,
    balance numeric(10,2) not null,
    payment_status public.payment_status not null default 'Pending'::public.payment_status,
    payment_date timestamp with time zone,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique (student_id, billing_month)
);

-- Payments
create table if not exists public.payments (
    id uuid primary key default uuid_generate_v4(),
    student_id uuid not null references public.students(id) on delete cascade,
    fee_invoice_id uuid not null references public.fee_invoices(id) on delete cascade,
    amount numeric(10,2) not null,
    payment_date timestamp with time zone default timezone('utc'::text, now()) not null,
    payment_method public.payment_method not null,
    transaction_id text,
    recorded_by uuid references public.profiles(id),
    notes text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Maintenance Complaints
create table if not exists public.complaints (
    id uuid primary key default uuid_generate_v4(),
    student_id uuid not null references public.students(id) on delete cascade,
    category public.complaint_category not null,
    title text not null,
    description text not null,
    priority public.complaint_priority not null default 'Medium'::public.complaint_priority,
    image_url text,
    status public.complaint_status not null default 'Submitted'::public.complaint_status,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Complaint Assignments
create table if not exists public.complaint_assignments (
    id uuid primary key default uuid_generate_v4(),
    complaint_id uuid not null references public.complaints(id) on delete cascade,
    staff_id uuid not null references public.profiles(id) on delete cascade,
    assigned_at timestamp with time zone default timezone('utc'::text, now()) not null,
    resolved_at timestamp with time zone,
    notes text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Complaint Comments
create table if not exists public.complaint_comments (
    id uuid primary key default uuid_generate_v4(),
    complaint_id uuid not null references public.complaints(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    comment text not null,
    image_url text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Announcements
create table if not exists public.announcements (
    id uuid primary key default uuid_generate_v4(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    title text not null,
    message text not null,
    target_audience text not null default 'ALL',
    target_hostel_id uuid references public.hostels(id) on delete set null,
    target_building_id uuid references public.buildings(id) on delete set null,
    publish_date timestamp with time zone default timezone('utc'::text, now()) not null,
    expiry_date timestamp with time zone,
    priority text not null default 'Normal',
    attachment_url text,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Notifications
create table if not exists public.notifications (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    title text not null,
    message text not null,
    type text not null,
    related_entity_id uuid,
    is_read boolean not null default false,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Audit Logs
create table if not exists public.audit_logs (
    id uuid primary key default uuid_generate_v4(),
    organization_id uuid references public.organizations(id) on delete cascade,
    user_id uuid references public.profiles(id) on delete set null,
    action text not null,
    entity_name text not null,
    entity_id uuid,
    metadata jsonb,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 3. Create Indexes for Performance (Idempotent)
create index if not exists idx_profiles_org_id on public.profiles(organization_id);
create index if not exists idx_hostels_org_id on public.hostels(organization_id);
create index if not exists idx_rooms_floor_id on public.rooms(floor_id);
create index if not exists idx_beds_room_id on public.beds(room_id);
create index if not exists idx_students_id on public.students(id);
create index if not exists idx_room_allocations_student on public.room_allocations(student_id);
create index if not exists idx_room_allocations_bed on public.room_allocations(bed_id);
create index if not exists idx_fee_invoices_student on public.fee_invoices(student_id);
create index if not exists idx_payments_invoice on public.payments(fee_invoice_id);
create index if not exists idx_complaints_student on public.complaints(student_id);
create index if not exists idx_complaint_assignments_staff on public.complaint_assignments(staff_id);
create index if not exists idx_announcements_org on public.announcements(organization_id);
create index if not exists idx_notifications_user on public.notifications(user_id);
create index if not exists idx_audit_logs_org on public.audit_logs(organization_id);

-- 4. Enable Row Level Security (RLS) on all tables
alter table public.organizations enable row level security;
alter table public.profiles enable row level security;
alter table public.hostels enable row level security;
alter table public.buildings enable row level security;
alter table public.floors enable row level security;
alter table public.rooms enable row level security;
alter table public.beds enable row level security;
alter table public.students enable row level security;
alter table public.room_allocations enable row level security;
alter table public.fee_invoices enable row level security;
alter table public.payments enable row level security;
alter table public.complaints enable row level security;
alter table public.complaint_assignments enable row level security;
alter table public.complaint_comments enable row level security;
alter table public.announcements enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_logs enable row level security;

-- 5. Helper Functions
create or replace function public.current_user_org_id()
returns uuid as $$
declare
  v_org_id uuid;
begin
  select organization_id into v_org_id from public.profiles where id = auth.uid();
  return v_org_id;
end;
$$ language plpgsql security definer set search_path = public;

create or replace function public.current_user_role()
returns public.user_role as $$
declare
  v_role public.user_role;
begin
  select role into v_role from public.profiles where id = auth.uid();
  return v_role;
end;
$$ language plpgsql security definer set search_path = public;

-- 6. Define RLS Policies (Idempotent by dropping if exists before creating)
drop policy if exists "Users can read own organization" on public.organizations;
create policy "Users can read own organization" on public.organizations for select using (id = public.current_user_org_id());

drop policy if exists "Users can read profiles in their org" on public.profiles;
create policy "Users can read profiles in their org" on public.profiles for select using (id = auth.uid() or organization_id = public.current_user_org_id());

drop policy if exists "Admins can manage profiles in their org" on public.profiles;
create policy "Admins can manage profiles in their org" on public.profiles for all using (organization_id = public.current_user_org_id() and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can update their own profile details" on public.profiles;
create policy "Users can update their own profile details" on public.profiles for update using (id = auth.uid());

drop policy if exists "Users can view hostels in their org" on public.hostels;
create policy "Users can view hostels in their org" on public.hostels for select using (organization_id = public.current_user_org_id());

drop policy if exists "Admins can manage hostels in their org" on public.hostels;
create policy "Admins can manage hostels in their org" on public.hostels for all using (organization_id = public.current_user_org_id() and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view buildings in their org" on public.buildings;
create policy "Users can view buildings in their org" on public.buildings for select using (exists (select 1 from public.hostels h where h.id = hostel_id and h.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage buildings" on public.buildings;
create policy "Admins can manage buildings" on public.buildings for all using (exists (select 1 from public.hostels h where h.id = hostel_id and h.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view floors" on public.floors;
create policy "Users can view floors" on public.floors for select using (exists (select 1 from public.buildings b join public.hostels h on b.hostel_id = h.id where b.id = building_id and h.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage floors" on public.floors;
create policy "Admins can manage floors" on public.floors for all using (exists (select 1 from public.buildings b join public.hostels h on b.hostel_id = h.id where b.id = building_id and h.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view rooms" on public.rooms;
create policy "Users can view rooms" on public.rooms for select using (exists (select 1 from public.floors f join public.buildings b on f.building_id = b.id join public.hostels h on b.hostel_id = h.id where f.id = floor_id and h.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage rooms" on public.rooms;
create policy "Admins can manage rooms" on public.rooms for all using (exists (select 1 from public.floors f join public.buildings b on f.building_id = b.id join public.hostels h on b.hostel_id = h.id where f.id = floor_id and h.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view beds" on public.beds;
create policy "Users can view beds" on public.beds for select using (exists (select 1 from public.rooms r join public.floors f on r.floor_id = f.id join public.buildings b on f.building_id = b.id join public.hostels h on b.hostel_id = h.id where r.id = room_id and h.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage beds" on public.beds;
create policy "Admins can manage beds" on public.beds for all using (exists (select 1 from public.rooms r join public.floors f on r.floor_id = f.id join public.buildings b on f.building_id = b.id join public.hostels h on b.hostel_id = h.id where r.id = room_id and h.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view student records in their org" on public.students;
create policy "Users can view student records in their org" on public.students for select using (exists (select 1 from public.profiles p where p.id = students.id and p.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage student records" on public.students;
create policy "Admins can manage student records" on public.students for all using (exists (select 1 from public.profiles p where p.id = students.id and p.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Students can update their own student record details" on public.students;
create policy "Students can update their own student record details" on public.students for update using (id = auth.uid());

drop policy if exists "Users can view room allocations in their org" on public.room_allocations;
create policy "Users can view room allocations in their org" on public.room_allocations for select using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage room allocations" on public.room_allocations;
create policy "Admins can manage room allocations" on public.room_allocations for all using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view invoices in their org" on public.fee_invoices;
create policy "Users can view invoices in their org" on public.fee_invoices for select using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()));

drop policy if exists "Admins and Accountants can manage invoices" on public.fee_invoices;
create policy "Admins and Accountants can manage invoices" on public.fee_invoices for all using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN', 'ACCOUNTANT'));

drop policy if exists "Users can view payments in their org" on public.payments;
create policy "Users can view payments in their org" on public.payments for select using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()));

drop policy if exists "Admins and Accountants can record payments" on public.payments;
create policy "Admins and Accountants can record payments" on public.payments for all using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN', 'ACCOUNTANT'));

drop policy if exists "Students can view and manage their own complaints" on public.complaints;
create policy "Students can view and manage their own complaints" on public.complaints for all using (student_id = auth.uid());

drop policy if exists "Admins can view and manage all complaints" on public.complaints;
create policy "Admins can view and manage all complaints" on public.complaints for all using (exists (select 1 from public.profiles p where p.id = student_id and p.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Maintenance staff can view complaints assigned to them" on public.complaints;
create policy "Maintenance staff can view complaints assigned to them" on public.complaints for select using (public.current_user_role() = 'MAINTENANCE_STAFF'::public.user_role and exists (select 1 from public.complaint_assignments ca where ca.complaint_id = complaints.id and ca.staff_id = auth.uid()));

drop policy if exists "Maintenance staff can update status of complaints assigned to them" on public.complaints;
create policy "Maintenance staff can update status of complaints assigned to them" on public.complaints for update using (public.current_user_role() = 'MAINTENANCE_STAFF'::public.user_role and exists (select 1 from public.complaint_assignments ca where ca.complaint_id = complaints.id and ca.staff_id = auth.uid()));

drop policy if exists "View assignments" on public.complaint_assignments;
create policy "View assignments" on public.complaint_assignments for select using (exists (select 1 from public.profiles p where p.id = staff_id and p.organization_id = public.current_user_org_id()));

drop policy if exists "Admins can manage assignments" on public.complaint_assignments;
create policy "Admins can manage assignments" on public.complaint_assignments for all using (exists (select 1 from public.profiles p where p.id = staff_id and p.organization_id = public.current_user_org_id()) and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "View comments for visible complaints" on public.complaint_comments;
create policy "View comments for visible complaints" on public.complaint_comments for select using (exists (select 1 from public.complaints c where c.id = complaint_id));

drop policy if exists "Post comments for visible complaints" on public.complaint_comments;
create policy "Post comments for visible complaints" on public.complaint_comments for insert with check (user_id = auth.uid());

drop policy if exists "Users can view announcements in their org" on public.announcements;
create policy "Users can view announcements in their org" on public.announcements for select using (organization_id = public.current_user_org_id());

drop policy if exists "Admins can manage announcements" on public.announcements;
create policy "Admins can manage announcements" on public.announcements for all using (organization_id = public.current_user_org_id() and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

drop policy if exists "Users can view and manage their own notifications" on public.notifications;
create policy "Users can view and manage their own notifications" on public.notifications for all using (user_id = auth.uid());

drop policy if exists "Admins can view audit logs" on public.audit_logs;
create policy "Admins can view audit logs" on public.audit_logs for select using (organization_id = public.current_user_org_id() and public.current_user_role() in ('SUPER_ADMIN', 'HOSTEL_ADMIN'));

-- 7. DB Functions and Triggers
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, full_name, email, role, organization_id)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.email),
    new.email,
    coalesce((new.raw_user_meta_data->>'role')::public.user_role, 'STUDENT'::public.user_role),
    (new.raw_user_meta_data->>'organization_id')::uuid
  );
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

create or replace function public.log_action()
returns trigger as $$
declare
  v_org_id uuid;
begin
  select organization_id into v_org_id from public.profiles where id = auth.uid();
  
  insert into public.audit_logs (organization_id, user_id, action, entity_name, entity_id, metadata)
  values (
    v_org_id,
    auth.uid(),
    TG_OP,
    TG_TABLE_NAME,
    case 
      when TG_OP = 'DELETE' then OLD.id
      else NEW.id
    end,
    row_to_json(case when TG_OP = 'DELETE' then OLD else NEW end)::jsonb
  );
  return null;
end;
$$ language plpgsql security definer;

drop trigger if exists audit_student_changes on public.students;
create trigger audit_student_changes
  after insert or update or delete on public.students
  for each row execute procedure public.log_action();

drop trigger if exists audit_allocation_changes on public.room_allocations;
create trigger audit_allocation_changes
  after insert or update or delete on public.room_allocations
  for each row execute procedure public.log_action();

drop trigger if exists audit_payment_changes on public.payments;
create trigger audit_payment_changes
  after insert or update or delete on public.payments
  for each row execute procedure public.log_action();

drop trigger if exists audit_complaint_changes on public.complaints;
create trigger audit_complaint_changes
  after update on public.complaints
  for each row execute procedure public.log_action();
