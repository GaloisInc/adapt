#! /usr/bin/env python3

import requests, readline, atexit, os, sys, json, subprocess, tempfile

if __name__ == '__main__':
    url = "http://localhost:8080"
    query = "g.V().hasLabel('AdmSubject').id()"
    puuids = requests.post(url + "/query/json", data={"query": query})
    if puuids.status_code != 200:
        print('Status code not 200: ' + str(puuids.status_code))
    puuids = puuids.json()
    print(len(puuids))
    events = ['EVENT_ACCEPT','EVENT_ADD_OBJECT_ATTRIBUTE','EVENT_BIND','EVENT_BLIND','EVENT_BOOT','EVENT_CHANGE_PRINCIPAL','EVENT_CHECK_FILE_ATTRIBUTES','EVENT_CLONE','EVENT_CLOSE','EVENT_CONNECT','EVENT_CREATE_OBJECT','EVENT_CREATE_THREAD','EVENT_DUP','EVENT_EXECUTE','EVENT_EXIT','EVENT_FLOWS_TO','EVENT_FCNTL','EVENT_FORK','EVENT_LINK','EVENT_LOADLIBRARY','EVENT_LOGCLEAR','EVENT_LOGIN','EVENT_LOGOUT','EVENT_LSEEK','EVENT_MMAP','EVENT_MODIFY_FILE_ATTRIBUTES','EVENT_MODIFY_PROCESS','EVENT_MOUNT','EVENT_MPROTECT','EVENT_OPEN','EVENT_OTHER','EVENT_READ','EVENT_READ_SOCKET_PARAMS','EVENT_RECVFROM','EVENT_RECVMSG','EVENT_RENAME','EVENT_SENDTO','EVENT_SENDMSG','EVENT_SERVICEINSTALL','EVENT_SHM','EVENT_SIGNAL','EVENT_STARTSERVICE','EVENT_TRUNCATE','EVENT_UMOUNT','EVENT_UNIT','EVENT_UNLINK','EVENT_UPDATE','EVENT_WAIT','EVENT_WRITE','EVENT_WRITE_SOCKET_PARAMS']
    f = open('AllCommandEvents.csv', "w")
    f.write("process_name,uuid,originalCdmUuids")
    for e in events:
        f.write("," + e)
    f.write("\n")
    for vid in puuids:
        query = "g.V(" + str(vid) + ").values('uuid')"
        res = requests.post(url + "/query/json", data={"query": query}).json()
        uuid = res[0]
        query = "g.V(" + str(vid) + ").out('cmdLine','exec','(cmdLine)').hasLabel('AdmPathNode').values('path')"
        res = requests.post(url + "/query/json", data={"query": query}).json()
        cmds = res
        if not cmds:
            cmds = ['NA']
        query = "g.V(" + str(vid) + ").values('originalCdmUuids')"
        res = requests.post(url + "/query/json", data={"query": query}).json()
        orgUuid =  res[0]
        query = "g.V(" + str(vid) + ").in('subject').groupCount().by('eventType')"
        res = requests.post(url + "/query/json", data={"query": query}).json()
        d = res[0]
        for cmd in cmds:
            f.write(cmd + ',' + uuid + ',' + orgUuid)
            for e in events:
                if e in d:
                    f.write("," + str(d[e]))
                else:
                    f.write(",0")
            f.write("\n")
    f.close()

