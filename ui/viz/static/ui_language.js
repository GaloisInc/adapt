var saved_queries = [
    {
        name : "File Events",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".out().or(hasLabel('EDGE_EVENT_AFFECTS_FILE'),hasLabel('EDGE_FILE_AFFECTS_EVENT')).out()",
        is_default : true
    }, {
        name : "Event By...",
        is_relevant : function(n) {return n.label === "Subject" && n['properties'].hasOwnProperty('subjectType') && n['properties']['subjectType'][0]['value'] === 4},
        floating_query : ".out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out()",
        is_default : true
    }, {
        name : "Agent's Processes",
        is_relevant : function(n) { return n.label === "Agent"},
        floating_query: ".in().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').in()",
        is_default : true
    }, {
        name : "File Lineage",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".union(until(out().hasLabel('EDGE_OBJECT_PREV_VERSION').count().is(eq(0))).repeat(out().hasLabel('EDGE_OBJECT_PREV_VERSION').out().as('a')).select('a').unfold(),__.in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',17).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().hasLabel('Entity-File').dedup(),__.in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().hasLabel('Subject').has('eventType',20).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().hasLabel('Entity-File').dedup())"
    }, {
        name : "Previous version",
        is_relevant : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 0},
        floating_query : ".outE('EDGE_OBJECT_PREV_VERSION out').inV().outE('EDGE_OBJECT_PREV_VERSION in').inV()"
    }, {
        name : "Process Owner",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out()",
        is_default : true
    }, {
        name : "Next version",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".in().hasLabel('EDGE_OBJECT_PREV_VERSION').in()"
    }, {
        name : "Readers",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',17).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Executor",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".out().hasLabel('EDGE_FILE_AFFECTS_EVENT').out().has('eventType',9).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Writer",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "ChModder",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',14).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out()"        
    }, {
        name : "Mmaps to",
        is_relevant : function(n) {return n.label === "Entity-File"},        
        floating_query : ".out().hasLabel('EDGE_FILE_AFFECTS_EVENT').out().has('eventType',13).both().hasLabel('EDGE_EVENT_AFFECTS_MEMORY').out().hasLabel('Entity-Memory')"
    }, {
        name : "Mmapper",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',13).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in()"
    }, {
        name : "Mprotector",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',15).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in()"
    },  {
        name : "Parent process",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".as('child').in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().both().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().has('pid',select('child').values('ppid'))"
    }, {
        name : "Files Written",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',21).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"
    }, {
        name : "Files Read",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',17).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"       
    }, {
        name : "Files Created",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',7).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().hasLabel('Entity-File')"
    }, {
        name : "Files Deleted",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',12).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File')"
    }, {
        name : "Children",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',10).outE().inV().hasLabel('Subject')"
    }, {
        name : "Process Lineage",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".until(both().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').count().is(0)).repeat(has('subjectType',0).in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().hasLabel('Subject').has('subjectType',4).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').has('subjectType',0).as('b')).select('b').unfold()"
    }, {
        name : "Owner changed by",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',2).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Receiver",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".out().hasLabel('EDGE_NETFLOW_AFFECTS_EVENT').out().hasLabel('Subject').has('eventType',19).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Sender",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_NETFLOW').in().has('eventType',34).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Closer",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_NETFLOW').in().has('eventType',5).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Binder",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_NETFLOW').in().has('eventType',1).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "Connected to",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_NETFLOW').in().has('eventType',6).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }
]

var saved_nodes = [
    {   // Icon codes:  http://ionicons.com/cheatsheet.html   
        // NOTE: the insertion of 'u' to make code prefixes of '\uf...' as below; because javascript.
        name : "Cluster",
        is_relevant : function(n) { return node_data_set.get(n.id) && network.isCluster(n.id) },
        icon_unicode : "\uf413",
        color : "red",  // setting color here will always override query-specific colors.
        size: 54
        // make_node_label : SPECIAL CASE!!! Don't put anything here right now.
    }, {
        name : "File",
        is_relevant : function(n) { return n.label === "Entity-File" },
        icon_unicode : "\uf41b",
        size: 40,
        make_node_label : function(node) {
            var url = (node['properties'].hasOwnProperty('url') ? node['properties']['url'][0]['value'] : "None")
            var file_version = (node['properties'].hasOwnProperty('file-version') ? node['properties']['file-version'][0]['value'] : "None")
            return url + " ; " + file_version
        }
    }, {
        name : "Memory",
        is_relevant : function(n) { return n.label === "Entity-Memory" },
        icon_unicode : "\uf376",
        size: 40,
        make_node_label : function(node) {
            var addr = (node['properties'].hasOwnProperty('address') ? node['properties']['address'][0]['value'] : "None")
            var size = (node['properties'].hasOwnProperty('properties') ? node['properties']['properties'][0]['value'] : "None")
            return size + "@" + addr
        }
    }, {
        name : "Agent",
        is_relevant : function(n) { return n.label === "Agent" },
        icon_unicode : "\uf25d",
        size: 50,
        make_node_label : function(node) {
            var at = (node['properties'].hasOwnProperty('agentType') ? node['properties']['agentType'][0]['value'] : "None")
            return at + " userID " + node['properties']['userID'][0]['value']
        }
    }, {
        name : "Entity-NetFlow",
        is_relevant : function(n) { return n.label === "Entity-NetFlow" },
        icon_unicode : "\uf262",
        make_node_label : function(node) {
            var dest = (node['properties'].hasOwnProperty('dstAddress') ? node['properties']['dstAddress'][0]['value'] : "None")
            var port = (node['properties'].hasOwnProperty('dstPort') ? node['properties']['dstPort'][0]['value'] : "None")
            return dest + " : " + port
        }
    }, {
        name : "Subject",
        is_relevant : function(n) { return n.label === "Subject" },
        icon_unicode : "\uf375",
        make_node_label : function(node) {
            var e = (node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'][0]['value'] : "None")
            var pid = (node['properties'].hasOwnProperty('pid') ? node['properties']['pid'][0]['value'] : "None")
            var t = (node['properties'].hasOwnProperty('subjectType') ? node['properties']['subjectType'][0]['value'] : "None")
            var seq = (node['properties'].hasOwnProperty('sequence') ? node['properties']['sequence'][0]['value'] : "no seq #")
            var timestamp = (node['properties'].hasOwnProperty('startedAtTime') ? node['properties']['startedAtTime'][0]['value'] : "no timestamp")
            switch(t) {
                case "Process":
                    return t + " " + pid + " " + timestamp
                case "Thread":
                    return t + " of " + pid + " " + timestamp
                case "Event":
                    if (e === "Write" || e === "Read") {
                        var temp = node['properties'].hasOwnProperty('size') ? node['properties']['size'][0]['value'] : "size unknown"
                        return t + " " + e + " (" + temp + ") #" + seq 
                    } else { return t + " " + e + " #" + seq}
                default:
                    return t + " seq:" + seq + ", @" + timestamp
            }
        }
    }, {
        name : "Default",   // This will override anything below here!!!!
        is_relevant : function(n) { return true },
        icon_unicode : "\uf3a6",
        size: 30,
        make_node_label : function(n) {
            return n['label'].replace(/^(EDGE_)/,"").replace(/^(EVENT_)/,"")
        }
     // color : do not set a color for default values, or it will always override query-time color choice.
    }
]

var starting_queries = [

    {
        name : "find file by name & version",
        base_query : "g.V().has(label,'Entity-File').has('url','{_}').has('file-version',{_})",
        default_values : ["myfile.txt",1]
    },
    {
        name : "find process by pid",
        base_query : "g.V().has(label,'Subject').has('subjectType',0).has('pid',{_})",
        default_values : [1001]
    },
    {
        name : "find up to n processes of an owner",
        base_query : "g.V().has(label,'localPrincipal').has('userID',{_}).both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out().has('label','Subject').has('subjectType',0).limit({_})",
        default_values : [1234,10]
    },
    {
        name : "find NetFlow by dstAddress & port",
        base_query : "g.V().has(label,'Entity_NetFlow').has('dstAddress','{_}').has('port',{_})",
        default_values : ["127.0.0.1",80]
    }
    
]