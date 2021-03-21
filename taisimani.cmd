rem  set path=c:\jdk8\bin;%path%;
mkdir build
cd build
del *.* /s /q
cd ..
del example.jar

cd static
jar cf ..\build\static.zip  *
 cd ..

cd src
javac -cp ..\mail.jar;. main\Main.java -d ..\build
cd ..\build
jar cfm ..\example.jar ..\src\manifest.mf *
cd ..
