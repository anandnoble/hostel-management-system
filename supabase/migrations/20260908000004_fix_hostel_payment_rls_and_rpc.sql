-- Migration: Sync all hostel rows & update RPCs for payment config
-- File: supabase/migrations/20260908000004_fix_hostel_payment_rls_and_rpc.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Ensure payment & fee columns exist on public.hostels
ALTER TABLE public.hostels
    ADD COLUMN IF NOT EXISTS upi_id text,
    ADD COLUMN IF NOT EXISTS upi_id_encrypted text,
    ADD COLUMN IF NOT EXISTS monthly_fee numeric(10,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS advance_deposit numeric(10,2) DEFAULT 0.00;

-- 2. Grant full permissions on hostels table
GRANT ALL ON public.hostels TO anon, authenticated, service_role;
ALTER TABLE public.hostels ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can read hostels for self-registration" ON public.hostels;
DROP POLICY IF EXISTS "Allow public select on hostels" ON public.hostels;
DROP POLICY IF EXISTS "Allow public insert on hostels" ON public.hostels;
DROP POLICY IF EXISTS "Allow public update on hostels" ON public.hostels;

CREATE POLICY "Allow public select on hostels" ON public.hostels FOR SELECT TO public USING (true);
CREATE POLICY "Allow public insert on hostels" ON public.hostels FOR INSERT TO public WITH CHECK (true);
CREATE POLICY "Allow public update on hostels" ON public.hostels FOR UPDATE TO public USING (true);

-- 3. Update all existing hostel rows to copy non-null payment config
UPDATE public.hostels
SET upi_id = '9390952712-3@ybl',
    monthly_fee = 5000.00,
    advance_deposit = 100.00
WHERE upi_id IS NULL OR upi_id = '';

-- 4. RPC to save payment settings safely across all hostel records for the organization
CREATE OR REPLACE FUNCTION save_hostel_payment_config(
    p_hostel_id text,
    p_upi_id text,
    p_monthly_fee numeric,
    p_advance_deposit numeric
) RETURNS json AS $$
DECLARE
    v_hostel_uuid uuid;
    v_org_uuid uuid;
    v_target_id uuid;
BEGIN
    BEGIN v_hostel_uuid := p_hostel_id::uuid; EXCEPTION WHEN OTHERS THEN v_hostel_uuid := NULL; END;

    IF v_hostel_uuid IS NOT NULL AND EXISTS (SELECT 1 FROM public.hostels WHERE id = v_hostel_uuid) THEN
        UPDATE public.hostels
        SET upi_id = p_upi_id,
            monthly_fee = p_monthly_fee,
            advance_deposit = p_advance_deposit
        WHERE id = v_hostel_uuid;
        v_target_id := v_hostel_uuid;
    ELSE
        -- Update all hostel records so every hostel row under this account gets the payment settings
        UPDATE public.hostels
        SET upi_id = p_upi_id,
            monthly_fee = p_monthly_fee,
            advance_deposit = p_advance_deposit;

        SELECT id INTO v_target_id FROM public.hostels ORDER BY created_at DESC LIMIT 1;

        IF v_target_id IS NULL THEN
            SELECT id INTO v_org_uuid FROM public.organizations LIMIT 1;
            IF v_org_uuid IS NULL THEN
                v_org_uuid := '00000000-0000-0000-0000-000000000000'::uuid;
            END IF;

            v_target_id := COALESCE(v_hostel_uuid, uuid_generate_v4());
            INSERT INTO public.hostels (id, organization_id, name, address, upi_id, monthly_fee, advance_deposit)
            VALUES (v_target_id, v_org_uuid, 'Main Property', 'Campus', p_upi_id, p_monthly_fee, p_advance_deposit);
        END IF;
    END IF;

    RETURN json_build_object(
        'success', true,
        'hostel_id', v_target_id::text,
        'upi_id', p_upi_id,
        'monthly_fee', p_monthly_fee,
        'advance_deposit', p_advance_deposit
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION save_hostel_payment_config(text, text, numeric, numeric) TO anon, authenticated, service_role;

-- 5. RPC to fetch payment settings for Web Portal (prioritizes records with valid payment info)
CREATE OR REPLACE FUNCTION get_hostel_payment_config(p_hostel_id text DEFAULT NULL)
RETURNS TABLE (
    id uuid,
    name text,
    upi_id text,
    monthly_fee numeric,
    advance_deposit numeric
) AS $$
BEGIN
    RETURN QUERY
    SELECT h.id, h.name, h.upi_id, h.monthly_fee, h.advance_deposit
    FROM public.hostels h
    WHERE (p_hostel_id IS NULL OR p_hostel_id = '' OR p_hostel_id = 'hostel_default' OR h.id::text = p_hostel_id)
      AND (h.upi_id IS NOT NULL AND h.upi_id != '')
    ORDER BY h.created_at DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN QUERY
        SELECT h.id, h.name, h.upi_id, h.monthly_fee, h.advance_deposit
        FROM public.hostels h
        WHERE (p_hostel_id IS NULL OR p_hostel_id = '' OR p_hostel_id = 'hostel_default' OR h.id::text = p_hostel_id)
        ORDER BY h.created_at DESC
        LIMIT 1;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION get_hostel_payment_config(text) TO anon, authenticated, service_role;
