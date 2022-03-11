SET JAR="C:\Program Files\Java\jdk1.8.0_271\bin\jar.exe"

cd %~dp0bin
del ..\dist\TessConvert.jar
%JAR% vcfm ..\dist\TessConvert.jar ..\MANIFEST.MF *
cd %~dp0
