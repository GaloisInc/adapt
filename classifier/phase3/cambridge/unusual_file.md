
unusual file accesses
=====================

Files like `/etc/ld.so.preload`, `localtime`, `libc.so.6`, and `resolv.conf`
may be "usual" for a given program, being accessed every single time it runs.
In contrast, a reference to`readme.txt` may be "unusual",
triggered by argv or some aspect of the environment.
We report each unusual_file_access event.


summarization
-------------

If we have already seen an execvp of `/bin/date`, then it does little
good to report references to `/etc/ld.so.preload`, `localtime`, and so on,
since only SEGV or a kill signal would prevent the usual reads from happening.
They add no extra information and may safely be ignored by downstream components.
We aim to reduce work performed downstream.


transient processes
-------------------

The primary target is events happening near program startup,
especially events during the run of a program that exits
within one minute of starting.


memory budget
-------------

Since Ac is a streaming component, its models are prohibited from consuming
unbounded resource, even as size of TA1 event stream increases without bound.
We arbitrarily set limits on the size of our input sets and history.
Computation is proportional to total number of file accesses processed.


algorithm
---------

The approach is very simple.
We are given program name `prog` and
a set of filenames it opened, or rather the first `max_file` filenames.
There are two cases.

If we lack historic counts for `prog` we simply copy `files` into the
history and report empty set, no "unusual" files were found.

    # Creating history[prog] may evict another program from the LRU.
    if prog not in history:
        history[prog].instances.append(files)
        history[prog].counts = { file, 1 for file in files }
        return set()

Otherwise find unusual files and add to history:

    n = len(history[prog].instances)
    # A file is "usual" if it appears in every single historic instance.
    usual = set([file if count == n for file, count in history[prog].counts])
    unusual = files - usual
    if n == max_instances:
        # Oldest instance falls off the end.
        for file in history[prog].instances[0]:
            history[prog].counts[file] -= 1
        history[prog].instances = history[prog].instances[1:]
    history[prog].instances.append(files)
    for file in files:
        history[prog].counts[file] += 1
    return unusual
