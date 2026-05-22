#!/bin/bash
# =====================================================
# 老年人用药管理系统 - 数据库初始化脚本
# 版本: 3.0
# 支持: Linux (Ubuntu, CentOS, Debian) / macOS
# 兼容: MySQL 5.7+ / MySQL 8.0+
# =====================================================

set -e  # 遇到错误立即退出
set -u  # 使用未定义变量时报错

# ----------------------------
# 脚本配置参数
# ----------------------------
SCRIPT_VERSION="3.0"
DB_NAME="elderly_medication"
DEFAULT_PORT="3306"

# 默认值
MYSQL_HOST="localhost"
MYSQL_PORT="${DEFAULT_PORT}"
MYSQL_USER="root"
MYSQL_PASSWORD=""
SQL_FILE="src/main/resources/init_database.sql"
BACKUP_ENABLED="1"
SKIP_TEST_DATA="0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 颜色定义
COLOR_HEADER='\033[95m'
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
    echo -e "${COLOR_HEADER}==============================================${COLOR_RESET}"
    echo -e "${COLOR_HEADER}  老年人用药管理系统 - 数据库初始化脚本 v${SCRIPT_VERSION}${COLOR_RESET}"
    echo -e "${COLOR_HEADER}==============================================${COLOR_RESET}"
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

# 检查MySQL命令
check_mysql() {
    MYSQL_CMD=""
    
    # 检查MySQL是否在PATH中
    if command -v mysql &> /dev/null; then
        MYSQL_CMD="mysql"
        return 0
    fi
    
    # 检查常见安装路径
    local MYSQL_PATHS=(
        "/usr/bin/mysql"
        "/usr/local/bin/mysql"
        "/opt/homebrew/bin/mysql"
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

# 测试MySQL连接
test_connection() {
    $MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" -e "SELECT VERSION();" &> /dev/null
    return $?
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
    
    BACKUP_DIR="${SCRIPT_DIR}/db_backups"
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
    echo "  -h, --host <主机>      MySQL主机 (默认: localhost)"
    echo "  -P, --port <端口>      MySQL端口 (默认: 3306)"
    echo "  --skip-backup          跳过数据库备份"
    echo "  --skip-test-data       跳过测试数据导入"
    echo "  --no-input             无需确认直接执行"
    echo "  --help                 显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0"
    echo "  $0 -u root -p mypassword"
    echo "  $0 -h localhost -P 3306 -u root"
    echo "  $0 --skip-test-data"
    echo ""
}

# ----------------------------
# 解析命令行参数
# ----------------------------
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
        --skip-test-data)
            SKIP_TEST_DATA="1"
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
# 参数确认
# ----------------------------
echo ""
log_info "配置信息:"
echo "  主机: $MYSQL_HOST"
echo "  端口: $MYSQL_PORT"
echo "  用户: $MYSQL_USER"
echo "  数据库: $DB_NAME"
echo "  SQL文件: $SQL_FILE"
if [ "$BACKUP_ENABLED" == "1" ]; then
    echo "  备份: 启用"
else
    echo "  备份: 禁用"
fi
if [ "$SKIP_TEST_DATA" == "1" ]; then
    echo "  测试数据: 跳过"
else
    echo "  测试数据: 导入"
fi
echo ""

# 确认执行
if [ "${NO_INPUT:-0}" != "1" ] && [ "$SKIP_TEST_DATA" != "1" ]; then
    echo -n "确认执行初始化？（输入yes继续，其他取消）: "
    read CONFIRM
    if [ "$CONFIRM" != "yes" ] && [ "$CONFIRM" != "YES" ]; then
        log_info "已取消执行"
        exit 0
    fi
fi

# ----------------------------
# 检查SQL文件
# ----------------------------
log_step 1 5 "检查SQL文件..."

# 尝试多种可能的路径
if [ -f "${SCRIPT_DIR}/${SQL_FILE}" ]; then
    FULL_SQL_PATH="${SCRIPT_DIR}/${SQL_FILE}"
elif [ -f "${SCRIPT_DIR}/backend/${SQL_FILE}" ]; then
    FULL_SQL_PATH="${SCRIPT_DIR}/backend/${SQL_FILE}"
elif [ -f "$(pwd)/${SQL_FILE}" ]; then
    FULL_SQL_PATH="$(pwd)/${SQL_FILE}"
elif [ -f "${SQL_FILE}" ]; then
    FULL_SQL_PATH="${SQL_FILE}"
else
    log_error "未找到SQL文件: $SQL_FILE"
    log_info "请确保在项目根目录执行此脚本"
    exit 1
fi

log_success "SQL文件存在: $FULL_SQL_PATH"

# ----------------------------
# 数据库备份
# ----------------------------
log_step 2 5 "备份现有数据库..."

backup_database

# ----------------------------
# 执行数据库初始化
# ----------------------------
log_step 3 5 "执行数据库初始化..."

log_info "正在执行SQL脚本，这可能需要几分钟时间..."

# 构建执行命令
MYSQL_EXEC_CMD="$MYSQL_CMD -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER"
if [ -n "$MYSQL_PASSWORD" ]; then
    MYSQL_EXEC_CMD="$MYSQL_EXEC_CMD --password=$MYSQL_PASSWORD"
fi
MYSQL_EXEC_CMD="$MYSQL_EXEC_CMD --default-character-set=utf8mb4 -e \"source $FULL_SQL_PATH\""

# 执行SQL
if ! eval "$MYSQL_EXEC_CMD" 2>&1; then
    log_error "SQL执行失败"
    log_info "请查看上述错误信息并修复后重试"
    exit 1
fi

log_success "SQL脚本执行完成"

# ----------------------------
# 验证结果
# ----------------------------
log_step 4 5 "验证初始化结果..."

TABLE_COUNT=$($MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" -sN -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$DB_NAME' AND TABLE_TYPE='BASE TABLE';" 2>/dev/null)

if [ "$TABLE_COUNT" == "11" ]; then
    log_success "验证通过: 已创建 11 张数据表"
else
    log_warning "表数量异常: 预期11张表，实际$TABLE_COUNT张"
fi

log_info "创建的表:"
$MYSQL_CMD -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" --password="${MYSQL_PASSWORD}" -sN -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='$DB_NAME' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;" 2>/dev/null

# ----------------------------
# 完成报告
# ----------------------------
log_step 5 5 "生成完成报告..."

echo ""
echo "=============================================="
echo "           数据库初始化完成报告"
echo "=============================================="
echo "数据库名称: $DB_NAME"
echo "MySQL版本: $MYSQL_VERSION"
echo "执行时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "表总数: $TABLE_COUNT"
echo "用户名: $MYSQL_USER"
echo ""

log_success "数据库初始化成功！"
echo "=============================================="
echo ""

# ----------------------------
# 使用说明
# ----------------------------
echo ""
echo "后续步骤:"
echo "  1. 启动后端应用: mvn spring-boot:run 或运行IDE中的启动类"
echo "  2. 访问前端应用: http://localhost:3000 或前端配置的地址"
echo ""
echo "常用命令:"
echo "  - 重新初始化: $0"
echo "  - 跳过测试数据: $0 --skip-test-data"
echo "  - 指定用户: $0 -u username -p password"
echo "  - 查看帮助: $0 --help"
echo ""

exit 0
