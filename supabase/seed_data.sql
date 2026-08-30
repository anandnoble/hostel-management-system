-- 1. Enable pgcrypto extension for password hashing
create extension if not exists pgcrypto;

-- 2. Insert Organization (Pacific University Hostels)
insert into public.organizations (id, name, domain)
values ('d77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Pacific University Hostels', 'pacific.edu')
on conflict (id) do nothing;

-- 3. Seed Users into auth.users
-- Password is 'password123' for all seeded users.
-- The triggers will automatically create corresponding records in public.profiles.

-- Super Admin
insert into auth.users (id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role)
values (
  'a1111111-1111-1111-1111-111111111111', 
  '00000000-0000-0000-0000-000000000000', 
  'admin@pacific.edu', 
  crypt('password123', gen_salt('bf')), 
  now(), 
  '{"provider":"email","providers":["email"]}', 
  '{"full_name":"Dr. Ashok Kumar (Super Admin)","role":"SUPER_ADMIN","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 
  'authenticated', 
  'authenticated'
) on conflict (id) do nothing;

-- Hostel Admin / Warden
insert into auth.users (id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role)
values (
  'b2222222-2222-2222-2222-222222222222', 
  '00000000-0000-0000-0000-000000000000', 
  'warden@pacific.edu', 
  crypt('password123', gen_salt('bf')), 
  now(), 
  '{"provider":"email","providers":["email"]}', 
  '{"full_name":"Prof. Rajesh Sharma (Warden)","role":"HOSTEL_ADMIN","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 
  'authenticated', 
  'authenticated'
) on conflict (id) do nothing;

-- Accountant
insert into auth.users (id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role)
values (
  'c3333333-3333-3333-3333-333333333333', 
  '00000000-0000-0000-0000-000000000000', 
  'accountant@pacific.edu', 
  crypt('password123', gen_salt('bf')), 
  now(), 
  '{"provider":"email","providers":["email"]}', 
  '{"full_name":"Sanjay Gupta (Accountant)","role":"ACCOUNTANT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 
  'authenticated', 
  'authenticated'
) on conflict (id) do nothing;

-- Maintenance Staff
insert into auth.users (id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role)
values (
  'd4444444-4444-4444-4444-444444444444', 
  '00000000-0000-0000-0000-000000000000', 
  'staff@pacific.edu', 
  crypt('password123', gen_salt('bf')), 
  now(), 
  '{"provider":"email","providers":["email"]}', 
  '{"full_name":"Ramesh Yadav (Electrician & Plumbing)","role":"MAINTENANCE_STAFF","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 
  'authenticated', 
  'authenticated'
) on conflict (id) do nothing;

-- Students (20 Students)
-- Using a loop or manual inserts to seed 20 students. Let's do manual inserts for stability.
insert into auth.users (id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role) values
('f0000001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'student1@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Aarav Mehta","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000002-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', 'student2@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Aditya Sharma","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000003-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000', 'student3@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Amit Patel","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000004-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000000', 'student4@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Ananya Sen","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000005-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000000', 'student5@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Devendra Singh","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000006-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000000', 'student6@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Gaurav Joshi","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000007-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000000', 'student7@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Ishaan Nair","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000008-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000000', 'student8@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Karan Malhotra","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000009-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000000', 'student9@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Kavya Iyer","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000010-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000010', 'student10@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Manish Roy","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000011-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000011', 'student11@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Neha Gupta","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000012-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000012', 'student12@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Pooja Rao","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000013-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000013', 'student13@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Rohan Verma","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000014-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000014', 'student14@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Siddharth Das","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000015-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000015', 'student15@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Sneha Reddy","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000016-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000016', 'student16@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Tanvi Deshmukh","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000017-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000017', 'student17@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Utkarsh Mishra","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000018-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000018', 'student18@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Vikram Seth","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000019-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000019', 'student19@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Yash Singhal","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated'),
('f0000020-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000020', 'student20@pacific.edu', crypt('password123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Zoya Khan","role":"STUDENT","organization_id":"d77bb459-715a-471f-b52f-e8b9dfbf87c9"}', 'authenticated', 'authenticated')
on conflict (id) do nothing;

-- 4. Fill in public.students records
insert into public.students (id, student_id_number, dob, gender, course, department, academic_year, parent_name, parent_phone, emergency_contact, address, admission_date, hostel_status, joining_date)
values
('f0000001-0000-0000-0000-000000000001', 'STU2026001', '2005-04-12', 'Male', 'B.Tech', 'Computer Science', 'First Year', 'Satish Mehta', '+919876543210', '+919876543211', 'Flat 101, Residency, Mumbai', '2026-08-01', 'Active', '2026-08-01'),
('f0000002-0000-0000-0000-000000000002', 'STU2026002', '2004-11-23', 'Male', 'B.Tech', 'Information Technology', 'Second Year', 'Vijay Sharma', '+919876543212', '+919876543213', 'House 5, Street 2, Delhi', '2025-08-01', 'Active', '2025-08-01'),
('f0000003-0000-0000-0000-000000000003', 'STU2026003', '2005-01-15', 'Male', 'B.Tech', 'Mechanical Engineering', 'First Year', 'Rakesh Patel', '+919876543214', '+919876543215', 'Ahmedabad, Gujarat', '2026-08-01', 'Active', '2026-08-01'),
('f0000004-0000-0000-0000-000000000004', 'STU2026004', '2004-08-30', 'Female', 'B.Tech', 'Electrical Engineering', 'Second Year', 'Amit Sen', '+919876543216', '+919876543217', 'Kolkata, WB', '2025-08-01', 'Active', '2025-08-01'),
('f0000005-0000-0000-0000-000000000005', 'STU2026005', '2003-05-18', 'Male', 'B.Tech', 'Civil Engineering', 'Third Year', 'Narendra Singh', '+919876543218', '+919876543219', 'Jaipur, Rajasthan', '2024-08-01', 'Active', '2024-08-01'),
('f0000006-0000-0000-0000-000000000006', 'STU2026006', '2004-12-05', 'Male', 'B.Sc', 'Physics', 'Second Year', 'Mahesh Joshi', '+919876543220', '+919876543221', 'Dehradun, UK', '2025-08-01', 'Active', '2025-08-01'),
('f0000007-0000-0000-0000-000000000007', 'STU2026007', '2005-07-22', 'Male', 'BCA', 'Computer Applications', 'First Year', 'Unnikrishnan Nair', '+919876543222', '+919876543223', 'Kochi, Kerala', '2026-08-01', 'Active', '2026-08-01'),
('f0000008-0000-0000-0000-000000000008', 'STU2026008', '2003-10-10', 'Male', 'B.Tech', 'Computer Science', 'Third Year', 'Sanjay Malhotra', '+919876543224', '+919876543225', 'Gurugram, Haryana', '2024-08-01', 'Active', '2024-08-01'),
('f0000009-0000-0000-0000-000000000009', 'STU2026009', '2004-03-14', 'Female', 'M.Tech', 'Biotechnology', 'First Year', 'Balakrishnan Iyer', '+919876543226', '+919876543227', 'Chennai, TN', '2026-08-01', 'Active', '2026-08-01'),
('f0000010-0000-0000-0000-000000000010', 'STU2026010', '2002-09-01', 'Male', 'B.Tech', 'Electronics', 'Fourth Year', 'Nirmal Roy', '+919876543228', '+919876543229', 'Patna, Bihar', '2023-08-01', 'Active', '2023-08-01'),
('f0000011-0000-0000-0000-000000000011', 'STU2026011', '2005-02-28', 'Female', 'B.A.', 'English', 'First Year', 'Subhash Gupta', '+919876543230', '+919876543231', 'Lucknow, UP', '2026-08-01', 'Active', '2026-08-01'),
('f0000012-0000-0000-0000-000000000012', 'STU2026012', '2004-06-17', 'Female', 'B.Com', 'Commerce', 'Second Year', 'Koteswara Rao', '+919876543232', '+919876543233', 'Hyderabad, Telangana', '2025-08-01', 'Active', '2025-08-01'),
('f0000013-0000-0000-0000-000000000013', 'STU2026013', '2004-01-20', 'Male', 'B.Tech', 'Chemical Engineering', 'Second Year', 'Sushil Verma', '+919876543234', '+919876543235', 'Kanpur, UP', '2025-08-01', 'Active', '2025-08-01'),
('f0000014-0000-0000-0000-000000000014', 'STU2026014', '2003-03-31', 'Male', 'B.Tech', 'Metallurgy', 'Third Year', 'Pradip Das', '+919876543236', '+919876543237', 'Bhubaneswar, Odisha', '2024-08-01', 'Active', '2024-08-01'),
('f0000015-0000-0000-0000-000000000015', 'STU2026015', '2004-09-12', 'Female', 'B.Tech', 'Computer Science', 'Second Year', 'Mallikharjuna Reddy', '+919876543238', '+919876543239', 'Guntur, AP', '2025-08-01', 'Active', '2025-08-01'),
('f0000016-0000-0000-0000-000000000016', 'STU2026016', '2005-03-24', 'Female', 'B.Des', 'Design', 'First Year', 'Vinay Deshmukh', '+919876543240', '+919876543241', 'Pune, Maharashtra', '2026-08-01', 'Active', '2026-08-01'),
('f0000017-0000-0000-0000-000000000017', 'STU2026017', '2002-12-08', 'Male', 'MBA', 'Management', 'First Year', 'Anoop Mishra', '+919876543242', '+919876543243', 'Bhopal, MP', '2026-08-01', 'Active', '2026-08-01'),
('f0000018-0000-0000-0000-000000000018', 'STU2026018', '2003-07-06', 'Male', 'B.Tech', 'Information Technology', 'Fourth Year', 'Harish Seth', '+919876543244', '+919876543245', 'Chandigarh', '2023-08-01', 'Active', '2023-08-01'),
('f0000019-0000-0000-0000-000000000019', 'STU2026019', '2004-10-18', 'Male', 'B.Tech', 'Computer Science', 'Second Year', 'Pradeep Singhal', '+919876543246', '+919876543247', 'Noida, UP', '2025-08-01', 'Active', '2025-08-01'),
('f0000020-0000-0000-0000-000000000020', 'STU2026020', '2005-02-15', 'Female', 'B.A.', 'Psychology', 'First Year', 'Javed Khan', '+919876543248', '+919876543249', 'Indore, MP', '2026-08-01', 'Active', '2026-08-01')
on conflict (id) do nothing;

-- 5. Insert Hostels (3 Hostels)
insert into public.hostels (id, organization_id, name, address)
values
('bbbbbbbb-1111-1111-1111-111111111111', 'd77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Aravalli Boys Hostel', 'Pacific Campus North Side'),
('bbbbbbbb-2222-2222-2222-222222222222', 'd77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Nilgiri Girls Hostel', 'Pacific Campus South Side'),
('bbbbbbbb-3333-3333-3333-333333333333', 'd77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Shivalik PG Hostel', 'Pacific Campus East Side')
on conflict (id) do nothing;

-- 6. Insert Buildings (Block A and Block B for hostels)
insert into public.buildings (id, hostel_id, name)
values
-- Aravalli
('cccccccc-1111-1111-1111-111111111111', 'bbbbbbbb-1111-1111-1111-111111111111', 'Block A'),
('cccccccc-1112-1111-1111-111111111112', 'bbbbbbbb-1111-1111-1111-111111111111', 'Block B'),
-- Nilgiri
('cccccccc-2221-2222-2222-222222222221', 'bbbbbbbb-2222-2222-2222-222222222222', 'Block A'),
-- Shivalik
('cccccccc-3331-3333-3333-333333333331', 'bbbbbbbb-3333-3333-3333-333333333333', 'Main Block')
on conflict (id) do nothing;

-- 7. Insert Floors (Ground, 1st, 2nd)
insert into public.floors (id, building_id, floor_number)
values
-- Aravalli Block A Floors
('dddddddd-1110-1111-1111-111111111110', 'cccccccc-1111-1111-1111-111111111111', 0),
('dddddddd-1111-1111-1111-111111111111', 'cccccccc-1111-1111-1111-111111111111', 1),
('dddddddd-1112-1111-1111-111111111112', 'cccccccc-1111-1111-1111-111111111111', 2),
-- Nilgiri Block A Floors
('dddddddd-2210-2222-2222-222222222210', 'cccccccc-2221-2222-2222-222222222221', 0),
('dddddddd-2211-2222-2222-222222222211', 'cccccccc-2221-2222-2222-222222222221', 1)
on conflict (id) do nothing;

-- 8. Insert Rooms (15 Rooms total across hostels)
insert into public.rooms (id, floor_id, room_number, capacity, room_type, status, notes)
values
-- Aravalli Floor 0 Rooms
('eeeeeeee-1001-1111-1111-111111111001', 'dddddddd-1110-1111-1111-111111111110', '101', 4, 'Non-AC Quad', 'Partially occupied', 'Near entrance'),
('eeeeeeee-1002-1111-1111-111111111002', 'dddddddd-1110-1111-1111-111111111110', '102', 4, 'Non-AC Quad', 'Full', 'Garden view'),
('eeeeeeee-1003-1111-1111-111111111003', 'dddddddd-1110-1111-1111-111111111110', '103', 2, 'AC Double', 'Available', 'Renovated'),
('eeeeeeee-1004-1111-1111-111111111004', 'dddddddd-1110-1111-1111-111111111110', '104', 2, 'AC Double', 'Maintenance', 'AC repair pending'),
('eeeeeeee-1005-1111-1111-111111111005', 'dddddddd-1110-1111-1111-111111111110', '105', 2, 'Non-AC Double', 'Inactive', 'Reserved for guest'),
-- Aravalli Floor 1 Rooms
('eeeeeeee-1101-1111-1111-111111111101', 'dddddddd-1111-1111-1111-111111111111', '201', 4, 'Non-AC Quad', 'Available', ''),
('eeeeeeee-1102-1111-1111-111111111102', 'dddddddd-1111-1111-1111-111111111111', '202', 2, 'AC Double', 'Available', ''),
('eeeeeeee-1103-1111-1111-111111111103', 'dddddddd-1111-1111-1111-111111111111', '203', 2, 'AC Double', 'Available', ''),
('eeeeeeee-1104-1111-1111-111111111104', 'dddddddd-1111-1111-1111-111111111111', '204', 2, 'Non-AC Double', 'Available', ''),
('eeeeeeee-1105-1111-1111-111111111105', 'dddddddd-1111-1111-1111-111111111111', '205', 1, 'AC Single', 'Available', ''),
-- Nilgiri Floor 0 Rooms
('eeeeeeee-2001-2222-2222-222222222001', 'dddddddd-2210-2222-2222-222222222210', '101', 2, 'AC Double', 'Available', 'Girls Block Ground'),
('eeeeeeee-2002-2222-2222-222222222002', 'dddddddd-2210-2222-2222-222222222210', '102', 2, 'Non-AC Double', 'Available', ''),
('eeeeeeee-2003-2222-2222-222222222003', 'dddddddd-2210-2222-2222-222222222210', '103', 4, 'Non-AC Quad', 'Available', ''),
-- Nilgiri Floor 1 Rooms
('eeeeeeee-2101-2222-2222-222222222101', 'dddddddd-2211-2222-2222-222222222211', '201', 2, 'AC Double', 'Available', ''),
('eeeeeeee-2102-2222-2222-222222222102', 'dddddddd-2211-2222-2222-222222222211', '202', 1, 'AC Single', 'Available', '')
on conflict (id) do nothing;

-- 9. Insert Beds for Rooms
-- Room 101 Aravalli (4 Beds)
insert into public.beds (id, room_id, bed_number, status) values
('b000101a-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed A', 'Occupied'),
('b000101b-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed B', 'Occupied'),
('b000101c-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed C', 'Occupied'),
('b000101d-0000-0000-0000-000000000101', 'eeeeeeee-1001-1111-1111-111111111001', 'Bed D', 'Available')
on conflict (id) do nothing;

-- Room 102 Aravalli (4 Beds)
insert into public.beds (id, room_id, bed_number, status) values
('b000102a-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed A', 'Occupied'),
('b000102b-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed B', 'Occupied'),
('b000102c-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed C', 'Occupied'),
('b000102d-0000-0000-0000-000000000102', 'eeeeeeee-1002-1111-1111-111111111002', 'Bed D', 'Occupied')
on conflict (id) do nothing;

-- Room 103 Aravalli (2 Beds)
insert into public.beds (id, room_id, bed_number, status) values
('b000103a-0000-0000-0000-000000000103', 'eeeeeeee-1003-1111-1111-111111111003', 'Bed A', 'Available'),
('b000103b-0000-0000-0000-000000000103', 'eeeeeeee-1003-1111-1111-111111111003', 'Bed B', 'Available')
on conflict (id) do nothing;

-- Room 201 Aravalli (4 Beds)
insert into public.beds (id, room_id, bed_number, status) values
('b000201a-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed A', 'Available'),
('b000201b-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed B', 'Available'),
('b000201c-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed C', 'Available'),
('b000201d-0000-0000-0000-000000000201', 'eeeeeeee-1101-1111-1111-111111111101', 'Bed D', 'Available')
on conflict (id) do nothing;

-- Room 101 Nilgiri (Girls - 2 Beds)
insert into public.beds (id, room_id, bed_number, status) values
('b0002101-0000-0000-0000-000000002101', 'eeeeeeee-2001-2222-2222-222222222001', 'Bed A', 'Occupied'),
('b0002102-0000-0000-0000-000000002101', 'eeeeeeee-2001-2222-2222-222222222001', 'Bed B', 'Available')
on conflict (id) do nothing;

-- 10. Room Allocations
insert into public.room_allocations (student_id, bed_id, allocated_at, status, notes, recorded_by)
values
-- Aravalli Room 101 Occupants
('f0000001-0000-0000-0000-000000000001', 'b000101a-0000-0000-0000-000000000101', '2026-08-01 10:00:00+00', 'Active', 'First year B.Tech CS', 'b2222222-2222-2222-2222-222222222222'),
('f0000002-0000-0000-0000-000000000002', 'b000101b-0000-0000-0000-000000000101', '2025-08-01 11:30:00+00', 'Active', 'Second year IT', 'b2222222-2222-2222-2222-222222222222'),
('f0000003-0000-0000-0000-000000000003', 'b000101c-0000-0000-0000-000000000101', '2026-08-01 09:15:00+00', 'Active', 'Mechanical Engineering', 'b2222222-2222-2222-2222-222222222222'),
-- Aravalli Room 102 Occupants
('f0000005-0000-0000-0000-000000000005', 'b000102a-0000-0000-0000-000000000102', '2024-08-01 12:00:00+00', 'Active', '', 'b2222222-2222-2222-2222-222222222222'),
('f0000006-0000-0000-0000-000000000006', 'b000102b-0000-0000-0000-000000000102', '2025-08-01 14:00:00+00', 'Active', '', 'b2222222-2222-2222-2222-222222222222'),
('f0000007-0000-0000-0000-000000000007', 'b000102c-0000-0000-0000-000000000102', '2026-08-01 10:15:00+00', 'Active', '', 'b2222222-2222-2222-2222-222222222222'),
('f0000008-0000-0000-0000-000000000008', 'b000102d-0000-0000-0000-000000000102', '2024-08-01 09:30:00+00', 'Active', '', 'b2222222-2222-2222-2222-222222222222'),
-- Nilgiri Room 101 Occupant (Girls)
('f0000009-0000-0000-0000-000000000009', 'b0002101-0000-0000-0000-000000002101', '2026-08-01 10:00:00+00', 'Active', 'Biotech student', 'b2222222-2222-2222-2222-222222222222');

-- 11. Fee Invoices (Monthly Mess Dues for August 2026)
insert into public.fee_invoices (id, student_id, billing_month, amount, due_date, discount, late_fee, amount_paid, balance, payment_status)
values
-- Student 1: Paid invoice
('99999999-1111-1111-1111-111111111111', 'f0000001-0000-0000-0000-000000000001', '2026-08', 2500.00, '2026-08-15', 0.00, 0.00, 2500.00, 0.00, 'Paid'),
-- Student 2: Partially paid
('99999999-2222-2222-2222-222222222222', 'f0000002-0000-0000-0000-000000000002', '2026-08', 2500.00, '2026-08-15', 0.00, 0.00, 1500.00, 1000.00, 'Partially Paid'),
-- Student 3: Pending
('99999999-3333-3333-3333-333333333333', 'f0000003-0000-0000-0000-000000000003', '2026-08', 2500.00, '2026-08-15', 0.00, 0.00, 0.00, 2500.00, 'Pending'),
-- Student 4: Overdue (billing month July 2026)
('99999999-4444-4444-4444-444444444444', 'f0000004-0000-0000-0000-000000000004', '2026-07', 2500.00, '2026-07-15', 0.00, 100.00, 0.00, 2600.00, 'Overdue'),
-- Student 5: Waived
('99999999-5555-5555-5555-555555555555', 'f0000005-0000-0000-0000-000000000005', '2026-08', 2500.00, '2026-08-15', 2500.00, 0.00, 0.00, 0.00, 'Waived');

-- 12. Payments (Fee records)
insert into public.payments (student_id, fee_invoice_id, amount, payment_date, payment_method, transaction_id, recorded_by, notes)
values
('f0000001-0000-0000-0000-000000000001', '99999999-1111-1111-1111-111111111111', 2500.00, '2026-08-10 14:32:00+00', 'UPI', 'TXN_UPI82736192', 'c3333333-3333-3333-3333-333333333333', 'Paid via GooglePay'),
('f0000002-0000-0000-0000-000000000002', '99999999-2222-2222-2222-222222222222', 1500.00, '2026-08-12 11:20:00+00', 'Cash', 'TXN_CASH_09281', 'c3333333-3333-3333-3333-333333333333', 'Partial payment received');

-- 13. Maintenance Complaints
insert into public.complaints (id, student_id, category, title, description, priority, status)
values
('11111111-aaaa-1111-1111-111111111111', 'f0000001-0000-0000-0000-000000000001', 'Electrical', 'Ceiling Fan Not Working', 'The ceiling fan in Room 101 is making a clicking sound and rotating extremely slowly.', 'High', 'Assigned'),
('22222222-bbbb-2222-2222-222222222222', 'f0000002-0000-0000-0000-000000000002', 'Plumbing', 'Water Leakage in Bathroom Tap', 'The tap in bathroom 3 on floor 1 is leaking water continuously.', 'Medium', 'Submitted'),
('33333333-cccc-3333-3333-333333333333', 'f0000009-0000-0000-0000-000000000009', 'Internet', 'WiFi Router Disconnecting', 'WiFi connection in Room 101 of Nilgiri Girls hostel is very unstable and drops every 5 minutes.', 'Low', 'Resolved');

-- 14. Complaint Assignments
insert into public.complaint_assignments (complaint_id, staff_id, assigned_at)
values
('11111111-aaaa-1111-1111-111111111111', 'd4444444-4444-4444-4444-444444444444', '2026-08-26 10:00:00+00');

-- 15. Complaint Comments
insert into public.complaint_comments (complaint_id, user_id, comment)
values
('11111111-aaaa-1111-1111-111111111111', 'd4444444-4444-4444-4444-444444444444', 'Inspected the fan. The capacitor is damaged and needs to be replaced. Will replace it by tomorrow morning.'),
('33333333-cccc-3333-3333-333333333333', 'd4444444-4444-4444-4444-444444444444', 'WiFi router restarted and channels reconfigured. Tested connection speed, stable now.');

-- 16. Announcements
insert into public.announcements (organization_id, title, message, target_audience, target_hostel_id, priority)
values
('d77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Independence Day Celebration', 'All hostelers are invited to the Independence Day flag hoisting ceremony tomorrow at 8:00 AM at the central library lawns.', 'ALL', null, 'High'),
('d77bb459-715a-471f-b52f-e8b9dfbf87c9', 'Power Shutdown Maintenance', 'Aravalli Hostel will face a temporary power outage on Sunday from 10:00 AM to 1:00 PM due to transformer maintenance.', 'HOSTEL', 'bbbbbbbb-1111-1111-1111-111111111111', 'Normal');
