REM java -Doption="-Xms256m -Xmx512m" -DclassName=gov.nist.p25.issi.testlauncher.DietsGUI -Dargs="-startup startup/diets-standalone.properties" -jar diets.jar
REM
javaw -Xms256m -Xmx512m -classpath "./diets.jar;./lib/*" -Djava.library.path=./bin/native/jpcap gov.nist.p25.issi.testlauncher.DietsGUI -startup startup/diets-standalone.properties

