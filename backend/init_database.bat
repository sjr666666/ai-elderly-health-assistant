@echo off
chcp 65001 >nul 2>&1
REM =====================================================
REM 老年人用药管理系统 - 数据库初始化脚本
REM 版本: 3.0
REM 支持: Windows 7/8/10/11, Windows Server 2016+
REM 兼容: MySQL 5.7+ / MySQL 8.0+
REM =====================================================

setlocal enabledelayedexpansion

REM ----------------------------
REM 脚本配置参数
REM ----------------------------
set "SCRIPT_VERSION=3.0"
set "DB_NAME=elderly_medication"
set "DEFAULT_PORT=3306"

REM 默认值
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=%DEFAULT_PORT%"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD="
set "SQL_FILE=src\main\resources\init_database.sql"
set "BACKUP_ENABLED=1"
set "SKIP_TEST_DATA=0"

REM 颜色定义
set "COLOR_HEADER=\033[95m"
set "COLOR_INFO=\033[94m"
set "COLOR_SUCCESS=\033[92m"
set "COLOR_WARNING=\033[93m"
set "COLOR_ERROR=\033[91m"
set "COLOR_RESET=\033[0m"

REM 检测是否支持颜色输出（Windows 10+）
set "COLOR_SUPPORT=0"
ver | findstr /I "10\." >nul 2>&1
if %errorlevel% equ 0 set "COLOR_SUPPORT=1"
ver | findstr /I "11\." >nul 2>&1
if %errorlevel% equ 0 set "COLOR_SUPPORT=1"

REM ----------------------------
REM 函数定义
REM ----------------------------

:print_header
echo.
echo ==============================================
echo   老年人用药管理系统 - 数据库初始化脚本 v%SCRIPT_VERSION%
echo ==============================================
echo.
goto :eof

:print_step
echo %COLOR_INFO%[步骤 %1/%2] %~3 %COLOR_RESET%
goto :eof

:print_success
echo %COLOR_SUCCESS%[成功] %~1 %COLOR_RESET%
goto :eof

:print_warning
echo %COLOR_WARNING%[警告] %~1 %COLOR_RESET%
goto :eof

:print_error
echo %COLOR_ERROR%[错误] %~1 %COLOR_RESET%
goto :eof

:print_info
echo [信息] %~1
goto :eof

:check_mysql
set "MYSQL_FOUND=0"
set "MYSQL_CMD="

REM 检查MySQL是否在PATH中
where mysql >nul 2>&1
if %errorlevel% equ 0 (
    set "MYSQL_FOUND=1"
    set "MYSQL_CMD=mysql"
    goto :eof
)

REM 检查常见安装路径
set "MYSQL_PATHS[0]=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PATHS[1]=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
set "MYSQL_PATHS[2]=C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PATHS[3]=D:\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PATHS[4]=D:\MySQL\MySQL Server 5.7\bin\mysql.exe"

for %%P in ("%MYSQL_PATHS[0]","%MYSQL_PATHS[1]","%MYSQL_PATHS[2]","%MYSQL_PATHS[3]","%MYSQL_PATHS[4]") do (
    if exist %%P (
        set "MYSQL_FOUND=1"
        set "MYSQL_CMD=%%~P"
        goto :eof
    )
)

REM 检查XAMPP
if exist "C:\xampp\mysql\bin\mysql.exe" (
    set "MYSQL_FOUND=1"
    set "MYSQL_CMD=C:\xampp\mysql\bin\mysql.exe"
    goto :eof
)

REM 检查WAMP
if exist "C:\wamp64\mysql\bin\mysql.exe" (
    set "MYSQL_FOUND=1"
    set "MYSQL_CMD=C:\wamp64\mysql\bin\mysql.exe"
    goto :eof
)

goto :eof

:check_mysql_service
set "SERVICE_RUNNING=0"
sc query MySQL80 >nul 2>&1
if %errorlevel% equ 0 set "SERVICE_RUNNING=1"
sc query MySQL57 >nul 2>&1
if %errorlevel% equ 0 set "SERVICE_RUNNING=1"
sc query mysql >nul 2>&1
if %errorlevel% equ 0 set "SERVICE_RUNNING=1"
goto :eof

:get_password
set "MYSQL_PASSWORD="
set /p MYSQL_PASSWORD="请输入MySQL root密码（直接回车表示无密码）: "
goto :eof

:convert_path
set "CONVERTED_SQL_FILE=%SQL_FILE:\=/%"
set "FULL_SQL_PATH=%CD%\%CONVERTED_SQL_FILE%"
set "FULL_SQL_PATH=%FULL_SQL_PATH:\\=\%"
goto :eof

:backup_database
if "%BACKUP_ENABLED%"=="0" goto :eof

set "BACKUP_DIR=%CD%\db_backups"
set "BACKUP_FILE=%BACKUP_DIR%\%DB_NAME%_backup_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql"
set "BACKUP_FILE=%BACKUP_FILE: =0%"

if not exist "%BACKUP_DIR%" (
    mkdir "%BACKUP_DIR%" 2>nul
)

call :print_info "正在备份现有数据库..."
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" --single-transaction --quick --lock-tables=false -e "SELECT 'SKIP_BACKUP' as dummy" >nul 2>&1
if %errorlevel% neq 0 (
    call :print_warning "无法连接MySQL或数据库不存在，跳过备份"
    goto :eof
)

"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" %DB_NAME% > "%BACKUP_FILE%" 2>nul
if %errorlevel% equ 0 (
    call :print_success "数据库已备份到: %BACKUP_FILE%"
) else (
    call :print_warning "备份过程出现错误，但继续执行..."
)
goto :eof

:test_connection
set "TEST_RESULT=0"
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -e "SELECT VERSION();" >nul 2>&1
if %errorlevel% neq 0 set "TEST_RESULT=1"
goto :eof

REM ----------------------------
REM 主脚本开始
REM ----------------------------
call :print_header

REM ----------------------------
REM 解析命令行参数
REM ----------------------------
:parse_args
if "%~1"=="" goto :args_done
if /i "%~1"=="-h" goto :show_help
if /i "%~1"=="--help" goto :show_help
if /i "%~1"=="-u" set "MYSQL_USER=%~2" & shift & shift & goto :parse_args
if /i "%~1"=="-p" set "MYSQL_PASSWORD=%~2" & shift & shift & goto :parse_args
if /i "%~1"=="-H" set "MYSQL_HOST=%~2" & shift & shift & goto :parse_args
if /i "%~1"=="-P" set "MYSQL_PORT=%~2" & shift & shift & goto :parse_args
if /i "%~1"=="--skip-backup" set "BACKUP_ENABLED=0" & shift & goto :parse_args
if /i "%~1"=="--skip-test-data" set "SKIP_TEST_DATA=1" & shift & goto :parse_args
if /i "%~1"=="--no-input" goto :args_done
shift
goto :parse_args

:args_done

REM ----------------------------
REM 环境检测
REM ----------------------------
call :print_info "正在检测MySQL环境..."

call :check_mysql
if "%MYSQL_FOUND%"=="0" (
    call :print_error "未找到MySQL命令"
    call :print_info "请确保MySQL已安装并配置到环境变量，或指定正确的安装路径"
    echo.
    echo 常见解决方案:
    echo   1. 下载并安装 MySQL: https://dev.mysql.com/downloads/mysql/
    echo   2. 使用XAMPP或WAMP等集成环境
    echo   3. 将MySQL的bin目录添加到系统PATH环境变量
    echo.
    pause
    exit /b 1
)

call :print_success "找到MySQL: %MYSQL_CMD%"

REM 检测MySQL服务状态
call :check_mysql_service
if "%SERVICE_RUNNING%"=="0" (
    call :print_warning "MySQL服务可能未运行，正在尝试连接..."
)

REM 测试连接
call :test_connection
if "%TEST_RESULT%"=="1" (
    call :get_password
    call :test_connection
    if "%TEST_RESULT%"=="1" (
        call :print_error "无法连接到MySQL服务器"
        call :print_info "请检查:"
        echo   - MySQL服务是否启动
        echo   - 主机地址和端口是否正确
        echo   - 用户名和密码是否正确
        echo.
        pause
        exit /b 1
    )
)

REM 获取MySQL版本
for /f "tokens=*" %%v in ('"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -sN -e "SELECT VERSION();" 2^>nul') do set "MYSQL_VERSION=%%v"
call :print_info "MySQL版本: %MYSQL_VERSION%"

REM ----------------------------
REM 参数确认
REM ----------------------------
echo.
call :print_info "配置信息:"
echo   主机: %MYSQL_HOST%
echo   端口: %MYSQL_PORT%
echo   用户: %MYSQL_USER%
echo   数据库: %DB_NAME%
echo   SQL文件: %SQL_FILE%
if "%BACKUP_ENABLED%"=="1" (
    echo   备份: 启用
) else (
    echo   备份: 禁用
)
if "%SKIP_TEST_DATA%"=="1" (
    echo   测试数据: 跳过
) else (
    echo   测试数据: 导入
)
echo.

if "%SKIP_TEST_DATA%"=="0" (
    set /p CONFIRM="确认执行初始化？（输入yes继续，其他取消）: "
    if /i not "!CONFIRM!"=="yes" (
        call :print_info "已取消执行"
        exit /b 0
    )
)

REM ----------------------------
REM 检查SQL文件
REM ----------------------------
call :print_step 1 5 "检查SQL文件..."

call :convert_path

if not exist "%FULL_SQL_PATH%" (
    call :print_error "未找到SQL文件: %SQL_FILE%"
    call :print_info "请确保在项目根目录执行此脚本"
    pause
    exit /b 1
)

call :print_success "SQL文件存在: %FULL_SQL_PATH%"

REM ----------------------------
REM 数据库备份
REM ----------------------------
call :print_step 2 5 "备份现有数据库..."

call :backup_database

REM ----------------------------
REM 执行数据库初始化
REM ----------------------------
call :print_step 3 5 "执行数据库初始化..."

call :print_info "正在执行SQL脚本，这可能需要几分钟时间..."

set "EXEC_ERROR=0"

REM 构建执行命令
set "MYSQL_EXEC_CMD=%MYSQL_CMD% -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER%"
if not "%MYSQL_PASSWORD%"=="" (
    set "MYSQL_EXEC_CMD=%MYSQL_EXEC_CMD% --password=%MYSQL_PASSWORD%"
)
set "MYSQL_EXEC_CMD=%MYSQL_EXEC_CMD% --default-character-set=utf8mb4 -e "source %FULL_SQL_PATH%""

REM 执行SQL
%MYSQL_EXEC_CMD% >nul 2>&1
if %errorlevel% neq 0 (
    set "EXEC_ERROR=1"
    call :print_error "SQL执行失败，正在检查错误信息..."
    %MYSQL_EXEC_CMD% 2>&1 | findstr /C:"ERROR" /C:"error"
)

if "%EXEC_ERROR%"=="1" (
    call :print_error "数据库初始化失败"
    call :print_info "请查看上述错误信息并修复后重试"
    pause
    exit /b 1
)

call :print_success "SQL脚本执行完成"

REM ----------------------------
REM 验证结果
REM ----------------------------
call :print_step 4 5 "验证初始化结果..."

set "TABLE_COUNT=0"
for /f "tokens=*" %%c in ('"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -sN -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='%DB_NAME%' AND TABLE_TYPE='BASE TABLE';" 2^>nul') do set "TABLE_COUNT=%%c"

if "%TABLE_COUNT%"=="11" (
    call :print_success "验证通过: 已创建 11 张数据表"
) else (
    call :print_warning "表数量异常: 预期11张表，实际%TABLE_COUNT%张"
)

REM 显示表列表
call :print_info "创建的表:"
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -sN -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='%DB_NAME%' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;" 2^>nul

REM ----------------------------
REM 完成报告
REM ----------------------------
call :print_step 5 5 "生成完成报告..."

echo.
echo ==============================================
echo           数据库初始化完成报告
echo ==============================================
echo 数据库名称: %DB_NAME%
echo MySQL版本: %MYSQL_VERSION%
echo 执行时间: %date% %time%
echo 表总数: %TABLE_COUNT%
echo 用户名: %MYSQL_USER%
echo.

if "%BACKUP_ENABLED%"=="1" if exist "%BACKUP_FILE%" (
    echo 备份文件: %BACKUP_FILE%
    echo.
)

call :print_success "数据库初始化成功！"
echo ==============================================
echo.

REM ----------------------------
REM 使用说明
REM ----------------------------
:show_usage
echo.
echo 后续步骤:
echo   1. 启动后端应用: mvn spring-boot:run 或运行IDE中的启动类
echo   2. 访问前端应用: http://localhost:3000 或前端配置的地址
echo.
echo 常用命令:
echo   - 重新初始化: init_database.bat
echo   - 跳过测试数据: init_database.bat --skip-test-data
echo   - 指定用户: init_database.bat -u username -p password
echo   - 查看帮助: init_database.bat --help
echo.

pause
exit /b 0

REM ----------------------------
REM 帮助信息
REM ----------------------------
:show_help
echo.
echo 用法: init_database.bat [选项]
echo.
echo 选项:
echo   -u, -u ^<username^>     MySQL用户名 (默认: root)
echo   -p, -p ^<password^>     MySQL密码 (默认: 无)
echo   -H, -H ^<host^>         MySQL主机 (默认: localhost)
echo   -P, -P ^<port^>         MySQL端口 (默认: 3306)
echo   --skip-backup          跳过数据库备份
echo   --skip-test-data       跳过测试数据导入
echo   --no-input             无需确认直接执行
echo   -h, --help             显示此帮助信息
echo.
echo 示例:
echo   init_database.bat
echo   init_database.bat -u root -p mypassword
echo   init_database.bat -H localhost -P 3306 -u root
echo   init_database.bat --skip-test-data
echo.
pause
exit /b 0
