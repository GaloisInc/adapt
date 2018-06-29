import argparse
import time
import json
import csv
import os
import subprocess
import pandas as pd
from collections import defaultdict


def compactAlarmFiles(alarmDir,alarmFNames):
    alarmFilesInDir = os.listdir(alarmFileDir)
    if all([f in alarmFilesInDir
            for f in map(lambda x: x+".c",alarmFNames)]):
        return "Alarm files already ready!"
    else:
        print("Current working directory: {}".format(alarmDir))
        def bashCommand(fname):
            return "cat {} | jq -c . > {}.c".format(fname,fname)
        for f in alarmFNames:
            print("Compacting {}".format(f))
            process = subprocess.run(bashCommand(f), shell=True, cwd=alarmDir)
        return "Done!"

def getAttackLines(attackFile):
    attacklines = []
    with open(attackFile,'r') as f:
        freader = csv.reader(f)
        for row in freader:
            attacklines.append(row)
    return attacklines

def getAlarmLines(alarmFile):
    print("Processing alarm file: ",alarmFile)
    try:
        with open(alarmFile,'r') as f:
            for line in f: # Only one line in the file
                jsonLine = json.loads(line)
    except:
        jsonLine = []
    return jsonLine

def containsKeyword(keywords,listOfWords):
    keywordList = keywords.split(",")
    intersection = [key for key in keywordList
                    if any([(key.lower() in word.lower() or word.lower() in key.lower())
                            for word in listOfWords])]
    return len(intersection)>0

def toEpoch(date):
    pattern = '%Y%m%d'
    return int(time.mktime(time.strptime(date, pattern)))

def getLocalProb(alarmLine):
    nodes = alarmLine[1][2]
    numNodes = len(nodes)
    lp = nodes[-1][1]

    for idx, node in enumerate(nodes):
        if node[3] == 1 and (idx + 1) < numNodes:
            lp = node[4]/node[5]
            break
    return lp

def extractFeaturesFromAlarms(alarmLines):
    alarmsExtracted = []
    for alarm in alarmLines:
        keywords = alarm[0]
        timestamp = alarm[1][0]
        ingestTimestamp = alarm[1][1]
        localProb = getLocalProb(alarm)  # alarm[1][2][-1][1]
        globalProb = alarm[1][2][-1][2]
        alarmsExtracted.append([keywords,timestamp,localProb,globalProb,ingestTimestamp])
    return alarmsExtracted

def attackPresentInAlarmExt(attackLine,alarmExt,matchingFunction):
    if matchingFunction(attackLine[2],alarmExt[0]):
        minEpochTime = toEpoch(attackLine[1])
        maxEpochTime = toEpoch(attackLine[1]) + 86400
        alarmTime = round(alarmExt[1]/1000000000)
        return (alarmTime==0 or (alarmTime>=minEpochTime and alarmTime<=maxEpochTime))
    else:
        return False

def alarmExtToAttackDF(alarmLinesExt,attackLines,matchingFunction):
    alarmToAttack = []
    for alarm in alarmLinesExt:
        attackFound = False
        attacksfound = []
        newlinestart = [",".join(alarm[0])] + alarm[1:]
        for attack in attackLines:
            if attackPresentInAlarmExt(attack,alarm,matchingFunction):
                attacksfound.append(attack[0])
                attackFound = True
        for attackName in list(set(attacksfound)):
            alarmToAttack.append(newlinestart + [attackName])

        if not attackFound:
            alarmToAttack.append(newlinestart + ["NA"])
    return pd.DataFrame(alarmToAttack,columns=["keyword","time","lp","gp","ingestTimestamp","attackName"])

def attackNamesDF(alarmLinesDF):
    return alarmLinesDF.attackName.unique()

def filterByThresholdDF(alarmLinesDF,threshold):
    return alarmLinesDF[alarmLinesDF["lp"]<=threshold]

def trueAlarmRateDF(alarmLinesDF):
    mal = alarmLinesDF[alarmLinesDF["attackName"] != "NA"]
    nonMal = alarmLinesDF[alarmLinesDF["attackName"] == "NA"]

    tp = mal.groupby(["keyword","time","lp","gp","ingestTimestamp"]).agg(lambda x: x.max).shape[0]
    fp = nonMal.shape[0]

    return (tp,fp)

def fewerAttacksDetected(tp,fp,attacks,initattacks): # A stopping condition
    return (len(initattacks)-len(attacks)) > 0

def findReasonableThreasholdFromDF(alarmLinesDF, initThreshold = 0.1, stepsize = 0.0005, maxsteps = 200):
    newDF = filterByThresholdDF(alarmLinesDF,initThreshold)
    tp,fp = trueAlarmRateDF(newDF)
    attacks = attackNamesDF(newDF)
    newthreshold = initThreshold
    print(newthreshold,tp,fp,attacks)
    for step in range(maxsteps):
        newthreshold = newthreshold - stepsize
        newDF = filterByThresholdDF(newDF,newthreshold)
        ntp,nfp = trueAlarmRateDF(newDF)
        nattacks = attackNamesDF(newDF)

        if fewerAttacksDetected(ntp,nfp,nattacks,attacks): # Try other stopping conditions?
            print(newthreshold+stepsize)
            return (newthreshold + stepsize)
    print(newthreshold)
    return newthreshold

def filterByThreshold(alarmLines,threshold,updateLP=False):
    passingAlarms = []
    for alarm in alarmLines:
        localProb = getLocalProb(alarm) #alarm[1][2][-1][1]
        if localProb <= threshold:
            if updateLP:
                alarm[1][2][-1][1] = localProb
            passingAlarms.append(alarm)
    return passingAlarms

def methodNamesToVec(methodNames,allMethodList):
    return [1 if m in methodNames else 0 for m in allMethodList]

def writeAttackLinesWithDetectionMethodsAndCompCount(ta1,alarmLinesExtByFile,attackLines,matchingFunction,allMethodList):
    allMethodList = sorted(allMethodList)
    attackIndexToMethodMap = dict([(i,[]) for i in range(len(attackLines))])
    for f,alarmLinesExt in alarmLinesExtByFile:
        method = fileNameToTree(f)
        listOfAlarmWords = list(set([k for a in alarmLinesExt for k in a[0]]))
        for i, attack in enumerate(attackLines):
            if any([attackPresentExtracted(attack,alarmExt,matchingFunction) for alarmExt in alarmLinesExt]):
                attackIndexToMethodMap[i].append(method)

    fAttackDetail = "stats/" + ta1 + "_attacks_with_methods.csv"
    print("Writing {}".format(fAttackDetail))
    with open(fAttackDetail,'w') as g:
        g.write("attack_name,date,keywords,comments,methods," + ",".join(allMethodList) + "\n")
        for i,attack in enumerate(attackLines):
            attackLineWithMethods = (attack + [";".join(attackIndexToMethodMap[i])] +
                                     methodNamesToVec(attackIndexToMethodMap[i],allMethodList))
            g.write(",".join(["\"{}\"".format(str(a)) for a in attackLineWithMethods])+"\n")

    # Group by attack_name, count components detected, and gather methods used.
    attackNameToCompCountMap = dict([(a[0],[0,0]) for a in attackLines]) # (name,(detected,total))
    attackNameToMethod = dict([(a[0],set([])) for a in attackLines])
    for idx,attack in enumerate(attackLines):
        attackNameToCompCountMap[attack[0]][1] += 1
        methods = attackIndexToMethodMap[idx]
        if len(methods) > 0:
            attackNameToCompCountMap[attack[0]][0] += 1
            attackNameToMethod[attack[0]] = attackNameToMethod[attack[0]].union(set(methods))


    fAttackRollup = "stats/"+ ta1 + "_attack_component_count.csv"
    print("Writing {}".format(fAttackRollup))
    with open(fAttackRollup,'w') as g:
        header = "attack_name,components_detected,components,methods,"+ ",".join(allMethodList)+"\n"
        g.write(header)
        for attackName in attackNameToCompCountMap:
            detectedComp,totalComp = attackNameToCompCountMap[attackName]
            methodsUsed = list(attackNameToMethod[attackName])
            compLine = ([attackName,detectedComp,totalComp,";".join(methodsUsed)] +
                        [methodNamesToVec(methodsUsed,allMethodList)])
            g.write(",".join(["\"{}\"".format(str(elt)) for elt in compLine])+"\n")
    return "Wahoo! All done!!"

def writeWhenDoAlarmsOccur(ta1,alarmLinesExtByFile,attackLines,matchingFunction,intervalInSec,useIngestTime=False):

    if useIngestTime:
        fileSuffix = "_ingestTime"
        times = [a[-1] for f,alines in alarmLinesExtByFile for a in alines]
        endE3 = max(times)
        startE3 = min(times)
        secondsInE3 = endE3 - startE3
        timeInBounds = lambda x: True
        timeOf = lambda xs: xs[-1]
    else:
        fileSuffix = ""
        secondsInE3 = 12*24*60*60
        startE3 = toEpoch('20180402')
        endE3 = toEpoch('20180414')
        timeInBounds = lambda x: (x>=startE3 and x<=endE3)
        timeOf = lambda xs: round(xs[1]/1000000000) # want time in seconds

    fAlarmTimes = "stats/" + ta1 + "_whenDoAlarmsOccur" + fileSuffix + ".csv"
    with open(fAlarmTimes,'w') as g:
        g.write("method,timeStep,count,isMal\n")
        for f,alarmLinesExt in alarmLinesExtByFile:
            method = fileNameToTree(f)
            malAlarmCntByInterval = defaultdict(int)
            nonMalAlarmCntByInterval = defaultdict(int)

            for alarmExt in alarmLinesExt:
                timeOfAlarm = timeOf(alarmExt)
                timeStep = round((timeOfAlarm-startE3)/intervalInSec)
                if (any([attackPresentExtracted(attack,alarmExt,matchingFunction) for attack in attackLines]) and
                    timeInBounds(timeOfAlarm)):
                    malAlarmCntByInterval[timeStep] += 1
                else:
                    nonMalAlarmCntByInterval[timeStep] += 1

            for timeStep in range(int(secondsInE3/intervalInSec)):
                g.write(",".join([method,str(timeStep),str(malAlarmCntByInterval[timeStep]),"1\n"]))
                g.write(",".join([method,str(timeStep),str(nonMalAlarmCntByInterval[timeStep]),"0\n"]))


    return "\nWriting timeseries info {} complete!".format(fAlarmTimes)

def attackPresentExtracted(attackLine,alarmExt,matchingFunction):
    if matchingFunction(attackLine[2],alarmExt[0]):
        minEpochTime = toEpoch(attackLine[1])
        maxEpochTime = toEpoch(attackLine[1]) + 86400
        alarmTime = round(alarmExt[1]/1000000000)
        return (alarmTime==0 or (alarmTime>=minEpochTime and alarmTime<=maxEpochTime))
    else:
        return False

def alarmExtractedAttackVec(alarmLinesExt,attackLines,matchingFunction):
    attackNames = list(set([a[0] for a in attackLines]))
    attackNames.sort()

    for alarm in alarmLinesExt:
        attackVec = [0 for a in attackNames]
        for attack in attackLines:
            if attackPresentExtracted(attack,alarm,matchingFunction):
                attackVec[attackNames.index(attack[0])] = 1
        alarm.append(attackVec)

    return alarmLinesExt # [keywords,time,localProb,globalProb,attackVec]


def writeLocalProbDistributionCSV(ta1,attackLines,alarmFileNames,matchingFunction=containsKeyword):
    linesToWrite = []
    for fname in alarmFileNames:
        alarmLinesExt = extractFeaturesFromAlarms(getAlarmLines(fname))
        alarmLinesExtWAttacks = alarmExtractedAttackVec(alarmLinesExt,attackLines,matchingFunction)
        tree = fileNameToTree(fname)
        for alarm in alarmLinesExtWAttacks:
            attackVec = alarm[-1]
            linesToWrite.append([tree,
                                "1" if 1 in attackVec else "0",
                                str(round(alarm[2],4))])

    with open("stats/"+ta1+"_lp_dist.csv",'w') as f:
        f.write("method,isMal,lp\n")
        for line in linesToWrite:
            f.write(",".join(list(map(str,line)))+"\n")

    return "Done!"

def writeAlarmTrueFalsePositives(ta1,alarmLinesExtByFile,attackLines,matchingFunction):
    fname = "stats/"+ta1+"_true_false_positives.csv"
    with open(fname,'w') as g:
        g.write("method,tp,fp\n")
        for f,alarmLinesExt in alarmLinesExtByFile:
            tp,fp = trueAlarmRateDF(alarmExtToAttackDF(alarmLinesExt,attackLines,matchingFunction))
            g.write(fileNameToTree(f)+",{},{}\n".format(tp,fp))
    return "Writing true and false positive counts in {} for each method complete.".format(fname)


def fullPathToTreeFile(treeList,directory,suffix):
    return list(map(lambda tree: directory+tree+suffix,treeList))

def fileNameToTree(fileName):
    return fileName.split("/")[-1].split("-")[0]


if __name__ == "__main__":

    allMethods = ['CommunicationPathThroughObject',
                'FileExecuteDelete',
                'FilesExecutedByProcesses',
                'FilesTouchedByProcesses',
                'FilesWrittenThenExecuted',
                'ParentChildProcesses',
                'ProcessDirectoryReadWriteTouches',
                'ProcessFileTouches',
                'ProcessWritesFileSoonAfterNetflowRead',
                'ProcessesChangingPrincipal',
                'ProcessesWithNetworkActivity',
                'SudoIsAsSudoDoes']

    parser = argparse.ArgumentParser(description='E3 Re-run: Methods Evaluated.')
    parser.add_argument('--ta1', default='cadets',
                        choices=['clearscope', 'cadets', 'fivedirections','trace','theia','faros'])
    parser.add_argument('--alarmdir', default='/Users/nls/Desktop/e3/post/alarms/cadets/',
                        help="Provide full path to alarm file directory.")
    parser.add_argument('--attackfilesuffix', default='_attacksFR',
                        help="Examples: \"_attacks\" or \"_attacksFR\"")

    parser.add_argument('--lpdist',action='store_true',default=False,
                        help="Create a file in 'stats' directory containing data on local probability distributions.")
    parser.add_argument('--savefilteredalarms',action='store_true',default=False,
                        help="Filter alarm files and save the results in the alarm file directory.")
    parser.add_argument('--nothresholds',action='store_true',default=False,
                        help="Analyze the set of unfiltered alarms.")
    parser.add_argument('--loadthresholds',action='store_true',default=False,
                        help="Used previously saved thresholds.")
    parser.add_argument('--timeseries',action='store_true',default=False,
                        help="Write out stats related to the timing of alarms. Note that it is possible to use the ingest time or the data time for these analysis.")

    args = parser.parse_args()

    TA1 = args.ta1
    print("\nProcessing {}".format(TA1))
    alarmFileDir = args.alarmdir
    attackFile = "attackFiles/" + TA1 + args.attackfilesuffix + ".csv"

    print("\nChecking for compact json alarm files...")
    compactAlarmFiles(alarmFileDir,map(lambda x: x + "-{}-save_alarm.json".format(TA1),allMethods))

    compactAlarmFileNames = fullPathToTreeFile(allMethods,alarmFileDir,"-{}-save_alarm.json.c".format(TA1))

    if not os.path.exists("stats"):
        print("\nMaking stats directory to store analysis.")
        os.makedirs("stats")


    thresholdMap = dict([(f,1) for f in compactAlarmFileNames]) # Default to no filtering by threshold

    if TA1 == "cadets": # Initial threshold values by TA1 and method
        thresholdTrees = [(0.1,'CommunicationPathThroughObject-cadets-save_alarm.json.c'),
                          (0.1,'FilesTouchedByProcesses-cadets-save_alarm.json.c'),
                          (0.1,'ProcessDirectoryReadWriteTouches-cadets-save_alarm.json.c'),
                          (0.1,'ProcessFileTouches-cadets-save_alarm.json.c'),
                          (0.1,'ProcessWritesFileSoonAfterNetflowRead-cadets-save_alarm.json.c'),
                          (0.1,'ProcessesWithNetworkActivity-cadets-save_alarm.json.c')]

    if TA1 == "fivedirections":
        thresholdTrees = [(0.1,'CommunicationPathThroughObject-fivedirections-save_alarm.json.c'),
                          (0.1,'FilesExecutedByProcesses-fivedirections-save_alarm.json.c'),
                          (0.1,'FilesTouchedByProcesses-fivedirections-save_alarm.json.c'),
                          (0.0001,'ProcessDirectoryReadWriteTouches-fivedirections-save_alarm.json.c'),
                          (0.004,'ProcessFileTouches-fivedirections-save_alarm.json.c'),
                          (0.1,'ProcessWritesFileSoonAfterNetflowRead-fivedirections-save_alarm.json.c')]

    if TA1 == "theia":
        thresholdTrees = [(0.001,'CommunicationPathThroughObject-theia-save_alarm.json.c'),
                          (0.1,'FilesExecutedByProcesses-theia-save_alarm.json.c'),
                          (0.01,'FilesTouchedByProcesses-theia-save_alarm.json.c'),
                          (0.1,'ParentChildProcesses-theia-save_alarm.json.c'),
                          (0.004,'ProcessDirectoryReadWriteTouches-theia-save_alarm.json.c'),
                          (0.1,'ProcessFileTouches-theia-save_alarm.json.c'),
                          (0.1,'ProcessWritesFileSoonAfterNetflowRead-theia-save_alarm.json.c'),
                          (0.1,'ProcessesWithNetworkActivity-theia-save_alarm.json.c'),
                          (0.3,'SudoIsAsSudoDoes-theia-save_alarm.json.c')]

    if TA1 == "clearscope":
        thresholdTrees = [(0.1,'CommunicationPathThroughObject-clearscope-save_alarm.json.c'),
                          (0.001,'FilesTouchedByProcesses-clearscope-save_alarm.json.c'),
                          (0.1,'ProcessDirectoryReadWriteTouches-clearscope-save_alarm.json.c'),
                          (0.0007,'ProcessFileTouches-clearscope-save_alarm.json.c'),
                          (0.0001,'ProcessWritesFileSoonAfterNetflowRead-clearscope-save_alarm.json.c'),
                          (0.001,'ProcessesWithNetworkActivity-clearscope-save_alarm.json.c')]


    attackLines = getAttackLines(attackFile)[1:] # remove header

    if not args.nothresholds:

        if args.loadthresholds:
            with open("stats/"+TA1+"_thresholds.csv",'r') as thresholdfile:
                methodKeys = sorted(list(thresholdMap.keys()))
                thresholds = thresholdfile.readlines()[-1].split(",")
                for idx,key in enumerate(methodKeys):
                    thresholdMap[key] = float(thresholds[idx+1]) # First column is the TA1 name

        else:
            for initT, tree in thresholdTrees:
                fname = alarmFileDir + tree
                alarmLines = getAlarmLines(fname)
                aext = extractFeaturesFromAlarms(alarmLines)
                df = alarmExtToAttackDF(aext,attackLines,containsKeyword)
                thresholdMap[fname] = findReasonableThreasholdFromDF(df,initThreshold=initT)

            with open("stats/"+TA1+"_thresholds.csv",'a') as ft:
                methods = sorted(list(thresholdMap.keys()))
                print(",".join([str(fileNameToTree(m)) for m in methods]))
                ft.write(TA1+","+",".join([str(thresholdMap[m]) for m in methods]) + "\n")


    if args.savefilteredalarms:
        alarmLinesByFile = ((f,filterByThreshold(getAlarmLines(f),thresholdMap[f],updateLP=True))
                            for f in compactAlarmFileNames)
        print()
        for f,alarmsFiltered in alarmLinesByFile:
            filteredAlarmFileName = alarmFileDir+fileNameToTree(f)+"-"+TA1+"-filtered_alarm.json"
            with open(filteredAlarmFileName,"w") as g:
                print("Writing {}".format(filteredAlarmFileName))
                g.write(json.dumps(alarmsFiltered))

    else:

        if args.lpdist:
            print(writeLocalProbDistributionCSV(TA1,attackLines,compactAlarmFileNames))

        elif args.timeseries:
            alarmLinesExtByFile = [(f,extractFeaturesFromAlarms(filterByThreshold(getAlarmLines(f),thresholdMap[f])))
                                   for f in compactAlarmFileNames]

            print(writeWhenDoAlarmsOccur(TA1,alarmLinesExtByFile,attackLines,containsKeyword,1800))
            print(writeWhenDoAlarmsOccur(TA1,alarmLinesExtByFile,attackLines,containsKeyword,1800,useIngestTime=True))

        else:

            alarmLinesExtByFile = [(f,extractFeaturesFromAlarms(filterByThreshold(getAlarmLines(f),thresholdMap[f])))
                                   for f in compactAlarmFileNames]


            print(writeAlarmTrueFalsePositives(TA1,alarmLinesExtByFile,attackLines,containsKeyword))

            print(writeAttackLinesWithDetectionMethodsAndCompCount(TA1,alarmLinesExtByFile,attackLines,containsKeyword,allMethods))

