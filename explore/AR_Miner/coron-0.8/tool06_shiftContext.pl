#!/usr/bin/perl -w

# AUTHOR:      Laszlo Szathmary, Szathmary.L@gmail.com
# DESCRIPTION: This little script shifts values of
#              a context to left or right with N values.
#              The thing is that contexts with values 
#              smaller than 1 are not allowed in Coron.
#              The goal of this tool is to correct these
#              contexts.
# USAGE:       ./tool06_shiftContext.pl  file.basenum  N

use strict;

my ($file, $shift, $line, $i);
my (@values, @new_values);
my $problemWithValue = 0;      # boolean, showing at the end if the new context is OK or not

if (scalar(@ARGV) < 2) {
   print_info_and_exit();
}
else {
   $file  = $ARGV[0];
      checkFile($file);
   $shift = $ARGV[1];
      $shift = checkShift($shift);
}

open F1, "<$file" || die("$0: I/O error while trying to open $file!\n");

print_header_info($file, $shift);
while (<F1>)
{
   chomp($line = $_);
   if ($line =~ /^#/) {      # if it's a comment => jump it over
      next;
   }
   # else

   @values = split(/\s/, $line);
   if (scalar(@values) == 0) {      # if it's an empty line => jump it over
      next;
   }
   # else, if it's a useful line

   @new_values = map { $_ + $shift } @values;

   print "@new_values\n";

   # post-checking
   foreach $i (@new_values) {
      if ($i < 1) {
         $problemWithValue = 1;
      }
   }
}
close F1;

if ($problemWithValue == 1) {
   print STDERR "\n";
   print STDERR "# Warning: the new context has values < 1, which will not be accepted by Coron.\n";
}


#############################################################################

sub print_info_and_exit
{
print <<END;
Coron ShiftContext 0.1, (c) copyright Laszlo Szathmary, 2008--2010 (Szathmary.L\@gmail.com)

This tool shifts values in a context to left or right with N values.

Usage: ./toolXX_shiftContext.pl  file.basenum  N

Parameters:
   file.basenum      input context in .basenum format
   N                 a positive or negative integer

The output is printed to the standard output.

END
exit 0;
}

#############################################################################

sub checkFile
{
   my ($file) = (@_);
   if ($file !~ /\.basenum$/) {
      die "$0: the input file should be a .basenum file!\n";
   }
}

#############################################################################

sub checkShift
{
   my ($shift) = (@_);

   if ($shift !~ /^(\+|-)?\d+$/) {
      die "$0: the shift value should be a positive or negative integer!\n";
   }
   # else
   return ($shift + 1 - 1);      # if it was '+1', then it converts it back to '1'
}

#############################################################################

sub print_header_info
{
   my ($file, $shift) = (@_);
   print "# Original file: $file\n";
   print "# Shift:         $shift\n";
   print "\n";
}
