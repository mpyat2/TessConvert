SET JAVAC="C:\Program Files\Java\jdk-17.0.5\bin\javac.exe"

%JAVAC% -cp extlib/commons-math-2.2.jar;extlib/tamfits.jar -d bin src/pmak/tools/tess_convert/*.java
