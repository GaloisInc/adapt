#! /usr/bin/env python3

# Copyright 2016, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.

#
# usage:
#     ./pid_demo.py
#

'''
Fork multiple times, and record the resulting (unique?) PIDs.

This easily bumps the pid counter 200 times/second,
so it wraps around within three minutes, from 32767 -> 300.
Note that it is common for a long-lived process to have
several long-lived children with sequential pids,
so sometimes fork will yield a pid that is e.g. prev_pid + 6.

System APIs often pass around a unique process identifier
to report on and manipulate a child that is running Right Now.
TC historic reports of what happened on a TA1 Monitored Host over
the course of a day cannot possibly refer only to the host and pid,
it is necessary to include an epoch or timestamp in the pid tuple.
'''
from subprocess import Popen, PIPE
import logging
import os

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


def spawn_children(fout):
    for i in range(int(1e5)):
        fork_child(fout)


def spawn_child(fout):
    proc = Popen(['/bin/true'], stdout=PIPE, stderr=PIPE)
    stdout, stderr = proc.communicate()
    # log.info('%6d  %s' % (proc.pid, stdout.decode('utf8').rstrip()))
    fout.write('%d\n' % proc.pid)
    assert proc.returncode == 0


def fork_child(fout):
    pid = os.fork()  # This is perhaps slightly faster than Popen.
    if pid == 0:
        os._exit(0)
    else:
        os.wait()  # Reap the zombie.
        fout.write('%d\n' % pid)


def report(fin):
    prev = 0
    for line in fin:
        pid = int(line)
        if pid != prev + 1:
            print('Skipped from %6d to %6d.' % (prev, pid))
        prev = pid


if __name__ == '__main__':
    fspec = '/tmp/pids.txt'

    with open(fspec, 'w') as fout:
        spawn_children(fout)

    report(open(fspec))
