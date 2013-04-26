#!/bin/bash

rm -rf storage
rm -f *.log
rm -f *.replay
./execute.py -s -n RIOTester -f 0 -c scripts/RIOTest

