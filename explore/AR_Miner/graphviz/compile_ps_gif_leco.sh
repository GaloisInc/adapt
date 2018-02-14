#!/bin/bash

dot -Tps lattice.dot -o lattice.ps
convert lattice.ps lattice.gif
\rm lattice.ps
