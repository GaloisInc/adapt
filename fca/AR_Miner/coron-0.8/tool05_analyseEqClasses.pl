#!/usr/bin/perl -w

# AUTHOR:      Laszlo Szathmary, Szathmary.L@gmail.com
# DESCRIPTION: This little script analyses equivalence
#              classes and prints some statistics about them.
#              The script can be used on the output of
#              the following algorithms:
#              - Zart
#              - Eclat-Z
#              - Touch
# USAGE:       ./05_analyseEqClasses.pl  file.txt

use strict;

my $file;

if (scalar(@ARGV) == 0) {
   print_info_and_exit();
}
else {
   $file = $ARGV[0];
}

open (F1, "<$file") || die("$0: error: cannot open $file!\n");

my ($closed, $gens, @genSet, $elem, $closedCnt, $genCnt, @sizeSet, $size, @tmp);
my $max = 0;   # length of the longest gen.
my $maxGensInEqCl = 0;   # max. number of min. gen.-s in equivalence classes
my @distrib;   # distribution of hypergraph sizes

while (<F1>)
{
   chomp;

   if ($_ =~ m#\{(.*?)\}.*;\s*\[(.*)\]#)
   {
      undef(@genSet);
      undef(@sizeSet);
      #
      $closed = $1;
      $gens = $2;

      while ($gens =~ m#\{(.*?)\}#g)
      {
         $elem = $1;
         push(@genSet, $elem);
         $size = scalar(@tmp = split(/,/,$elem));
         if ($size > $max) { $max = $size; }
         push(@sizeSet, $size);
      }

      if (scalar(@genSet) > $maxGensInEqCl) { $maxGensInEqCl = scalar(@genSet); }
      ++$distrib[scalar(@genSet)];

      #print "$closed -> "."@sizeSet"."\n";

      ++$closedCnt;
      $genCnt += scalar(@genSet);
   }
}
close F1;

print "# Input file: ".$file."\n";
print "# FCIs: ".$closedCnt."\n";
print "# Total number of minimal generators: ".$genCnt."\n";
print "# Average number of minimal generators in equivalence classes: ".sprintf("%.2f", ($genCnt/$closedCnt))."\n";
print "# Max. number of minimal generators in equivalence classes: ".$maxGensInEqCl."\n";
print "# Longest generator: ".$max."\n";
print "# Distribution of hypergraph sizes:"."\n";
print_distibution();

#############################################################################

sub print_distibution
{
   my $i;
   my ($distrib) = (@_);

   print "# hyp_size   occurence\n";
   foreach $i (1..$maxGensInEqCl)
   {
      if (!defined($distrib[$i])) { $distrib[$i] = 0; }
      print "# ".$i.(" "x10).$distrib[$i]."\n";
   }
}

#############################################################################

sub print_info_and_exit
{
print <<END;
Coron AnalyseEqClasses 0.1, (c) copyright Laszlo Szathmary, 2008--2010 (Szathmary.L\@gmail.com)

This tool analyses equivalence classes and prints some statistics about them.
Can be used on the output of the following algorithms: Zart, Eclat-Z, and Touch.

Usage: ./toolXX_analyseEqClasses.pl  file.txt

Parameter:
   file.txt       containing equivalence classes

The output is printed to the standard output.

END
exit 0;
}
