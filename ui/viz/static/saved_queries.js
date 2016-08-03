var saved_queries = [
    {
        // This query is just an example. It should be deleted once a few more have been created.
        "name" : "2 Hops Away (limit 10)",
        "is_relevant" : function(n) {return n.label === "Subject"},
        "floating_query" : ".bothE().bothV().bothE().bothV().limit(10)"
    },
    {
        "name" : "previous",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".outE('EDGE_OBJECT_PREV_VERSION out').inV().outE('EDGE_OBJECT_PREV_VERSION in').inV()"
    },
    {
        "name" : "owner",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out()"
    },
    {
        "name" : "next",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".in().hasLabel('EDGE_OBJECT_PREV_VERSION').in()"
    },
    {
        "name" : "affectedEvent",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_FILE_AFFECTS_EVENT').both().hasLabel('Subject').has('subjectType',4)"
    },
    {
        "name" : "readers",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',17).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    },
    {
        "name" : "writer",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    },
    {
        "name" : "parent",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".as('child').in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().both().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().has('pid',select('child').values('ppid'))"
    },
    {
        "name" : "urlsWritten",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',21).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"
    },
    {
        "name" : "urlsRead",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',17).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"       
    },
    {
        "name" : "child",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',10).outE().inV().hasLabel('Subject')"
    }
]