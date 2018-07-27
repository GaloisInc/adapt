#!/bin/bash

dot -Tps eq_classes.dot -o eq_classes.ps
convert eq_classes.ps eq_classes.eps
\rm eq_classes.ps
