#!/bin/bash

rm -rf storage
rm -f *.log
rm -f *.replay
xterm -geometry 150x30+0+0 -e "./router.pl -p 1025; bash" &
sleep 3
# note that this is for 2 nodes (aka ChitterNode should have NUM_NODES = 2)
xterm -geometry 150x30+1000+0 -e "./execute.pl -e -n ChitterNode -router-port 1025 -a 0 -f 0 -k; bash" &
xterm -geometry 150x30+0+500 -e "./execute.pl -e -n ChitterNode -router-port 1025 -a 1 -f 0 -k; bash" &
