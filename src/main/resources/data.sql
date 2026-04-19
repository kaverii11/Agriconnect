-- PREPOPULATE SYSTEM DATA
-- Note: Using JOINED inheritance strategy requires inserts into both 'users' and specialized tables.

-- 1. Create a Logistics Coordinator to manage slots
INSERT INTO users (user_id, name, email, password, user_type)
VALUES (1, 'Logistics Lead', 'admin@agriconnect.com', 'admin123', 'LOGISTICS');

INSERT INTO logistics_coordinators (user_id, zone_covered, vehicle_type)
VALUES (1, 'ALL', 'TRUCK');

-- 2. Create 2 Sample Farmers
INSERT INTO users (user_id, name, email, password, user_type)
VALUES (2, 'Farmer John Doe', 'john.doe@farmers.com', 'farmer123', 'FARMER');

INSERT INTO farmers (user_id, farm_name, farm_location, verified)
VALUES (2, 'Green Valley Farm', 'North Hills', true);

INSERT INTO users (user_id, name, email, password, user_type)
VALUES (3, 'Farmer Sarah Smith', 'sarah.smith@rural.com', 'farmer123', 'FARMER');

INSERT INTO farmers (user_id, farm_name, farm_location, verified)
VALUES (3, 'Sunny Pastures', 'East Ridge', true);

-- 3. Create 3 Sample Consumers
INSERT INTO users (user_id, name, email, password, user_type)
VALUES (4, 'Alice Consumer', 'alice@gmail.com', 'user123', 'CONSUMER');

INSERT INTO consumers (user_id, delivery_address, phone_number)
VALUES (4, '123 Maple St, City Center', '555-0101');

INSERT INTO users (user_id, name, email, password, user_type)
VALUES (5, 'Bob Brown', 'bob.brown@yahoo.com', 'user123', 'CONSUMER');

INSERT INTO consumers (user_id, delivery_address, phone_number)
VALUES (5, '456 Oak Ave, Suburbs', '555-0202');

INSERT INTO users (user_id, name, email, password, user_type)
VALUES (6, 'Charlie Clark', 'charlie.clark@outlook.com', 'user123', 'CONSUMER');

INSERT INTO consumers (user_id, delivery_address, phone_number)
VALUES (6, '789 Pine Rd, Downtown', '555-0303');

-- 4. Create 5 Delivery Slots for testing
INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (1, '2026-05-01 09:00:00', 10, 0, 'City Center', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (2, '2026-05-01 14:00:00', 15, 0, 'Suburbs', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (3, '2026-05-02 10:00:00', 8, 2, 'North Hills', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (4, '2026-05-03 11:00:00', 5, 5, 'Downtown', 'FULL', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (5, '2026-05-04 16:30:00', 20, 0, 'East Ridge', 'OPEN', 1);
