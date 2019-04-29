ifndef URL
  URL=http://localhost
endif

ifndef PORT
  PORT=8080
endif

ifndef DATABASE
  DATABASE=adapt
endif

all: extract avfbatch

e5test: extract klmeansfastbatchdescribe rarebatchdescribe fcabatchdescribe

e5: extract klmeansfastbatchdescribe krimpbatchdescribe rarebatchdescribe fcabatchdescribe

basecontexts = ProcessEvent ProcessExec ProcessParent ProcessNetflow

contexts = $(basecontexts) ProcessUniform ProcessMax ProcessCombined  ProcessGeometric ProcessAverage ProcessMin ProcessMedian

extract: $(foreach context,$(basecontexts), contexts/$(context).csv)
dat: $(foreach context,$(contexts), contexts/$(context).dat)
mat: $(foreach context,$(contexts), contexts/$(context).mat)

avfbatch: $(foreach context,$(contexts), results/$(context).avf.batch.scored.csv)

avfstream: $(foreach context,$(contexts), results/$(context).avf.stream.scored.csv)

avcbatch: $(foreach context,$(contexts), results/$(context).avc.batch.scored.csv)

avcstream: $(foreach context,$(contexts), results/$(context).avc.stream.scored.csv)

raspbatch: $(foreach context,$(contexts), results/$(context).rasp.batch.scored.csv)

raspstream: $(foreach context,$(contexts), results/$(context).rasp.stream.scored.csv)

klmeansbatch: $(foreach context,$(contexts), results/$(context).klmeans.batch.scored.csv)
klmeansfastbatch: $(foreach context,$(contexts), results/$(context).klmeans.fastbatch.scored.csv)

klmeansfastbatchdescribe: results/ProcessCombined.klmeans.fastbatch.described.csv

klmeansstream: $(foreach context,$(contexts), results/$(context).klmeans.stream.scored.csv)

krimpbatch: $(foreach context,$(contexts), results/$(context).krimp.batch.scored.csv)

krimpbatchdescribe:  results/ProcessCombined.krimp.batch.described.csv

krimpstar: $(foreach context,$(contexts), results/$(context).krimpstar.batch.scored.csv)

rarebatch: $(foreach context,$(contexts), results/$(context).rare.batch.scored.csv)

rarebatchdescribe: results/ProcessMax.rare.batch.described.csv

fcabatch: $(foreach context,$(contexts), results/$(context).fca.batch.scored.csv)

fcabatchdescribe: results/ProcessGeometric.fca.batch.described.csv

contexts/ProcessTimes.csv:
	python3 times.py --url $(URL) --port $(PORT) --database $(DATABASE) $(PROVIDER) -o contexts/ProcessTimes.csv

contexts/%.csv: specifications/%.json contexts/ProcessTimes.csv
	python3 extract.py --url $(URL) --port $(PORT) --database $(DATABASE) $(PROVIDER) -i specifications/$*.json -o contexts/$*Raw.csv
	python3 timesort.py -i contexts/$*Raw.csv -t contexts/ProcessTimes.csv -o contexts/$*.csv

contexts/%.dat: contexts/%.csv
	python3 convert.py -i contexts/$*.csv -o contexts/$*.dat

results/ProcessGeometric.avf.%.scored.csv: results/ProcessEvent.avf.%.scored.csv results/ProcessExec.avf.%.scored.csv results/ProcessParent.avf.%.scored.csv results/ProcessNetflow.avf.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a geometric -o results/ProcessGeometric.avf.$*.scored.csv

results/ProcessAverage.avf.%.scored.csv: results/ProcessEvent.avf.%.scored.csv results/ProcessExec.avf.%.scored.csv results/ProcessParent.avf.%.scored.csv results/ProcessNetflow.avf.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a average -o results/ProcessAverage.avf.$*.scored.csv

results/ProcessMin.avf.%.scored.csv: results/ProcessEvent.avf.%.scored.csv results/ProcessExec.avf.%.scored.csv results/ProcessParent.avf.%.scored.csv results/ProcessNetflow.avf.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a minrank -o results/ProcessMin.avf.$*.scored.csv

results/ProcessMedian.avf.%.scored.csv: results/ProcessEvent.avf.%.scored.csv results/ProcessExec.avf.%.scored.csv results/ProcessParent.avf.%.scored.csv results/ProcessNetflow.avf.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a median -o results/ProcessMedian.avf.$*.scored.csv

results/ProcessGeometric.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -r -a geometric -o results/ProcessGeometric.$*.scored.csv

results/ProcessAverage.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a average -o results/ProcessAverage.$*.scored.csv -r

results/ProcessMin.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a minrank -o results/ProcessMin.$*.scored.csv -r

results/ProcessMedian.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv  -a median -o results/ProcessMedian.$*.scored.csv -r

results/ProcessUniform.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a uniform -o results/ProcessUniform.$*.scored.csv

results/ProcessCombined.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a weighted -o results/ProcessCombined.$*.scored.csv

results/ProcessMax.%.scored.csv: results/ProcessEvent.%.scored.csv results/ProcessExec.%.scored.csv results/ProcessParent.%.scored.csv results/ProcessNetflow.%.scored.csv
	python3 combine.py $^ -t contexts/ProcessTimes.csv -a maxscore -o results/ProcessMax.$*.scored.csv

contexts/%.mat: contexts/%.csv
	python3 convertmat.py -i contexts/$*.csv -o contexts/$*.mat

results/%.avf.batch.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avf.batch.scored.csv -s avf


results/%.avc.batch.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avc.batch.scored.csv -s avc

results/%.rasp.batch.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.rasp.batch.scored.csv -s rasp

results/%.klmeans.batch.scored.csv: contexts/%.csv
	python3 klmeans.py -k 10 -i contexts/$*.csv -o results/$*.klmeans.batch.scored.csv

results/%.klmeans.fastbatch.scored.csv: contexts/%.csv
	python3 klmeans.py -k 20 -i contexts/$*.csv -o results/$*.klmeans.fastbatch.scored.csv -m fastbatch

results/%.krimp.batch.scored.csv: contexts/%.csv
	python3 krimp.py -i contexts/$*.csv -o results/$*.krimp.batch.scored.csv

results/%.krimpstar.batch.scored.csv: contexts/%.csv
	python3 krimpstar.py -i contexts/$*.csv -o results/$*.krimpstar.batch.scored.csv -k 10

results/%.rare.batch.scored.csv: contexts/%.csv
	python3 rare.py -i contexts/$*.csv -o results/$*.rare.batch.scored.csv -s 0.05

results/%.fca.batch.scored.csv: contexts/%.csv
	python3 fca.py -i contexts/$*.csv -o results/$*.fca.batch.scored.csv -s 42 -c 70 -m conf

results/%.avf.stream.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avf.stream.scored.csv -s avf -m stream

results/%.avc.stream.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avc.stream.scored.csv -s avc -m stream

results/%.rasp.stream.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.rasp.stream.scored.csv -s rasp -m stream

results/%.klmeans.stream.scored.csv: contexts/%.csv
	python3 klmeans.py -m stream -i contexts/$*.csv -o results/$*.klmeans.stream.scored.csv

results/ProcessCombined.%.described.csv: results/ProcessCombined.%.scored.csv
	python3 describe.py --url $(URL) --port $(PORT) --database $(DATABASE) -i results/ProcessCombined.$*.scored.csv -o results/ProcessCombined.$*.described.csv -r


results/ProcessGeometric.%.described.csv: results/ProcessGeometric.%.scored.csv
	python3 describe.py --url $(URL) --port $(PORT) --database $(DATABASE) -i results/ProcessGeometric.$*.scored.csv -o results/ProcessGeometric.$*.described.csv

results/ProcessMax.%.described.csv: results/ProcessMax.%.scored.csv
	python3 describe.py --url $(URL) --port $(PORT) --database $(DATABASE) -i results/ProcessMax.$*.scored.csv -o results/ProcessMax.$*.described.csv -r


clean:
	rm -rf test/*ranked.csv test/*scored.csv results/*.ranked.csv results/*.scored.csv results/*.described.csv

spotless: clean
	rm -rf contexts/*.csv contexts/*.dat contexts/*.mat contexts/*.rcf
