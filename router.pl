#!/usr/bin/perl

# Simple script to start a Router

main("edu.washington.cs.cse490h.lib.Router");

sub main {
    my $classname = shift;
    my $classpath = join ":", (
        "jars/plume.jar",
        "jars/lib.jar"
    );
    my $args = join " ", @ARGV;

    exec("java -cp $classpath $classname $args");
}
