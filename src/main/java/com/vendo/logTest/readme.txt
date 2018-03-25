- log4j properties file must be in classpath
- default files is named log4j.properties

java -cp .;LogTest.jar LogTest.LogTest

java -cp .;LogTest.jar -Dlog4j.configuration=log4j.stderr.properties -DLEVEL=DEBUG LogTest.LogTest
java -cp .;LogTest.jar -Dlog4j.configuration=log4j.stderr.properties -DLEVEL=INFO  LogTest.LogTest
java -cp .;LogTest.jar -Dlog4j.configuration=log4j.stderr.properties -DLEVEL=WARN  LogTest.LogTest

java -cp .;LogTest.jar -Dlog4j.configuration=log4j.logfile.properties -DLEVEL=DEBUG -DLOGFILE=log.log LogTest.LogTest
java -cp .;LogTest.jar -Dlog4j.configuration=log4j.logfile.properties -DLEVEL=INFO  -DLOGFILE=log.log LogTest.LogTest
java -cp .;LogTest.jar -Dlog4j.configuration=log4j.logfile.properties -DLEVEL=WARN  -DLOGFILE=log.log LogTest.LogTest
