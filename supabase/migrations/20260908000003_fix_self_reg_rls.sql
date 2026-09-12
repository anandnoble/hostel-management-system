-- Complete Migration: Create public.student_self_registrations table & RLS Policies
-- File: supabase/migrations/20260908000003_fix_self_reg_rls.sql
-- Paste and run this ENTIRE script in your Supabase SQL Editor:

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 0. Ensure payment & fee columns exist on public.hostels
ALTER TABLE public.hostels
    ADD COLUMN IF NOT EXISTS upi_id text,
    ADD COLUMN IF NOT EXISTS upi_id_encrypted text,
    ADD COLUMN IF NOT EXISTS monthly_fee numeric(10,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS advance_deposit numeric(10,2) DEFAULT 0.00;

-- 1. Create the pending registrations table if it does not exist
CREATE TABLE IF NOT EXISTS public.student_self_registrations (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    hostel_id uuid REFERENCES public.hostels(id) ON DELETE SET NULL,
    full_name text NOT NULL,
    email text NOT NULL,
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
    status text NOT NULL DEFAULT 'Pending',
    notes text,
    aadhaar_number text,
    photo_url text,
    aadhaar_doc_url text,
    preferred_floor_id uuid REFERENCES public.floors(id) ON DELETE SET NULL,
    preferred_room_id uuid REFERENCES public.rooms(id) ON DELETE SET NULL,
    preferred_bed_id uuid REFERENCES public.beds(id) ON DELETE SET NULL,
    room_number text,
    bed_number text,
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Grant explicit table permissions
GRANT ALL ON public.student_self_registrations TO anon, authenticated, service_role;

-- 3. Enable Row Level Security
ALTER TABLE public.student_self_registrations ENABLE ROW LEVEL SECURITY;

-- 4. Create RLS Policies for self-registration
DROP POLICY IF EXISTS "Anyone can self-register" ON public.student_self_registrations;
CREATE POLICY "Anyone can self-register"
    ON public.student_self_registrations
    FOR INSERT
    TO public
    WITH CHECK (true);

DROP POLICY IF EXISTS "Anyone can read self-registrations" ON public.student_self_registrations;
CREATE POLICY "Anyone can read self-registrations"
    ON public.student_self_registrations
    FOR SELECT
    TO public
    USING (true);

DROP POLICY IF EXISTS "Admins can manage registrations" ON public.student_self_registrations;
CREATE POLICY "Admins can manage registrations"
    ON public.student_self_registrations
    FOR ALL
    TO authenticated
    USING (true);

-- 5. Allow unauthenticated visitors to read hostel structure for registration dropdowns
DROP POLICY IF EXISTS "Anyone can read hostels for self-registration" ON public.hostels;
CREATE POLICY "Anyone can read hostels for self-registration"
    ON public.hostels FOR SELECT TO public USING (true);

DROP POLICY IF EXISTS "Anyone can read buildings for self-registration" ON public.buildings;
CREATE POLICY "Anyone can read buildings for self-registration"
    ON public.buildings FOR SELECT TO public USING (true);

DROP POLICY IF EXISTS "Anyone can read floors for self-registration" ON public.floors;
CREATE POLICY "Anyone can read floors for self-registration"
    ON public.floors FOR SELECT TO public USING (true);

DROP POLICY IF EXISTS "Anyone can read rooms for self-registration" ON public.rooms;
CREATE POLICY "Anyone can read rooms for self-registration"
    ON public.rooms FOR SELECT TO public USING (true);

DROP POLICY IF EXISTS "Anyone can read beds for self-registration" ON public.beds;
CREATE POLICY "Anyone can read beds for self-registration"
    ON public.beds FOR SELECT TO public USING (true);

-- 6. Create Indexes for performance
CREATE INDEX IF NOT EXISTS idx_self_reg_hostel ON public.student_self_registrations(hostel_id);
CREATE INDEX IF NOT EXISTS idx_self_reg_status ON public.student_self_registrations(status);
CREATE INDEX IF NOT EXISTS idx_self_reg_email ON public.student_self_registrations(email);
