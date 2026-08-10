-- Preserve existing values while allowing fractional medication inventory.
ALTER TABLE `user_medicine_box`
    MODIFY COLUMN `total_quantity` DECIMAL(12,3) NULL COMMENT '总数量',
    MODIFY COLUMN `remaining_quantity` DECIMAL(12,3) NULL COMMENT '剩余数量';
