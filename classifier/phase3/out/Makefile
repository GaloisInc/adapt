
OUT = \
 5d_youtube_ie_output.bin.txt \
 apt1.bin.txt \
 cameragrab1.bin.txt \
 gather.avro.bin.txt \
 imagegrab.bin.txt \
 imagegrab2.bin.txt \
 infoleak_small_units.avro.txt \
 recordaudio1.bin.txt \
 recorder.bin.txt \
 screengrab.bin.txt \
 screengrab1.bin.txt \
 ta5attack1_units.avro.txt \
 ta5attack2_units.avro.txt \
 youtube_ie_update.bin.txt \

all: $(OUT)

CFG = ~/adapt/config/supervisord.conf

%.txt:
	@! egrep '^command=.*/segmenter/segmentd.py' < $(CFG)  # Remove from config, please.
	@! egrep '^command=.*/ad/test/StartAD.py'    < $(CFG)  # Remove from config, please.
	@echo; cd ../out  # Verify we're in the right place.
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	Trint -p ~/adapt/trace/current/$(subst .txt,,$@) > $@
	sleep 10; Trint -f; sleep 10
	bash -c "echo reading $@ >> /tmp/ingestd-stderr---supervisor-*.log"
	time ~/adapt/classifier/phase3/simple_segments_by_pid.py >> $@
	time ~/adapt/classifier/phase3/fg_classifier.py >> $@
	(~/adapt/tools/label_count.py --with-edges; echo) >> $@
	../report.py --all >> $@

clean:
	rm -f $(OUT)
