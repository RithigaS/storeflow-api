CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INTEGER NOT NULL,
                             unit_price NUMERIC(12,2) NOT NULL,
                             subtotal NUMERIC(12,2) NOT NULL,
                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_product
                                 FOREIGN KEY (product_id) REFERENCES products(id),
                             CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
                             CONSTRAINT chk_order_items_unit_price_positive CHECK (unit_price > 0)
);