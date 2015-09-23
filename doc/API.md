
# APIs for ADAPT's Characterizer component

- Kafka
- Blackboard
- adjacent components

## Kafka

TA1 sends the Ingester an event stream via Apache Kafka,
and Adapt components can optionally send event notifications
to this pub-sub system.

### component startup

A component that plans to send events to a channel *must* send
a "started" notification whenever it restarts.
The channel name should mention the name of the component
which "owns" the channel and defines its semantics.
We don't currently have a use case for sending ACKs upstream,
so a channel's messages will be from one or more instances
of a single component.
We don't currently define "shutdown" or "heartbeat" messages.

Downstream components are encouraged to listen for startup events,
and to report them in the debug log.

### event messages

A notification is a hint that "now would be a really good time to poll,"
as there is fresh data waiting.
Receipt is optional - a subsequent poll should  eventually notice the new tuple(s).
The notification will typically mention a recent timestamp or node name,
to encourage querying for everything in a range up to that item,
or perhaps a range filtered by a minimum risk score threshold.

### rate limiting

Notifications are intended to reduce the latency that is due to a
recipient's polling cycle. Subscribers typically can't issue more than
a thousand DB queries per second, so it is appropriate to suppress
notifications that would be excessive.
Think of Kafka short messages as being for control flow ("wakeup!"),
with Titan tuples being used for bulk data flow.

### message format

Notifications passed among internal Adapt components are always in JSON format.


## Blackboard

Data flow among all Adapt components is via the persistent tuple store.
Currently our interim storage solution is a Titan frontend with a Cassandra backend.

<!-- https://github.com/GaloisInc/AdaptMisc/blob/master/WhitePapers/Schema/Schema.md -->
http://git.io/vnirq#Schema.md
describes what valid provenance tuples look like -
provenance graph nodes are expressed in a local dialect of W3CPROV.
The ingest component impedance matches from TA1 to our schema,
in the case of Stamatogiannakis's qemu approach, and SRI's SPADE,
and any others that offer us a code / data release.

### queries

Adapt's Characterizer / Classifier component is written in python.

Communication with the blackboard is via the native Rexter interface,
using the bulbs.titan python package.
Currently there is no Adapt-specific glue library
for future-proofing client queries
in the event that Titan is swapped out for another tuple store.

#### classifier queries

The Classifier consumes every single segment that the Segmenter produces.
It relies on node names being generated in lexically increasing order,
the moral equivalent of increasing timestamps.
The Classifier remembers the highest node it has read,
and simply asks for all nodes greater than that one.
Each output classification node stored in the graph
has its own unique name, and and edge to the relevant segment,
along with a risk score and a timestamp.

Classifier queries are boring, and therefore easy to optimize.

Output nodes with high risk score may be announced via Kafka,
subject to rate limiting.

#### DX queries

The DX component will poll Titan for Classification nodes
filtered by high risk score and by "recent".
It will subsequently issue queries for ancestors of the resulting nodes.


## adjacent components

An interface to the KB is not currently defined.
We don't know what is in the Knowledge Base, nor its format,
so we do not yet attempt to query it or use it as an input for any models.

Here is list of all upstream components we receive nodes from:
- Segmenter
- (possibly) Ingester

Here are all the downstream components that directly read Classification nodes:
- DX

Here are all the downstream components that may indirectly read Classification nodes:
- Recommender
