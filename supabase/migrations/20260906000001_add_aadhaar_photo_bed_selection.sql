-- Migration: Add Aadhaar, Photo, and Preferred Room/Bed columns to student_self_registrations
-- Run this in your Supabase SQL Editor

ALTER TABLE public.student_self_registrations
    ADD COLUMN IF NOT EXISTS aadhaar_number text,
    ADD COLUMN IF NOT EXISTS photo_url text,
    ADD COLUMN IF NOT EXISTS aadhaar_doc_url text,
    ADD COLUMN IF NOT EXISTS preferred_floor_id uuid REFERENCES public.floors(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS preferred_room_id uuid REFERENCES public.rooms(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS preferred_bed_id uuid REFERENCES public.beds(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS room_number text,
    ADD COLUMN IF NOT EXISTS bed_number text;

-- Allow unauthenticated public to read hostel hierarchy for self-registration page dropdowns
DROP POLICY IF EXISTS "Anyone can read hostels for self-registration" ON public.hostels;
CREATE POLICY "Anyone can read hostels for self-registration"
    ON public.hostels FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can read buildings for self-registration" ON public.buildings;
CREATE POLICY "Anyone can read buildings for self-registration"
    ON public.buildings FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can read floors for self-registration" ON public.floors;
CREATE POLICY "Anyone can read floors for self-registration"
    ON public.floors FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can read rooms for self-registration" ON public.rooms;
CREATE POLICY "Anyone can read rooms for self-registration"
    ON public.rooms FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can read beds for self-registration" ON public.beds;
CREATE POLICY "Anyone can read beds for self-registration"
    ON public.beds FOR SELECT USING (true);
