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
classname = "edu.washington.cs.cse490h.lib.MessageLayer"

subprocess.call(["java", "-cp", classpath, classname] + sys.argv[1:])
