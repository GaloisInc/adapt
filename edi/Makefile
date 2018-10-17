
all: extract avfbatch

contexts = ProcessEvent ProcessExec ProcessParent ProcessChild ProcessNetflow

extract: $(foreach context,$(contexts), contexts/$(context).csv)

avfbatch: $(foreach context,$(contexts), results/$(context).avf.batch.scored.csv)

avfstream: $(foreach context,$(contexts), results/$(context).avf.stream.scored.csv)

avcbatch: $(foreach context,$(contexts), results/$(context).avc.batch.scored.csv)

avcstream: $(foreach context,$(contexts), results/$(context).avc.stream.scored.csv)

raspbatch: $(foreach context,$(contexts), results/$(context).rasp.batch.scored.csv)

raspstream: $(foreach context,$(contexts), results/$(context).rasp.stream.scored.csv)

klmeansbatch: $(foreach context,$(contexts), results/$(context).klmeans.batch.scored.csv)
klmeansstream: $(foreach context,$(contexts), results/$(context).klmeans.stream.scored.csv)

contexts/%.csv: specifications/%.json
	python3 extract.py -i specifications/$*.json -o contexts/$*.csv

results/%.avf.batch.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avf.batch.scored.csv -s avf

results/%.avc.batch.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avc.batch.scored.csv -s avc

results/%.rasp.batch.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.rasp.batch.scored.csv -s rasp

results/%.klmeans.batch.scored.csv: contexts/%.csv
	python3 klmeans.py -n 5 -k 10 -i contexts/$*.csv -o results/$*.klmeans.batch.scored.csv

results/%.avf.stream.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avf.stream.scored.csv -s avf -m stream

results/%.avc.stream.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.avc.stream.scored.csv -s avc -m stream

results/%.rasp.stream.scored.csv: contexts/%.csv
	python3 ad.py -i contexts/$*.csv -o results/$*.rasp.stream.scored.csv -s rasp -m stream

results/%.klmeans.stream.scored.csv: contexts/%.csv
	python3 klmeans.py -m stream -i contexts/$*.csv -o results/$*.klmeans.stream.scored.csv


clean:
	rm -rf test/*ranked.csv test/*scored.csv results/*.ranked.csv results/*.scored.csv

spotless: clean
	rm -rf contexts/*.csv
