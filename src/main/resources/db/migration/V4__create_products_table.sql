CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(150) NOT NULL,
                          description VARCHAR(3000) NOT NULL,
                          sku VARCHAR(100) NOT NULL,
                          price NUMERIC(12,2) NOT NULL,
                          stock_quantity INTEGER NOT NULL,
                          category_id BIGINT NOT NULL,
                          image_url VARCHAR(255),
                          status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          CONSTRAINT uk_products_sku UNIQUE (sku),
                          CONSTRAINT fk_products_category
                              FOREIGN KEY (category_id) REFERENCES categories(id),
                          CONSTRAINT chk_products_price_positive CHECK (price > 0),
                          CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0)
);