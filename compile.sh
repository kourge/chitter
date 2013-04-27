#!/bin/bash

javac -Xlint:unchecked -cp './jars/plume.jar:./jars/jline.jar:./jars/jython.jar:./jars/plyjy.jar:lib/edu/washington/cs/cse490h/lib/*.py' lib/edu/washington/cs/cse490h/lib/*.java proj/*.java
cd lib
jar cf ../jars/lib.jar edu/washington/cs/cse490h/lib/*.class

exit
