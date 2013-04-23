#!/bin/bash

rm -rf storage
rm -f *.log
rm -f *.replay
./execute.pl -s -n ChitterNode -f 0 -c scripts/chitter_test1
