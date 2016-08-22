
unusual file accesses
=====================

Files like `/etc/ld.so.preload`, `localtime`, `libc.so.6`, and `resolv.conf`
may be "usual" for a given program, being accessed every single time it runs.
In contrast, a reference to `readme.txt` may be "unusual",
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
a set of filenames it opened, or rather the first `max_files` filenames.
There are two cases.

If we lack historic counts for `prog` we simply copy `files` into the
history and report empty set, no "unusual" files were found.

        # Creating prog entry in history may evict another program from the LRU.
        instances, counts = self.history(prog)
        n = len(instances)
        if n == 0:
            instances.append(files)
            for file in files:
                counts[file] = 1


Otherwise find unusual files and add to history:

        # A file is "usual" if it appears in every single historic instance.
        usual = set((file
                     for file, count in counts.items()
                     if count == n))
        unusual = files - usual

        if n == self.max_instance:
            # Oldest instance falls off the end.
            for file in instances[0]:
                counts[file] -= 1
            instances = instances[1:]
        instances.append(files)
        for file in files:
            counts[file] += 1

Then map each member of `unusual` to `ident` of corresponding base node,
and report it as an unusual_file_access activity.
