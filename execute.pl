#!/usr/bin/perl

# Simple script to start a Node Manager that uses a compiled lib.jar

main("edu.washington.cs.cse490h.lib.MessageLayer");

sub main {
    my $classname = shift;
    my $classpath = join ":", (
        "proj/",
        "jars/plume.jar",
        "jars/lib.jar",
        "jars/jline.jar",
        "jars/jython.jar",
        "jars/plyjy.jar"
    );
    my $args = join " ", @ARGV;

    exec("java -cp $classpath $classname $args");
}
