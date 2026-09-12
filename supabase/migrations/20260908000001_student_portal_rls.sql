-- Migration: Enable pgcrypto backend encryption, payment config RPCs, and student portal RLS policies
-- File: supabase/migrations/20260908000001_student_portal_rls.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Add encrypted payment & fee columns to public.hostels
ALTER TABLE public.hostels
    ADD COLUMN IF NOT EXISTS upi_id_encrypted text,
    ADD COLUMN IF NOT EXISTS monthly_fee numeric(10,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS advance_deposit numeric(10,2) DEFAULT 0.00;

-- 2. Create RPC to save and encrypt hostel payment config (Hostel Admin function)
CREATE OR REPLACE FUNCTION public.save_hostel_payment_config(
    p_hostel_id uuid,
    p_upi_id text,
    p_monthly_fee numeric,
    p_advance_deposit numeric
)
RETURNS void AS $$
BEGIN
    UPDATE public.hostels
    SET upi_id_encrypted = CASE
            WHEN p_upi_id IS NULL OR TRIM(p_upi_id) = '' THEN NULL
            ELSE encode(pgp_sym_encrypt(TRIM(p_upi_id), 'hostel_secret_key_2026'), 'hex')
        END,
        monthly_fee = COALESCE(p_monthly_fee, 0.00),
        advance_deposit = COALESCE(p_advance_deposit, 0.00)
    WHERE id = p_hostel_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

GRANT EXECUTE ON FUNCTION public.save_hostel_payment_config(uuid, text, numeric, numeric) TO authenticated, anon;

-- 3. Create RPC to safely decrypt and read hostel payment config
CREATE OR REPLACE FUNCTION public.get_hostel_payment_config(p_hostel_id uuid)
RETURNS TABLE(
    hostel_id uuid,
    hostel_name text,
    address text,
    upi_id text,
    monthly_fee numeric,
    advance_deposit numeric
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        h.id AS hostel_id,
        h.name AS hostel_name,
        h.address,
        CASE
            WHEN h.upi_id_encrypted IS NULL OR TRIM(h.upi_id_encrypted) = '' THEN ''
            ELSE pgp_sym_decrypt(decode(h.upi_id_encrypted, 'hex'), 'hostel_secret_key_2026')
        END AS upi_id,
        COALESCE(h.monthly_fee, 0.00) AS monthly_fee,
        COALESCE(h.advance_deposit, 0.00) AS advance_deposit
    FROM public.hostels h
    WHERE h.id = p_hostel_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

GRANT EXECUTE ON FUNCTION public.get_hostel_payment_config(uuid) TO authenticated, anon;

-- 4. Enable/Ensure student access RLS policies
DROP POLICY IF EXISTS "Students can view their own invoices" ON public.fee_invoices;
CREATE POLICY "Students can view their own invoices"
    ON public.fee_invoices FOR SELECT
    USING (student_id = auth.uid() OR EXISTS (
        SELECT 1 FROM public.profiles p WHERE p.id = fee_invoices.student_id AND p.organization_id = public.current_user_org_id()
    ));

DROP POLICY IF EXISTS "Students can view their own payments" ON public.payments;
CREATE POLICY "Students can view their own payments"
    ON public.payments FOR SELECT
    USING (student_id = auth.uid() OR EXISTS (
        SELECT 1 FROM public.profiles p WHERE p.id = payments.student_id AND p.organization_id = public.current_user_org_id()
    ));

DROP POLICY IF EXISTS "Students can insert payment submissions" ON public.payments;
CREATE POLICY "Students can insert payment submissions"
    ON public.payments FOR INSERT
    WITH CHECK (student_id = auth.uid());

DROP POLICY IF EXISTS "Students can view their own room allocations" ON public.room_allocations;
CREATE POLICY "Students can view their own room allocations"
    ON public.room_allocations FOR SELECT
    USING (student_id = auth.uid() OR EXISTS (
        SELECT 1 FROM public.profiles p WHERE p.id = room_allocations.student_id AND p.organization_id = public.current_user_org_id()
    ));

DROP POLICY IF EXISTS "Students can view announcements" ON public.announcements;
CREATE POLICY "Students can view announcements"
    ON public.announcements FOR SELECT
    USING (organization_id = public.current_user_org_id() OR target_audience = 'ALL');
