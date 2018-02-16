#!/usr/bin/perl -w

# AUTHOR:      Laszlo Szathmary, Szathmary.L@gmail.com
# DESCRIPTION: This script converts a .basenum file
#              to IBM's .ascii format which is often used
#              by other data mining algorithms.
#              IBM's ascii format:
#              <cid> <tid> <numitem> <item list>
#              CID TID #ITEMS LIST_OF_ITEMS
#         e.g. 1   1   4      0 1 4 6
#              2   2   3      4 7 9
#              Note that the basenum format only 
#              contains the list of items.


use strict;

my $file;
my $isBasenum = 0;

if (scalar(@ARGV) == 0) {
   print_info_and_exit();
}
else {
   $file = $ARGV[0];
}

if ($file =~ m#.*\.(.*)#)
{
   if ($1 eq "basenum") {
      $isBasenum = 1;
   }
}
if ($isBasenum == 0) {
   die("$0: error: the input file must have .basenum extension!\n");
}


##################################
## OK, we have a .basenum input ##
##################################

my $line;
my @elems;
my $cnt = 1;

open (F1, "<$file") || die("$0: error: cannot open $file!\n");
while (<F1>)
{
   chomp($line = $_);
   $line =~ s#^\s*##;  $line =~ s|\s*$||;    # removing trailing white spaces
   if ( ($line eq "") || ($line =~ "^#") ) { next; }

   # OK, usefule line:
   @elems = split(/\s/, $line);
   print "$cnt $cnt ".(scalar @elems)." @elems", "\n";

   ++$cnt;
}
close F1;

#############################################################################

sub print_info_and_exit
{
print <<END;
Coron Basenum2IBM 0.1, (c) copyright Laszlo Szathmary, 2008--2010 (Szathmary.L\@gmail.com)

This tool converts a .basenum dataset into IBM format.

Usage: ./preXX_basenum2ibm.pl  input.basenum

Parameter:
   input.basenum        the input database in .basenum format

The output is printed to the standard output.

END
exit 0;
}
