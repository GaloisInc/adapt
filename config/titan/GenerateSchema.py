#! /usr/bin/env python3

import os
import re
from tornado import gen
from tornado.ioloop import IOLoop
from gremlinclient.tornado_client import submit

with open(os.path.expanduser('~/adapt/ingest/Ingest/Language.md')) as langFile:
  langCont = langFile.read()
  regexp = re.compile(r"^\s*\[schema\]: #\s*(?:\n|\r\n?)\s*(.+$)", re.MULTILINE)
  matches = [m.groups() for m in regexp.finditer(langCont)]

  schema = "mgmt = graph.openManagement(); "
  for m in matches:
      schema += "mgmt." + m[0] + ".make(); "
  schema += "mgmt.commit()"

loop = IOLoop.current()


@gen.coroutine
def go():
    resp = yield submit("ws://localhost:8182/", schema)
    while True:
        msg = yield resp.read()
        if msg is None:
            break
        print(msg)


if __name__ == '__main__':
    loop.run_sync(go)
