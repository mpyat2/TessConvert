SET JAR="C:\Program Files\Java\jdk-17.0.5\bin\jar.exe"

cd %~dp0bin
del ..\dist\TessConvert.jar
%JAR% vcfm ..\dist\TessConvert.jar ..\MANIFEST.MF *
cd %~dp0
