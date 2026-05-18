@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: =====================================================
:: 老年人用药管理系统 - 数据库初始化脚本
:: =====================================================
:: 使用说明:
:: 1. 确保MySQL服务已启动
:: 2. 双击此脚本运行
:: 3. 根据提示输入MySQL root密码
:: =====================================================

echo.
echo =====================================================
echo           老年人用药管理系统 - 数据库初始化
echo =====================================================
echo.

:: 检查MySQL是否可用
echo [步骤1] 检查MySQL连接...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到MySQL命令行工具，请确保MySQL已安装并配置到环境变量PATH中
    pause
    exit /b 1
)
echo MySQL命令行工具检测成功

:: 获取MySQL密码
set "mysql_password="
set /p "mysql_password=请输入MySQL root密码: "

:: 检查密码是否为空
if not defined mysql_password (
    echo 错误: 密码不能为空
    pause
    exit /b 1
)

echo.
echo [步骤2] 开始初始化数据库...

:: 执行SQL脚本
mysql -u root -p%mysql_password% < "src/main/resources/init_database.sql"

:: 检查执行结果
if %errorlevel% equ 0 (
    echo.
    echo =====================================================
    echo                     初始化成功！
    echo =====================================================
    echo 数据库已成功创建，包含以下内容:
    echo   - 数据库: elderly_medication
    echo   - 11张数据表（空表）
    echo.
    echo 请在系统中注册新账号后使用。
    echo =====================================================
) else (
    echo.
    echo =====================================================
    echo                     初始化失败！
    echo =====================================================
    echo 错误信息:
    echo   请检查MySQL服务是否启动
    echo   请检查root密码是否正确
    echo   请确保脚本文件路径正确
    echo =====================================================
)

pause