## Explain Attack
> "Given the ID of an event or file from a trace as a starting point,
> discover, display, and explain the attack-relevant portions of the
> provenance graph of that starting point. Then, discover display and
> explain other downstream system objects or events affected by
> elements of that graph."

- The given ID or file will pinpoint a vertex or a tiny subgraph in the provenance graph. This will narrow down the number of segments need to consider. We will define several segmentation criteria based on pid, time interval or radius etc. The "se" will then extract a list of segments that are related to or contain the given ID or file.

- The "ad" module will be used to prune down the number of segments further. The "ad" module will do that by choosing segments with high anomaly score.

- At this point, we have managable number of segments with high degree of belief that they contain the entire trace of the attack. The "dx" module will then operate inside each of these segments or inter segment level to extract the skeleton of the attack.

- The "px", "ac" will operate inside each segment and help "dx" to connect and grow the attack skeleton using the APT grammer.

- Once we have some candidate skeleton we are done with discovery phase

- Displaying the attack skeletons using high level activity graph might be simple ( we already have the anotated activity from "ac")

- Not sure how to explain, maybe, while "dx" is growing the skeleton we will already have some form of explanation

