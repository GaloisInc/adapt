
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
Predictably, different TA1's have used
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

See also `egrep -m1 InstrumentationSource *.txt | awk '{print $1,$4}' | column -t`,
bearing in mind that infoleak additionally mentions `LINUX_BEEP_TRACE` as a source.


hotwash targets
===============

Features that TA5 might possibly point out as relevant to a recent
trace analysis:

- `file:///etc/shadow` (also `passwd`)
- `/dev/video0`
- `/dev/snd/controlC1` (also `pcmC1D0c1`) 
- `C:\Windows\inf\hdaudio.PNF`
- `file:///tmp/victim/secret1` (also `simple`)
- `C:\Users\5d-tc\tc\malware\out.wav`

The first four should have TA1 tags of `CONFIDENTIALITY_SENSITIVE` (or secret).
We can synthesize such tags.

In the case of hdaudio it is "accidental" that it is read as part of
microphone access; malware that manipulates I/O ports directly
would not go through the card's driver and would not go through
a TA1-logged system interface.

In the case of /dev/video0, note that we see the arg that goes
through the interface, but not the result of symlink resolution,
so `ln -s /dev/video0 readme.txt` would make a TA5 trace more obscure.
We would need help from TA1: a midnight listing of (at least) symlink files,
perhaps near-realtime `stat()` or `lstat()` output added to the event stream.


lessons learned
===============

Getting an automated and reliable sequence of component processing
steps to trigger, in background or foreground, proved tougher than anticipated.
What I wanted was: In done -> Se done -> Ac done.
It appeared the implemented code was posting "done" on the pe channel,
so I tried using `tools/await_completion.py pe` with Pe disabled,
but still never obtained "done" messages until I manually injected
them with `signal_completion.py`.
It may be related to this ingestd stderr report: "Kafka signaling failed: KafkaNoOffset"

Discarding old forensic traces wastes much processing time.
It would be desirable to query for "trace segments"
that associate a forensic filename with a set of segment IDs.

Gremlin continues to offer apparently random time-outs
and serialization errors, which disappear with a re-run.

Kafka persists all messages to disk.
In the presence of client restarts,
it is very important for clients to initially `seek_to_end()`,
else they will read lots of stale "done" messages.
If we move to a fancier API, it would be useful
to add app-level timestamps to messages we produce,
so downstream clients will know how stale they are.
This would also let a discard policy assess how far
behind TA1 we are.

TA1's are clearly torn between two trace suffixes.
It would be useful for BBN to offer guidance that all forensic
trace files shall end with `.avro` (or conversely, shall end with `.bin`).

It seems doubtful that we will be able to make good
use of machine addresses in the event stream.
They should probably be pruned early, perhaps even by TA1.
On the other hand, there is rich, not yet exploited information
in the "tag" data we see.

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
