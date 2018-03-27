## Anomaly Detector

The anomaly detector Isolation Forest (IF) accepts two files: a set of event counts for each benign process and another set of event counts for test processes. IF then assigns anomaly score to each test process based on the AD model created from the benign data. The anomaly score ranges from 0 to 1, where, scores close to 1 indicate higher chance of anomalousness.

## Instalation and Usage
```
$ cd osu_iforest
$ make
$ ./iforest.exe -h
Usage: ./iforest.exe [options]
Options:
Options:
        -i FILE, --infile=FILE
                Specify path to input data file. (Required).
        -o FILE, --outfile=FILE
                Specify path to output results file. (Required).
        -m COLS, --metacol=COLS
                Specify columns to preserve as meta-data. (Separated by ',' Use '-' to specify ranges).
        -t N, --ntrees=N
                Specify number of trees to build.
                Default value is 100.
        -s S, --sampsize=S
                Specify subsampling rate for each tree. (Value of 0 indicates to use entire data set).
                Default value is 2048.
        -d MAX, --maxdepth=MAX
                Specify maximum depth of trees. (Value of 0 indicates no maximum).
                Default value is 0.
        -H, --header
                Toggle whether or not to expect a header input.
                Default value is true.
        -v, --verbose
                Toggle verbose ouput.
                Default value is false.
        -w N, --windowsize=N
                specify window size.
                Default value is 512.
        -c C, --cvdata=C
                specify test file
        -r R, --range-check=R
                Specify whether to use range-check
        -n N, --normalize=N
                Normalization type (1 for binary, 2 for row normalization). Default 0 for None
        -k K, --skip=K
                Skip commands with less than k instances
        -z Z, --threshold=Z
                Specify z (threshold is selected as top anomaly score shared by at least z instances)
                Default value is 1.
        -p P, --sep_alarms=P
                Specify whether to write the alarms in separate files (relevant and irrelevant)
                Default value is 0.
        -h, --help
                Print this help message and exit.
```

## Sample Input and Output

An example input file for benign data (Cadets Bovia) is given below, where each row represents the event counts for a particular process.

```
process_name,uuid,originalCdmUuids,EVENT_MPROTECT,EVENT_READ,EVENT_FORK,EVENT_SENDMSG,EVENT_EXIT,EVENT_OPEN,EVENT_CONNECT,EVENT_LSEEK,EVENT_RECVFROM,EVENT_RECVMSG,EVENT_EXECUTE,EVENT_CHANGE_PRINCIPAL,EVENT_CLOSE,EVENT_LINK,EVENT_RENAME,EVENT_LOGIN,EVENT_SIGNAL,EVENT_ACCEPT,EVENT_CREATE_OBJECT,EVENT_OTHER,EVENT_BIND,EVENT_FCNTL,EVENT_MODIFY_PROCESS,EVENT_UNLINK,EVENT_WRITE,EVENT_MODIFY_FILE_ATTRIBUTES,EVENT_SENDTO,EVENT_TRUNCATE,EVENT_MMAP
devd,1e72a856-08f4-3ff4-9f1c-c59fccb003b2,9510c0a6-3408-11e7-b61b-076f926d3775,0,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,12,0,0
syslogd,9057601f-8c51-3ca4-9763-d71c07d55738,954680ac-3408-11e7-b61b-076f926d3775,0,16,0,0,0,45,0,0,1005,0,0,0,44,0,0,0,0,0,0,0,0,0,0,0,1006,0,0,0,0
ulog-helper,0177deb7-8534-3771-9f72-4288a77ab175,29ea6fab-340b-11e7-b61b-076f926d3775,0,6,0,0,1,11,0,2,0,0,0,0,11,0,0,0,0,0,0,0,0,1,0,0,3,0,0,0,5
postgres,5383d28c-b3f9-30ca-b79a-344cdb887967,957c4706-3408-11e7-b61b-076f926d3775,0,31947,0,0,0,19242,0,4611,0,0,0,0,19241,0,0,0,6351,0,0,0,0,0,0,0,12705,0,6390,0,0
postgres,404b7c26-8629-3c73-8ed8-a89242c0747a,957c631f-3408-11e7-b61b-076f926d3775,0,0,0,0,0,31832,0,0,6610,0,0,0,31832,0,31832,0,0,0,0,0,0,0,0,0,31832,0,0,0,0
postgres,da963775-5e13-3ccb-aa7c-db6f49b91774,95738dd0-3408-11e7-b61b-076f926d3775,0,3183,6352,0,0,3183,0,0,0,0,0,0,3183,0,0,0,0,0,0,0,0,0,0,0,0,110,0,0,0
postgres,385942b1-3d54-3514-8c5c-1bab324366aa,2c990cdf-340b-11e7-b61b-076f926d3775,0,10,0,0,1,13,0,3,0,0,0,0,10,0,0,0,1,0,1,1,0,2,0,0,0,0,1,0,0
cron,a922bba8-8838-3400-8a7d-b29a7ef3a344,ffe6203e-340b-11e7-b61b-076f926d3775,0,1,1,0,1,0,0,0,0,0,0,0,3,0,0,0,0,0,2,0,0,1,0,0,0,0,0,0,0
sudo,501bbefe-cdd7-3ad5-8c68-01d1e579a44c,3170099a-340b-11e7-b61b-076f926d3775,2,28,1,0,0,42,1,12,1,0,0,1,41,0,0,0,0,0,3,0,0,4,1,0,0,0,1,0,21
mv,efd9c5b6-11ad-3fac-b899-5b57c01d6e03,fff6b745-340b-11e7-b61b-076f926d3775,0,2,0,0,1,3,0,1,0,0,1,0,3,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2
...
```

An example test file (Cadets Pandex) is given below.

```
process_name,uuid,originalCdmUuids,EVENT_MPROTECT,EVENT_READ,EVENT_FORK,EVENT_SENDMSG,EVENT_EXIT,EVENT_OPEN,EVENT_CONNECT,EVENT_LSEEK,EVENT_RECVFROM,EVENT_RECVMSG,EVENT_EXECUTE,EVENT_CHANGE_PRINCIPAL,EVENT_CLOSE,EVENT_LINK,EVENT_RENAME,EVENT_LOGIN,EVENT_SIGNAL,EVENT_ACCEPT,EVENT_CREATE_OBJECT,EVENT_OTHER,EVENT_BIND,EVENT_FCNTL,EVENT_MODIFY_PROCESS,EVENT_UNLINK,EVENT_WRITE,EVENT_MODIFY_FILE_ATTRIBUTES,EVENT_SENDTO,EVENT_TRUNCATE,EVENT_MMAP
postgres,a1c0fcaf-7472-38a9-8148-bb5d76b381b3,c976017a-371e-11e7-ab10-23858a5599f4,0,119579,0,0,0,72154,0,14223,0,0,0,0,72154,0,0,0,23714,0,0,0,0,0,0,0,47425,0,23900,0,0
postgres,de874eb4-74cf-3e0b-9d41-5180f3e2a092,c97638f3-371e-11e7-ab10-23858a5599f4,0,0,0,0,0,118929,0,0,24666,0,0,0,118926,0,118924,0,0,0,0,0,0,0,0,0,118926,0,0,0,0
postgres,d3aa5105-fe53-326b-9a47-e473d5f1b31a,c933ab4b-371e-11e7-ab10-23858a5599f4,0,11893,23711,0,0,11893,0,0,0,0,0,0,11893,0,0,0,0,0,0,0,0,0,0,0,0,414,0,0,0
postgres,e683c5fe-94a1-33d1-8d04-0102c69dcfc2,77988607-3977-11e7-ab10-23858a5599f4,0,10,0,0,1,13,0,3,0,0,0,0,10,0,0,0,1,0,1,1,0,2,0,0,0,0,1,0,0
cron,7e058cca-e7e4-39e7-ba9f-0d66b9d3bf4a,caef7063-371e-11e7-ab10-23858a5599f4,0,0,3891,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
cron,9656fcbf-704b-3cc9-bb00-64e8190c33e3,7be9f964-3977-11e7-ab10-23858a5599f4,0,1,1,0,1,0,0,0,0,0,0,0,3,0,0,0,0,0,2,0,0,1,0,0,0,0,0,0,0
cron,5caee32b-eb02-3c95-885f-968474d1b53e,7bea399c-3977-11e7-ab10-23858a5599f4,0,9,0,0,1,16,1,4,0,0,2,3,18,0,0,1,0,0,1,0,0,0,4,0,0,0,1,0,9
sh,5caee32b-eb02-3c95-885f-968474d1b53e,7bea399c-3977-11e7-ab10-23858a5599f4,0,9,0,0,1,16,1,4,0,0,2,3,18,0,0,1,0,0,1,0,0,0,4,0,0,0,1,0,9
atrun,5caee32b-eb02-3c95-885f-968474d1b53e,7bea399c-3977-11e7-ab10-23858a5599f4,0,9,0,0,1,16,1,4,0,0,2,3,18,0,0,1,0,0,1,0,0,0,4,0,0,0,1,0,9
syslogd,0ade7a32-6630-32d7-97c6-851a8596ebb7,c898700e-371e-11e7-ab10-23858a5599f4,0,58,0,0,0,168,0,0,3810,0,0,0,168,0,0,0,0,0,0,0,0,0,0,0,3841,0,0,0,0
...
```

The corresponding run command and output files are given below.
Note that, the output is two separate files, one containing all the processes that didn't have a IF model (ended by _irrelevant.csv) due to lack of instances to build a proper IF model (specified by -k option) or the process was not part of the benign data. The anomaly score in this case is the minimum score over all the IF models that was created from the benign data and also above the corresponding threshold. This file is the ranking over potential malicious processes. The other one (ended by _relevant.csv) contains all the processes that did have IF model and the anomaly score is greater than the corresponding threshold. This file is the ranking of potential process spoofing.
```
$/d/ADAPT/E3/SrcAdaptAdm/adapt/legacy/ad/osu_iforest/iforest.exe -t 100 -s 512 -m 1-3 -r 1 -n 0 -k 50 -z 1 -p 1 -i /d/ADAPT/E3Prep/PreStructuredIF/Run/RawCounts/BenignCadetsBovia.csv -c /d/ADAPT/E3Prep/PreStructuredIF/Run/RawCounts/BenignMaliciousCadetsPandex.csv -o /d/ADAPT/E3Prep/PreStructuredIF/test/final/out/raw_thType_1.csv
# Trees     = 100
# Samples   = 512
# MaxHeight = 0
Check Range = 1
Norm. Type  = 0
Skip Limit  = 50
TH param    = 1
Sep. Alarms = 1
Train Data Dimension: 38484,29
 Test Data Dimension: 125330,29
Meta cols: 3
input_name: D:/ADAPT/E3Prep/PreStructuredIF/Run/RawCounts/BenignCadetsBovia.csv
test_name:  D:/ADAPT/E3Prep/PreStructuredIF/Run/RawCounts/BenignMaliciousCadetsPandex.csv
output_name:D:/ADAPT/E3Prep/PreStructuredIF/test/final/out/raw_thType_1.csv

CMD: devd -> 1

CMD: syslogd -> 1

CMD: ulog-helper -> 1

CMD: postgres -> 6354
TH = 0.889668(1)
Avg. # of leaf nodes: 33
Avg. leaf depth:      8.27704
...

$ cat /d/ADAPT/E3Prep/PreStructuredIF/test/final/out/raw_thType_1_irrelevent.csv
process_name,uuid,originalCdmUuids,EVENT_MPROTECT,EVENT_READ,EVENT_FORK,EVENT_SENDMSG,EVENT_EXIT,EVENT_OPEN,EVENT_CONNECT,EVENT_LSEEK,EVENT_RECVFROM,EVENT_RECVMSG,EVENT_EXECUTE,EVENT_CHANGE_PRINCIPAL,EVENT_CLOSE,EVENT_LINK,EVENT_RENAME,EVENT_LOGIN,EVENT_SIGNAL,EVENT_ACCEPT,EVENT_CREATE_OBJECT,EVENT_OTHER,EVENT_BIND,EVENT_FCNTL,EVENT_MODIFY_PROCESS,EVENT_UNLINK,EVENT_WRITE,EVENT_MODIFY_FILE_ATTRIBUTES,EVENT_SENDTO,EVENT_TRUNCATE,EVENT_MMAP,anomaly_score
qmgr,39cd08b3-e9b8-3474-bfb7-91509a259193,cb61cb0a-371e-11e7-ab10-23858a5599f4,0,17064,0,0,0,46555,3497,6820,0,0,0,0,52365,0,3443,0,0,2415,3504,0,0,2560,0,3410,7314,2893,451,0,0,0.850759
nginx,b3e57c7f-dc6d-3a81-9ce2-7c451e101822,ca314d38-371e-11e7-ab10-23858a5599f4,11,73,1,0,0,80,10,66,1020,0,0,0,131,0,0,0,0,27,29,6,0,0,4,0,74,0,25,0,0,0.816282
nginx,13ba115a-dbbb-3d49-aa2a-5f1e692258ae,480af8f6-3fc4-11e7-ab10-23858a5599f4,6,41,2,0,0,46,2,39,153,1,0,0,47,0,0,0,0,1,3,0,0,0,1,3,3,1,18,0,0,0.758476
nginx,01f86137-2286-32a5-8dbf-8095a9a5aa78,ca315dab-371e-11e7-ab10-23858a5599f4,5,29,1,0,0,32,3,21,848,0,0,0,41,0,0,0,0,6,8,5,0,0,3,1,12,1,16,0,0,0.753627
makewhatis,12fb82dc-6f8f-32ef-a183-59ed263fd162,ac75e7a2-3d34-11e7-ab10-23858a5599f4,0,4662,0,0,1,4649,0,4606,0,0,0,0,4656,0,2,0,0,0,0,0,0,1,1,16,56,15,0,0,1,0.744036
wget,37ebad71-2733-31bb-bdd5-03e7cf97fb5f,646b0211-3eed-11e7-ab10-23858a5599f4,1,91,0,0,1,42,43,1,84,0,1,0,84,0,0,0,0,0,43,0,0,0,0,0,44,0,2,0,28,0.729383
pickup,3bea763e-3bc9-3543-80fc-556db8577ef9,84b04e68-3ff5-11e7-ab10-23858a5599f4,0,75,0,0,0,89,1,5,0,0,1,1,149,0,0,0,0,59,3,0,0,61,1,0,59,0,0,0,15,0.724569
fcgiwrap,dd634b34-d4a5-3fe2-9e05-060472b1b7c0,764198ae-34df-11e7-b61b-076f926d3775,1,124,0,0,1,203,0,41,0,0,2,0,203,0,0,0,0,0,0,0,0,1,2,0,1,0,0,0,33,0.724488
pickup,fa59f969-b647-34d5-8076-9d51fc124e6f,3ad8c880-3df2-11e7-ab10-23858a5599f4,0,123,0,0,1,135,3,7,0,0,1,1,237,0,0,0,0,100,5,0,0,98,1,2,98,0,1,0,15,0.721383
...

$ cat /d/ADAPT/E3Prep/PreStructuredIF/test/final/out/raw_thType_1_relevent.csv
process_name,uuid,originalCdmUuids,EVENT_MPROTECT,EVENT_READ,EVENT_FORK,EVENT_SENDMSG,EVENT_EXIT,EVENT_OPEN,EVENT_CONNECT,EVENT_LSEEK,EVENT_RECVFROM,EVENT_RECVMSG,EVENT_EXECUTE,EVENT_CHANGE_PRINCIPAL,EVENT_CLOSE,EVENT_LINK,EVENT_RENAME,EVENT_LOGIN,EVENT_SIGNAL,EVENT_ACCEPT,EVENT_CREATE_OBJECT,EVENT_OTHER,EVENT_BIND,EVENT_FCNTL,EVENT_MODIFY_PROCESS,EVENT_UNLINK,EVENT_WRITE,EVENT_MODIFY_FILE_ATTRIBUTES,EVENT_SENDTO,EVENT_TRUNCATE,EVENT_MMAP,anomaly_score
postgres,a1c0fcaf-7472-38a9-8148-bb5d76b381b3,c976017a-371e-11e7-ab10-23858a5599f4,0,119579,0,0,0,72154,0,14223,0,0,0,0,72154,0,0,0,23714,0,0,0,0,0,0,0,47425,0,23900,0,0,0.893397
master,dc1a5e3f-4b94-3bd8-8e0d-51790f628e00,cb5dd8f4-371e-11e7-ab10-23858a5599f4,0,17837,2509,0,0,0,14340,0,0,0,0,0,14329,0,0,0,0,0,14340,0,0,13177,0,0,16387,0,0,0,0,0.854222
sh,5c3f5c62-22fa-35d0-94d8-870b8a4d6781,6b2c7083-3d34-11e7-ab10-23858a5599f4,0,2,0,0,0,45050,0,1,0,0,1,0,45192,0,0,0,0,0,0,0,0,0,22945,0,33,0,0,0,3,0.828301
sh,4db32a10-64a8-3d3d-b084-b3822d33e5d1,56e19e40-3d2a-11e7-ab10-23858a5599f4,0,2,0,0,0,66769,0,1,0,0,1,0,66855,0,0,0,0,0,0,0,0,0,34012,0,0,0,0,0,3,0.801555
sh,c3fa086b-8e3d-34f7-894c-b49925143255,c0e601f2-3b97-11e7-ab10-23858a5599f4,0,2,0,0,0,49167,0,1,0,0,1,0,49231,0,0,0,0,0,0,0,0,0,25048,0,0,0,0,0,3,0.801106
sh,dbcc7207-eb7d-3e73-94bd-ce756a01f913,d655f7ce-3f85-11e7-ab10-23858a5599f4,0,2,0,0,0,68306,0,1,0,0,1,0,68413,0,0,0,0,0,0,0,0,0,34785,0,0,0,0,0,3,0.800976
sh,de96eef8-e9a9-3c49-9476-63eb290c9721,8196a768-3df3-11e7-ab10-23858a5599f4,0,2,0,0,0,70841,0,1,0,0,1,0,70964,0,0,0,0,0,0,0,0,0,36055,0,0,0,0,0,3,0.800764
cron,770407fa-7fce-3a7a-a7df-2e0470452cf7,b6fe9027-3a50-11e7-ab10-23858a5599f4,0,16,1,0,1,23,1,7,0,0,2,2,26,1,3,1,1,0,1,0,0,0,3,1,2,5,1,0,7,0.757822
cat,8c9e6d29-1d01-3993-955e-a6d3a33988cf,b4527963-3a05-11e7-ab10-23858a5599f4,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0.691833
...
```
