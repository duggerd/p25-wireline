REM
call inst2xsd xml/%1.xml -design rd -validate
REM
call mv schema0.xsd xsd/%1.xsd
REM
call scomp -javasource 1.5 -src src -out jar/xmlbeans-%1.jar  xsd/%1.xsd test.xsdconfig
