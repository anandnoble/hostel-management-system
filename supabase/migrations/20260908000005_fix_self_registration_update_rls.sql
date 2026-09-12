-- Migration: Fix RLS for student_self_registrations & add SECURITY DEFINER approval RPC
-- File: supabase/migrations/20260908000005_fix_self_registration_update_rls.sql

-- 1. Ensure full permissions on student_self_registrations table
GRANT ALL ON public.student_self_registrations TO anon, authenticated, service_role;
ALTER TABLE public.student_self_registrations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can self-register" ON public.student_self_registrations;
DROP POLICY IF EXISTS "Anyone can read self-registrations" ON public.student_self_registrations;
DROP POLICY IF EXISTS "Admins can manage registrations" ON public.student_self_registrations;
DROP POLICY IF EXISTS "Allow public select on self_registrations" ON public.student_self_registrations;
DROP POLICY IF EXISTS "Allow public insert on self_registrations" ON public.student_self_registrations;
DROP POLICY IF EXISTS "Allow public update on self_registrations" ON public.student_self_registrations;
DROP POLICY IF EXISTS "Allow public all on self_registrations" ON public.student_self_registrations;

CREATE POLICY "Allow public select on self_registrations"
    ON public.student_self_registrations FOR SELECT TO public USING (true);

CREATE POLICY "Allow public insert on self_registrations"
    ON public.student_self_registrations FOR INSERT TO public WITH CHECK (true);

CREATE POLICY "Allow public update on self_registrations"
    ON public.student_self_registrations FOR UPDATE TO public USING (true);

CREATE POLICY "Allow public all on self_registrations"
    ON public.student_self_registrations FOR ALL TO public USING (true);

-- 2. SECURITY DEFINER RPC to approve student self registration guaranteed
CREATE OR REPLACE FUNCTION approve_student_self_registration(
    p_reg_id text,
    p_status text DEFAULT 'Approved'
) RETURNS json AS $$
DECLARE
    v_target_uuid uuid;
BEGIN
    BEGIN
        v_target_uuid := p_reg_id::uuid;
    EXCEPTION WHEN OTHERS THEN
        v_target_uuid := NULL;
    END;

    IF v_target_uuid IS NOT NULL THEN
        UPDATE public.student_self_registrations
        SET status = p_status
        WHERE id = v_target_uuid;
    ELSE
        UPDATE public.student_self_registrations
        SET status = p_status
        WHERE id::text = p_reg_id OR email = p_reg_id;
    END IF;

    RETURN json_build_object(
        'success', true,
        'reg_id', p_reg_id,
        'status', p_status
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION approve_student_self_registration(text, text) TO anon, authenticated, service_role;
