#!/bin/bash
# =====================================================
# 老年人用药管理系统 - 数据库初始化脚本
# 版本: 4.0
# 支持: Linux (Ubuntu, CentOS, Debian) / macOS
# 兼容: MySQL 5.7+ / MySQL 8.0+
# 支持目录: 可以在 backend 目录或项目根目录运行
# =====================================================

set -e
set -u
set -o pipefail

# ----------------------------
# 脚本配置参数
# ----------------------------
SCRIPT_VERSION="4.0"
DB_NAME="elderly_medication"
DEFAULT_PORT="3306"

# 默认值
MYSQL_HOST="localhost"
MYSQL_PORT="${DEFAULT_PORT}"
MYSQL_USER="root"
MYSQL_PASSWORD=""
INIT_SQL="init_database.sql"
DRUG_SQL="init_drug_data.sql"
BACKUP_ENABLED="1"
SKIP_DRUG_DATA="0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 颜色定义
COLOR_INFO='\033[94m'
COLOR_SUCCESS='\033[92m'
COLOR_WARNING='\033[93m'
COLOR_ERROR='\033[91m'
COLOR_RESET='\033[0m'

# 检测是否支持颜色输出
COLOR_SUPPORT="1"
if [ ! -t 1 ]; then
    COLOR_SUPPORT="0"
fi

# ----------------------------
# 函数定义
# ----------------------------

log_header() {
    echo ""
    echo -e "${COLOR_INFO}==============================================${COLOR_RESET}"
    echo -e "${COLOR_INFO}  老年人用药管理系统 - 数据库初始化脚本 v${SCRIPT_VERSION}${COLOR_RESET}"
    echo -e "${COLOR_INFO}==============================================${COLOR_RESET}"
    echo ""
}

log_step() {
    echo -e "${COLOR_INFO}[步骤 $1/$2] $3${COLOR_RESET}"
}

log_success() {
    echo -e "${COLOR_SUCCESS}[成功] $1${COLOR_RESET}"
}

log_warning() {
    echo -e "${COLOR_WARNING}[警告] $1${COLOR_RESET}"
}

log_error() {
    echo -e "${COLOR_ERROR}[错误] $1${COLOR_RESET}"
}

log_info() {
    echo "[信息] $1"
}

# 检查 MySQL 命令
check_mysql() {
    MYSQL_CMD=""

    # 检查 MySQL 是否在 PATH 中
    if command -v mysql &> /dev/null; then
        MYSQL_CMD="mysql"
        return 0
    fi

    # 检查常见安装路径
    local MYSQL_PATHS=(
        "/usr/bin/mysql"
        "/usr/local/bin/mysql"
        "/opt/homebrew/bin/mysql"
        "/opt/homebrew/opt/mysql-client/bin/mysql"
        "/opt/mysql/bin/mysql"
        "/Applications/XAMPP/bin/mysql"
    )

    for path in "${MYSQL_PATHS[@]}"; do
        if [ -x "$path" ]; then
            MYSQL_CMD="$path"
            return 0
        fi
    done

    return 1
}

# 测试 MySQL 连接
test_connection() {
    $MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" -e "SELECT VERSION();" &> /dev/null
    return $?
}

# 解析 SQL 文件路径：支持在 backend 目录或项目根目录运行
resolve_paths() {
    PROJECT_ROOT=""
    INIT_SQL_PATH=""
    DRUG_SQL_PATH=""

    # 1. 当前脚本位于 backend 目录（最常见）
    if [ -f "${SCRIPT_DIR}/src/main/resources/${INIT_SQL}" ]; then
        PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
        INIT_SQL_PATH="${SCRIPT_DIR}/src/main/resources/${INIT_SQL}"
        DRUG_SQL_PATH="${SCRIPT_DIR}/src/main/resources/${DRUG_SQL}"
        return 0
    fi

    # 2. 当前脚本位于项目根目录，SQL 在 backend 子目录
    if [ -f "${SCRIPT_DIR}/backend/src/main/resources/${INIT_SQL}" ]; then
        PROJECT_ROOT="${SCRIPT_DIR}"
        INIT_SQL_PATH="${SCRIPT_DIR}/backend/src/main/resources/${INIT_SQL}"
        DRUG_SQL_PATH="${SCRIPT_DIR}/backend/src/main/resources/${DRUG_SQL}"
        return 0
    fi

    # 3. 当前工作目录在 backend
    if [ -f "$(pwd)/src/main/resources/${INIT_SQL}" ]; then
        PROJECT_ROOT="$(cd "$(pwd)/.." && pwd)"
        INIT_SQL_PATH="$(pwd)/src/main/resources/${INIT_SQL}"
        DRUG_SQL_PATH="$(pwd)/src/main/resources/${DRUG_SQL}"
        return 0
    fi

    # 4. 当前工作目录在项目根
    if [ -f "$(pwd)/backend/src/main/resources/${INIT_SQL}" ]; then
        PROJECT_ROOT="$(pwd)"
        INIT_SQL_PATH="$(pwd)/backend/src/main/resources/${INIT_SQL}"
        DRUG_SQL_PATH="$(pwd)/backend/src/main/resources/${DRUG_SQL}"
        return 0
    fi

    return 1
}

# 备份数据库
backup_database() {
    if [ "$BACKUP_ENABLED" != "1" ]; then
        return 0
    fi

    log_info "正在备份现有数据库..."

    # 检查数据库是否存在
    if ! $MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" -e "USE $DB_NAME;" &> /dev/null; then
        log_warning "数据库不存在或无法连接，跳过备份"
        return 0
    fi

    if [ -n "${PROJECT_ROOT:-}" ]; then
        BACKUP_DIR="${PROJECT_ROOT}/db_backups"
    else
        BACKUP_DIR="${SCRIPT_DIR}/db_backups"
    fi
    mkdir -p "$BACKUP_DIR"

    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_backup_${TIMESTAMP}.sql"

    $MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" \
        --single-transaction --quick --lock-tables=false "$DB_NAME" > "$BACKUP_FILE" 2>/dev/null

    if [ $? -eq 0 ]; then
        log_success "数据库已备份到: $BACKUP_FILE"
    else
        log_warning "备份过程出现错误，但继续执行..."
    fi
}

# 显示帮助信息
show_help() {
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -u, --user <用户名>     MySQL用户名 (默认: root)"
    echo "  -p, --password <密码>   MySQL密码 (默认: 无)"
    echo "  -h, --host <主机>       MySQL主机 (默认: localhost)"
    echo "  -P, --port <端口>       MySQL端口 (默认: 3306)"
    echo "  --skip-backup           跳过数据库备份"
    echo "  --skip-drug-data        跳过完整药品数据导入（仅导入 init_database.sql 中的基础数据）"
    echo "  --no-input              无需确认直接执行"
    echo "  --help                  显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0"
    echo "  $0 -u root -p mypassword"
    echo "  $0 --host localhost --port 3306 -u root"
    echo "  $0 --skip-drug-data"
    echo ""
    echo "说明:"
    echo "  此脚本会依次执行 init_database.sql 与 init_drug_data.sql"
    echo "  脚本可重复执行：init_database.sql 会 DROP 重建表，init_drug_data.sql 使用 INSERT IGNORE"
    echo "  可在 backend 目录或项目根目录运行此脚本"
    echo ""
}

# ----------------------------
# 解析命令行参数
# ----------------------------
NO_INPUT="0"
while [[ $# -gt 0 ]]; do
    case $1 in
        -u|--user)
            MYSQL_USER="$2"
            shift 2
            ;;
        -p|--password)
            MYSQL_PASSWORD="$2"
            shift 2
            ;;
        -h|--host)
            MYSQL_HOST="$2"
            shift 2
            ;;
        -P|--port)
            MYSQL_PORT="$2"
            shift 2
            ;;
        --skip-backup)
            BACKUP_ENABLED="0"
            shift
            ;;
        --skip-drug-data)
            SKIP_DRUG_DATA="1"
            shift
            ;;
        --no-input)
            NO_INPUT="1"
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            log_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

# ----------------------------
# 主脚本开始
# ----------------------------
log_header

# ----------------------------
# 环境检测
# ----------------------------
log_info "正在检测MySQL环境..."

if ! check_mysql; then
    log_error "未找到MySQL命令"
    log_info "请确保MySQL已安装并配置到环境变量，或指定正确的安装路径"
    echo ""
    echo "常见解决方案:"
    echo "  1. macOS: brew install mysql"
    echo "  2. Ubuntu/Debian: sudo apt-get install mysql-client"
    echo "  3. CentOS/RHEL: sudo yum install mysql"
    echo "  4. 下载并安装 MySQL: https://dev.mysql.com/downloads/mysql/"
    echo ""
    exit 1
fi

log_success "找到MySQL: $MYSQL_CMD"

# 测试连接
if ! test_connection; then
    if [ "$NO_INPUT" = "1" ]; then
        log_error "无法连接到MySQL服务器（--no-input 模式不会提示输入密码）"
        exit 1
    fi

    if [ -z "$MYSQL_PASSWORD" ]; then
        echo -n "请输入MySQL root密码（直接回车表示无密码）: "
        read -s MYSQL_PASSWORD
        echo ""
    fi

    if ! test_connection; then
        log_error "无法连接到MySQL服务器"
        log_info "请检查:"
        echo "  - MySQL服务是否启动"
        echo "  - 主机地址和端口是否正确"
        echo "  - 用户名和密码是否正确"
        echo ""
        exit 1
    fi
fi

# 获取MySQL版本
MYSQL_VERSION=$($MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" -sN -e "SELECT VERSION();" 2>/dev/null)
log_info "MySQL版本: $MYSQL_VERSION"

# ----------------------------
# 解析 SQL 文件路径
# ----------------------------
if ! resolve_paths; then
    log_error "未找到SQL文件: $INIT_SQL"
    log_info "请在项目根目录或 backend 目录运行此脚本"
    exit 1
fi

if [ ! -f "$INIT_SQL_PATH" ]; then
    log_error "未找到SQL文件: $INIT_SQL_PATH"
    exit 1
fi

# ----------------------------
# 参数确认
# ----------------------------
echo ""
log_info "配置信息:"
echo "  主机: $MYSQL_HOST"
echo "  端口: $MYSQL_PORT"
echo "  用户: $MYSQL_USER"
echo "  数据库: $DB_NAME"
echo "  SQL文件: $INIT_SQL_PATH"
if [ "$SKIP_DRUG_DATA" = "1" ]; then
    echo "  药品扩展: 跳过"
else
    echo "  药品扩展: $DRUG_SQL_PATH"
fi
if [ "$BACKUP_ENABLED" = "1" ]; then
    echo "  备份: 启用"
else
    echo "  备份: 禁用"
fi
echo ""

# 确认执行
if [ "$NO_INPUT" != "1" ]; then
    echo -n "确认执行初始化？（输入yes继续，其他取消）: "
    read CONFIRM
    if [ "$CONFIRM" != "yes" ] && [ "$CONFIRM" != "YES" ]; then
        log_info "已取消执行"
        exit 0
    fi
fi

# ----------------------------
# 数据库备份
# ----------------------------
log_step 1 4 "备份现有数据库..."
backup_database

# ----------------------------
# 执行 init_database.sql
# ----------------------------
log_step 2 4 "执行数据库结构与基础数据..."

log_info "正在执行 init_database.sql，这可能需要几十秒..."

if ! $MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" \
    --default-character-set=utf8mb4 < "$INIT_SQL_PATH"; then
    log_error "init_database.sql 执行失败"
    log_info "请查看上方错误信息并修复后重试"
    exit 1
fi

log_success "init_database.sql 执行完成"

# ----------------------------
# 执行 init_drug_data.sql
# ----------------------------
if [ "$SKIP_DRUG_DATA" = "1" ]; then
    log_info "已跳过 init_drug_data.sql"
else
    log_step 3 4 "补充完整药品数据..."

    if [ ! -f "$DRUG_SQL_PATH" ]; then
        log_warning "未找到 $DRUG_SQL，跳过完整药品数据导入"
    else
        log_info "正在执行 init_drug_data.sql..."
        if ! $MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" \
            --default-character-set=utf8mb4 < "$DRUG_SQL_PATH"; then
            log_error "init_drug_data.sql 执行失败"
            log_info "请查看上方错误信息并修复后重试"
            exit 1
        fi
        log_success "init_drug_data.sql 执行完成"
    fi
fi

# ----------------------------
# 验证结果
# ----------------------------
log_step 4 4 "验证初始化结果..."

TABLE_COUNT=$($MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" \
    -sN -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$DB_NAME' AND TABLE_TYPE='BASE TABLE';" 2>/dev/null)

if [ "$TABLE_COUNT" = "13" ]; then
    log_success "验证通过: 已创建 13 张数据表"
else
    log_warning "表数量异常: 预期13张表，实际$TABLE_COUNT张"
fi

log_info "创建的表:"
$MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" \
    -sN -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='$DB_NAME' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;" 2>/dev/null

log_info "各表数据统计:"
$MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" \
    --table -e "SELECT 'sys_user' AS '表名', COUNT(*) AS '行数' FROM sys_user UNION ALL SELECT 'drug_base', COUNT(*) FROM drug_base UNION ALL SELECT 'user_medicine_box', COUNT(*) FROM user_medicine_box UNION ALL SELECT 'medication_plan', COUNT(*) FROM medication_plan UNION ALL SELECT 'medication_log', COUNT(*) FROM medication_log UNION ALL SELECT 'drug_conflict_rules', COUNT(*) FROM drug_conflict_rules UNION ALL SELECT 'drug_aliases', COUNT(*) FROM drug_aliases UNION ALL SELECT 'drug_category_keywords', COUNT(*) FROM drug_category_keywords;" 2>/dev/null

# ----------------------------
# 完成报告
# ----------------------------
echo ""
echo "=============================================="
echo "           数据库初始化完成报告"
echo "=============================================="
echo "数据库名称: $DB_NAME"
echo "MySQL版本: $MYSQL_VERSION"
echo "表总数: $TABLE_COUNT"
echo "用户名: $MYSQL_USER"
echo ""

if [ -n "${BACKUP_FILE:-}" ] && [ "$BACKUP_ENABLED" = "1" ] && [ -f "$BACKUP_FILE" ]; then
    echo "备份文件: $BACKUP_FILE"
    echo ""
fi

log_success "数据库初始化成功！"
echo "=============================================="
echo ""

# ----------------------------
# 后续步骤提示
# ----------------------------
echo "后续步骤:"
echo "  1. 配置 application-local.properties（数据库密码与第三方 API 密钥）"
echo "       复制 backend/src/main/resources/application-local.properties.example"
echo "       为 backend/src/main/resources/application-local.properties"
echo "  2. 启动后端: cd backend && mvn spring-boot:run"
echo "  3. 启动前端: cd frontend && npm install && npm start"
echo "  4. 访问应用: http://localhost:3000"
echo ""
echo "测试账号（密码统一为 123456）:"
echo "  - 老人: laowang / 10001"
echo "  - 家属: zhangsan / 10002"
echo "  - 老人: laoli / 10003"
echo ""

exit 0
