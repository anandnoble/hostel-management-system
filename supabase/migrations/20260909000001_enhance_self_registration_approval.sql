-- Migration: Enhanced Self-Registration Approval RPC & Monthly Payment Submissions Table
-- File: supabase/migrations/20260909000001_enhance_self_registration_approval.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create monthly_payment_submissions table for student portal fee payments
CREATE TABLE IF NOT EXISTS public.monthly_payment_submissions (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    hostel_id uuid REFERENCES public.hostels(id) ON DELETE SET NULL,
    student_id uuid,
    email text NOT NULL,
    full_name text NOT NULL,
    room_number text,
    bed_number text,
    amount numeric(10,2) NOT NULL,
    utr_number text NOT NULL,
    billing_month text NOT NULL,
    status text NOT NULL DEFAULT 'Pending', -- Pending / Verified / Rejected
    notes text,
    created_at timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RLS for monthly_payment_submissions
ALTER TABLE public.monthly_payment_submissions ENABLE ROW LEVEL SECURITY;
GRANT ALL ON public.monthly_payment_submissions TO anon, authenticated, service_role;

DROP POLICY IF EXISTS "Allow public select on monthly_payment_submissions" ON public.monthly_payment_submissions;
DROP POLICY IF EXISTS "Allow public insert on monthly_payment_submissions" ON public.monthly_payment_submissions;
DROP POLICY IF EXISTS "Allow public update on monthly_payment_submissions" ON public.monthly_payment_submissions;
DROP POLICY IF EXISTS "Allow public all on monthly_payment_submissions" ON public.monthly_payment_submissions;

CREATE POLICY "Allow public select on monthly_payment_submissions" ON public.monthly_payment_submissions FOR SELECT TO public USING (true);
CREATE POLICY "Allow public insert on monthly_payment_submissions" ON public.monthly_payment_submissions FOR INSERT TO public WITH CHECK (true);
CREATE POLICY "Allow public update on monthly_payment_submissions" ON public.monthly_payment_submissions FOR UPDATE TO public USING (true);
CREATE POLICY "Allow public all on monthly_payment_submissions" ON public.monthly_payment_submissions FOR ALL TO public USING (true);

-- 2. Enhanced SECURITY DEFINER RPC to approve registration AND allocate bed
CREATE OR REPLACE FUNCTION approve_student_self_registration(
    p_reg_id text,
    p_status text DEFAULT 'Approved'
) RETURNS json AS $$
DECLARE
    v_reg record;
    v_target_uuid uuid;
    v_bed_id uuid;
    v_profile_id uuid;
BEGIN
    -- Parse UUID if possible
    BEGIN
        v_target_uuid := p_reg_id::uuid;
    EXCEPTION WHEN OTHERS THEN
        v_target_uuid := NULL;
    END;

    -- Fetch registration record
    IF v_target_uuid IS NOT NULL THEN
        SELECT * INTO v_reg FROM public.student_self_registrations WHERE id = v_target_uuid;
    ELSE
        SELECT * INTO v_reg FROM public.student_self_registrations WHERE id::text = p_reg_id OR email = p_reg_id LIMIT 1;
    END IF;

    IF v_reg IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Registration record not found');
    END IF;

    -- Update registration status
    UPDATE public.student_self_registrations
    SET status = p_status
    WHERE id = v_reg.id;

    -- If approved, perform real bed allocation and mark bed occupied
    IF p_status = 'Approved' THEN
        v_bed_id := v_reg.preferred_bed_id;

        -- Fallback: locate bed by bed_number if preferred_bed_id is null
        IF v_bed_id IS NULL AND v_reg.bed_number IS NOT NULL AND v_reg.bed_number NOT ILIKE '%unassigned%' AND v_reg.bed_number NOT ILIKE '%select%' THEN
            SELECT id INTO v_bed_id FROM public.beds 
            WHERE bed_number ILIKE '%' || v_reg.bed_number || '%' 
            LIMIT 1;
        END IF;

        -- Mark bed as Occupied
        IF v_bed_id IS NOT NULL THEN
            UPDATE public.beds
            SET status = 'Occupied'::public.bed_status
            WHERE id = v_bed_id;

            -- Check for existing profile matching email or use registration id
            SELECT id INTO v_profile_id FROM public.profiles WHERE email = v_reg.email LIMIT 1;
            IF v_profile_id IS NULL THEN
                v_profile_id := v_reg.id;
            END IF;

            -- Insert room allocation record if not already exists
            INSERT INTO public.room_allocations (student_id, bed_id, allocated_at, status, notes)
            VALUES (v_profile_id, v_bed_id, now(), 'Active', 'Approved self-registration')
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    RETURN json_build_object(
        'success', true,
        'reg_id', v_reg.id,
        'status', p_status,
        'bed_id', v_bed_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION approve_student_self_registration(text, text) TO anon, authenticated, service_role;
