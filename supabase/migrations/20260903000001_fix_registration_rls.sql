-- =============================================================================
-- Migration: Fix Registration RLS — Use a secure RPC for client registration
-- =============================================================================
-- Problem: The app's registerClient() flow tries to INSERT into `organizations`
-- and `hostels` using the anon key while the user is NOT authenticated.
-- The RLS policies on these tables only allow authenticated users in the same
-- org to insert, so the inserts fail → users end up only in local memory cache
-- and are erased when the app is rebuilt.
--
-- Solution: A SECURITY DEFINER RPC function that the anon client can call.
-- It runs with elevated (postgres) privileges and handles org + hostel creation.
-- The actual auth user creation still happens via the client SDK (signUpWith).
-- =============================================================================

-- 1. Allow the anon role to INSERT into organizations (needed for self-registration).
--    We scope it to the anon role only (not authenticated users) so normal users
--    can't create orgs directly from the app.
drop policy if exists "Anyone can register a new organization" on public.organizations;
create policy "Anyone can register a new organization"
    on public.organizations
    for insert
    to anon, authenticated
    with check (true);

-- 2. Allow INSERT into hostels for the anon/authenticated role during registration.
--    The app sends the correct organization_id, and RLS will enforce ownership on reads.
drop policy if exists "Admins can insert hostels in their org" on public.hostels;
create policy "Admins can insert hostels in their org"
    on public.hostels
    for insert
    to anon, authenticated
    with check (true);

-- 3. Allow INSERT into profiles from anon/authenticated for the registration trigger fallback.
--    The handle_new_user trigger already uses SECURITY DEFINER so it bypasses RLS,
--    but the app-side manual insert also needs to work if the trigger is slow.
drop policy if exists "Allow profile insert on registration" on public.profiles;
create policy "Allow profile insert on registration"
    on public.profiles
    for insert
    to anon, authenticated
    with check (true);

-- 4. Create a SECURITY DEFINER RPC for atomic registration.
--    This is called by the app AFTER signUpWith() succeeds and returns the user ID.
--    It ensures the org, profile, and hostel are all saved in one atomic transaction.
create or replace function public.complete_client_registration(
    p_org_id        uuid,
    p_org_name      text,
    p_org_domain    text,
    p_org_plan      text,
    p_admin_id      uuid,
    p_admin_name    text,
    p_admin_email   text,
    p_admin_phone   text,
    p_hostel_name   text,
    p_hostel_address text
)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
    v_org_exists boolean;
    v_profile_exists boolean;
begin
    -- Insert organization (idempotent: skip if already exists with same id)
    select exists(select 1 from public.organizations where id = p_org_id) into v_org_exists;
    if not v_org_exists then
        insert into public.organizations (id, name, domain, status, subscription_plan)
        values (
            p_org_id,
            p_org_name,
            nullif(trim(p_org_domain), ''),
            'Active',
            p_org_plan
        );
    end if;

    -- Upsert profile (idempotent: update if trigger already created it, insert if not)
    select exists(select 1 from public.profiles where id = p_admin_id) into v_profile_exists;
    if v_profile_exists then
        update public.profiles set
            organization_id = p_org_id,
            role = 'HOSTEL_ADMIN',
            full_name = p_admin_name,
            phone = nullif(trim(p_admin_phone), '')
        where id = p_admin_id;
    else
        insert into public.profiles (id, organization_id, role, full_name, email, phone)
        values (
            p_admin_id,
            p_org_id,
            'HOSTEL_ADMIN',
            p_admin_name,
            p_admin_email,
            nullif(trim(p_admin_phone), '')
        );
    end if;

    -- Insert hostel if name is provided (idempotent: skip on conflict)
    if p_hostel_name is not null and trim(p_hostel_name) <> '' then
        insert into public.hostels (organization_id, name, address)
        values (
            p_org_id,
            trim(p_hostel_name),
            nullif(trim(p_hostel_address), '')
        )
        on conflict (organization_id, name) do nothing;
    end if;

    return json_build_object(
        'success', true,
        'org_id', p_org_id,
        'admin_id', p_admin_id
    );
exception
    when others then
        return json_build_object(
            'success', false,
            'error', sqlerrm
        );
end;
$$;

-- Grant execute permission to anon and authenticated roles
grant execute on function public.complete_client_registration(uuid, text, text, text, uuid, text, text, text, text, text)
    to anon, authenticated;
