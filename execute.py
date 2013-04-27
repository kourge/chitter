#!/usr/bin/python

import sys, subprocess

classpath = "proj/:jars/plume.jar:jars/lib.jar:jars/jline.jar:jars/jython.jar:jars/plyjy.jar"

subprocess.call(["java", "-cp", classpath, "edu.washington.cs.cse490h.lib.MessageLayer"] + sys.argv[1:])
