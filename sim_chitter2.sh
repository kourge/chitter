#!/bin/bash

rm -rf storage
rm -f *.log
rm -f *.replay
./execute.pl -s -n chitter_node.py -f 0 -c scripts/chitter_test2
