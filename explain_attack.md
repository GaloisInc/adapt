## Explain Attack
> "Given the ID of an event or file from a trace as a starting point,
> discover, display, and explain the attack-relevant portions of the
> provenance graph of that starting point. Then, discover display and
> explain other downstream system objects or events affected by
> elements of that graph."

A Baseline Approach: 

- We will assume that the "ad" and "ac" have already labeled many types of segments in the provenenence graph. For example, the "ad" will be defining a variety of "views" and each view instance will be a segment (of a particular type) that is labeled by an anomaly score. 

- The result of running the "ad" and "ac" will be to indicate that certain segments are suspicious. This would be based on having a high anomaly score, or an activity label that corresponds to an attack activity. 

- Given the attack seed we can then do a radius-bounded graph traversal from that seed to identify paths in the provenence graph to any segments that are labeled as suspious. 

- The seed, those paths, and the connected suspicious segments could then serve as an initial guess at the attack graph. 

- This could serve as a starting point from which to then apply more sophisticated reasoning mechanisms, e.g. via the "dx" as time allows. 

More Sophisticated Approach 

- The given ID or file will pinpoint a vertex or a tiny subgraph in the provenance graph. This will narrow down the number of segments need to be considered. We will define several segmentation criteria based on pid, time interval or radius etc. The "se" will then extract a list of segments that are related to or contain the given ID or file.

- The "ad" module will be used to prune down the number of segments further. The "ad" module will do that by choosing segments with high anomaly score.

- At this point, we have a managable number of segments that hopefully contain the entire trace of the attack. The "dx" module will then operate inside each of these segments or inter segment level to extract the skeleton of the attack.

- The "px", "ac" will operate inside each segment and help "dx" to connect and grow the attack skeleton using the APT grammer.

- Once we have some candidate skeleton we are done with discovery phase

- Displaying the attack skeletons using high level activity graph might be simple ( we already have the anotated activity from "ac")

- Not sure how to explain, maybe, while "dx" is growing the skeleton we will already have some form of explanation
 



