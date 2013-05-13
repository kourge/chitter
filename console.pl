#!/usr/bin/perl

main("Repl");

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
