#!/usr/bin/python

import sys, subprocess

classpath = ":".join([
    "jars/plume.jar",
    "jars/lib.jar"
])
classname = "edu.washington.cs.cse490h.lib.Router"

subprocess.call(["java", "-cp", classpath, classname] + sys.argv[1:])
