-- Deterministic seed set for live API route smoke testing
-- Credentials for all api_* users: Strong@123

INSERT INTO users (id, username, email, phone_number, full_name, avatar_url, role, status, password_hash, created_at, updated_at)
VALUES
('12121212-1212-1212-1212-121212121212', 'api_admin', 'api_admin@foodya.local', '+84910000001', 'API Admin', NULL, 'ADMIN', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('13131313-1313-1313-1313-131313131313', 'api_merchant', 'api_merchant@foodya.local', '+84910000002', 'API Merchant', NULL, 'MERCHANT', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('14141414-1414-1414-1414-141414141414', 'api_delivery', 'api_delivery@foodya.local', '+84910000003', 'API Delivery', NULL, 'DELIVERY', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('15151515-1515-1515-1515-151515151515', 'api_customer', 'api_customer@foodya.local', '+84910000004', 'API Customer', NULL, 'CUSTOMER', 'ACTIVE', '$2a$10$slYQmyNdGzin7olVMznqOeMRE/s5/Ej8KkBPFYHEKj3J1g5e7fEH2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO restaurants (id, owner_user_id, name, cuisine_type, description, address_line, latitude, longitude, h3_index_res9, avg_rating, review_count, status, is_open, max_delivery_km, created_at, updated_at)
VALUES
('16161616-1616-1616-1616-161616161616', '13131313-1313-1313-1313-131313131313', 'API Smoke Bistro', 'Fusion', 'Seeded restaurant for end-to-end API route checks', '100 Test Street, District 1, HCMC', 10.7750000, 106.7000000, '895285a8fffffff', 4.3, 5, 'ACTIVE', true, 10.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO menu_categories (id, restaurant_id, name, sort_order, is_active, created_at, updated_at)
VALUES
('17171717-1717-1717-1717-171717171717', '16161616-1616-1616-1616-161616161616', 'Seed Main', 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO menu_items (id, restaurant_id, category_id, name, description, price, is_active, is_available, deleted_at, created_at, updated_at)
VALUES
('18181818-1818-1818-1818-181818181818', '16161616-1616-1616-1616-161616161616', '17171717-1717-1717-1717-171717171717', 'Seed Noodles', 'Reusable seeded menu item for route smoke test', 89000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('19191919-1919-1919-1919-191919191919', '16161616-1616-1616-1616-161616161616', '17171717-1717-1717-1717-171717171717', 'Seed Soup', 'Secondary item for order composition', 45000, true, true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO orders (id, order_code, customer_user_id, idempotency_key, restaurant_id, delivery_address, delivery_latitude, delivery_longitude, customer_note, subtotal_amount, delivery_fee, total_amount, payment_method, payment_status, status, commission_amount, shipping_fee_margin_amount, platform_profit_amount, placed_at, completed_at)
VALUES
('1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a', 'API-SMOKE-SUCCESS-001', '15151515-1515-1515-1515-151515151515', 'api-smoke-key-success-001', '16161616-1616-1616-1616-161616161616', '200 Test Avenue, District 3, HCMC', 10.7800000, 106.6900000, 'Deliver quickly', 134000, 10000, 144000, 'COD', 'PAID', 'SUCCESS', 13400, 0, 13400, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2b2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b', 'API-SMOKE-ASSIGNED-001', '15151515-1515-1515-1515-151515151515', 'api-smoke-key-assigned-001', '16161616-1616-1616-1616-161616161616', '201 Test Avenue, District 3, HCMC', 10.7810000, 106.6910000, NULL, 89000, 10000, 99000, 'COD', 'UNPAID', 'ASSIGNED', 8900, 0, 8900, CURRENT_TIMESTAMP, NULL),
('3c3c3c3c-3c3c-3c3c-3c3c-3c3c3c3c3c3c', 'API-SMOKE-ACCEPTED-001', '15151515-1515-1515-1515-151515151515', 'api-smoke-key-accepted-001', '16161616-1616-1616-1616-161616161616', '202 Test Avenue, District 3, HCMC', 10.7820000, 106.6920000, NULL, 89000, 10000, 99000, 'COD', 'UNPAID', 'ACCEPTED', 8900, 0, 8900, CURRENT_TIMESTAMP, NULL);

INSERT INTO order_items (id, order_id, menu_item_id, menu_item_name_snapshot, unit_price_snapshot, quantity, line_total)
VALUES
('4d4d4d4d-4d4d-4d4d-4d4d-4d4d4d4d4d4d', '1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a', '18181818-1818-1818-1818-181818181818', 'Seed Noodles', 89000, 1, 89000),
('5e5e5e5e-5e5e-5e5e-5e5e-5e5e5e5e5e5e', '1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a', '19191919-1919-1919-1919-191919191919', 'Seed Soup', 45000, 1, 45000),
('6f6f6f6f-6f6f-6f6f-6f6f-6f6f6f6f6f6f', '2b2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b', '18181818-1818-1818-1818-181818181818', 'Seed Noodles', 89000, 1, 89000),
('70707070-7070-7070-7070-707070707070', '3c3c3c3c-3c3c-3c3c-3c3c-3c3c3c3c3c3c', '18181818-1818-1818-1818-181818181818', 'Seed Noodles', 89000, 1, 89000);

INSERT INTO order_payments (id, order_id, payment_method, payment_status, amount, paid_at, external_ref, created_at)
VALUES
('81818181-8181-8181-8181-818181818181', '1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a', 'COD', 'PAID', 144000, CURRENT_TIMESTAMP, 'COD-SMOKE-001', CURRENT_TIMESTAMP),
('82828282-8282-8282-8282-828282828282', '2b2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b', 'COD', 'UNPAID', 99000, NULL, NULL, CURRENT_TIMESTAMP),
('83838383-8383-8383-8383-838383838383', '3c3c3c3c-3c3c-3c3c-3c3c-3c3c3c3c3c3c', 'COD', 'UNPAID', 99000, NULL, NULL, CURRENT_TIMESTAMP);

INSERT INTO order_reviews (id, order_id, restaurant_id, customer_user_id, stars, comment, merchant_response, responded_at, created_at, updated_at)
VALUES
('84848484-8484-8484-8484-848484848484', '1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a', '16161616-1616-1616-1616-161616161616', '15151515-1515-1515-1515-151515151515', 5, 'Seed review for API route test', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_tracking_points (id, order_id, lat, lng, recorded_at, created_at)
VALUES
('85858585-8585-8585-8585-858585858585', '2b2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b', 10.7790000, 106.6980000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('86868686-8686-8686-8686-868686868686', '2b2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b', 10.7800000, 106.6990000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO carts (id, customer_user_id, restaurant_id, status, created_at, updated_at)
VALUES
('87878787-8787-8787-8787-878787878787', '15151515-1515-1515-1515-151515151515', '16161616-1616-1616-1616-161616161616', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cart_items (id, cart_id, menu_item_id, quantity, unit_price_snapshot, note)
VALUES
('88888888-8888-8888-8888-888888888880', '87878787-8787-8787-8787-878787878787', '18181818-1818-1818-1818-181818181818', 1, 89000, 'seed cart item');

INSERT INTO ai_chat_histories (id, user_id, prompt, response_summary, context_latitude, context_longitude, weather_h3_index_res8, created_at, updated_at)
VALUES
('89898989-8989-8989-8989-898989898989', '15151515-1515-1515-1515-151515151515', 'Recommend something warm', 'Try Seed Noodles from API Smoke Bistro.', 10.7750000, 106.7000000, '885285a8fffffff', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO notification_logs (id, receiver_user_id, receiver_type, event_type, title, message, status, order_id, provider_response, sent_at, created_at, read_at)
VALUES
('90909090-9090-9090-9090-909090909090', '15151515-1515-1515-1515-151515151515', 'CUSTOMER', 'ORDER_ACCEPTED', 'Order accepted', 'Your seeded smoke order has been accepted.', 'SENT', '3c3c3c3c-3c3c-3c3c-3c3c-3c3c3c3c3c3c', 'ok', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('91919191-9191-9191-9191-919191919191', '13131313-1313-1313-1313-131313131313', 'MERCHANT', 'NEW_ORDER', 'New order incoming', 'A customer just placed a seeded smoke order.', 'SENT', '1a1a1a1a-1a1a-1a1a-1a1a-1a1a1a1a1a1a', 'ok', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);
