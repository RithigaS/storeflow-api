CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        reference_number VARCHAR(100) NOT NULL,
                        customer_id BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        shipping_street VARCHAR(255) NOT NULL,
                        shipping_city VARCHAR(150) NOT NULL,
                        shipping_country VARCHAR(150) NOT NULL,
                        shipping_postal_code VARCHAR(50) NOT NULL,
                        total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL,
                        CONSTRAINT uk_orders_reference_number UNIQUE (reference_number),
                        CONSTRAINT fk_orders_customer
                            FOREIGN KEY (customer_id) REFERENCES users(id)
);