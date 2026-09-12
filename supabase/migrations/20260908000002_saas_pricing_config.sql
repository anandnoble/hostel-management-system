-- Migration: SaaS Pricing Configuration & Pricing Model Support
-- File: supabase/migrations/20260908000002_saas_pricing_config.sql

-- 1. Create table for global SaaS pricing settings
CREATE TABLE IF NOT EXISTS public.saas_pricing_config (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    per_student_rate numeric(10,2) NOT NULL DEFAULT 15.00,
    slab1_max_students integer NOT NULL DEFAULT 50,
    slab1_price numeric(10,2) NOT NULL DEFAULT 499.00,
    slab2_max_students integer NOT NULL DEFAULT 150,
    slab2_price numeric(10,2) NOT NULL DEFAULT 1299.00,
    slab3_max_students integer NOT NULL DEFAULT 300,
    slab3_price numeric(10,2) NOT NULL DEFAULT 2499.00,
    updated_at timestamp WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Seed default initial pricing config row if not exists
INSERT INTO public.saas_pricing_config (id, per_student_rate, slab1_max_students, slab1_price, slab2_max_students, slab2_price, slab3_max_students, slab3_price)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, 15.00, 50, 499.00, 150, 1299.00, 300, 2499.00
WHERE NOT EXISTS (SELECT 1 FROM public.saas_pricing_config);

-- 2. Add pricing_model column to public.organizations
ALTER TABLE public.organizations
    ADD COLUMN IF NOT EXISTS pricing_model text DEFAULT 'Slab';

-- 3. Enable RLS & Policies
ALTER TABLE public.saas_pricing_config ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can read SaaS pricing config" ON public.saas_pricing_config;
CREATE POLICY "Anyone can read SaaS pricing config"
    ON public.saas_pricing_config FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admins can manage SaaS pricing config" ON public.saas_pricing_config;
CREATE POLICY "Admins can manage SaaS pricing config"
    ON public.saas_pricing_config FOR ALL USING (true);
