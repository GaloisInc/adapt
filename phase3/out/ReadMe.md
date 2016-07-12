
trace reporting
===============

This is a corpus of summary trace reports, listing items like distinct
entity values for many of the CDM13 traces that TA1's have thus far supplied.
The selected seaside traces are

- 5d_youtube_ie_output
- apt1
- cameragrab1
- gather
- imagegrab2
- imagegrab
- infoleak_small_units
- recordaudio1
- recorder
- screengrab1
- screengrab
- ta5attack1_units
- ta5attack2_units
- youtube_ie_update

One can rapidly `grep` across the corpus to ask "what's in there?",
and then choose to drill down on specifics and implement detectors
or detailed reports around specific features.
Here, for example, is a means of rapidly discriminating
Windows from Linux traces:

    $ grep -im1 windows *.txt | awk '{print $1,$4}' | column -t
    5d_youtube_ie_output.bin.txt:  C:\Users\Sarah\AppData\Local\Microsoft\Windows
    imagegrab2.bin.txt:            C:\Windows\Fonts\StaticCache.dat
    imagegrab.bin.txt:             C:\Windows\Globalization\Sorting\SortDefault.nls
    recorder.bin.txt:              C:\Windows\Globalization\Sorting\SortDefault.nls
    screengrab.bin.txt:            C:\Windows\Globalization\Sorting\SortDefault.nls
    youtube_ie_update.bin.txt:     C:\ProgramData\Microsoft\Search\Data\Applications\Windows

    $ grep -im1 /etc/ *.txt | awk '{print $1,$4}' | column -t
    apt1.bin.txt:                   /etc/group
    cameragrab1.bin.txt:            /etc/group
    infoleak_small_units.avro.txt:  file:///etc/default/nss
    recordaudio1.bin.txt:           /etc/group
    screengrab1.bin.txt:            /etc/ld.so.cache
    ta5attack1_units.avro.txt:      file:///etc/bash.bashrc
    ta5attack2_units.avro.txt:      file:///etc/bash.bashrc

The spec defines a FileObject url as
"the location of the file absolute path or remote url";
note that predictably different TA1's have used
the flexibility afforded by the disjunction.
Sadly, the "absolute" becomes "relative" in some traces,
as for `./run.sh` and `run.sh` in recordaudio1.
Interesting representation choices can be seen in this example:

    $ grep pipe *.txt | awk '{print $1,$4}' | column -t
    apt1.bin.txt:               pipe
    cameragrab1.bin.txt:        pipe
    cameragrab1.bin.txt:        pipe
    recordaudio1.bin.txt:       pipe
    screengrab1.bin.txt:        pipe
    ta5attack1_units.avro.txt:  file:///usr/bin/lesspipe
    ta5attack1_units.avro.txt:  file://pipe:[3-4]
    ta5attack2_units.avro.txt:  file:///usr/bin/lesspipe
    ta5attack2_units.avro.txt:  file://pipe:[3-4]


lessons learned
===============

Getting an automated and reliable sequence of component processing
steps to trigger, in background or foreground, proved tougher than anticipated.
What I wanted was: In done -> Se done -> Ac done.
It appeared the implemented code was posting "done" on the pe channel,
so I tried using `tools/await_completion.py pe` with Pe disabled,
but still never obtained "done" messages until I manually injected
them with `signal_completion.py`.

Discarding old forensic traces wastes much processing time.
It would be desirable to query for "trace segments"
that associate a forensic filename with a set of segment IDs.

Gremlin continues to offer apparently random time-outs
and serialization errors, which disappear with a re-run.

Kafka persists all messages to disk.
In the presence of client restarts,
it is very important for clients to `seek_to_end()`,
else they will read lots of stale "done" messages.
If we move to a fancier API, it would be useful
to add app-level timestamps to messages we produce,
so downstream clients will know how stale they are.
This would also let a discard policy assess how far
behind TA1 we are.

Some TA1's apparently always send `edge(v1, v2)` first,
followed by `v1` and/or `v2`. So gremlin logs N exceptions
for N edges, impacting throughput.
Unconditionally deferring all edge inserts by using a very short FIFO
deque of edge requests would reduce the exception rate.

Actual TA1's inject events that were not covered by `Language.md`,
so I added them, trying to make `gremlin-server.log` less noisy
so we could see what true issues remained.
At least one of them, PART_OF_PATTERN, should be deleted
with extreme prejudice once TA1 stops producing them
or ingestd can suppress such events.
