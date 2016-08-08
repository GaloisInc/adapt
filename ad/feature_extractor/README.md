
### Anomaly View
An anomaly view has two components:

##### 1.  Instance Specification:
	Defines a set of objects/nodes to be scored (e.g. netflow nodes)
##### 2. Feature Specification
	Defines the set of features for each instance (e.g. number of read/write events associated with a netflow node)

### Anomaly View in JSON
An anomaly view can be specified using the following JSON file format:

```json
{
    "view_name" : {
        "instance_set" : "gremlin query returning set of node ids",
        "feature_set" : {
            "feature_1" :
                "gremlin query to compute feature_1 for an instance",
            "feature_2" :
                "gremlin query to compute feature_2 for an instance",
            ...
            ...
            ...
        }
    },
    ...
    ...
}
```

An example of JSON file for netflow anomaly view is given below:

```json
{
    "netflow" : {
        "instance_set" : "g.V().hasLabel('Entity-NetFlow').id()",
        "feature_set" : {
            "writeBytes" :
                "g.V({id}).both().both().has('eventType',21).values('size').sum()",
            "readBytes" :
                "g.V({id}).both().both().has('eventType',17).values('size').sum()",
            "numWrites" :
                "g.V({id}).both().both().has('eventType',21).count()",
            "numReads" :
                "g.V({id}).both().both().has('eventType',17).count()"
        }
    }
}
```
