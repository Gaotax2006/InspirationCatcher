@echo off
chcp 65001 > nul
echo ========================================
echo   灵感捕手 - 创建发布包
echo ========================================

REM 设置版本信息
set VERSION=1.0.0
set APP_NAME=InspirationCatcher
set RELEASE_DIR=%APP_NAME%-v%VERSION%

echo 2. 创建发布目录结构...
rmdir /s /q release 2>nul
mkdir release
mkdir release\%RELEASE_DIR%
mkdir release\%RELEASE_DIR%\bin
mkdir release\%RELEASE_DIR%\lib
mkdir release\%RELEASE_DIR%\config
mkdir release\%RELEASE_DIR%\logs

echo 3. 复制可执行文件...
copy target\InspirationCatcher.jar release\%RELEASE_DIR%\%APP_NAME%.jar

echo 4. 复制依赖库...
xcopy /E /I target\lib release\%RELEASE_DIR%\lib\
xcopy /E /I javafx-sdk-21.0.1 release\%RELEASE_DIR%\javafx-sdk-21.0.1

echo 5. 复制数据库文件...
if exist inspiration1.db copy inspiration.db release\%RELEASE_DIR%\
if not exist inspiration1.db echo 警告: 未找到数据库文件，将创建空数据库

echo 6. 创建启动脚本...
echo 创建Windows启动脚本...

REM 创建Windows启动脚本
echo @echo off > release\%RELEASE_DIR%\run.bat
echo chcp 65001 > nul >>  release\%RELEASE_DIR%\run.bat
echo echo 正在启动灵感捕手... >> release\%RELEASE_DIR%\run.bat
echo echo ======================================== >> release\%RELEASE_DIR%\run.bat
echo java --module-path "./javafx-sdk-21.0.1/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar "%APP_NAME%.jar" >> release\%RELEASE_DIR%\run.bat
echo echo. >> release\%RELEASE_DIR%\run.bat
echo echo 程序已退出 >> release\%RELEASE_DIR%\run.bat
echo pause >> release\%RELEASE_DIR%\run.bat

REM 创建Mac/Linux启动脚本
echo #!/bin/bash > release\%RELEASE_DIR%\run.sh
echo echo "正在启动灵感捕手..." >> release\%RELEASE_DIR%\run.sh
echo echo "========================================" >> release\%RELEASE_DIR%\run.sh
echo java --module-path "./javafx-sdk-21.0.1/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar "%APP_NAME%.jar" >> release\%RELEASE_DIR%\run.sh
echo echo "" >> release\%RELEASE_DIR%\run.sh
echo echo "程序已退出" >> release\%RELEASE_DIR%\run.sh

echo 7. 创建配置文件...
echo # 应用配置 > release\%RELEASE_DIR%\config\app.properties
echo app.name=灵感捕手 >> release\%RELEASE_DIR%\config\app.properties
echo app.version=%VERSION% >> release\%RELEASE_DIR%\config\app.properties
echo database.file=inspiration.db >> release\%RELEASE_DIR%\config\app.properties
echo log.dir=logs >> release\%RELEASE_DIR%\config\app.properties

echo 8. 创建说明文档...
echo # 灵感捕手使用说明 > release\%RELEASE_DIR%\README.md
echo. >> release\%RELEASE_DIR%\README.md
echo ## 系统要求 >> release\%RELEASE_DIR%\README.md
echo - Java Runtime Environment (JRE) 17或更高版本 >> release\%RELEASE_DIR%\README.md
echo - 至少500MB可用磁盘空间 >> release\%RELEASE_DIR%\README.md
echo - 至少2GB内存 >> release\%RELEASE_DIR%\README.md
echo. >> release\%RELEASE_DIR%\README.md
echo ## 快速开始 >> release\%RELEASE_DIR%\README.md
echo 1. 确保已安装Java (17+) >> release\%RELEASE_DIR%\README.md
echo    - 检查方法: 打开命令行，输入 `java -version` >> release\%RELEASE_DIR%\README.md
echo 2. 双击 `run.bat` (Windows) 或运行 `run.sh` (Mac/Linux) >> release\%RELEASE_DIR%\README.md
echo. >> release\%RELEASE_DIR%\README.md
echo ## 手动启动 >> release\%RELEASE_DIR%\README.md
echo 1. 打开命令行 >> release\%RELEASE_DIR%\README.md
echo 2. 进入程序目录 >> release\%RELEASE_DIR%\README.md
echo 3. 运行: `java -jar InspirationCatcher.jar` >> release\%RELEASE_DIR%\README.md
echo. >> release\%RELEASE_DIR%\README.md
echo ## 故障排除 >> release\%RELEASE_DIR%\README.md
echo ### 错误: "找不到Java" >> release\%RELEASE_DIR%\README.md
echo - 下载并安装Java 17+: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html >> release\%RELEASE_DIR%\README.md
echo. >> release\%RELEASE_DIR%\README.md
echo ### 错误: "JavaFX模块找不到" >> release\%RELEASE_DIR%\README.md
echo 使用以下命令启动: >> release\%RELEASE_DIR%\README.md
echo ``` bash >> release\%RELEASE_DIR%\README.md
echo java --module-path "./javafx-sdk-21.0.1/lib" --add-modules javafx.controls,javafx.fxml,javafx.web -jar InspirationCatcher.jar >> release\%RELEASE_DIR%\README.md
echo ``` >> release\%RELEASE_DIR%\README.md

pause