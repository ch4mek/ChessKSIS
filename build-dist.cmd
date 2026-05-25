@echo off
chcp 65001 >nul
setlocal

:: ============================================================
:: ChessKSiS Client Distribution Builder
:: ============================================================
:: Creates a portable folder with .exe + bundled JRE.
:: The resulting folder can be zipped and sent to another user.
::
:: PREREQUISITES:
:: 1. JDK 17+ installed (with JAVA_HOME set, or in PATH)
:: 2. Download Liberica JDK 17 Full (with JavaFX) from:
::    https://bell-sw.com/pages/downloads/#/java-17-current
::    Extract it and set JRE_PATH below to its location
:: ============================================================

:: --- CONFIGURATION ---
:: Path to a JDK/JRE with JavaFX support (Liberica Full recommended).
:: This JRE will be bundled into the distribution.
:: Option 1: Set environment variable JRE_PATH
:: Option 2: Edit the path below

if defined JRE_PATH (
    set "BUNDLED_JRE=%JRE_PATH%"
) else (
    :: EDIT THIS PATH — point to your Liberica JDK 17 Full installation:
    set "BUNDLED_JRE=C:\Program Files\BellSoft\LibericaJDK-17-Full"
)

set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6\plugins\maven\lib\maven3\bin\mvn.cmd"
set "DIST_DIR=dist\ChessKSiS"

echo ============================================================
echo  ChessKSiS Distribution Builder
echo ============================================================
echo.

:: Step 1: Build the project
echo [1/4] Building project...
"%MVN%" clean package -pl chess-client -am -q
if errorlevel 1 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo       Build successful.

:: Step 2: Create distribution folder
echo [2/4] Creating distribution folder...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"

:: Step 3: Copy executables
echo [3/4] Copying files...
copy /y "chess-client\target\ChessKSiS.exe" "%DIST_DIR%\" >nul
if errorlevel 1 (
    echo ERROR: ChessKSiS.exe not found! Launch4j may have failed.
    echo Trying to copy JAR instead...
    copy /y "chess-client\target\chess-client.jar" "%DIST_DIR%\" >nul
)

:: Step 4: Copy JRE
echo [4/4] Bundling JRE from: %BUNDLED_JRE%
if not exist "%BUNDLED_JRE%\bin\java.exe" (
    echo.
    echo WARNING: JRE not found at: %BUNDLED_JRE%
    echo.
    echo The .exe will still work if the target machine has JDK 17+ installed.
    echo To bundle a JRE:
    echo   1. Download Liberica JDK 17 Full from https://bell-sw.com/pages/downloads/
    echo   2. Set JRE_PATH environment variable or edit this script.
    echo.
    echo Skipping JRE bundling. Distribution will require JRE on target machine.
) else (
    echo       Copying JRE...
    xcopy "%BUNDLED_JRE%" "%DIST_DIR%\jre\" /e /i /q >nul
    echo       JRE bundled successfully.
)

:: Create README
(
echo ChessKSiS - Online Chess
echo ========================
echo.
echo How to run:
echo   Double-click ChessKSiS.exe
echo.
echo To connect to a server:
echo   1. Enter the server's IP address and port on the login screen
echo   2. Register a new account or log in
echo   3. Create or join a game room
echo.
echo Requirements:
echo   - If jre/ folder is present: no additional software needed
echo   - If jre/ folder is missing: JDK 17+ must be installed on this machine
echo.
) > "%DIST_DIR%\README.txt"

echo.
echo ============================================================
echo  Distribution created: %DIST_DIR%\
echo.
echo  Contents:
dir /b "%DIST_DIR%\"
echo.
echo  To distribute: ZIP the entire ChessKSiS folder and send it.
echo ============================================================
pause
