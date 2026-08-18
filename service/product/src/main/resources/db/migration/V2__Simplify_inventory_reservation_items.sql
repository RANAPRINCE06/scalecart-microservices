ALTER TABLE inventory_reservation_items
    DROP COLUMN IF EXISTS product_name,
    DROP COLUMN IF EXISTS sku;
