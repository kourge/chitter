#!/bin/bash

javac -cp './jars/plume.jar:./jars/jline.jar' lib/edu/washington/cs/cse490h/lib/*.java proj/*.java
cd lib
jar cvf ../jars/lib.jar edu/washington/cs/cse490h/lib/*.class

exit
