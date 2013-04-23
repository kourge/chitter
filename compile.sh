#!/bin/bash

javac -cp ./jars/plume.jar lib/edu/washington/cs/cse490h/lib/*.java jline-1.0.jar proj/*.java
cd lib
jar cvf ../jars/lib.jar edu/washington/cs/cse490h/lib/*.class

exit
