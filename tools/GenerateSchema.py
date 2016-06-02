#! /usr/bin/env python2

import os
import re

schemaFile = open('/opt/titan/scripts/schema.groovy', 'w')
schemaFile.truncate()
schemaFile.write("graph = TitanFactory.open('/opt/titan/conf/gremlin-server/titan-cassandra-server.properties')\n")
schemaFile.write("graph.tx().rollback()\n")
schemaFile.write("mgmt = graph.openManagement()\n")
schemaFile.write("if(!mgmt.containsRelationType('EDGE_SUBJECT_HASPARENT_SUBJECT out')) {\n")

# generate schema groovy from Language.md
spec = os.path.expanduser('~/adapt/ingest/Ingest/Language.md')
with open(spec) as langFile:
    langCont = langFile.read()
    regexp = re.compile(r"^\s*\[schema\]: #\s*(?:\n|\r\n?)\s*(.+$)", re.MULTILINE)
    matches = [m.groups() for m in regexp.finditer(langCont)]
    for m in matches:
        schemaFile.write("    mgmt." + m[0] + ".make()\n")

schemaFile.write("}\n")
# We only want one commit within a single transaction, not per schema item.
schemaFile.write("mgmt.commit()\n")

schemaFile.close()
