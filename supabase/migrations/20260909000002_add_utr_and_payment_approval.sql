-- Migration: Enhanced Self-Registration Approval with UTR & Payment Status (Paid / Not Paid)
-- File: supabase/migrations/20260909000002_add_utr_and_payment_approval.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Add utr_number and amount_paid columns to student_self_registrations
ALTER TABLE public.student_self_registrations
    ADD COLUMN IF NOT EXISTS utr_number text,
    ADD COLUMN IF NOT EXISTS amount_paid numeric(10,2) DEFAULT 0.0;

-- 2. Enhanced SECURITY DEFINER RPC to approve registration with payment options & create corresponding invoice/payment
CREATE OR REPLACE FUNCTION approve_student_self_registration(
    p_reg_id text,
    p_status text DEFAULT 'Approved',
    p_payment_status text DEFAULT 'Paid',
    p_amount_paid numeric DEFAULT 0.0
) RETURNS json AS $$
DECLARE
    v_reg record;
    v_target_uuid uuid;
    v_bed_id uuid;
    v_profile_id uuid;
    v_hostel_id uuid;
    v_monthly_fee numeric;
    v_final_amount numeric;
    v_invoice_id uuid;
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

    -- Update registration status & payment fields
    UPDATE public.student_self_registrations
    SET status = p_status,
        amount_paid = COALESCE(NULLIF(p_amount_paid, 0), amount_paid, 0.0)
    WHERE id = v_reg.id;

    -- If approved, perform real bed allocation AND create invoice/payment record
    IF p_status = 'Approved' THEN
        v_bed_id := v_reg.preferred_bed_id;
        v_hostel_id := v_reg.hostel_id;

        IF v_hostel_id IS NOT NULL THEN
            SELECT monthly_fee INTO v_monthly_fee FROM public.hostels WHERE id = v_hostel_id LIMIT 1;
        END IF;

        IF p_amount_paid > 0 THEN
            v_final_amount := p_amount_paid;
        ELSIF v_reg.amount_paid IS NOT NULL AND v_reg.amount_paid > 0 THEN
            v_final_amount := v_reg.amount_paid;
        ELSIF v_monthly_fee IS NOT NULL AND v_monthly_fee > 0 THEN
            v_final_amount := v_monthly_fee;
        ELSE
            v_final_amount := 5000.00;
        END IF;

        -- Fallback: locate bed by bed_number if preferred_bed_id is null
        IF v_bed_id IS NULL AND v_reg.bed_number IS NOT NULL AND v_reg.bed_number NOT ILIKE '%unassigned%' AND v_reg.bed_number NOT ILIKE '%select%' THEN
            SELECT id INTO v_bed_id FROM public.beds 
            WHERE bed_number ILIKE '%' || v_reg.bed_number || '%' 
            LIMIT 1;
        END IF;

        -- Ensure profile exists
        SELECT id INTO v_profile_id FROM public.profiles WHERE email = v_reg.email LIMIT 1;
        IF v_profile_id IS NULL THEN
            v_profile_id := v_reg.id;
            INSERT INTO public.profiles (id, full_name, email, phone, role)
            VALUES (v_profile_id, v_reg.full_name, v_reg.email, v_reg.phone, 'STUDENT'::public.user_role)
            ON CONFLICT (id) DO NOTHING;
        END IF;

        -- Ensure student record exists
        INSERT INTO public.students (id, student_id_number, aadhaar_number, hostel_status)
        VALUES (v_profile_id, v_reg.student_id_number, v_reg.aadhaar_number, 'Active')
        ON CONFLICT (id) DO NOTHING;

        -- Mark bed as Occupied & insert allocation
        IF v_bed_id IS NOT NULL THEN
            UPDATE public.beds
            SET status = 'Occupied'::public.bed_status
            WHERE id = v_bed_id;

            INSERT INTO public.room_allocations (student_id, bed_id, allocated_at, status, notes)
            VALUES (v_profile_id, v_bed_id, now(), 'Active', 'Approved self-registration')
            ON CONFLICT DO NOTHING;
        END IF;

        -- Create Fee Invoice & Payment for this student
        v_invoice_id := uuid_generate_v4();
        IF p_payment_status = 'Paid' THEN
            INSERT INTO public.invoices (id, student_id, billing_month, amount, balance, payment_status, due_date)
            VALUES (v_invoice_id, v_profile_id, to_char(now(), 'FMMonth YYYY'), v_final_amount, 0.00, 'Paid', CURRENT_DATE)
            ON CONFLICT DO NOTHING;

            INSERT INTO public.payments (invoice_id, amount_paid, payment_mode, notes)
            VALUES (v_invoice_id, v_final_amount, 'UPI', 'Registration Advance Payment (Verified UTR: ' || COALESCE(v_reg.utr_number, 'Verified') || ')')
            ON CONFLICT DO NOTHING;
        ELSE
            INSERT INTO public.invoices (id, student_id, billing_month, amount, balance, payment_status, due_date)
            VALUES (v_invoice_id, v_profile_id, to_char(now(), 'FMMonth YYYY'), v_final_amount, v_final_amount, 'Pending', CURRENT_DATE)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    RETURN json_build_object(
        'success', true,
        'reg_id', v_reg.id,
        'status', p_status,
        'payment_status', p_payment_status,
        'bed_id', v_bed_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION approve_student_self_registration(text, text, text, numeric) TO anon, authenticated, service_role;
