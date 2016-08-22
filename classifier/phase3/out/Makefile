
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
 simple_with_marker_3.avro.txt \
 ta5attack1_units.avro.txt \
 ta5attack2_units.avro.txt \
 youtube_ie_update.bin.txt \

all: $(OUT)

# In e.g. 5D's imagegrab In discards EDGE_OBJECT_PREV_VERSION
# & EDGE_EVENT_AFFECTS_REGISTRYKEY (PART_OF_PATTERN).

CFG = ~/adapt/config/supervisord.conf
SEG = ./adapt_segmenter.py --broker ws://localhost:8182/ --criterion pid --radius 2 --store-segment Yes --spec ~/adapt/config/segmentByPID.json --log-to-kafka --kafka localhost:9092
SDIR = ~/adapt/segment/segmenter

%.txt:
	@! egrep '^command=.*/segmenter/segmentd.py' < $(CFG)  # Remove from config, please.
	@! egrep '^command=.*/ad/test/StartAD.py'    < $(CFG)  # Remove from config, please.
	@echo; cd ../out  # Verify we're in the right place.
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	test -r  ~/adapt/trace/current/$(subst .txt,,$@) || ~/adapt/trace/trace_rsync.sh
	Trint -p ~/adapt/trace/current/$(subst .txt,,$@) > $@
	sleep 30; Trint -f; sleep 30; Trint -F; sleep 30
	bash -c "echo reading $@ >> /tmp/ingestd-stderr---supervisor-*.log"
	(cd $(SDIR) && time $(SEG) 2>&1; echo) >> $@
	(time ~/adapt/classifier/phase3/fg_classifier.py 2>&1; echo) >> $@
	(~/adapt/tools/label_count.py --with-edges; echo) >> $@
	../report.py --all >> $@

test:
	~/adapt/tests/test_interpreter.py \
            -m ac \
            -t ../marker/tests.json \
            -d ~/adapt/trace/current/simple_with_marker_3.avro

clean:
	rm -f $(OUT)
