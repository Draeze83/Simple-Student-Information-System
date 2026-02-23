@echo off
cd /d "%~dp0"
javac Main\StudentInformationSystem.java Main\Managers\*.java Main\Models\*.java Main\Panels\*.java
if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)
java -cp . Main.StudentInformationSystem
