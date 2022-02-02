SET JAVAC="C:\Program Files\Java\jdk-11.0.10\bin\javac.exe"

%JAVAC% -cp extlib/commons-math3-3.6.1.jar;extlib/nom-tam-fits-1.16.0.jar -d bin src/pmak/tools/tess_convert/*.java
