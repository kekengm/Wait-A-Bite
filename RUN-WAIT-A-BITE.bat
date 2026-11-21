@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java gui\*.java
if errorlevel 1 (echo Compilation failed. & pause & exit /b 1)
java -cp out Main
if errorlevel 1 pause
