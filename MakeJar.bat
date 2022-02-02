SET JAR="C:\Program Files\Java\jdk-11.0.10\bin\jar.exe"

cd %~dp0bin
del ..\dist\TessConvert.jar
%JAR% --verbose --create --file ..\dist\TessConvert.jar --manifest ..\MANIFEST.MF *
cd %~dp0
