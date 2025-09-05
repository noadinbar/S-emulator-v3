@echo off
setlocal


cd /d "%~dp0"


if not exist "ui.jar"     echo Missing ui.jar     & goto :fail
if not exist "engine.jar" echo Missing engine.jar & goto :fail
if not exist "dto.jar"    echo Missing dto.jar    & goto :fail

set "JAVA_EXE=java"
if not "%JAVA_HOME%"=="" set "JAVA_EXE=%JAVA_HOME%\bin\java"


"%JAVA_EXE%" -jar "ui.jar"
exit /b %ERRORLEVEL%

:fail
echo.
echo Make sure ui.jar, engine.jar, dto.jar (and lib\ if needed) are in this folder.
#exit /b 1
