---
alwaysApply: false
description: 当需要修改数据库时，需要遵循以下规范
scene: database_change
---
## 数据库变更规范

### 1. 检查现有字段
在新增字段前，先检查 `init_database.sql` 中对应表是否已存在该字段。

### 2. 数据库脚本要求
- 使用 `CREATE TABLE IF NOT EXISTS` 防止重复创建
- 使用 `DROP TABLE IF EXISTS` 后跟完整建表语句，确保干净重建
- 添加必要的索引和外键约束
- 提供完整的测试数据

### 3. 向后兼容
- ALTER TABLE 语句使用 `IF NOT EXISTS` 或条件判断
- 新增字段必须放在表最后
- 不删除已有字段

### 4. 团队协作
- 脚本必须能重复执行不报错
- 执行前打印提示信息
- 脚本末尾输出执行结果统计

### 5. 提交规范
- 提交前验证脚本语法正确性
- 在 commit message 中说明数据库变更内容
