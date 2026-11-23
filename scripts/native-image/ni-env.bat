@echo off
REM ============================================================
REM  native-image build environment (Windows x64)
REM
REM  usage:  call ni-env.bat <NIK_HOME>
REM  e.g.    call ni-env.bat D:\nik\bellsoft-liberica-vm-full-openjdk21-23.1.8
REM
REM  Do NOT use vcvars64.bat: it shells out to vswhere/reg, which get blocked
REM  in restricted accounts, CI and sandboxes, with very misleading errors.
REM  Set INCLUDE/LIB/PATH explicitly instead.
REM
REM  This file must stay ASCII-only and CRLF. cmd.exe parses batch files with
REM  the OEM code page; UTF-8 Chinese text here would be garbled and can break
REM  the parser. LF-only line endings also break if/for blocks.
REM ============================================================

set "NIK_HOME=%~1"
REM normalise forward slashes: cmd's "if exist" does not accept D:/a/b
set "NIK_HOME=%NIK_HOME:/=\%"
if "%NIK_HOME%"=="" (
    echo usage: call ni-env.bat ^<NIK_HOME^>
    exit /b 1
)

REM ---- 1) must be the "full" build: check AWT native libs ----
set "MISSING="
for %%F in (awt.dll jawt.dll fontmanager.dll freetype.dll lcms.dll jsound.dll javajpeg.dll) do (
    if not exist "%NIK_HOME%\bin\%%F" set "MISSING=%MISSING% %%F"
)
if not "%MISSING%"=="" (
    echo [X] NIK is not the full build, missing:%MISSING%
    echo     Use bellsoft-liberica-vm-full-openjdk21-* ; standard has no AWT.
    exit /b 2
)
if not exist "%NIK_HOME%\bin\native-image.cmd" (
    echo [X] not found: "%NIK_HOME%\bin\native-image.cmd"
    exit /b 2
)

REM ---- 2) locate MSVC (vswhere first, hardcoded fallback) ----
set "PF86=%ProgramFiles(x86)%"
set "MSVC="
if exist "%PF86%\Microsoft Visual Studio\Installer\vswhere.exe" (
    for /f "usebackq delims=" %%i in (`"%PF86%\Microsoft Visual Studio\Installer\vswhere.exe" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
        if exist "%%i\VC\Tools\MSVC" (
            for /f "delims=" %%v in ('dir /b /ad /o-n "%%i\VC\Tools\MSVC" 2^>nul') do (
                if not defined MSVC set "MSVC=%%i\VC\Tools\MSVC\%%v"
            )
        )
    )
)
if not defined MSVC set "MSVC=D:\BuildTools\VC\Tools\MSVC\14.44.35207"
if not exist "%MSVC%\bin\Hostx64\x64\link.exe" (
    echo [X] MSVC linker not found: "%MSVC%\bin\Hostx64\x64\link.exe"
    echo     Install Build Tools:
    echo       winget install Microsoft.VisualStudio.2022.BuildTools --override "--quiet --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"
    echo     Or edit the hardcoded MSVC line in this file.
    exit /b 2
)

REM ---- 3) locate Windows SDK (highest version directory) ----
set "SDKROOT=%PF86%\Windows Kits\10"
set "SDKVER="
for /f "delims=" %%v in ('dir /b /ad /o-n "%SDKROOT%\Include" 2^>nul') do (
    if not defined SDKVER set "SDKVER=%%v"
)
if not defined SDKVER (
    echo [X] Windows SDK not found: "%SDKROOT%\Include"
    exit /b 2
)

REM ---- 4) export environment ----
set "PATH=%MSVC%\bin\Hostx64\x64;%NIK_HOME%\bin;%PATH%"
set "JAVA_HOME=%NIK_HOME%"
set "INCLUDE=%MSVC%\include;%SDKROOT%\Include\%SDKVER%\ucrt;%SDKROOT%\Include\%SDKVER%\shared;%SDKROOT%\Include\%SDKVER%\um;%SDKROOT%\Include\%SDKVER%\winrt;%SDKROOT%\Include\%SDKVER%\cppwinrt"
set "LIB=%MSVC%\lib\x64;%SDKROOT%\Lib\%SDKVER%\ucrt\x64;%SDKROOT%\Lib\%SDKVER%\um\x64"

echo [OK] NIK       = %NIK_HOME%
echo [OK] MSVC      = %MSVC%
echo [OK] SDK       = %SDKVER%
echo [OK] JAVA_HOME = %JAVA_HOME%

REM ---- 5) smoke test: compile a trivial C program ----
set "T=%TEMP%\ni_env_probe"
if not exist "%T%" mkdir "%T%"
> "%T%\t.c" echo int main(void) { return 0; }
cl /nologo /O2 "%T%\t.c" /Fe:"%T%\t.exe" >nul 2>&1
if not exist "%T%\t.exe" (
    echo [X] MSVC smoke test failed: cannot compile a trivial C file.
    echo     Check INCLUDE and LIB above.
    exit /b 3
)
del /q "%T%\t.exe" >nul 2>&1
echo [OK] MSVC compile smoke test passed
exit /b 0
