var saved_queries = [
    {
        // This query is just an example. It should be deleted once a few more have been created.
        "name" : "2 Hops Away (limit 10)",
        "is_relevant" : function(n) {return n.label === "Subject"},
        "floating_query" : ".bothE().bothV().bothE().bothV().limit(10)"
    },
    {
        "name" : "Previous File Version",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".outE('EDGE_OBJECT_PREV_VERSION out').inV().outE('EDGE_OBJECT_PREV_VERSION in').inV()"
    },
    {
        "name" : "Who owns it?",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out()"
    }
]