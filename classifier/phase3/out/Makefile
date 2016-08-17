
OUT = \
 5d_youtube_ie_output.bin.txt \
 apt1.bin.txt \
 cameragrab1.bin.txt \
 download_with_wget.cdm.bin.txt \
 gather.avro.bin.txt \
 imagegrab.bin.txt \
 imagegrab2.bin.txt \
 infoleak_small_units.avro.txt \
 recordaudio1.bin.txt \
 recorder.bin.txt \
 remove_directory.cdm.bin.txt \
 remove_file.cdm.bin.txt \
 screengrab.bin.txt \
 screengrab1.bin.txt \
 ta5attack1_units.avro.txt \
 ta5attack2_units.avro.txt \
 youtube_ie_update.bin.txt \

all: $(OUT)

CFG = ~/adapt/config/supervisord.conf
SEG = ./adapt_segmenter.py --broker ws://localhost:8182/ --criterion pid --radius 2 --store-segment Yes --spec ~/adapt/config/segmentByPID.json --log-to-kafka --kafka localhost:9092
SDIR = ~/adapt/segment/segmenter

%.txt:
	@! egrep '^command=.*/segmenter/segmentd.py' < $(CFG)  # Remove from config, please.
	@! egrep '^command=.*/ad/test/StartAD.py'    < $(CFG)  # Remove from config, please.
	@echo; cd ../out  # Verify we're in the right place.
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	Trint -p ~/adapt/trace/current/$(subst .txt,,$@) > $@
	sleep 20; Trint -F; sleep 20
	bash -c "echo reading $@ >> /tmp/ingestd-stderr---supervisor-*.log"
	(cd $(SDIR) && time $(SEG) 2>&1; echo) >> $@
	(time ~/adapt/classifier/phase3/fg_classifier.py 2>&1; echo) >> $@
	(~/adapt/tools/label_count.py --with-edges; echo) >> $@
	../report.py --all >> $@

clean:
	rm -f $(OUT)
