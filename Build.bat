SET JAVAC="C:\Program Files\Java\jdk1.8.0_271\bin\javac.exe"

%JAVAC% -cp extlib/commons-math-2.2.jar;extlib/tamfits.jar -d bin src/pmak/tools/tess_convert/*.java
