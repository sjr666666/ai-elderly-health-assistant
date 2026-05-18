@echo off
chcp 65001 >nul
echo ==============================================
echo 老年人用药管理系统 - 数据库初始化脚本
echo ==============================================
echo.

:: 设置路径变量
set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin"
set "SQL_FILE=src/main/resources/init_database.sql"
set "DB_NAME=elderly_medication"

:: 检查MySQL是否可用
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 正在尝试使用完整路径...
    set "PATH=%MYSQL_PATH%;%PATH%"
    mysql --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo 错误: 未找到MySQL命令，请确保MySQL已安装并配置到环境变量中
        echo 或者修改此脚本中的MYSQL_PATH变量
        echo 当前路径: %MYSQL_PATH%
        pause
        exit /b 1
    )
)

:: 检查SQL文件是否存在
if not exist "%SQL_FILE%" (
    echo 错误: 未找到SQL文件: %CD%\%SQL_FILE%
    pause
    exit /b 1
)

echo 正在初始化数据库...
echo 数据库: %DB_NAME%
echo 脚本: %SQL_FILE%
echo.

:: 执行SQL脚本 - 使用source命令避免重定向问题
mysql -u root -p060504 --execute="source %CD:/=\%/%SQL_FILE%"

if %errorlevel% equ 0 (
    echo.
    echo ==============================================
    echo 数据库初始化成功!
    echo ==============================================
    echo 数据库名称: %DB_NAME%
    echo 脚本位置: %CD%\%SQL_FILE%
    echo.
    echo 测试数据已插入:
    echo - 8种药品数据
    echo - 2个测试用户
    echo ==============================================
) else (
    echo.
    echo ==============================================
    echo 数据库初始化失败!
    echo ==============================================
    echo 请检查:
    echo 1. MySQL服务是否已启动
    echo 2. root用户密码是否正确(当前密码: 060504)
    echo 3. SQL文件路径是否正确
    echo ==============================================
)

echo.
pause