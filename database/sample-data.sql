
-- Insert More Categories
INSERT INTO categories (id, name, description, is_active, created_at, updated_at) VALUES
(5, 'Sports & Outdoors', 'Equipment for sports and outdoor activities', true, NOW(), NOW()),
(6, 'Toys & Games', 'Fun and games for all ages', true, NOW(), NOW());

-- Insert More Products
INSERT INTO products (id, name, description, sku, price, cost_price, stock_quantity, min_stock_level, brand, weight, is_active, is_featured, images_url, created_at, updated_at, category_id) VALUES
(8, 'Running Shoes', 'Lightweight and comfortable running shoes', 'SH-RUN-008', 95.00, 40.00, 150, 20, 'SportyBrand', 0.6, true, true, 'http://example.com/runningshoes.jpg', NOW(), NOW(), 5),
(9, 'Yoga Mat', 'Eco-friendly and non-slip yoga mat', 'YM-ECO-009', 30.00, 12.00, 200, 30, 'ZenFit', 1.2, true, false, 'http://example.com/yogamat.jpg', NOW(), NOW(), 5),
(10, 'Board Game', 'A fun strategy board game for the family', 'BG-STRAT-010', 40.00, 18.00, 100, 15, 'GameMakers', 1.0, true, false, 'http://example.com/boardgame.jpg', NOW(), NOW(), 6),
(11, 'Action Figure', 'Collectible action figure from a popular movie', 'AF-MOVIE-011', 22.50, 9.00, 300, 50, 'ToyMasters', 0.4, true, false, 'http://example.com/actionfigure.jpg', NOW(), NOW(), 6),
(12, 'Wireless Headphones', 'High-fidelity wireless headphones', 'HP-WIRELESS-012', 150.00, 70.00, 80, 10, 'TechBrand', 0.3, true, true, 'http://example.com/headphones.jpg', NOW(), NOW(), 1);
