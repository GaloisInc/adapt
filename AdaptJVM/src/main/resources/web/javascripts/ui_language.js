
var starting_queries = [

     {
        name : "find file by predicateObject file path",
        base_query : "g.V().hasLabel('FileObject').as('file').in('predicateObject').hasLabel('Event').has('predicateObjectPath','{_}').select('file')",
        default_values : ["<filename>"]
    }, {
        name : "find file by predicateObject2 file path",
        base_query : "g.V().hasLabel('FileObject').as('file').in('predicateObject2').hasLabel('Event').has('predicateObjectPath2','{_}').select('file')",
        default_values : ["<filename>"]
    }, {
        name : "find file by file descriptor",
        base_query : "g.V().hasLabel('FileObject').has('fileDescriptor',{_}).dedup()",
        default_values : ["1"]
    }, {
        name : "find process by pid",
        base_query : "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').has('cid',{_})",
        default_values : ["1001"]
    }, {
        name : "find up to n processes of an owner",
        base_query : "g.V().hasLabel('Principal').has('userId','{_}').in('localPrincipal').hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').limit({_})",
        default_values : ["10004",10]
    }, {
        name : "find NetFlow by local address & port",
        base_query : "g.V().hasLabel('NetFlowObject').has('localAddress','{_}').has('localPort',{_}).dedup()",
        default_values : ["127.0.0.1",80]
    }, {
        name : "find NetFlow by remote address & port",
        base_query : "g.V().hasLabel('NetFlowObject').has('remoteAddress','{_}').has('remotePort',{_}).dedup()",
        default_values : ["127.0.0.1",80]
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
        name : "File",
        is_relevant : function(n) { return n.label === "FileObject" },
        icon_unicode : "\uf41b",
        size: 40,
        make_node_label : function(node) {
           var fd = (node.hasOwnProperty('fileDescriptor') ? node['fileDescriptor'][0]['value'] : "None")
            var fp = (node['properties'].hasOwnProperty('path') ? node['properties']['path'][0]['value'] : "None")
            var fsize = (node['properties'].hasOwnProperty('size') ? node['properties']['size'][0]['value'] : "None")
            return fp + " \n of size " + fsize + " @ descr " + fd
        }
    },
    {
        name : "MemoryObject",
        is_relevant : function(n) { return n.label === "MemoryObject" },
        icon_unicode : "\uf376",
        size: 40,
        make_node_label : function(node) {
            var addr = (node['properties'].hasOwnProperty('memoryAddress') ? node['properties']['memoryAddress'][0]['value'] : "None")
            var size = (node['properties'].hasOwnProperty('size') ? node['properties']['size'][0]['value'] : "None")
            return size + "@" + addr
        }
    }, {
        name : "Pipe",
        is_relevant : function(n) { return n.label === "UnnamedPipeObject" },
        icon_unicode : "\uf2c0",
        size: 40,
        make_node_label : function(node) {
            var source = (node['properties'].hasOwnProperty('sourceFileDescriptor') ? node['properties']['sourceFileDescriptor'][0]['value'] : "None")
            var sink = (node['properties'].hasOwnProperty('sinkFileDescriptor') ? node['properties']['sinkFileDescriptor'][0]['value'] : "None")
            return "src:" + source + ", sink:" + sink
        }
    }, {
        name : "Principal",
        is_relevant : function(n) { return n.label === "Principal" },
        icon_unicode : "\uf419",
        size: 50,
        make_node_label : function(node) {
            var at = (node['properties'].hasOwnProperty('userId') ? node['properties']['userId'][0]['value'] : "None")
            return at + " userId " + node['properties']['userId'][0]['value']
        }
   }, {
        name : "NetFlow",
        is_relevant : function(n) { return n.label === "NetFlowObject" },
        icon_unicode : "\uf262",
        make_node_label : function(node) {
            var localA = (node['properties'].hasOwnProperty('localAddress') ? node['properties']['localAddress'][0]['value'] : "None")
            var localP = (node['properties'].hasOwnProperty('localPort') ? node['properties']['localPort'][0]['value'] : "None")
            var remoteA = (node['properties'].hasOwnProperty('remoteAddress') ? node['properties']['remoteAddress'][0]['value'] : "None")
            var remoteP = (node['properties'].hasOwnProperty('remotePort') ? node['properties']['remotePort'][0]['value'] : "None")
            return "local: " + localA + ":" + localP + ", remote: " + remoteA + ":" + remoteP
        }
   }, {
        name : "RegistryKey",
        is_relevant : function(n) { return n.label === "RegistryKeyObject" },
        icon_unicode : "\uf296",
        make_node_label : function(node) {
            var key = (node['properties'].hasOwnProperty('key') ? node['properties']['key'][0]['value'] : "None")
            var val = (node['properties'].hasOwnProperty('value') ? node['properties']['value'][0]['value'] : "None")
            return key + ": " + value
        }
   }, {
        name : "Event",
        is_relevant : function(n) { return n.label === "Event" },
        icon_unicode : "\uf375",
        make_node_label : function(node) {
            var sequence = (node['properties'].hasOwnProperty('sequence') ? node['properties']['sequence'][0]['value'] : "None")
            var type = (node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'][0]['value'] : "None")
            var programPoint = (node['properties'].hasOwnProperty('programPoint') ? node['properties']['programPoint'][0]['value'] : "None")
            var name = (node['properties'].hasOwnProperty('name') ? node['properties']['name'][0]['value'] : "None")
            return type + ", seq:" + sequence + " \n ppt:" + programPoint // + " \n " + name
        }
   }, {
        name : "SrcSink",
        is_relevant : function(n) { return n.label === "SrcSinkObject" },
        icon_unicode : "\uf313",
        make_node_label : function(node) {
            var uuid = (node['properties'].hasOwnProperty('uuid') ? node['properties']['uuid'][0]['value'] : "None")
            var type = (node['properties'].hasOwnProperty('srcSinkType') ? node['properties']['srcSinkType'][0]['value'] : "None")
            return type + " \n uuid: " + uuid
        }
   }, {
        name : "PTN",
        is_relevant : function(n) { return n.label === "ProvenanceTagNode" },
        icon_unicode : "\uf277",
        make_node_label : function(node) {
            var systemCall = (node['properties'].hasOwnProperty('systemCall') ? node['properties']['systemCall'][0]['value'] : "None")
            var opcode = (node['properties'].hasOwnProperty('opcode') ? node['properties']['opcode'][0]['value'] : "None")
            var itag = (node['properties'].hasOwnProperty('itag') ? node['properties']['itag'][0]['value'] : "None")
            var ctag = (node['properties'].hasOwnProperty('ctag') ? node['properties']['ctag'][0]['value'] : "None")
            return "function:" + opcode +  /* ", call:" + systemCall +  */ " \n itag:" + itag + ", ctag: " + ctag
        }
    }, {
       name : "Subject",
        is_relevant : function(n) { return n.label === "Subject" },
        icon_unicode : "\uf375",
        make_node_label : function(node) {
            var cid = (node['properties'].hasOwnProperty('cid') ? node['properties']['cid'][0]['value'] : "None")
            var t = (node['properties'].hasOwnProperty('subjectType') ? node['properties']['subjectType'][0]['value'] : "None")
            var cmd = (node['properties'].hasOwnProperty('cmdLine') ? node['properties']['cmdLine'][0]['value'] : "no cmd line")
            var timestamp = (node['properties'].hasOwnProperty('startedTimestampNanos') ? new Date(node['properties']['startTimestampNanos'][0]['value']/1000).toGMTString() + " ." + node['properties']['startTimestampNanos'][0]['value']%1000 : "no timestamp")
            switch(t) {
                case "SUBJECT_PROCESS":
                    return "Process: " + cid + " \n cmd: " + cmd + " \n " + timestamp
                case "SUBJECT_THREAD":
                    return "Thread " + cid + " \n " + timestamp
                case "SUBJECT_UNIT":
                    return "Unit " + cid + " \n " + timestamp
                default:
                    return t + ", @" + timestamp
            }
        }
    }, {
        name : "Default",   // This default will override anything below!
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
       name: "refObject",
       is_relevant : function(n) {return n.label === "ProvenanceTagNode"},
       floating_query : ".out('flowObject')",
       is_default : true
    }, {
        name: "refSubject",
        is_relevant : function(n) {return n.label === "ProvenanceTagNode"},
        floating_query : ".out('subject')"
    }, {
        name: "ancestor tags",
        is_relevant : function(n) {return n.label === "ProvenanceTagNode"},
        floating_query : ".emit().repeat(_.out('prevTagId','tagId')).dedup().path().unrollPath().dedup()"
    }, {
        name: "descendant tags",
        is_relevant : function(n) {return n.label === "ProvenanceTagNode"},
        floating_query : ".emit().repeat(_.in('prevTagId','tagId')).dedup().path().unrollPath().dedup()"
    },
// File Object
    {
       name : "Events",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.out('parentSubject'))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup()"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup()"
    }, {
        name : "Subjects reading",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Executing",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_EXECUTE').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.label === "FileObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, 
// Memory Object
    {
       name : "Events",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.out('parentSubject'))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup()"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup()"
    }, {
        name : "Subjects reading",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Executing",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_EXECUTE').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.label === "MemoryObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, 
// Subject
        {
        name : "Events Caused",
        is_relevant : function(n) {return n.label === "Subject" && n['properties'].hasOwnProperty('subjectType') && n['properties']['subjectType'][0]['value'] === 'SUBJECT_PROCESS'},
        floating_query : ".in('subject').hasLabel('Event')",
        is_default : true
    }, {
        name : "Principal",
        is_relevant : function(n) {return n.label === "Subject" },
        floating_query : ".out('localPrincipal')",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.label === "Subject"},
        floating_query : ".as('subjectOfInterest').emit().repeat(_.out('parentSubject')).select('subjectOfInterest').union(_,_.out('localPrincipal')).select('subjectOfInterest').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).path().unrollPath().dedup()"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.label === "Subject"},
        floating_query : ".in('subject').hasLabel('ProvenanceTagNode')"
    }, {
        name : "Children",
        is_relevant : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 'SUBJECT_PROCESS'},
        floating_query : ".in('parentSubject')"
    }, 
// Unnamed Pipe Object
        {
        name : "Processes connected",
        is_relevant : function(n) {return n.label === "UnnamedPipeObject"},
        floating_query : ".union(_.in('predicateObject',_.in('predicateObject2')).out('subject')"
    }, {
        name : "Events",
        is_relevant : function(n) {return n.label === "UnnamedPipeObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, 
// NetFlowObject
    {
       name : "Events",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.out('parentSubject'))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup()"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup()"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, {
        name : "Processes connected",
        is_relevant : function(n) {return n.label === "NetFlowObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2')).out('subject')"
   }, 
// SrcSinkObject
    {
       name : "Events",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.out('parentSubject'))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup()"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup()"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
    }, {
        name : "Processes connected",
        is_relevant : function(n) {return n.label === "SrcSinkObject"},
        floating_query : ".union(_.in('predicateObject',_.in('predicateObject2')).out('subject')"
   }, 
// Registry Key Object
    {
       name : "Events",
        is_relevant : function(n) {return n.label === "RegistryKeyObject"},
        floating_query : ".union(_.in('predicateObject'),_.in('predicateObject2'))",
        is_default : true
    }, {
        name : "Provenance",
        is_relevant : function(n) {return n.label === "RegistryKeyObject"},
        floating_query : ".as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.out('parentSubject'))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup()"
    }, {
        name : "PTN",
        is_relevant : function(n) {return n.label === "RegistryKeyObject"},
        floating_query : ".in('flowObject')"
    }, {
        name : "Progenance",
        is_relevant : function(n) {return n.label === "RegistryKeyObject"},
        floating_query : ".as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup()"
    }, {
        name : "Subjects Reading",
        is_relevant : function(n) {return n.label === "RegistryKeyObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_READ').out('subject')"
    }, {
        name : "Subjects Writing",
        is_relevant : function(n) {return n.label === "RegistryKeyObject"},
        floating_query : ".in('predicateObject').has('eventType','EVENT_WRITE').out('subject')"
   }, 

// Principal
        {
        name : "Processes owned",
        is_relevant : function(n) { return n.label === "Principal"},
        floating_query: ".in('localPrincipal').hasLabel('Subject')",
        is_default : true
    }
]

