
var starting_queries = [
    {
        name : "Get a few nodes",
        base_query : "g.V().limit({_})",
        default_values : [10]
    }, {
        name : "Get nodes by their UUID(s)",
        base_query : "g.V().has('uuid',within([{_}]))",
        default_values : ["271d6c79-ff9a-2b63-297c-bf948375a868"]
    }, {
        name : "Get a few nodes by label",
        base_query : "g.V().hasLabel('{_}').limit({_})",
        default_values : ["AdmPathNode",10]
    }, {
        name : "Get a Path Node by its path",
        base_query : "g.V().hasLabel('AdmPathNode').has('path','{_}')",
        default_values : ["/etc/passwd"]
    }, {
        name : "find process by pid",
        base_query : "g.V().hasLabel('Subject').has('cid',{_})",
        default_values : ["1001"]
    }, {
        name : "Find a file by its path",
        base_query : "g.V().hasLabel('AdmFileObject').has('path','{_}')",
        default_values : ["/etc/passwd"]
    }, {
        name : "find NetFlow by remote address & port",
        base_query : "g.V().hasLabel('NetFlowObject').has('remoteAddress','{_}').has('remotePort',{_})",
        default_values : ["127.0.0.1",80]
    }
]

var node_appearance = [
    {   // Icon codes:  https://ionicons.com/v2/cheatsheet.html
        // NOTE: the insertion of 'u' is required to make code prefixes of '\uf...' as below; because javascript.
        name : "Cluster",
        is_relevant : function(n) { return node_data_set.get(n.id) && network.isCluster(n.id) },
        icon_unicode : "\uf413",
       // color : "gray",  // setting color here will always override query-specific colors.
        size: 54
        // make_node_label : SPECIAL CASE!!! Don't put anything here for Clusters right now.
    }, {
        name : "File",
        is_relevant : function(n) { return n.db_label === "FileObject" },
        icon_unicode : "\uf41b",
        size: 40,
        make_node_label : function(node) {
           var fd = (node.hasOwnProperty('fileDescriptor') ? node['fileDescriptor'] : "")
            var fp = (node['properties'].hasOwnProperty('path') ? node['properties']['path'] : "none")
            return fp + " fd " + fd
        }
    }, {
        name : "MemoryObject",
        is_relevant : function(n) { return n.db_label === "MemoryObject" },
        icon_unicode : "\uf376",
        size: 40,
        make_node_label : function(node) {
            var addr = (node['properties'].hasOwnProperty('memoryAddress') ? node['properties']['memoryAddress'] : "None")
            var size = (node['properties'].hasOwnProperty('size') ? node['properties']['size'] : "None")
            return addr + "\nsize: " + size
        }
    }, {
        name : "Pipe",
        is_relevant : function(n) { return n.db_label === "UnnamedPipeObject" },
        icon_unicode : "\uf2c0",
        size: 40,
        make_node_label : function(node) {
            var source = (node['properties'].hasOwnProperty('sourceFileDescriptor') ? node['properties']['sourceFileDescriptor'] : "None")
            var sink = (node['properties'].hasOwnProperty('sinkFileDescriptor') ? node['properties']['sinkFileDescriptor'] : "None")
            return "src: " + source + "\nsink: " + sink
        }
    }, {
        name : "Principal",
        is_relevant : function(n) { return n.db_label === "Principal" },
        icon_unicode : "\uf419",
        size: 50,
        make_node_label : function(node) {
            var at = (node['properties'].hasOwnProperty('userId') ? node['properties']['userId'] : "None")
            return "userId: " + at
        }
    }, {
        name : "NetFlow",
        is_relevant : function(n) { return n.db_label === "NetFlowObject" },
        icon_unicode : "\uf262",
        make_node_label : function(node) {
            var localA = (node['properties'].hasOwnProperty('localAddress') ? node['properties']['localAddress'] : "None")
            var localP = (node['properties'].hasOwnProperty('localPort') ? node['properties']['localPort'] : "None")
            var remoteA = (node['properties'].hasOwnProperty('remoteAddress') ? node['properties']['remoteAddress'] : "None")
            var remoteP = (node['properties'].hasOwnProperty('remotePort') ? node['properties']['remotePort'] : "None")
            return "Local: " + localA + ":" + localP + "\nRemote: " + remoteA + ":" + remoteP
        }
    }, {
        name : "RegistryKey",
        is_relevant : function(n) { return n.db_label === "RegistryKeyObject" },
        icon_unicode : "\uf296",
        make_node_label : function(node) {
            var key = (node['properties'].hasOwnProperty('key') ? node['properties']['key'] : "None")
            var value = (node['properties'].hasOwnProperty('value') ? node['properties']['value'] : "None")
            return key + " : " + value
        }
    }, {
        name : "Event",
        is_relevant : function(n) { return n.db_label === "Event" },
        icon_unicode : "\uf375",
        make_node_label : function(node) {
            var sequence = (node['properties'].hasOwnProperty('sequence') ? node['properties']['sequence'] : "None")
            var type = (node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'] : "None")
            // var programPoint = (node['properties'].hasOwnProperty('programPoint') ? node['properties']['programPoint'] : "None")
            // var name = (node['properties'].hasOwnProperty('name') ? node['properties']['name'] : "None")
            return type + "\n# " + sequence
        }
    }, {
        name : "SrcSink",
        is_relevant : function(n) { return n.db_label === "SrcSinkObject" },
        icon_unicode : "\uf313",
        make_node_label : function(node) {
            // var uuid = (node['properties'].hasOwnProperty('uuid') ? node['properties']['uuid'] : "None")
            var type = (node['properties'].hasOwnProperty('srcSinkType') ? node['properties']['srcSinkType'] : "None")
            return type
        }
    }, {
        name : "PTN",
        is_relevant : function(n) { return n.db_label === "ProvenanceTagNode" },
        icon_unicode : "\uf48e",
        make_node_label : function(node) {
            // var systemCall = (node['properties'].hasOwnProperty('systemCall') ? node['properties']['systemCall'] : "None")
            var opcode = (node['properties'].hasOwnProperty('opcode') ? node['properties']['opcode'] : "None")
            var itag = (node['properties'].hasOwnProperty('itag') ? node['properties']['itag'] : "None")
            var ctag = (node['properties'].hasOwnProperty('ctag') ? node['properties']['ctag'] : "None")
            return "OpCode: " + opcode +  /* ", call:" + systemCall +  */ " \nItag: " + itag + "\nCtag: " + ctag
        }
    }, {
       name : "Subject",
        is_relevant : function(n) { return n.db_label === "Subject" },
        icon_unicode : "\uf375",
        make_node_label : function(node) {
            var cid = (node['properties'].hasOwnProperty('cid') ? node['properties']['cid'] : "None")
            var t = (node['properties'].hasOwnProperty('subjectType') ? node['properties']['subjectType'] : "None")
            var cmd = (node['properties'].hasOwnProperty('cmdLine') ? node['properties']['cmdLine'] : "no cmd line")
            // var timestamp = (node['properties'].hasOwnProperty('startTimestampNanos') ? new Date(node['properties']['startTimestampNanos']/1000).toGMTString() + " ." + node['properties']['startTimestampNanos']%1000 : "no timestamp")
            switch(t) {
                case "SUBJECT_PROCESS":
                    return "Process: " + cid
                case "SUBJECT_THREAD":
                    return "Thread: " + cid
                case "SUBJECT_UNIT":
                    return "Unit: " + cid
                default:
                    return t + ": " + cid
            }
        }
    }, 


// ADM:
    {
        name: "Host",
        is_relevant: function(n) { return n.db_label === "Host" || n.db_label === "AdmHost" },
        icon_unicode: "\uf390",
        size: 40,
        make_node_label: function(node) {
            var hostName = node['properties'].hasOwnProperty('hostName') ? node['properties']['hostName']+"\n" : "no_host_name"+"\n"
            var hostType = node['properties'].hasOwnProperty('hostType') ? node['properties']['hostType'] : "(unknown_type)"
            return hostName + "(" + hostType + ")"
        }
    }, {
        name : "ADM Path Node",
        is_relevant : function(n) { return n.db_label === "AdmPathNode" },
        icon_unicode : "\uf3fb",
        size: 40,
        make_node_label : function(node) {
            return node['properties'].hasOwnProperty('path') ? node['properties']['path'] : "???"
        }
    }, {
       name : "ADM Subject",
        is_relevant : function(n) { return n.db_label === "AdmSubject" },
        icon_unicode : "\uf375",
        make_node_label : function(node) {
            var cid = node['properties'].hasOwnProperty('cid') ? node['properties']['cid'] : "None"
            var timestamp = node['properties'].hasOwnProperty('startTimestampNanos') ? ("\n" + new Date(node['properties']['startTimestampNanos']/1000000).toGMTString().replace(" GMT","")) : ""
            return "Process: " + cid + timestamp
        }
    }, {
        name : "ADM File",
        is_relevant : function(n) { return n.db_label === "AdmFileObject" },
        icon_unicode : "\uf41b",
        size: 40,
        make_node_label : function(node) {
            return node['properties'].hasOwnProperty('fileObjectType') ? node['properties']['fileObjectType'] : "UNKNOWN TYPE"
        }
    }, {
        name : "ADM Event",
        is_relevant : function(n) { return n.db_label === "AdmEvent" },
        icon_unicode : "\uf29a",
        make_node_label : function(node) {
            var firstTime = node['properties'].hasOwnProperty('earliestTimestampNanos') ? new Date(node['properties']['earliestTimestampNanos']/1000000).toGMTString() : "???"
            // var lastTime = (node['properties'].hasOwnProperty('latestTimestampNanos') ? new Date(node['properties']['latestTimestampNanos']/1000000).toGMTString() : "???")
            var type = node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'] : "None"
            return type + "\n" + firstTime //+ " - " + lastTime
        }
    }, {
       name : "ADM Principal",
        is_relevant : function(n) { return n.db_label === "AdmPrincipal" },
        icon_unicode : "\uf419",
        size : 50,
        make_node_label : function(node) {
            var username = (node['properties'].hasOwnProperty('username') ? node['properties']['username'] : "")
            var userId = (node['properties'].hasOwnProperty('userId') ? " #" + node['properties']['userId'] : "")
            return username + userId
        }
    }, {
        name : "ADM NetFlowObject",
        is_relevant : function(n) { return n.db_label === "AdmNetFlowObject" },
        icon_unicode : "\uf262",
        make_node_label : function(node) {
            var localA = node['properties'].hasOwnProperty('localAddress') ? node['properties']['localAddress'] : "None"
            var localP = node['properties'].hasOwnProperty('localPort') ? node['properties']['localPort'] : "None"
            var remoteA = node['properties'].hasOwnProperty('remoteAddress') ? node['properties']['remoteAddress'] : "None"
            var remoteP = node['properties'].hasOwnProperty('remotePort') ? node['properties']['remotePort'] : "None"
            return "Local: " + localA + ":" + localP + "\nRemote: " + remoteA + ":" + remoteP
        }
    }, {
        name : "ADM SrcSink",
        is_relevant : function(n) { return n.db_label === "AdmSrcSinkObject" },
        icon_unicode : "\uf313",
        make_node_label : function(node) {
            return node['properties'].hasOwnProperty('srcSinkType') ? node['properties']['srcSinkType'] : "??"
        }
    }, {
        name : "ADM Provenance Tag Node",
        is_relevant : function(n) { return n.db_label === "AdmProvenanceTagNode" },
        icon_unicode : "\uf48e",
        make_node_label : function(node) {
            return node['properties'].hasOwnProperty('programPoint') ? node['properties']['programPoint'] : "??"
        }
    }, {
        name : "ADM Address",
        is_relevant : function(n) { return n.db_label === "AdmAddress" },
        icon_unicode : "\uf448",
        make_node_label : function(node) {
            return node['properties'].hasOwnProperty('address') ? node['properties']['address'] : "??"
        }
    }, {
        name : "ADM Port",
        is_relevant : function(n) { return n.db_label === "AdmPort" },
        icon_unicode : "\uf447",
        make_node_label : function(node) {
            return node['properties'].hasOwnProperty('port') ? node['properties']['port'] : "??"
        }
    },




// DEFAULT:
    {
        name : "Default",   // This default will override anything that comes below it!
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
// ProvenanceTagNode
       {
        name: "RefObject",
        is_relevant : function(n) {return n.db_label === "ProvenanceTagNode"},
        floating_query : ".out('flowObject')",
        is_default : true
    }, {
        name: "RefSubject",
        is_relevant : function(n) {return n.db_label === "ProvenanceTagNode"},
        floating_query : ".out('subject')"
    }, {
        name: "Ancestor tags",
        is_relevant : function(n) {return n.db_label === "ProvenanceTagNode"},
        floating_query : ".emit().repeat(_.out('prevTagId','tagId')).dedup().path().unrollPath().dedup()"
    }, {
        name: "Descendant tags",
        is_relevant : function(n) {return n.db_label === "ProvenanceTagNode"},
        floating_query : ".emit().repeat(_.in('prevTagId','tagId')).dedup().path().unrollPath().dedup()"
    },
// File Object
       {
        name : "Events",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "2-hop Causality",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_LSEEK','EVENT_LINK','EVENT_TRUNCATE','EVENT_RENAME','EVENT_UNLINK','EVENT_UPDATE','EVENT_MODIFY_FILE_ATTRIBUTES'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject')).dedup().union(_,_.in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_UPDATE'])).out('subject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "1-hop Causality",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_LSEEK','EVENT_LINK','EVENT_TRUNCATE','EVENT_RENAME','EVENT_UNLINK','EVENT_UPDATE','EVENT_MODIFY_FILE_ATTRIBUTES'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "Direct Causality",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_LSEEK','EVENT_LINK','EVENT_TRUNCATE','EVENT_RENAME','EVENT_UNLINK','EVENT_UPDATE','EVENT_MODIFY_FILE_ATTRIBUTES'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Executing",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_EXECUTE').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, {
        name : "Affected By",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_LSEEK','EVENT_LINK','EVENT_TRUNCATE','EVENT_UNLINK','EVENT_UPDATE','EVENT_MODIFY_FILE_ATTRIBUTES'])).out('subject')"
    }, {
        name : "Affects",
        is_relevant : function(n) {return n.db_label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_MMAP','EVENT_MPROTECT','EVENT_RECVFROM','EVENT_RECVMSG'])).out('subject')"
    },
// Memory Object
      {
       name : "Events",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "2-hop Causality",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_MPROTECT','EVENT_MMAP'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject')).dedup().union(_,_.in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_UPDATE','EVENT_SHM'])).out('subject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "1-hop Causality",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_MPROTECT','EVENT_MMAP'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "Subjects reading",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Executing",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_EXECUTE').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, {
        name : "Affected By",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_MMAP','EVENT_MPROTECT'])).out('subject')"
    }, {
        name : "Affects",
        is_relevant : function(n) {return n.db_label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_MMAP','EVENT_MPROTECT'])).out('subject')"
    }, 
// Subject
       {
        name : "Events Caused",
        is_relevant : function(n) {return n.db_label === "Subject" && n['properties'].hasOwnProperty('subjectType') && n['properties']['subjectType'] === 'SUBJECT_PROCESS'},
        floating_query : ".in('subject').hasLabel('Event')",
        is_default : true
    }, {
        name: "NetFlows Affected",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".in('subject').out('predicateObject').hasLabel('NetFlowObject')"
    }, {
        name: "Files Affected",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".in('subject').out('predicateObject').hasLabel('FileObject')"
    }, {
        name: "Memory Access",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".in('subject').out('predicateObject').hasLabel('MemoryObject')"
    }, {
        name : "Principal",
        is_relevant : function(n) {return n.db_label === "Subject" },
        floating_query : ".out('localPrincipal')",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".as('subjectOfInterest').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo'))).select('subjectOfInterest').union(_,_.out('localPrincipal')).select('subjectOfInterest').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).path().unrollPath().dedup().where(neq('subjectOfInterest'))"
    }, {
        name: "2-hop Causality",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".emit().repeat(_.as('foo').out('parentSubject').where(neq('foo'))).dedup().in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject').union(_,_.in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_UPDATE'])).out('subject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name: "1-hop Causality",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".emit().repeat(_.as('foo').out('parentSubject').where(neq('foo'))).dedup().in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject').path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".in('subject').hasLabel('ProvenanceTagNode')"
    }, {
        name : "Parent Process",
        is_relevant : function(n) {return n.db_label === "Subject"},
        floating_query : ".out('parentSubject')"
    }, {
        name : "Child Processes",
        is_relevant : function(n) {return n.db_label === "Subject" && n['properties']['subjectType'] == 'SUBJECT_PROCESS'},
        floating_query : ".in('parentSubject')"
    }, 
// Unnamed Pipe Object
       {
        name : "Processes connected",
        is_relevant : function(n) {return n.db_label === "UnnamedPipeObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).out('subject')"
    }, {
        name : "2-hop Causality",
        is_relevant : function(n) {return n.db_label === "UnnamedPipeObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_READ','EVENT_CLOSE'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).dedup().out('predicateObject')).union(_,_.in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_UPDATE'])).out('subject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "1-hop Causality",
        is_relevant : function(n) {return n.db_label === "UnnamedPipeObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_READ','EVENT_CLOSE'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).dedup().out('predicateObject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "Affected By",
        is_relevant : function(n) {return n.db_label === "UnnamedPipeObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_OPEN','EVENT_CLOSE'])).out('subject')"
    }, {
        name : "Affects",
        is_relevant : function(n) {return n.db_label === "UnnamedPipeObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_READ'])).out('subject')"
    }, {
        name : "Events",
        is_relevant : function(n) {return n.db_label === "UnnamedPipeObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, 
// NetFlowObject
       {
        name : "Events",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Affected By",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_SENDMSG','EVENT_SEND','EVENT_CLOSE','EVENT_CONNECT','EVENT_ACCEPT'])).out('subject')"
    }, {
        name : "Affects",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_READ','EVENT_RECVMSG','EVENT_RECV'])).out('subject')"
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "2-hop Causality",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_ACCEPT','EVENT_CONNECT','EVENT_CLOSE','EVENT_SENDMSG','EVENT_SEND'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).dedup().out('predicateObject')).union(_,_.in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_UPDATE'])).out('subject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "1-hop Causality",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_ACCEPT','EVENT_CONNECT','EVENT_CLOSE','EVENT_SENDMSG','EVENT_SEND'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).dedup().out('predicateObject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, {
        name : "Processes Connected",
        is_relevant : function(n) {return n.db_label === "NetFlowObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).out('subject')"
    }, 
// SrcSinkObject
       {
        name : "Events",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Affected By",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_WRITE'])).out('subject')"
    }, {
        name : "Affects",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".in('predicateObject').has('eventType',within(['EVENT_READ'])).out('subject')"
    }, {
        name : "2-hop Causality",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_READ'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject')).dedup().union(_,_.in('predicateObject').has('eventType',within(['EVENT_WRITE','EVENT_CREATE_OBJECT','EVENT_UPDATE'])).out('subject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "1-hop Causality",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).has('eventType',within(['EVENT_WRITE','EVENT_READ'])).out('subject').union(_,_.emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup().union(_,_.in('subject').hasLabel('Event').has('eventType',within(['EVENT_READ','EVENT_EXECUTE','EVENT_LOADLIBRARY','EVENT_MMAP','EVENT_RECVFROM','EVENT_RECVMSG'])).out('predicateObject')).path().unrollPath().dedup().hasNot('eventType')"
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, {
        name : "Processes connected",
        is_relevant : function(n) {return n.db_label === "SrcSinkObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).out('subject')"
    }, 
// Registry Key Object
       {
        name : "Events",
        is_relevant : function(n) {return n.db_label === "RegistryKeyObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "RegistryKeyObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.db_label === "RegistryKeyObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.db_label === "RegistryKeyObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.db_label === "RegistryKeyObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.db_label === "RegistryKeyObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, 

// Principal
    {
        name : "Processes Owned",
        is_relevant : function(n) { return n.db_label === "Principal"},
        floating_query : ".in('localPrincipal').hasLabel('Subject')",
        is_default : true
    },



// Both CDM and ADM Hosts:
    {
        name : "Host",
        is_relevant : function(node) { return node['properties'].hasOwnProperty('host') },
        floating_query : function(node) { return "; g.V().has('uuid', "+ node['properties']['host'] +")" }
    // },{
    //     name: "Between Nodes",
    //     is_relevant: function(clickedNode){ return network.getSelectedNodes().length === 2 },
    //     floating_query : function(clickedNode) { 
    //         var twoNodes = network.getSelectedNodes()
    //         "g.V("+twoNodes[0]+").both().as('n').both()"
    //         return asd
    //     }
    },


// ADM:

// AdmSubject
    {
        name : "Path Names",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".outE('cmdLine','exec','(cmdLine)').inV().hasLabel('AdmPathNode')",
        is_default : true
    }, {
        name : "Parent Process",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".outE('parentSubject').inV().hasLabel('AdmSubject')"
    }, {
        name : "Child Processes",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".inE('parentSubject').outV()"
    }, {
        name : "NetFlows",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmNetFlowObject')"
    }, {
        name : "Affected Files",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject')"
    },{
        name : "Affected File Names",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject').out('path', '(path)').hasLabel('AdmPathNode')"
    },{
        name : "Affected Objects",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2')"
    },{
        name : "Affected Object Names",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').out('path', '(path)', 'cmdLine', '(cmdLine)', 'exec').hasLabel('AdmPathNode')"
    },{
        name : "Provenance",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".in('provSubject').hasLabel('AdmProvenanceTagNode')"
    },{
        name : "Principal",
        is_relevant : function(n) {return n.db_label === "AdmSubject"},
        floating_query : ".out('localPrincipal').hasLabel('AdmPrincipal')"
    },
// AdmPathNode
    {
        name : "Processes",
        is_relevant : function(n) {return n.db_label === "AdmPathNode"},
        floating_query : ".inE('cmdLine','exec','(cmdLine)').outV().hasLabel('AdmSubject')"
    }, {
        name : "Files",
        is_relevant : function(n) {return n.db_label === "AdmPathNode"},
        floating_query : ".inE('path','(path)').outV().hasLabel('AdmFileObject')"
    },
// AdmFileObject
    {
        name : "Path Names",
        is_relevant : function(n) {return n.db_label === "AdmFileObject"},
        floating_query : ".outE('path','(path)').inV().hasLabel('AdmPathNode')",
        is_default : true
    }, {
        name : "Reading Processes",
        is_relevant : function(n) {return n.db_label === "AdmFileObject"},
        floating_query : ".inE('predicateObject','predicateObject2').outV().hasLabel('AdmEvent').has('eventType','EVENT_READ').outE('subject').inV().hasLabel('AdmSubject')"
    }, {
        name : "Writing Processes",
        is_relevant : function(n) {return n.db_label === "AdmFileObject"},
        floating_query : ".inE('predicateObject','predicateObject2').outV().hasLabel('AdmEvent').has('eventType','EVENT_WRITE').outE('subject').inV().hasLabel('AdmSubject')"
    }, {
        name : "Reading Processes Names",
        is_relevant : function(n) {return n.db_label === "AdmFileObject"},
        floating_query : ".inE('predicateObject','predicateObject2').outV().hasLabel('AdmEvent').has('eventType','EVENT_READ').outE('subject').inV().hasLabel('AdmSubject').out('exec','cmdLine','(cmdLine)').hasLabel('AdmPathNode')"
    }, {
        name : "Writing Processes Names",
        is_relevant : function(n) {return n.db_label === "AdmFileObject"},
        floating_query : ".inE('predicateObject','predicateObject2').outV().hasLabel('AdmEvent').has('eventType','EVENT_WRITE').outE('subject').inV().hasLabel('AdmSubject').out('exec','cmdLine','(cmdLine)').hasLabel('AdmPathNode')"
    },

 // AdmNetFlowObject
    {
        name : "NetFlow Subject",
        is_relevant : function(n) {return n.db_label === "AdmNetFlowObject"},
        floating_query : ".in('predicateObject','predicateObject2').out('subject').hasLabel('AdmSubject')"
    }, {
        name : "NetFlow Principal",
        is_relevant : function(n) {return n.db_label === "AdmNetFlowObject"},
        floating_query : ".inE('predicateObject','predicateObject2').outV().outE('subject').inV().outE('localPrincipal').inV()"
    // }, {   // TODO:
    //     name : "Client Connected NetFlows",
    //     is_relevant : function(n) {return n.db_label === "AdmNetFlowObject"},
    //     floating_query : ""
    }, 

 // AdmProvenanceTagNode
    {
        name : "Flow Object",
        is_relevant : function(n) {return n.db_label === "AdmProvenanceTagNode"},
        floating_query : ".out('flowObject')"
    }
]

