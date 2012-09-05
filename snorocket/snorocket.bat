@echo off

rem ------
rem Snorocket startup script
rem ------
rem Copyright (c) 2007-2008 CSIRO Australian e-Health Research Centre (http://aehrc.com)
rem All rights reserved. Use is subject to license terms and conditions.

rem ----- Save Environment Variables That May Change ------------------------

verify other 2>nul
setlocal enableextensions
if errorlevel 1 echo Unable to enable extensions
    
set _CP=%CP%
set _JAVA_HOME=%JAVA_HOME%
set _CLASSPATH=%CLASSPATH%
set _PATH=%PATH%


rem ----- Verify and Set Required Environment Variables ---------------------

if not "%JAVA_HOME%" == "" goto gotJavaHome
rem ----- Attempting to discover Java Installation location -----------------
rem prefer JDKs over JREs and prefer "later" versions as defined by loop order

for /D %%d in ( "C:\Program Files\Java\jdk*" ) do if exist "%%d\bin\java.exe" set JAVA_HOME='%%d'
if not "%JAVA_HOME%" == "" goto gotJavaHome
for /D %%d in ( "C:\Program Files\Java\jre*" ) do if exist "%%d\bin\java.exe" set JAVA_HOME='%%d'
if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You should set JAVA_HOME to point at your Java (JRE or JDK) installation
goto cleanup
:gotJavaHome

rem ----- Run SNOROCKET -------------------------------------------------------

path "%JAVA_HOME%\bin";%PATH%

java.exe -client -Xmx1024m -jar lib/snorocket-${project.version}.jar %*

goto :cleanup

rem ----- Restore Environment Variables ---------------------------------------

:cleanup
path %_PATH%
set CLASSPATH=%_CLASSPATH%
set _CLASSPATH=
set JAVA_HOME=%_JAVA_HOME%
set _JAVA_HOME=
set CP=%_CP%
set _CP=

:finish
