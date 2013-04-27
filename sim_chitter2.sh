#!/bin/bash

rm -rf storage
rm -f *.log
rm -f *.replay
./execute.py -s -n ChitterNode -f 0 -c scripts/chitter_test2
