@echo off
setlocal
cd /d "%~dp0"
if not exist "ui-fx.jar" echo Missing ui-fx.jar & goto :fail
if not exist "engine.jar" echo Missing engine.jar & goto :fail
if not exist "dto.jar" echo Missing dto.jar & goto :fail
if not exist "lib\jaxb\angus-activation.jar" echo Missing lib\jaxb\angus-activation.jar & goto :fail
if not exist "lib\jaxb\jakarta.activation-api.jar" echo Missing lib\jaxb\jakarta.activation-api.jar & goto :fail
if not exist "lib\jaxb\jakarta.xml.bind-api.jar" echo Missing lib\jaxb\jakarta.xml.bind-api.jar & goto :fail
if not exist "lib\jaxb\jaxb-core.jar" echo Missing lib\jaxb\jaxb-core.jar & goto :fail
if not exist "lib\jaxb\jaxb-impl.jar" echo Missing lib\jaxb\jaxb-impl.jar & goto :fail
if not exist "lib\jfx" echo Missing lib\jfx & goto :fail
if not exist "lib\jfx\bin" echo Missing lib\jfx\bin & goto :fail
set "JAVA_EXE=java"
if not "%JAVA_HOME%"=="" set "JAVA_EXE=%JAVA_HOME%\bin\java"
set "PATH=%CD%\lib\jfx\bin;%PATH%"
set "JFX_MODULES=javafx.controls,javafx.fxml"
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -Djava.library.path="%CD%\lib\jfx\bin" --module-path "%CD%\lib\jfx" --add-modules %JFX_MODULES% -jar "ui-fx.jar" %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
:fail
exit /b 1
