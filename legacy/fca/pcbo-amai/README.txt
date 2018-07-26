PCBO(7)                         FCA Algorithms                         PCBO(7)



NAME
       pcbo - computes formal concepts and maximal frequent itemsets

SYNOPSIS
       pcbo [OPTION]... [INPUT-FILE] [OUTPUT-FILE]

DESCRIPTION
       This  program  computes  intents  of  all formal concepts in an object-
       attribute data set (a formal context), i.e. the algorithm computes  all
       maximal submatrices of a boolean matrix which are full of 1's. The pro-
       gram implements PCbO, a parallel algorithm based on Kuznetsov's CbO.

       The INPUT-FILE is in the usual FIMI  format:  each  line  represents  a
       transaction  or  an object and it contains of a list of attributes/fea-
       tures/items. If the INPUT-FILE is omitted or if it equals to  `-',  the
       program  reads the input form the stdin.  The OUTPUT-FILE has a similar
       format, each line represents one intent (itemset), where numbers  indi-
       cate  attributes in the intent (itemset). If the OUTPUT-FILE is omitted
       or if it equals to `-', the program writes the input to the stdout.

   Optional arguments
       -index,
              sets the initial index of the first attribute. The default value
              is  0, meaning that attributes are numbered from 0 upwards. If a
              data set uses attributes numbered from 1,  you  should  use  the
              `-1' switch, and so on.

       -Pcpus,
              sets  the  number  of  threads  to cpus. The default value is 1,
              meaning that pcbo runs in the single-threaded version. In  order
              to  benefit  from the parallel computation, you have to set cpus
              to at least 2. The recommended values is the number of  hardware
              processors  (processor  cores)  in  your system or higher (typi-
              cally, two or three times the number of all CPU cores).

       -Ldepth,
              sets the initial stage  recursion  level  to  depth.  The  value
              influences the number of formal concepts which are computed dur-
              ing the initial sequential stage. Namely,  the  algorithm  first
              computes  all  concepts  which  are derivable in less than depth
              steps. The default value is 3. A reasonable choice of the  value
              is  3 or 4. In general, higher values may lead to a more uniform
              distribution of concepts over the separate threads. On the other
              hand, too large values of depth degrade the parallel computation
              into the serial one. Some experimentation to  achieve  the  best
              results is necessary.  Anyway, a good starting value seems to be
              3 or 4.

       -Smin-support,
              the minimal  support  considered  is  set  to  min-support.  The
              default  value is 0, meaning that the support is disregarded and
              all intents (itemsets) are written to the output. If min-support
              is set to a positive value, only itemsets having extents with at
              least min-support are written to the output.

       -Vlevel,
              sets the verbosity level to a specified value. Permitted  values
              are  numbers  from  0 up to 3. The default value is 1. Verbosity
              level 0 (no output) just computes the intents  and  produces  no
              output. Verbosity level 1 produces lists of intents with no aux-
              iliary output. Verbosity levels 2 and  higher  write  additional
              information to stderr.

EXAMPLES
       pcbo -1 mushroom.dat

       Computes all intents in the file named mushroom.dat where 1 denotes the
       first attribute in mushroom.dat. The output is written to the  standard
       output.

       pcbo -1 -P6 mushroom.dat

       Computes  all  intents  in  mushroom.dat with first attribute 1 using 6
       threads. The output is written to the standard output.

       pcbo -P8 -L4 foo.dat output-intents.dat

       Computes all intents in mushroom.dat with 8 threads using  the  initial
       stage recursion depth 4, and writing results to output-intents.dat.

       pcbo -P4 -L3 -V2 - output.dat

       Computes  all  intents  in  data from the standard input with 4 threads
       using the initial stage recursion depth 3, and verbosity level 2, writ-
       ing result to output.dat.

AUTHORS
       Written by Petr Krajca, Jan Outrata, and Vilem Vychodil.

REPORTING BUGS
       Report bugs to <fcalgs-bugs@lists.sourceforge.net>.

COPYRIGHT
       GNU  GPL  2  (http://www.gnu.org/licenses/gpl-2.0.html).   This is free
       software: you are free to change and redistribute it.  There is NO WAR-
       RANTY, to the extent permitted by law.

       Users  in  academia are kindly asked to cite the following resources if
       the software is used to pursue any research activities which may result
       in publications:

       Krajca  P.,  Outrata  J., Vychodil V.: Parallel Recursive Algorithm for
       FCA.  In: Proc. CLA 2008, CEUR WS, 433(2008), 71-82.

SEE ALSO
       Preliminary version of PCbO is described in  the  aforementioned  paper
       that can be downloaded from

       http://sunsite.informatik.rwth-aachen.de/Publications/CEUR-
       WS/Vol-433/paper6.pdf

       Further information can be found at http://fcalgs.sourceforge.net



http://fcalgs.sourceforge.net    February 2009                         PCBO(7)
