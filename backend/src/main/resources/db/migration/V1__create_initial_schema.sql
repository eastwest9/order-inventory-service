CREATE TABLE member (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    member_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_member
        PRIMARY KEY (member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE product (
    product_id BIGINT NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(200) NOT NULL,
    product_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_product
        PRIMARY KEY (product_id),

    CONSTRAINT chk_product_status
        CHECK (
            product_status IN ('ON_SALE', 'STOPPED')
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE product_variant (
    variant_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    variant_name VARCHAR(200) NOT NULL,
    sale_price DECIMAL(19, 2) NOT NULL,
    variant_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_product_variant
        PRIMARY KEY (variant_id),

    CONSTRAINT uk_product_variant_sku_code
        UNIQUE (sku_code),

    CONSTRAINT fk_product_variant_product
        FOREIGN KEY (product_id)
        REFERENCES product (product_id),

    CONSTRAINT chk_product_variant_price
        CHECK (
            sale_price >= 0
        ),

    CONSTRAINT chk_product_variant_status
        CHECK (
            variant_status IN ('ON_SALE', 'STOPPED')
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE inventory (
    inventory_id BIGINT NOT NULL AUTO_INCREMENT,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_inventory
        PRIMARY KEY (inventory_id),

    CONSTRAINT uk_inventory_variant
        UNIQUE (variant_id),

    CONSTRAINT fk_inventory_product_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id),

    CONSTRAINT chk_inventory_quantity
        CHECK (
            quantity >= 0
        ),

    CONSTRAINT chk_inventory_version
        CHECK (
            version >= 0
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE shop_order (
    order_id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL,
    member_id BIGINT NOT NULL,
    order_status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    canceled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_shop_order
        PRIMARY KEY (order_id),

    CONSTRAINT uk_shop_order_order_number
        UNIQUE (order_number),

    CONSTRAINT fk_shop_order_member
        FOREIGN KEY (member_id)
        REFERENCES member (member_id),

    CONSTRAINT chk_shop_order_status
        CHECK (
            order_status IN ('CREATED', 'CANCELED')
        ),

    CONSTRAINT chk_shop_order_total_amount
        CHECK (
            total_amount >= 0
        ),

    CONSTRAINT chk_shop_order_cancel_status
        CHECK (
            (
                order_status = 'CREATED'
                AND canceled_at IS NULL
            )
            OR
            (
                order_status = 'CANCELED'
                AND canceled_at IS NOT NULL
            )
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE order_item (
    order_item_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    variant_name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_order_item
        PRIMARY KEY (order_item_id),

    CONSTRAINT fk_order_item_shop_order
        FOREIGN KEY (order_id)
        REFERENCES shop_order (order_id),

    CONSTRAINT fk_order_item_product_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id),

    CONSTRAINT chk_order_item_unit_price
        CHECK (
            unit_price >= 0
        ),

    CONSTRAINT chk_order_item_quantity
        CHECK (
            quantity >= 1
        ),

    CONSTRAINT chk_order_item_total_price
        CHECK (
            total_price >= 0
        ),

    CONSTRAINT chk_order_item_total_price_consistency
        CHECK (
            total_price = unit_price * quantity
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE inventory_history (
    history_id BIGINT NOT NULL AUTO_INCREMENT,
    variant_id BIGINT NOT NULL,
    order_item_id BIGINT NULL,
    change_type VARCHAR(30) NOT NULL,
    change_quantity INT NOT NULL,
    before_quantity INT NOT NULL,
    after_quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_inventory_history
        PRIMARY KEY (history_id),

    CONSTRAINT uk_inventory_history_order_item_change_type
        UNIQUE (order_item_id, change_type),

    CONSTRAINT fk_inventory_history_product_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variant (variant_id),

    CONSTRAINT fk_inventory_history_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_item (order_item_id),

    CONSTRAINT chk_inventory_history_change_type
        CHECK (
            change_type IN (
                'INITIAL',
                'RECEIPT',
                'ORDER',
                'ORDER_CANCEL',
                'ADJUSTMENT'
            )
        ),

    CONSTRAINT chk_inventory_history_before_quantity
        CHECK (
            before_quantity >= 0
        ),

    CONSTRAINT chk_inventory_history_after_quantity
        CHECK (
            after_quantity >= 0
        ),

    CONSTRAINT chk_inventory_history_quantity_consistency
        CHECK (
            after_quantity = before_quantity + change_quantity
        ),

    CONSTRAINT chk_inventory_history_change_direction
        CHECK (
            (
                change_type = 'INITIAL'
                AND change_quantity >= 0
            )
            OR
            (
                change_type = 'RECEIPT'
                AND change_quantity > 0
            )
            OR
            (
                change_type = 'ORDER'
                AND change_quantity < 0
            )
            OR
            (
                change_type = 'ORDER_CANCEL'
                AND change_quantity > 0
            )
            OR
            (
                change_type = 'ADJUSTMENT'
                AND change_quantity <> 0
            )
        ),

    CONSTRAINT chk_inventory_history_order_reference
        CHECK (
            (
                change_type IN ('ORDER', 'ORDER_CANCEL')
                AND order_item_id IS NOT NULL
            )
            OR
            (
                change_type IN ('INITIAL', 'RECEIPT', 'ADJUSTMENT')
                AND order_item_id IS NULL
            )
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;