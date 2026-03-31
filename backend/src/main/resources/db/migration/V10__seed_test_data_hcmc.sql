-- Seed Test Data for HCMC Food Delivery Platform V10
-- Complete test data with valid UUIDs and realistic Ho Chi Minh City coordinates

-- ============================================================================
-- 1. USERS: Customers, Merchants, Admin, Delivery Person
-- ============================================================================

INSERT INTO users (id, username, email, phone_number, full_name, avatar_url, role, status, password_hash, created_at, updated_at)
VALUES
('11111111-1111-1111-1111-111111111111', 'admin_foodya', 'admin@foodya.local', '+84912345670', 'System Administrator', NULL, 'ADMIN', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 'merchant_pho', 'merchant_pho@foodya.local', '+84912345671', 'Pho Vietnam Restaurant Owner', NULL, 'MERCHANT', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'merchant_pizza', 'merchant_pizza@foodya.local', '+84912345672', 'Pizza Paradise Manager', NULL, 'MERCHANT', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444', 'merchant_sushi', 'merchant_sushi@foodya.local', '+84912345673', 'Sushi Master Restaurant', NULL, 'MERCHANT', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555555', 'delivery_user', 'delivery@foodya.local', '+84912345674', 'Delivery Agent', NULL, 'DELIVERY', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('66666666-6666-6666-6666-666666666666', 'customer_alice', 'alice@foodya.local', '+84912345675', 'Alice Nguyen', NULL, 'CUSTOMER', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-777777777777', 'customer_bob', 'bob@foodya.local', '+84912345676', 'Bob Tran', NULL, 'CUSTOMER', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('88888888-8888-8888-8888-888888888888', 'customer_carol', 'carol@foodya.local', '+84912345677', 'Carol Dang', NULL, 'CUSTOMER', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('99999999-9999-9999-9999-999999999999', 'customer_david', 'david@foodya.local', '+84912345678', 'David Ho', NULL, 'CUSTOMER', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 2. RESTAURANTS: Located in Ho Chi Minh City
-- ============================================================================

INSERT INTO restaurants (id, owner_user_id, name, cuisine_type, description, address_line, latitude, longitude, h3_index_res9, avg_rating, review_count, status, is_open, max_delivery_km, created_at, updated_at)
VALUES
('a0a0a0a0-0001-0001-0001-a0a0a0a0a001', '22222222-2222-2222-2222-222222222222', 'Pho Vietnam Hà Nội', 'Vietnamese Noodles', 'Traditional Vietnamese Pho restaurant with 20 years experience. Fresh broth made daily.', '45 Lê Lợi, Quận 1, TP. Hồ Chí Minh', 10.7747, 106.7022, '895285a8fffffff', 4.5, 120, 'ACTIVE', true, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0a0a0a0-0002-0002-0002-a0a0a0a0a002', '33333333-3333-3333-3333-333333333333', 'Pizza Paradise Italian Pizzeria', 'Italian Pizza', 'Authentic Italian pizza baked in wood-fired oven. Imported ingredients from Italy.', '123 Võ Văn Tần, Quận 3, TP. Hồ Chí Minh', 10.8006, 106.6702, '895285ab7ffffff', 4.7, 95, 'ACTIVE', true, 6.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a0a0a0a0-0003-0003-0003-a0a0a0a0a003', '44444444-4444-4444-4444-444444444444', 'Sushi Master - Japanese Premium', 'Japanese Sushi', 'Fresh sushi and sashimi from Tokyo fish market. Premium quality ingredients.', '456 Nguyễn Hữu Cảnh, Quận 7, TP. Hồ Chí Minh', 10.7007, 106.7253, '895285a83ffffff', 4.8, 150, 'ACTIVE', true, 7.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 3. MENU CATEGORIES
-- ============================================================================

INSERT INTO menu_categories (id, restaurant_id, name, sort_order, is_active, created_at, updated_at)
VALUES
('b0b0b0b0-0001-0001-0001-b0b0b0b0b001', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'Phở', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0002-0002-0002-b0b0b0b0b002', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'Bún & Cơm', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0003-0003-0003-b0b0b0b0b003', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'Nước & Tráng Miệng', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0004-0004-0004-b0b0b0b0b004', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'Pizza Đặc Biệt (Special)', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0005-0005-0005-b0b0b0b0b005', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'Pizza Cổ Điển (Classic)', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0006-0006-0006-b0b0b0b0b006', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'Salads & Appetizers', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0007-0007-0007-b0b0b0b0b007', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'Sushi Rolls', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0008-0008-0008-b0b0b0b0b008', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'Sashimi Platters', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0b0b0b0-0009-0009-0009-b0b0b0b0b009', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'Miso Soup & Appetizers', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 4. MENU ITEMS
-- ============================================================================

INSERT INTO menu_items (id, restaurant_id, category_id, name, description, price, is_active, is_available, deleted_at, created_at, updated_at)
VALUES
('c0c0c0c0-0001-0001-0001-c0c0c0c0c001', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0001-0001-0001-b0b0b0b0b001', 'Phở Tái (Rare Beef)', 'Traditional pho with rare beef slices in aromatic broth', 65000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0002-0002-0002-c0c0c0c0c002', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0001-0001-0001-b0b0b0b0b001', 'Phở Nạm (Beef Brisket)', 'Tender beef brisket in slow-cooked broth', 70000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0003-0003-0003-c0c0c0c0c003', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0001-0001-0001-b0b0b0b0b001', 'Phở Gà (Chicken)', 'Fragrant chicken pho with chicken breast', 60000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0004-0004-0004-c0c0c0c0c004', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0002-0002-0002-b0b0b0b0b002', 'Bún Thang (Chicken Vermicelli)', 'Delicious chicken vermicelli soup with egg', 55000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0005-0005-0005-c0c0c0c0c005', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0002-0002-0002-b0b0b0b0b002', 'Bún Bò Huế (Hue Beef Noodles)', 'Spicy beef vermicelli noodles, Hue style', 60000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0006-0006-0006-c0c0c0c0c006', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0002-0002-0002-b0b0b0b0b002', 'Cơm Tấm Tài Sính (Broken Rice Special)', 'Fragrant broken rice with grilled chops and egg', 80000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0007-0007-0007-c0c0c0c0c007', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0003-0003-0003-b0b0b0b0b003', 'Nước Mía Đá (Sugar Cane Ice)', 'Fresh sugar cane juice over ice', 25000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0008-0008-0008-c0c0c0c0c008', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0003-0003-0003-b0b0b0b0b003', 'Sữa Đậu Nành (Soy Milk)', 'Warm or cold soy milk', 20000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0009-0009-0009-c0c0c0c0c009', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'b0b0b0b0-0003-0003-0003-b0b0b0b0b003', 'Chè Ba Màu (Three Color Dessert)', 'Traditional layered Vietnamese dessert', 35000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0010-0010-0010-c0c0c0c0c010', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0004-0004-0004-b0b0b0b0b004', 'Pizza Margarita Premium', 'Fresh mozzarella, tomato, basil, olive oil', 185000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0011-0011-0011-c0c0c0c0c011', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0004-0004-0004-b0b0b0b0b004', 'Pizza Bọ Cày (Bacon Paradise)', 'Bacon, pepperoni, ham, and extra cheese', 210000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0012-0012-0012-c0c0c0c0c012', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0004-0004-0004-b0b0b0b0b004', 'Pizza BBQ Chicken', 'Tender chicken, caramelized onions, BBQ sauce', 200000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0013-0013-0013-c0c0c0c0c013', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0005-0005-0005-b0b0b0b0b005', 'Pizza Không Phô Mai (No Cheese)', 'Tomato base with fresh vegetables', 150000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0014-0014-0014-c0c0c0c0c014', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0005-0005-0005-b0b0b0b0b005', 'Pizza Pepperoni', 'Classic pepperoni with mozzarella', 170000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0015-0015-0015-c0c0c0c0c015', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0006-0006-0006-b0b0b0b0b006', 'Caesar Salad', 'Crisp romaine, parmesan, croutons, Caesar dressing', 95000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0016-0016-0016-c0c0c0c0c016', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', 'b0b0b0b0-0006-0006-0006-b0b0b0b0b006', 'Garlic Bread', 'Crispy garlic bread with mozzarella', 60000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0017-0017-0017-c0c0c0c0c017', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0007-0007-0007-b0b0b0b0b007', 'California Roll', 'Crab, avocado, cucumber roll with sesame seeds', 120000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0018-0018-0018-c0c0c0c0c018', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0007-0007-0007-b0b0b0b0b007', 'Toro (Fatty Tuna) Roll', 'Premium fatty tuna with spicy mayo', 180000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0019-0019-0019-c0c0c0c0c019', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0007-0007-0007-b0b0b0b0b007', 'Spicy Salmon Roll', 'Fresh salmon with spicy mayo and sriracha', 140000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0020-0020-0020-c0c0c0c0c020', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0008-0008-0008-b0b0b0b0b008', 'Assorted Sashimi Platter', 'Tuna, salmon, mackeral, and sea bream - 10 pieces', 250000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0021-0021-0021-c0c0c0c0c021', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0008-0008-0008-b0b0b0b0b008', 'Premium Sashimi Combo', 'Chef selection of premium cuts - 15 pieces', 340000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0022-0022-0022-c0c0c0c0c022', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0009-0009-0009-b0b0b0b0b009', 'Miso Soup (Miso Shiru)', 'Traditional soybean-based soup', 35000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0c0c0c0-0023-0023-0023-c0c0c0c0c023', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', 'b0b0b0b0-0009-0009-0009-b0b0b0b0b009', 'Edamame (Steamed Soybeans)', 'Lightly salted steamed soybean pods', 45000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 5. SAMPLE ORDERS
-- ============================================================================

INSERT INTO orders (id, order_code, customer_user_id, idempotency_key, restaurant_id, delivery_address, delivery_latitude, delivery_longitude, customer_note, subtotal_amount, delivery_fee, total_amount, payment_method, payment_status, status, commission_amount, shipping_fee_margin_amount, platform_profit_amount, placed_at, completed_at)
VALUES
('d0d0d0d0-0001-0001-0001-d0d0d0d0d001', 'ORD20260329001', '66666666-6666-6666-6666-666666666666', 'idempotency-key-001', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', '789 Trần Hưng Đạo, Quận 1, TP. Hồ Chí Minh', 10.7654, 106.7089, 'No onions please', 135000, 30000, 165000, 'COD', 'PAID', 'SUCCESS', 13500, 6000, 6600, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d0d0d0d0-0002-0002-0002-d0d0d0d0d002', 'ORD20260330001', '77777777-7777-7777-7777-777777777777', 'idempotency-key-002', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', '456 Nguyễn Hữu Cảnh, Quận 7, TP. Hồ Chí Minh', 10.7123, 106.7198, 'Extra cheese on pizza', 395000, 35000, 430000, 'COD', 'PAID', 'SUCCESS', 39500, 7000, 8750, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d0d0d0d0-0003-0003-0003-d0d0d0d0d003', 'ORD20260331001', '88888888-8888-8888-8888-888888888888', 'idempotency-key-003', 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003', '234 Họp Lực, Quận 1, TP. Hồ Chí Minh', 10.7765, 106.7045, 'Fresh wasabi on the side', 490000, 40000, 530000, 'COD', 'UNPAID', 'PENDING', 49000, 8000, 10300, CURRENT_TIMESTAMP, NULL),
('d0d0d0d0-0004-0004-0004-d0d0d0d0d004', 'ORD20260331002', '99999999-9999-9999-9999-999999999999', 'idempotency-key-004', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', '678 Lê Duẩn, Quận 1, TP. Hồ Chí Minh', 10.7834, 106.6956, NULL, 180000, 30000, 210000, 'COD', 'UNPAID', 'ASSIGNED', 18000, 6000, 4200, CURRENT_TIMESTAMP, NULL)
;

-- ============================================================================
-- 6. ORDER ITEMS
-- ============================================================================

INSERT INTO order_items (id, order_id, menu_item_id, menu_item_name_snapshot, unit_price_snapshot, quantity, line_total)
VALUES
('e0e0e0e0-0001-0001-0001-e0e0e0e0e001', 'd0d0d0d0-0001-0001-0001-d0d0d0d0d001', 'c0c0c0c0-0001-0001-0001-c0c0c0c0c001', 'Phở Tái (Rare Beef)', 65000, 1, 65000),
('e0e0e0e0-0002-0002-0002-e0e0e0e0e002', 'd0d0d0d0-0001-0001-0001-d0d0d0d0d001', 'c0c0c0c0-0006-0006-0006-c0c0c0c0c006', 'Cơm Tấm Tài Sính (Broken Rice Special)', 80000, 1, 80000),
('e0e0e0e0-0003-0003-0003-e0e0e0e0e003', 'd0d0d0d0-0002-0002-0002-d0d0d0d0d002', 'c0c0c0c0-0011-0011-0011-c0c0c0c0c011', 'Pizza Bọ Cày (Bacon Paradise)', 210000, 1, 210000),
('e0e0e0e0-0004-0004-0004-e0e0e0e0e004', 'd0d0d0d0-0002-0002-0002-d0d0d0d0d002', 'c0c0c0c0-0015-0015-0015-c0c0c0c0c015', 'Caesar Salad', 95000, 1, 95000),
('e0e0e0e0-0005-0005-0005-e0e0e0e0e005', 'd0d0d0d0-0002-0002-0002-d0d0d0d0d002', 'c0c0c0c0-0016-0016-0016-c0c0c0c0c016', 'Garlic Bread', 60000, 1, 60000),
('e0e0e0e0-0006-0006-0006-e0e0e0e0e006', 'd0d0d0d0-0003-0003-0003-d0d0d0d0d003', 'c0c0c0c0-0021-0021-0021-c0c0c0c0c021', 'Premium Sashimi Combo', 340000, 1, 340000),
('e0e0e0e0-0007-0007-0007-e0e0e0e0e007', 'd0d0d0d0-0003-0003-0003-d0d0d0d0d003', 'c0c0c0c0-0022-0022-0022-c0c0c0c0c022', 'Miso Soup (Miso Shiru)', 35000, 2, 70000),
('e0e0e0e0-0008-0008-0008-e0e0e0e0e008', 'd0d0d0d0-0003-0003-0003-d0d0d0d0d003', 'c0c0c0c0-0023-0023-0023-c0c0c0c0c023', 'Edamame (Steamed Soybeans)', 45000, 1, 45000),
('e0e0e0e0-0009-0009-0009-e0e0e0e0e009', 'd0d0d0d0-0004-0004-0004-d0d0d0d0d004', 'c0c0c0c0-0002-0002-0002-c0c0c0c0c002', 'Phở Nạm (Beef Brisket)', 70000, 2, 140000),
('e0e0e0e0-0010-0010-0010-e0e0e0e0e010', 'd0d0d0d0-0004-0004-0004-d0d0d0d0d004', 'c0c0c0c0-0007-0007-0007-c0c0c0c0c007', 'Nước Mía Đá (Sugar Cane Ice)', 25000, 1, 25000)
;

-- ============================================================================
-- 7. ORDER REVIEWS
-- ============================================================================

INSERT INTO order_reviews (id, order_id, restaurant_id, customer_user_id, stars, comment, merchant_response, responded_at, created_at, updated_at)
VALUES
('f0f0f0f0-0001-0001-0001-f0f0f0f0f001', 'd0d0d0d0-0001-0001-0001-d0d0d0d0d001', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', '66666666-6666-6666-6666-666666666666', 5, 'Excellent pho! Very fresh ingredients and delicious broth. Delivery was fast.', 'Thank you for your kind words! We hope to serve you again soon.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('f0f0f0f0-0002-0002-0002-f0f0f0f0f002', 'd0d0d0d0-0002-0002-0002-d0d0d0d0d002', 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002', '77777777-7777-7777-7777-777777777777', 4, 'Good pizza! Crust was crispy. Only note: could use more toppings.', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 8. ORDER PAYMENTS
-- ============================================================================

INSERT INTO order_payments (id, order_id, payment_method, payment_status, amount, paid_at, external_ref, created_at)
VALUES
('fa010001-0001-0001-0001-fa010001a001', 'd0d0d0d0-0001-0001-0001-d0d0d0d0d001', 'COD', 'PAID', 165000, CURRENT_TIMESTAMP, 'COD-12345', CURRENT_TIMESTAMP),
('fa010002-0002-0002-0002-fa010002a002', 'd0d0d0d0-0002-0002-0002-d0d0d0d0d002', 'COD', 'PAID', 430000, CURRENT_TIMESTAMP, 'COD-12346', CURRENT_TIMESTAMP),
('fa010003-0003-0003-0003-fa010003a003', 'd0d0d0d0-0003-0003-0003-d0d0d0d0d003', 'COD', 'UNPAID', 530000, NULL, NULL, CURRENT_TIMESTAMP),
('fa010004-0004-0004-0004-fa010004a004', 'd0d0d0d0-0004-0004-0004-d0d0d0d0d004', 'COD', 'UNPAID', 210000, NULL, NULL, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 9. ACTIVE CART
-- ============================================================================

INSERT INTO carts (id, customer_user_id, restaurant_id, status, created_at, updated_at)
VALUES
('ca000001-0001-0001-0001-ca000001a001', '99999999-9999-9999-9999-999999999999', 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 10. CART ITEMS
-- ============================================================================

INSERT INTO cart_items (id, cart_id, menu_item_id, quantity, unit_price_snapshot, note)
VALUES
('cb000001-0001-0001-0001-cb000001a001', 'ca000001-0001-0001-0001-ca000001a001', 'c0c0c0c0-0005-0005-0005-c0c0c0c0c005', 2, 60000, NULL)
;

-- ============================================================================
-- 11. DELIVERY TRACKING POINTS
-- ============================================================================

INSERT INTO order_tracking_points (id, order_id, lat, lng, recorded_at, created_at)
VALUES
('cc000001-0001-0001-0001-cc000001a001', 'd0d0d0d0-0004-0004-0004-d0d0d0d0d004', 10.7745, 106.7012, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cc000002-0002-0002-0002-cc000002a002', 'd0d0d0d0-0004-0004-0004-d0d0d0d0d004', 10.7765, 106.7020, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================================
-- 12. SYSTEM PARAMETERS
-- ============================================================================

INSERT INTO system_parameters (param_key, value_type, param_value, runtime_applicable, version, description, updated_by_actor, updated_at)
VALUES
('currency.code', 'STRING', 'VND', false, 1, 'Currency code for Vietnam Dong', 'SYSTEM', CURRENT_TIMESTAMP),
('currency.minor_unit', 'NUMBER', '0', false, 1, 'Minor unit for VND (0 decimals)', 'SYSTEM', CURRENT_TIMESTAMP),
('currency.rounding_mode', 'STRING', 'HALF_UP', false, 1, 'Rounding mode for monetary calculations', 'SYSTEM', CURRENT_TIMESTAMP),
('shipping.base_distance_km', 'NUMBER', '2.0', true, 1, 'Base free delivery distance in km', 'SYSTEM', CURRENT_TIMESTAMP),
('shipping.base_delivery_fee', 'NUMBER', '10000', true, 1, 'Base delivery fee in VND', 'SYSTEM', CURRENT_TIMESTAMP),
('shipping.fee_per_km', 'NUMBER', '5000', true, 1, 'Additional fee per km beyond base distance', 'SYSTEM', CURRENT_TIMESTAMP),
('shipping.max_delivery_km', 'NUMBER', '15.0', true, 1, 'Maximum delivery distance in km', 'SYSTEM', CURRENT_TIMESTAMP),
('search.nearby.max_radius_km', 'NUMBER', '10.0', true, 1, 'Maximum nearby search radius in km', 'SYSTEM', CURRENT_TIMESTAMP),
('finance.commission_rate_percent', 'NUMBER', '10.0', true, 1, 'Commission rate percentage on subtotal', 'SYSTEM', CURRENT_TIMESTAMP),
('finance.shipping_margin_rate_percent', 'NUMBER', '0.0', true, 1, 'Margin rate percentage on delivery fee', 'SYSTEM', CURRENT_TIMESTAMP),
('search.default_page_size', 'NUMBER', '20', true, 1, 'Default page size for search results', 'SYSTEM', CURRENT_TIMESTAMP),
('search.max_page_size', 'NUMBER', '100', true, 1, 'Maximum page size for search results', 'SYSTEM', CURRENT_TIMESTAMP),
('catalog.menu_item_price_min', 'NUMBER', '1000', true, 1, 'Minimum allowed menu item price', 'SYSTEM', CURRENT_TIMESTAMP),
('catalog.menu_item_price_max', 'NUMBER', '10000000', true, 1, 'Maximum allowed menu item price', 'SYSTEM', CURRENT_TIMESTAMP),
('retention.customer_data_days', 'NUMBER', '365', false, 1, 'Days to retain customer data', 'SYSTEM', CURRENT_TIMESTAMP),
('retention.order_history_days', 'NUMBER', '730', false, 1, 'Days to retain order history', 'SYSTEM', CURRENT_TIMESTAMP),
('retention.audit_logs_days', 'NUMBER', '365', false, 1, 'Days to retain audit logs', 'SYSTEM', CURRENT_TIMESTAMP),
('retention.tracking_points_days', 'NUMBER', '30', false, 1, 'Days to retain delivery tracking points', 'SYSTEM', CURRENT_TIMESTAMP),
('retention.ai_chat_days', 'NUMBER', '90', false, 1, 'Days to retain AI chat history', 'SYSTEM', CURRENT_TIMESTAMP),
('ops.backup.rpo_minutes', 'NUMBER', '15', false, 1, 'Recovery Point Objective in minutes', 'SYSTEM', CURRENT_TIMESTAMP),
('ops.backup.rto_minutes', 'NUMBER', '60', false, 1, 'Recovery Time Objective in minutes', 'SYSTEM', CURRENT_TIMESTAMP)
;
