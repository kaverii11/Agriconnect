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

-- 4. Create an Admin
INSERT INTO users (user_id, name, email, password, user_type)
VALUES (7, 'Admin User', 'admin@agriconnect.io', 'admin123', 'ADMIN');

INSERT INTO administrators (user_id, department, access_level)
VALUES (7, 'Operations', 5);

-- 5. Create Harvest Batches (Farmer John - ID 2)
INSERT INTO harvest_batches (batch_id, produce_type, total_quantity, quantity_sold, harvest_date, expiry_date, base_price, farmer_id)
VALUES (1, 'Tomatoes', 500, 150, '2026-04-10', '2026-05-10', 2.50, 2);

INSERT INTO harvest_batches (batch_id, produce_type, total_quantity, quantity_sold, harvest_date, expiry_date, base_price, farmer_id)
VALUES (2, 'Carrots', 300, 280, '2026-04-12', '2026-05-12', 1.20, 2);

INSERT INTO harvest_batches (batch_id, produce_type, total_quantity, quantity_sold, harvest_date, expiry_date, base_price, farmer_id)
VALUES (3, 'Potatoes', 1000, 400, '2026-04-15', '2026-06-15', 0.80, 2);

-- Farmer Sarah - ID 3
INSERT INTO harvest_batches (batch_id, produce_type, total_quantity, quantity_sold, harvest_date, expiry_date, base_price, farmer_id)
VALUES (4, 'Organic Apples', 200, 50, '2026-04-18', '2026-05-18', 3.00, 3);

INSERT INTO harvest_batches (batch_id, produce_type, total_quantity, quantity_sold, harvest_date, expiry_date, base_price, farmer_id)
VALUES (5, 'Spinach', 150, 20, '2026-04-19', '2026-04-30', 1.50, 3);

-- 6. Create Group Orders (Active Community Pools)
INSERT INTO group_orders (order_id, batch_id, target_minimum_order, pool_total_quantity, status, created_at)
VALUES (1, 1, 50.0, 25.0, 'OPEN', '2026-04-18 10:00:00');

INSERT INTO group_orders (order_id, batch_id, target_minimum_order, pool_total_quantity, status, created_at)
VALUES (2, 3, 100.0, 40.0, 'OPEN', '2026-04-18 14:30:00');

INSERT INTO group_orders (order_id, batch_id, target_minimum_order, pool_total_quantity, status, created_at)
VALUES (3, 4, 30.0, 28.0, 'OPEN', '2026-04-19 09:00:00');

-- Add some consumers as participants to pool 1 (Tomatoes)
INSERT INTO consumer_group_orders (order_id, user_id) VALUES (1, 4);
INSERT INTO consumer_group_orders (order_id, user_id) VALUES (1, 5);

-- Add Bob to pool 2 (Potatoes)
INSERT INTO consumer_group_orders (order_id, user_id) VALUES (2, 5);

-- 7. Create Delivery Slots for testing
INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (1, '2026-05-01 09:00:00', 10, 0, 'City Center', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (2, '2026-05-01 14:00:00', 15, 0, 'Suburbs', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (3, '2026-05-02 10:00:00', 8, 2, 'North Hills', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (4, '2026-05-03 11:00:00', 12, 0, 'Downtown', 'OPEN', 1);

INSERT INTO delivery_slots (slot_id, slot_time, max_capacity, current_bookings, zone, slot_status, coordinator_id)
VALUES (5, '2026-05-04 16:30:00', 20, 0, 'East Ridge', 'OPEN', 1);

-- 8. Create a sample subscription for Alice
INSERT INTO subscription_boxes (subscription_id, box_type, frequency, active, consumer_id, next_delivery_date, price_per_cycle, veggie_preference, fruit_preference)
VALUES (1, 'VeggieBox', 'WEEKLY', true, 4, '2026-04-26', 15.99, 'No cilantro, local only', null);
