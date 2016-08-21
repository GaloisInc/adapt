var saved_queries = [
    {
        // This query is just an example. It should be deleted once a few more have been created.
        "name" : "2 Hops Away (limit 10)",
        "is_relevant" : function(n) {return n.label === "Subject"},
        "floating_query" : ".bothE().bothV().bothE().bothV().limit(10)"
    }, {
        "name" : "Previous",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".outE('EDGE_OBJECT_PREV_VERSION out').inV().outE('EDGE_OBJECT_PREV_VERSION in').inV()"
    }, {
        "name" : "Owner",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out()",
        "is_default" : true
    }, {
        "name" : "Next",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".in().hasLabel('EDGE_OBJECT_PREV_VERSION').in()"
    }, {
        "name" : "Affected Event",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_FILE_AFFECTS_EVENT').both().hasLabel('Subject').has('subjectType',4)"
    }, {
        "name" : "Readers",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',17).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        "name" : "Writer",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        "name" : "Parent",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".as('child').in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().both().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().has('pid',select('child').values('ppid'))"
    }, {
        "name" : "URLs Written",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',21).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"
    }, {
        "name" : "URLs Read",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',17).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"       
    }, {
        "name" : "Child",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',10).outE().inV().hasLabel('Subject')"
    }
]

var saved_icons = [
    {   // Icon codes:  http://ionicons.com/cheatsheet.html   
        // NOTE: the insertion of 'u' to make code prefixes of '\uf...' below, because javascript.
        name : "Cluster",
        is_relevant : function(n) { return node_data_set.get(n.id) && network.isCluster(n.id) },
        icon_unicode : "\uf413",
        size: 54
    }, {
        name : "File",   // This will override anything below here!!!!
        is_relevant : function(n) { return n.label === "Entity-File" },
        icon_unicode : "\uf381"
    }, {
        name : "Agent",   // This will override anything below here!!!!
        is_relevant : function(n) { return n.label === "Agent" },
        icon_unicode : "\uf25d",
        size: 50
    }, {
        name : "Default",   // This will override anything below here!!!!
        is_relevant : function(n) { return true },
        icon_unicode : "\uf3a6",
        size: 30
    }
]