#!/bin/bash

javac -Xlint:unchecked -cp './jars/plume.jar:./jars/jline.jar' lib/edu/washington/cs/cse490h/lib/*.java proj/*.java
cd lib
jar cf ../jars/lib.jar edu/washington/cs/cse490h/lib/*.class

exit
