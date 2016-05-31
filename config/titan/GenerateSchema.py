#! /usr/bin/env python2

import os
import re
from tornado import gen
from tornado.ioloop import IOLoop
from gremlinclient.tornado_client import submit


def get_schema():
    spec = os.path.expanduser('~/adapt/ingest/Ingest/Language.md')
    with open(spec) as langFile:
        langCont = langFile.read()
        regexp = re.compile(r"^\s*\[schema\]: #\s*(?:\n|\r\n?)\s*(.+$)",
                            re.MULTILINE)
        matches = [m.groups() for m in regexp.finditer(langCont)]

        # generate schema from Language.md to insert in a single transaction
        lines = ["mgmt = graph.openManagement();"]
        for m in matches:
            lines.append("mgmt." + m[0] + ".make();")
        # We only want one commit within a single transaction, not per schema item.
        lines.append("mgmt.commit();")

    schema = " ".join(lines)
    return schema


@gen.coroutine
def go(schema):
    resp = yield submit("ws://localhost:8182/", schema)
    while True:
        msg = yield resp.read()
        if msg is None:
            break
        print(msg)


if __name__ == '__main__':
    loop = IOLoop.current()
    loop.run_sync(lambda: go(get_schema()))
