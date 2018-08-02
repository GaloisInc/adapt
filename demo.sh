#!/bin/bash
trash ./persistence-multimap_by_event.db
sbt -mem 12000 -Dadapt.runflow=quine -Dadapt.ingest.loadfiles.0=/Users/ryan/Documents/Projects/Adapt\ \(Transparent\ Computing\)/Engagement\ 2/e2data-bovia/ta1-cadets-bovia-cdm17.bin -Dadapt.ingest.loadlimit=10000 run

