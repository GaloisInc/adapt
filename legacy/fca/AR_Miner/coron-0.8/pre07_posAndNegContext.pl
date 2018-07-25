#!/usr/bin/perl -w

# AUTHOR:      Laszlo Szathmary, Szathmary.L@gmail.com
# DESCRIPTION: Merge a context K1 and its complementary (inverted)
#              context K2 together.
# USAGE:       ./posAndNegContext.pl  input.rcf  [-of:output.rcf]

use strict;

my ($file, $out);
my $tmp = 'tmp/20080320_djsfhwyccv.rcf';
my $contextMerger = 'pre05_contextMerger.sh';
my $negator = 'pre06_negator.sh';
my $cmd;
my $toFile = 0;   # should the output be written in a file?

if (scalar(@ARGV) == 0) {
   print_info_and_exit();
}
elsif (scalar(@ARGV) == 1) {
   $file = $ARGV[0];
}
elsif (scalar(@ARGV) == 2) {
   $file = $ARGV[0];
   $out = getOutFilename($ARGV[1]);
   $toFile = 1;
}


if (-e $tmp) {
   unlink($tmp) || die("$0: error: cannot remove the temp file $tmp!\n");
}

# Step 1: create the inverted context
$cmd = "./$negator  $file  -of:$tmp";
#print $cmd."\n";
system($cmd);

# Step 2: merge the two contexts (input + inverted)
$cmd = "./$contextMerger  $file  $tmp";
if ($toFile == 1) {
   $cmd .= " -of:$out";
}
#print $cmd."\n";
system($cmd);

unlink($tmp) || warn("$0: warning: could not remove the temp file $tmp!\n");

#############################################################################

sub print_info_and_exit
{
print <<END;
Coron PosAndNegContext 0.1, (c) copyright Laszlo Szathmary, 2008--2010 (Szathmary.L\@gmail.com)

This tool allows merging a context K1 and its complementary (inverted)
context K2 together.

Usage: ./preXX_posAndNegContext.pl  input.rcf  [-of:output.rcf]

Parameter:
   input.rcf            the input database in RCF format
Option:
   -of:output.rcf       write the result in a file (instead of stdout)

END
exit 0;
}


sub getOutFilename
{
   my ($param) = (@_);
   my $fileName;

   if ($param =~ m#-of:(.*)#)
   {
      $fileName = $1;
   }
   else {
      die("$0: there is something wrong with the 2nd parameter!\n");
   }

   if (length($fileName) == 0) {
      die("$0: there is something wrong with the 2nd parameter!\n");
   }

   return $fileName;
}

