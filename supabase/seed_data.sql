-- 1. Enable pgcrypto extension for password hashing
create extension if not exists pgcrypto;

-- 2. Insert Organization (Pacific University Hostels)
insert into public.organizations (id, name, domain)
values ('d77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Pacific University Hostels', 'pacific.edu')
on conflict (id) do nothing;

-- Note: Users must be created via Supabase Auth API (signUp / admin auth API).
-- Direct SQL insertion into auth.users is removed per Supabase Auth guidelines.

-- 3. Insert Hostels (3 Hostels)
insert into public.hostels (id, organization_id, name, address)
values
('bbbbbbbb-1111-1111-1111-111111111111', 'd77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Aravalli Boys Hostel', 'Pacific Campus North Side'),
('bbbbbbbb-2222-2222-2222-222222222222', 'd77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Nilgiri Girls Hostel', 'Pacific Campus South Side'),
('bbbbbbbb-3333-3333-3333-333333333333', 'd77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Shivalik PG Hostel', 'Pacific Campus East Side')
on conflict (id) do nothing;

-- 4. Insert Buildings (Block A and Block B for hostels)
insert into public.buildings (id, hostel_id, name)
values
('cccccccc-1111-1111-1111-111111111111', 'bbbbbbbb-1111-1111-1111-111111111111', 'Block A'),
('cccccccc-1112-1111-1111-111111111112', 'bbbbbbbb-1111-1111-1111-111111111111', 'Block B'),
('cccccccc-2221-2222-2222-222222222221', 'bbbbbbbb-2222-2222-2222-222222222222', 'Block A'),
('cccccccc-3331-3333-3333-333333333331', 'bbbbbbbb-3333-3333-3333-333333333333', 'Main Block')
on conflict (id) do nothing;

-- 5. Insert Floors (Ground, 1st, 2nd)
insert into public.floors (id, building_id, floor_number)
values
('dddddddd-1110-1111-1111-111111111110', 'cccccccc-1111-1111-1111-111111111111', 0),
('dddddddd-1111-1111-1111-111111111111', 'cccccccc-1111-1111-1111-111111111111', 1),
('dddddddd-1112-1111-1111-111111111112', 'cccccccc-1111-1111-1111-111111111111', 2),
('dddddddd-2210-2222-2222-222222222210', 'cccccccc-2221-2222-2222-222222222221', 0),
('dddddddd-2211-2222-2222-222222222211', 'cccccccc-2221-2222-2222-222222222221', 1)
on conflict (id) do nothing;

-- 6. Insert Rooms
insert into public.rooms (id, floor_id, room_number, capacity, room_type, status, notes)
values
('eeeeeeee-1001-1111-1111-111111111001', 'dddddddd-1110-1111-1111-111111111110', '101', 4, 'Non-AC Quad', 'Partially occupied', 'Near entrance'),
('eeeeeeee-1002-1111-1111-111111111002', 'dddddddd-1110-1111-1111-111111111110', '102', 4, 'Non-AC Quad', 'Full', 'Garden view'),
('eeeeeeee-1003-1111-1111-111111111003', 'dddddddd-1110-1111-1111-111111111110', '103', 2, 'AC Double', 'Available', 'Renovated'),
('eeeeeeee-1004-1111-1111-111111111004', 'dddddddd-1110-1111-1111-111111111110', '104', 2, 'AC Double', 'Maintenance', 'AC repair pending'),
('eeeeeeee-1005-1111-1111-111111111005', 'dddddddd-1110-1111-1111-111111111110', '105', 2, 'Non-AC Double', 'Inactive', 'Reserved for guest'),
('eeeeeeee-1101-1111-1111-111111111101', 'dddddddd-1111-1111-1111-111111111111', '201', 4, 'Non-AC Quad', 'Available', ''),
('eeeeeeee-1102-1111-1111-111111111102', 'dddddddd-1111-1111-1111-111111111111', '202', 2, 'AC Double', 'Available', ''),
('eeeeeeee-1103-1111-1111-111111111103', 'dddddddd-1111-1111-1111-111111111111', '203', 2, 'AC Double', 'Available', ''),
('eeeeeeee-1104-1111-1111-111111111104', 'dddddddd-1111-1111-1111-111111111111', '204', 2, 'Non-AC Double', 'Available', ''),
('eeeeeeee-1105-1111-1111-111111111105', 'dddddddd-1111-1111-1111-111111111111', '205', 1, 'AC Single', 'Available', ''),
('eeeeeeee-2001-2222-2222-222222222001', 'dddddddd-2210-2222-2222-222222222210', '101', 2, 'AC Double', 'Available', 'Girls Block Ground'),
('eeeeeeee-2002-2222-2222-222222222002', 'dddddddd-2210-2222-2222-222222222210', '102', 2, 'Non-AC Double', 'Available', ''),
('eeeeeeee-2003-2222-2222-222222222003', 'dddddddd-2210-2222-2222-222222222210', '103', 4, 'Non-AC Quad', 'Available', ''),
('eeeeeeee-2101-2222-2222-222222222101', 'dddddddd-2211-2222-2222-222222222211', '201', 2, 'AC Double', 'Available', ''),
('eeeeeeee-2102-2222-2222-222222222102', 'dddddddd-2211-2222-2222-222222222211', '202', 1, 'AC Single', 'Available', '')
on conflict (id) do nothing;

-- 7. Insert Beds for Rooms
insert into public.beds (id, room_id, bed_number, status) values
('b000101a-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed A', 'Occupied'),
('b000101b-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed B', 'Occupied'),
('b000101c-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed C', 'Occupied'),
('b000101d-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed D', 'Available'),
('b000102a-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed A', 'Occupied'),
('b000102b-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed B', 'Occupied'),
('b000102c-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed C', 'Occupied'),
('b000102d-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed D', 'Occupied'),
('b000103a-0000-0000-0000-000000000103', 'eeeeeeee-1003-1111-1111-111111111003', 'Bed A', 'Available'),
('b000103b-0000-0000-0000-000000000103', 'eeeeeeee-1003-1111-1111-111111111003', 'Bed B', 'Available'),
('b000201a-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed A', 'Available'),
('b000201b-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed B', 'Available'),
('b000201c-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed C', 'Available'),
('b000201d-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed D', 'Available'),
('b0002101-0000-0000-0000-000000002101', 'eeeeeeee-2001-2222-2222-222222222001', 'Bed A', 'Occupied'),
('b0002102-0000-0000-0000-000000002101', 'eeeeeeee-2001-2222-2222-222222222001', 'Bed B', 'Available')
on conflict (id) do nothing;
