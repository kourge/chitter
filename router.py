#!/usr/bin/python

import sys, subprocess

classpath = "jars/plume.jar:jars/lib.jar"

subprocess.call(["java", "-cp", classpath, "edu.washington.cs.cse490h.lib.Router"] + sys.argv[1:])
