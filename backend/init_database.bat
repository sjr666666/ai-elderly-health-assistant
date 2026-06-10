@echo off
chcp 65001 >nul 2>&1
REM =====================================================
REM 老年人用药管理系统 - 数据库初始化脚本
REM 版本: 4.0
REM 支持: Windows 7/8/10/11, Windows Server 2016+
REM 兼容: MySQL 5.7+ / MySQL 8.0+
REM 支持目录: 可以在 backend 目录或项目根目录运行
REM =====================================================

setlocal enabledelayedexpansion

REM ----------------------------
REM 脚本配置参数
REM ----------------------------
set "SCRIPT_VERSION=4.0"
set "DB_NAME=elderly_medication"
set "DEFAULT_PORT=3306"

REM 默认值
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=%DEFAULT_PORT%"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD="
set "INIT_SQL=init_database.sql"
set "DRUG_SQL=init_drug_data.sql"
set "BACKUP_ENABLED=1"
set "SKIP_DRUG_DATA=0"
set "PROJECT_ROOT="

REM 颜色定义（仅 Windows 10+ 支持 ANSI）
set "COLOR_INFO=\033[94m"
set "COLOR_SUCCESS=\033[92m"
set "COLOR_WARNING=\033[93m"
set "COLOR_ERROR=\033[91m"
set "COLOR_RESET=\033[0m"

REM 检测是否支持颜色输出（Windows 10+ 默认开启，Win11 完全支持）
set "COLOR_SUPPORT=0"
ver | findstr /I "10\." >nul 2>&1
if !errorlevel! equ 0 set "COLOR_SUPPORT=1"
ver | findstr /I "11\." >nul 2>&1
if !errorlevel! equ 0 set "COLOR_SUPPORT=1"

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
if !errorlevel! equ 0 (
    set "MYSQL_FOUND=1"
    set "MYSQL_CMD=mysql"
    goto :eof
)

REM 检查常见安装路径
set "MYSQL_PATHS[0]=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PATHS[1]=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
set "MYSQL_PATHS[2]=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
set "MYSQL_PATHS[3]=C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PATHS[4]=D:\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "MYSQL_PATHS[5]=D:\MySQL\MySQL Server 5.7\bin\mysql.exe"

for %%P in ("%MYSQL_PATHS[0]","%MYSQL_PATHS[1]","%MYSQL_PATHS[2]","%MYSQL_PATHS[3]","%MYSQL_PATHS[4]","%MYSQL_PATHS[5]") do (
    if exist %%P (
        set "MYSQL_FOUND=1"
        set "MYSQL_CMD=%%~P"
        goto :eof
    )
)

REM 检查XAMPP / WAMP
if exist "C:\xampp\mysql\bin\mysql.exe" (
    set "MYSQL_FOUND=1"
    set "MYSQL_CMD=C:\xampp\mysql\bin\mysql.exe"
    goto :eof
)
if exist "C:\wamp64\mysql\bin\mysql.exe" (
    set "MYSQL_FOUND=1"
    set "MYSQL_CMD=C:\wamp64\mysql\bin\mysql.exe"
    goto :eof
)

goto :eof

:check_mysql_service
set "SERVICE_RUNNING=0"
sc query MySQL80 >nul 2>&1
if !errorlevel! equ 0 set "SERVICE_RUNNING=1"
sc query MySQL84 >nul 2>&1
if !errorlevel! equ 0 set "SERVICE_RUNNING=1"
sc query MySQL57 >nul 2>&1
if !errorlevel! equ 0 set "SERVICE_RUNNING=1"
sc query mysql >nul 2>&1
if !errorlevel! equ 0 set "SERVICE_RUNNING=1"
goto :eof

:get_password
set "MYSQL_PASSWORD="
set /p MYSQL_PASSWORD="请输入MySQL root密码（直接回车表示无密码）: "
goto :eof

REM 解析 SQL 文件路径：支持在 backend 目录或项目根目录运行
:resolve_paths
set "PROJECT_ROOT="
set "INIT_SQL_PATH="
set "DRUG_SQL_PATH="

REM 1. 优先：当前目录含 init_database.bat（说明是 backend 目录）
if exist "%CD%\init_database.bat" (
    set "PROJECT_ROOT=%CD%\.."
    set "INIT_SQL_PATH=%CD%\src\main\resources\%INIT_SQL%"
    set "DRUG_SQL_PATH=%CD%\src\main\resources\%DRUG_SQL%"
    goto :eof
)

REM 2. 兼容：当前目录含 init_database.bat，且 SQL 已在 backend 兄弟目录
if exist "%CD%\..\backend\init_database.bat" (
    set "PROJECT_ROOT=%CD%"
    set "INIT_SQL_PATH=%CD%\..\backend\src\main\resources\%INIT_SQL%"
    set "DRUG_SQL_PATH=%CD%\..\backend\src\main\resources\%DRUG_SQL%"
    goto :eof
)

REM 3. 当前在 backend 目录，SQL 在 src/main/resources/
if exist "%CD%\src\main\resources\%INIT_SQL%" (
    set "PROJECT_ROOT=%CD%\.."
    set "INIT_SQL_PATH=%CD%\src\main\resources\%INIT_SQL%"
    set "DRUG_SQL_PATH=%CD%\src\main\resources\%DRUG_SQL%"
    goto :eof
)

REM 4. 当前在项目根目录，SQL 在 backend 子目录
if exist "%CD%\backend\src\main\resources\%INIT_SQL%" (
    set "PROJECT_ROOT=%CD%"
    set "INIT_SQL_PATH=%CD%\backend\src\main\resources\%INIT_SQL%"
    set "DRUG_SQL_PATH=%CD%\backend\src\main\resources\%DRUG_SQL%"
    goto :eof
)

goto :eof

:backup_database
if "%BACKUP_ENABLED%"=="0" goto :eof

if "%PROJECT_ROOT%"=="" (
    set "BACKUP_DIR=%CD%\db_backups"
) else (
    set "BACKUP_DIR=%PROJECT_ROOT%\db_backups"
)

set "BACKUP_FILE=%BACKUP_DIR%\%DB_NAME%_backup_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql"
set "BACKUP_FILE=%BACKUP_FILE: =0%"

if not exist "%BACKUP_DIR%" (
    mkdir "%BACKUP_DIR%" 2>nul
)

call :print_info "正在备份现有数据库..."
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -e "USE %DB_NAME%;" >nul 2>&1
if !errorlevel! neq 0 (
    call :print_warning "数据库不存在或无法连接，跳过备份"
    goto :eof
)

"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" %DB_NAME% > "%BACKUP_FILE%" 2>nul
if !errorlevel! equ 0 (
    call :print_success "数据库已备份到: %BACKUP_FILE%"
) else (
    call :print_warning "备份过程出现错误，但继续执行..."
)
goto :eof

:test_connection
set "TEST_RESULT=0"
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -e "SELECT VERSION();" >nul 2>&1
if !errorlevel! neq 0 set "TEST_RESULT=1"
goto :eof

:show_help
echo.
echo 用法: init_database.bat [选项]
echo.
echo 选项:
echo   -u ^<用户名^>            MySQL用户名 (默认: root)
echo   -p ^<密码^>              MySQL密码 (默认: 无)
echo   -H ^<主机^>              MySQL主机 (默认: localhost)
echo   -P ^<端口^>              MySQL端口 (默认: 3306)
echo   --skip-backup           跳过数据库备份
echo   --skip-drug-data        跳过完整药品数据导入（仅导入 init_database.sql 中的基础数据）
echo   --no-input              无需确认直接执行
echo   -h, --help              显示此帮助信息
echo.
echo 示例:
echo   init_database.bat
echo   init_database.bat -u root -p mypassword
echo   init_database.bat -H localhost -P 3306 -u root
echo   init_database.bat --skip-drug-data
echo.
echo 说明:
echo   此脚本会依次执行 init_database.sql 与 init_drug_data.sql
echo   脚本可重复执行：init_database.sql 会 DROP 重建表，init_drug_data.sql 使用 INSERT IGNORE
echo   可在 backend 目录或项目根目录运行此脚本
echo.
pause
exit /b 0

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
if /i "%~1"=="--skip-drug-data" set "SKIP_DRUG_DATA=1" & shift & goto :parse_args
if /i "%~1"=="--no-input" set "NO_INPUT=1" & shift & goto :parse_args
echo 未知参数: %~1
echo 使用 --help 查看帮助
exit /b 1

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

call :check_mysql_service
if "%SERVICE_RUNNING%"=="0" (
    call :print_warning "MySQL服务可能未运行，正在尝试连接..."
)

REM 测试连接
call :test_connection
if "%TEST_RESULT%"=="1" (
    if "%NO_INPUT%"=="1" (
        call :print_error "无法连接到MySQL服务器（--no-input 模式不会提示输入密码）"
        exit /b 1
    )
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
REM 解析 SQL 文件路径
REM ----------------------------
call :resolve_paths

if "%INIT_SQL_PATH%"=="" (
    call :print_error "未找到SQL文件: %INIT_SQL%"
    call :print_info "请在项目根目录或 backend 目录运行此脚本"
    pause
    exit /b 1
)

if not exist "%INIT_SQL_PATH%" (
    call :print_error "未找到SQL文件: %INIT_SQL_PATH%"
    pause
    exit /b 1
)

REM ----------------------------
REM 参数确认
REM ----------------------------
echo.
call :print_info "配置信息:"
echo   主机: %MYSQL_HOST%
echo   端口: %MYSQL_PORT%
echo   用户: %MYSQL_USER%
echo   数据库: %DB_NAME%
echo   SQL文件: %INIT_SQL_PATH%
if not "%SKIP_DRUG_DATA%"=="1" (
    echo   药品扩展: %DRUG_SQL_PATH%
) else (
    echo   药品扩展: 跳过
)
if "%BACKUP_ENABLED%"=="1" (
    echo   备份: 启用
) else (
    echo   备份: 禁用
)
echo.

if "%NO_INPUT%"=="1" goto :skip_confirm
set /p CONFIRM="确认执行初始化？（输入yes继续，其他取消）: "
if /i not "!CONFIRM!"=="yes" (
    call :print_info "已取消执行"
    exit /b 0
)
:skip_confirm

REM ----------------------------
REM 数据库备份
REM ----------------------------
call :print_step 1 4 "备份现有数据库..."
call :backup_database

REM ----------------------------
REM 执行 init_database.sql
REM ----------------------------
call :print_step 2 4 "执行数据库结构与基础数据..."

call :print_info "正在执行 init_database.sql，这可能需要几十秒..."

set "EXEC_ERROR=0"
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" --default-character-set=utf8mb4 < "%INIT_SQL_PATH%" > "%TEMP%\init_db_out.log" 2>&1
if !errorlevel! neq 0 (
    set "EXEC_ERROR=1"
    call :print_error "init_database.sql 执行失败"
    type "%TEMP%\init_db_out.log" | findstr /C:"ERROR" /C:"error"
)

if "%EXEC_ERROR%"=="1" (
    call :print_info "请查看上方错误信息并修复后重试"
    pause
    exit /b 1
)

call :print_success "init_database.sql 执行完成"

REM ----------------------------
REM 执行 init_drug_data.sql
REM ----------------------------
if "%SKIP_DRUG_DATA%"=="1" (
    call :print_info "已跳过 init_drug_data.sql"
) else (
    call :print_step 3 4 "补充完整药品数据..."

    if not exist "%DRUG_SQL_PATH%" (
        call :print_warning "未找到 %DRUG_SQL%，跳过完整药品数据导入"
    ) else (
        call :print_info "正在执行 init_drug_data.sql..."
        "%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" --default-character-set=utf8mb4 < "%DRUG_SQL_PATH%" > "%TEMP%\init_drug_out.log" 2>&1
        if !errorlevel! neq 0 (
            call :print_error "init_drug_data.sql 执行失败"
            type "%TEMP%\init_drug_out.log" | findstr /C:"ERROR" /C:"error"
        ) else (
            call :print_success "init_drug_data.sql 执行完成"
        )
    )
)

REM ----------------------------
REM 验证结果
REM ----------------------------
call :print_step 4 4 "验证初始化结果..."

set "TABLE_COUNT=0"
for /f "tokens=*" %%c in ('"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -sN -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='%DB_NAME%' AND TABLE_TYPE='BASE TABLE';" 2^>nul') do set "TABLE_COUNT=%%c"

if "%TABLE_COUNT%"=="13" (
    call :print_success "验证通过: 已创建 13 张数据表"
) else (
    call :print_warning "表数量异常: 预期13张表，实际%TABLE_COUNT%张"
)

call :print_info "创建的表:"
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" -sN -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='%DB_NAME%' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;" 2>nul

call :print_info "各表数据统计:"
"%MYSQL_CMD%" -h "%MYSQL_HOST%" -P "%MYSQL_PORT%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" --table -e "SELECT 'sys_user' AS '表名', COUNT(*) AS '行数' FROM sys_user UNION ALL SELECT 'drug_base', COUNT(*) FROM drug_base UNION ALL SELECT 'user_medicine_box', COUNT(*) FROM user_medicine_box UNION ALL SELECT 'medication_plan', COUNT(*) FROM medication_plan UNION ALL SELECT 'medication_log', COUNT(*) FROM medication_log UNION ALL SELECT 'drug_conflict_rules', COUNT(*) FROM drug_conflict_rules UNION ALL SELECT 'drug_aliases', COUNT(*) FROM drug_aliases UNION ALL SELECT 'drug_category_keywords', COUNT(*) FROM drug_category_keywords;" 2>nul

REM ----------------------------
REM 完成报告
REM ----------------------------
echo.
echo ==============================================
echo           数据库初始化完成报告
echo ==============================================
echo 数据库名称: %DB_NAME%
echo MySQL版本: %MYSQL_VERSION%
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
REM 后续步骤提示
REM ----------------------------
echo 后续步骤:
echo   1. 配置 application-local.properties（数据库密码与第三方 API 密钥）
echo        复制 backend/src/main/resources/application-local.properties.example
echo        为 backend/src/main/resources/application-local.properties
echo   2. 启动后端: cd backend ^&^& mvn spring-boot:run
echo   3. 启动前端: cd frontend ^&^& npm install ^&^& npm start
echo   4. 访问应用: http://localhost:3000
echo.
echo 测试账号（密码统一为 123456）:
echo   - 老人: laowang / 10001
echo   - 家属: zhangsan / 10002
echo   - 老人: laoli / 10003
echo.

pause
exit /b 0
