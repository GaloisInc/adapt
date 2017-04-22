var starting_queries = [
    {
        name : "find file by name & version",
        base_query : "g.V().has(label,'Entity-File').has('url','{_}').has('file-version',{_}).dedup()",
        default_values : ["file:///tmp/zqxf1",1]
    }, {
        name : "find file by partial name",
        base_query : "g.V().has(label,'Entity-File').has('url',regex('.*{_}.*')).has('file-version',{_}).dedup()",
        default_values : ["file:///tmp/zqxf1",1]
    }, {
        name : "find anomalous processes",
        base_query : "g.V().has(label,'Subject').has('anomalyScore').order().by(_.values('anomalyScore').max(),decr).limit({_})",
        default_values : [10]
    }, {
        name : "find anomalous netflows",
        base_query : "g.V().has('eventType',6).out('EDGE_EVENT_AFFECTS_NETFLOW out').out('EDGE_EVENT_AFFECTS_NETFLOW in').dedup().has('anomalyScore').order().by(_.values('anomalyScore').max(),decr).limit({_})",
        default_values : [10]
    }, {
        name : "find anomalous files",
        base_query : "g.V().has('eventType',21).out('EDGE_EVENT_AFFECTS_FILE out').out('EDGE_EVENT_AFFECTS_FILE in').dedup().has('anomalyScore').order().by(_.values('anomalyScore').max(),decr).limit({_})",
        default_values : [10]
    }, {
        name : "find process by pid",
        base_query : "g.V().has(label,'Subject').has('subjectType',0).has('pid',{_}).dedup().by('pid')",
        default_values : [1001]
    }, {
        name : "find up to n processes of an owner",
        base_query : "g.V().has(label,'localPrincipal').has('userID',{_}).both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out().has('label','Subject').has('subjectType',0).limit({_})",
        default_values : [1234,10]
    }, {
        name : "find NetFlow by dstAddress & port",
        base_query : "g.V().has(label,'Entity_NetFlow').has('dstAddress','{_}').has('port',{_}).dedup()",
        default_values : ["127.0.0.1",80]
    }, {
        name : "find APTs labeled by DX",
        base_query : "g.V().has(label,'APT')",
        default_values : []
    }
]

var node_appearance = [
    {   // Icon codes:  http://ionicons.com/cheatsheet.html
        // NOTE: the insertion of 'u' to make code prefixes of '\uf...' as below; because javascript.
        name : "Cluster",
        is_relevant : function(n) { return node_data_set.get(n.id) && network.isCluster(n.id) },
        icon_unicode : "\uf413",
       // color : "gray",  // setting color here will always override query-specific colors.
        size: 54
        // make_node_label : SPECIAL CASE!!! Don't put anything here right now.
    }, {
        name : "AnomFile",
        is_relevant : function(n) { return n.label === "FileObject" && n['properties'].hasOwnProperty('anomalyScore') },
        icon_unicode : "\uf41b",
        size: 40,
        color: "red",
        make_node_label : function(node) {
            var anomscore = (node['properties'].hasOwnProperty('anomalyScore') ? ": " + node['properties']['anomalyScore'][0]['value'] : "")
            var anomtype = (node['properties'].hasOwnProperty('anomalyType') ? "\n" + node['properties']['anomalyType'][0]['value'] : "")
            var anom = anomtype + anomscore
            var url = (node['properties'].hasOwnProperty('url') ? node['properties']['url'][0]['value'] : "None")
            var file_version = (node['properties'].hasOwnProperty('file-version') ? node['properties']['file-version'][0]['value'] : "None")
            return url + " ; " + file_version + " " + anom
        }
    }, {
        name : "File",
        is_relevant : function(n) { return n.label === "FileObject" },
        icon_unicode : "\uf41b",
        size: 40,
        make_node_label : function(node) {
            var file_type = (node['properties'].hasOwnProperty('fileObjectType') ? node['properties']['fileObjectType'][0]['value'] : "Unknown")
            var url = (node['properties'].hasOwnProperty('path') ? node['properties']['path'][0]['value'] : "None")
            return file_type+"\n"+url
        }
    }, {
        name : "Memory",
        is_relevant : function(n) { return n.label === "MemoryObject" },
        icon_unicode : "\uf376",
        size: 40,
        make_node_label : function(node) {
            var anomscore = (node['properties'].hasOwnProperty('anomalyScore') ? ": " + node['properties']['anomalyScore'][0]['value'] : "")
            var anomtype = (node['properties'].hasOwnProperty('anomalyType') ? "\n" + node['properties']['anomalyType'][0]['value'] : "")
            var anom = anomtype + anomscore
            var addr = (node['properties'].hasOwnProperty('address') ? node['properties']['address'][0]['value'] : "None")
            var size = (node['properties'].hasOwnProperty('properties') ? node['properties']['properties'][0]['value'] : "None")
            return size + "@" + addr + " " + anom
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
        name : "Principal",
        is_relevant : function(n) { return n.label === "Principal" },
        icon_unicode : "\uf3bb",
        size: 35,
        make_node_label : function(node) {
            var id = (node['properties'].hasOwnProperty('userId') ? node['properties']['userId'][0]['value'] : "None")
            return "UserID: " + id
        }
    }, {
        name : "Events",
        is_relevant : function(n) { return n.label === "Event" },
        icon_unicode : "\uf470",
        size: 35,
        make_node_label : function(node) {
            var id = (node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'][0]['value'] : "Some Event")
            return id
        }
    }, {
        name : "AnomEntity-NetFlow",
        is_relevant : function(n) { return n.label === "Entity-NetFlow" &&  n['properties'].hasOwnProperty('anomalyScore') },
        icon_unicode : "\uf262",
        color: "red",
        make_node_label : function(node) {
            var anomscore = (node['properties'].hasOwnProperty('anomalyScore') ? ": " + node['properties']['anomalyScore'][0]['value'] : "")
            var anomtype = (node['properties'].hasOwnProperty('anomalyType') ? "\n" + node['properties']['anomalyType'][0]['value'] : "")
            var anom = anomtype + anomscore
            var dest = (node['properties'].hasOwnProperty('dstAddress') ? node['properties']['dstAddress'][0]['value'] : "None")
            var port = (node['properties'].hasOwnProperty('dstPort') ? node['properties']['dstPort'][0]['value'] : "None")
            return dest + " : " + port + " " + anom
        }
    }, {
        name : "Entity-NetFlow",
        is_relevant : function(n) { return n.label === "NetFlowObject" },
        icon_unicode : "\uf262",
        make_node_label : function(node) {
            var dest = (node['properties'].hasOwnProperty('dstAddress') ? node['properties']['dstAddress'][0]['value'] : "None")
            var port = (node['properties'].hasOwnProperty('dstPort') ? node['properties']['dstPort'][0]['value'] : "None")
            return dest + " : " + port + " "
        }
    }, {
        name : "AnomSubject",
        is_relevant : function(n) { return n.label === "Subject" &&  n['properties'].hasOwnProperty('anomalyScore') },
        icon_unicode : "\uf375",
        color: "red",
        make_node_label : function(node) {
            var anomscore = (node['properties'].hasOwnProperty('anomalyScore') ? ": " + node['properties']['anomalyScore'][0]['value'] : "")
            var anomtype = (node['properties'].hasOwnProperty('anomalyType') ? "\n" + node['properties']['anomalyType'][0]['value'] : "")
            var anom = anomtype + anomscore
            var e = (node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'][0]['value'] : "None")
            var pid = (node['properties'].hasOwnProperty('pid') ? node['properties']['pid'][0]['value'] : "None")
            var t = (node['properties'].hasOwnProperty('subjectType') ? node['properties']['subjectType'][0]['value'] : "None")
            var seq = (node['properties'].hasOwnProperty('sequence') ? node['properties']['sequence'][0]['value'] : "no seq #")
            var timestamp = (node['properties'].hasOwnProperty('startedAtTime') ? new Date(node['properties']['startedAtTime'][0]['value']/1000).toGMTString() + " ." + node['properties']['startedAtTime'][0]['value']%1000 : "no timestamp")
            switch(t) {
                case "Process":
                    return t + " " + pid + " \n " + timestamp + " " + anom
                case "Thread":
                    return t + " of " + pid + " \n " + timestamp + " " + anom
                case "Event":
                    if (e === "Write" || e === "Read") {
                        var temp = node['properties'].hasOwnProperty('size') ? node['properties']['size'][0]['value'] : "size unknown"
                        return t + " " + e + " (" + temp + ") #" + seq + "\n" + timestamp
                    } else { 
                        return t + " " + e + " #" + seq + "\n" + timestamp
                    }
                default:
                    return t + " seq:" + seq + ", @" + timestamp + " " + anom
            }
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
            var timestamp = (node['properties'].hasOwnProperty('startedAtTime') ? new Date(node['properties']['startedAtTime'][0]['value']/1000).toGMTString() + " ." + node['properties']['startedAtTime'][0]['value']%1000 : "no timestamp")
            switch(t) {
                case "Process":
                    return t + " " + pid + " \n " + timestamp
                case "Thread":
                    return t + " of " + pid + " \n " + timestamp
                case "Event":
                    if (e === "Write" || e === "Read") {
                        var temp = node['properties'].hasOwnProperty('size') ? node['properties']['size'][0]['value'] : "size unknown"
                        return t + " " + e + " (" + temp + ") #" + seq + "\n" + timestamp
                    } else { 
                        return t + " " + e + " #" + seq + "\n" + timestamp
                    }
                default:
                    return t + " seq:" + seq + ", @" + timestamp
            }
        }
    }, {
        name : "Activity",
        is_relevant : function(n) { return n.label === "Activity" },
        icon_unicode : "\uf29a",
        size: 30,
        make_node_label : function(node) {
            return addr = (node['properties'].hasOwnProperty('activity:type') ? node['properties']['activity:type'][0]['value'] : "None")
        }
    }, {
        name : "Phase",
        is_relevant : function(n) { return n.label === "Phase" },
        icon_unicode : "\uf228",
        size: 30,
        make_node_label : function(node) {
            return addr = (node['properties'].hasOwnProperty('phase:name') ? node['properties']['phase:name'][0]['value'] : "None")
        }
    }, {
        name : "APT",
        is_relevant : function(n) { return n.label === "APT" },
        icon_unicode : "\uf229",
        size: 30,
        make_node_label : function(node) {
            return "APT"
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

var predicates = [
    {
        name : "Annotated Activities",
        is_relevant : function(n) {return n.label === "Segment"},
        floating_query : ".out('segment:activity')",
    }, {
        name : "Segment Low-level Events",
        is_relevant : function(n) {return n.label === "Segment"},
        floating_query : ".out('segment:includes')",
    }, {
        name : "APT Phases",
        is_relevant : function(n) {return n.label === "APT"},
        floating_query : ".out('apt:includes')",
    }, {
        name : "APT Phase Segments",
        is_relevant : function(n) {return n.label === "Phase"},
        floating_query : ".out('phase:includes')",
    }, {
        name : "APT Phase Low-level Events",
        is_relevant : function(n) {return n.label === "Phase"},
        floating_query : ".out('phase:includes').out('segment:includes')",
    }, {
        name : "File Events",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".out().or(_.hasLabel('EDGE_EVENT_AFFECTS_FILE'),_.hasLabel('EDGE_FILE_AFFECTS_EVENT')).out()",
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
        name : "Agent's Segments",
        is_relevant : function(n) { return n.label === "Agent"},
        floating_query: ".in('segment:includes')",
        is_default : true
    }, {
        name : "File Lineage",
        is_relevant : function(n) {return n.label === "Entity-File"},
        floating_query : ".union(_.until(_.out().hasLabel('EDGE_OBJECT_PREV_VERSION').count().is(0)).repeat(_.out().hasLabel('EDGE_OBJECT_PREV_VERSION').out().as('a')).select('a').unfold(),_.in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',17).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().hasLabel('Entity-File').dedup(),_.in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().hasLabel('Subject').has('eventType',20).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().hasLabel('Entity-File').dedup(),_.in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',13).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().dedup())"
    }, {
        name : "Previous version",
        is_relevant : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 0},
        floating_query : ".outE('EDGE_OBJECT_PREV_VERSION out').inV().outE('EDGE_OBJECT_PREV_VERSION in').inV()"
    }, {
        name : "Process Owner",
        is_relevant : function(n) {return n.label === "Subject" /*&& n['properties']['subjectType'][0]['value'] == 0 */},
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
        name : "My commands (text)",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".as('parent').union(_.values('commandLine').as('cmd'), _.until(_.in().has(label,'EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',10).count().is(0)).repeat(_.in().has(label,'EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',10).out().has(label,'EDGE_EVENT_AFFECTS_SUBJECT').out().has(label,'Subject').has('subjectType',0)).values('commandLine').as('cmd')).select('parent').dedup().values('pid').as('parent_pid').select('parent_pid','cmd')"
    }, {
        name : "My mmaps",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',13).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in()"
    }, {
        name : "My protects",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',15).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in()"
    }, {
        name : "My parent",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".as('child').in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().both().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().has('pid',_.select('child').values('ppid'))"
    }, {
        name : "Files I wrote",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',21).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"
    }, {
        name : "Files I read",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',17).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"
    }, {
        name : "Files I created",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',7).in().hasLabel('EDGE_FILE_AFFECTS_EVENT').in().hasLabel('Entity-File')"
    }, {
        name : "Files I deleted",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().has('eventType',12).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File')"
    }, {
        name : "My children",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',10).outE().inV().hasLabel('Subject')"
    }, {
        name : "My ancestors",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".until(_.both().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').count().is(0)).repeat(_.has('subjectType',0).in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().hasLabel('Subject').has('subjectType',4).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').has('subjectType',0).as('b')).select('b').unfold()"
    }, {
        name : "Owner changed by",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',2).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        name : "URLs Executed",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in('EDGE_EVENT_ISGENERATEDBY_SUBJECT in').in('EDGE_EVENT_ISGENERATEDBY_SUBJECT out').has('eventType',9).in('EDGE_FILE_AFFECTS_EVENT in').in('EDGE_FILE_AFFECTS_EVENT out').dedup()"
    }, {
        name : "NetFlow Read",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in('EDGE_EVENT_ISGENERATEDBY_SUBJECT in').in('EDGE_EVENT_ISGENERATEDBY_SUBJECT out').has('eventType',17).in('EDGE_NETFLOW_AFFECTS_EVENT in').in('EDGE_NETFLOW_AFFECTS_EVENT out').dedup()"
    }, {
        name : "NetFlow Written",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        floating_query : ".in('EDGE_EVENT_ISGENERATEDBY_SUBJECT in').in('EDGE_EVENT_ISGENERATEDBY_SUBJECT out').has('eventType',21).out('EDGE_EVENT_AFFECTS_NETFLOW out').out('EDGE_EVENT_AFFECTS_NETFLOW in').dedup()"
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
    }, {
        name : "Reader",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".out('EDGE_NETFLOW_AFFECTS_EVENT out').out('EDGE_NETFLOW_AFFECTS_EVENT in').has('eventType',17).out('EDGE_EVENT_ISGENERATEDBY_SUBJECT out').out('EDGE_EVENT_ISGENERATEDBY_SUBJECT in').dedup().by('pid')"
    }, {
        name : "Writer",
        is_relevant : function(n) {return n.label === "Entity-NetFlow"},
        floating_query : ".in('EDGE_EVENT_AFFECTS_NETFLOW in').in('EDGE_EVENT_AFFECTS_NETFLOW out').has('eventType',21).out('EDGE_EVENT_ISGENERATEDBY_SUBJECT out').out('EDGE_EVENT_ISGENERATEDBY_SUBJECT in').dedup().by('pid')"
    }
]
