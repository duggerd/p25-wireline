REM java -Doption="-Xms256m -Xmx512m" -DclassName=gov.nist.p25.issi.analyzer.gui.TraceAnalyzerGUI -jar diets.jar
REM
javaw -Xms256m -Xmx512m -classpath "./diets.jar;./lib/*" gov.nist.p25.issi.analyzer.gui.TraceAnalyzerGUI
