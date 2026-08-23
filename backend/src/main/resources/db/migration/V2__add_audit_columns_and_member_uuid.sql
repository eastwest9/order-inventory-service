ALTER TABLE member
    ADD COLUMN member_uuid CHAR(36) NULL,
    ADD COLUMN created_id CHAR(36) NULL,
    ADD COLUMN updated_id CHAR(36) NULL,
    ADD CONSTRAINT uk_member_member_uuid UNIQUE (member_uuid);


ALTER TABLE product
    ADD COLUMN created_id CHAR(36) NULL,
    ADD COLUMN updated_id CHAR(36) NULL;


ALTER TABLE product_variant
    ADD COLUMN created_id CHAR(36) NULL,
    ADD COLUMN updated_id CHAR(36) NULL;


ALTER TABLE inventory
    ADD COLUMN created_id CHAR(36) NULL,
    ADD COLUMN updated_id CHAR(36) NULL;


ALTER TABLE shop_order
    ADD COLUMN created_id CHAR(36) NULL,
    ADD COLUMN updated_id CHAR(36) NULL;


ALTER TABLE order_item
    ADD COLUMN created_id CHAR(36) NULL;


ALTER TABLE inventory_history
    ADD COLUMN created_id CHAR(36) NULL;
