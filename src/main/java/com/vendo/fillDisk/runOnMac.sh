WIP!

#!/bin/sh
#set -x

CP=/Users/maryrich/bin/jars/log4j-1.2.14.jar
CP=${CP}:/Users/maryrich/bin/jars/bcprov-jdk16-142.jar
CP=${CP}:/Users/maryrich/bin/jars/vendo.jar
CP=${CP}:/Users/maryrich/bin/jars
/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Commands/java -cp ${CP} DropBox.DropBox $@

