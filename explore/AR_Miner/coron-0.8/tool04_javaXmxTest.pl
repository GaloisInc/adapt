#!/usr/bin/perl -w

# AUTHOR:      Laszlo Szathmary, Szathmary.L@gmail.com
# DESCRIPTION: This script tests the maximum heap
#              size available for your Java programs.
# USAGE:       Just launch it. You can play with the
#              $mb variable, but it's OK as it is.
# LAST UPDATE: 2008.04.15. (yyyy.mm.dd.)

use strict;

#############################################################################
# START: config part
my $mb = 10;         # in MB (megabytes)
# END: config part
#############################################################################

print_info();

my $status;          # status of the child process
my $offset = 1000;   # initial offset
my $latest_ok;       # latest correct heap size
my $test;            # test size

$status = system("java -Xmx${mb}m -version 2>/dev/null");
if ($status != 0)
{
   print <<END;
$0: there is a problem with the initial setting -Xmx${mb}m
Tip: change it (lower it?).
END
exit 1;
}
# else, if the initial setting was OK

$latest_ok = $mb;
$test = $mb;
# we'll jump out of the loop from inside
while (1)
{
   if ($offset == 1) { last; }
   # else

   $test += $offset;
   $status = system("java -Xmx${test}m -version 2>/dev/null");
   if ($status == 0) {
      # the value $test was OK
      $latest_ok = $test;
   }
   else {
      $offset /= 10;
      $test = $latest_ok;
   }
}

# remove log files (there might be lots of them)
unlink glob "*.log";

print <<END;

The latest correct setting was '-Xmx${latest_ok}m'.
Example: java -Xmx${latest_ok}m -version
END

#############################################################################

sub print_info
{
print <<END;
Coron JavaXmxTest 0.1, (c) copyright Laszlo Szathmary, 2008--2010 (Szathmary.L\@gmail.com)

This script tests the maximum heap size available for your Java programs.

END
}
