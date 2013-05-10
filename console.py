#!/usr/bin/python

import sys, subprocess

classpath = ":".join([
    "proj/",
    "jars/plume.jar",
    "jars/lib.jar",
    "jars/jline.jar",
    "jars/jython.jar",
    "jars/plyjy.jar"
])
classname = "Repl"

subprocess.call(["java", "-cp", classpath, classname] + sys.argv[1:])
