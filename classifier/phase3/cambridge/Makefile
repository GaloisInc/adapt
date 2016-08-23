
#
# Typical usage, faster than "make clean all",
# is to start with the segmentation phase:
#
#     make clean && touch /tmp/1_trace_loaded.txt && time make
# or
#     make TRACE=5d_youtube_ie_output.bin
#

TRACE ?= ta5attack2_units.avro

SHELL=bash -o pipefail


all: check_daemons /tmp/3_classified.txt


check_daemons:
        # pgrep verifies the start_daemons.sh precondition, or bails out early.
	pgrep supervisord > /dev/null

# Don't let zero status from tee mask an earlier failure:
SHELL = /bin/bash -o pipefail

SDIR = ~/adapt/segment/segmenter
SEG = ./adapt_segmenter.py --broker ws://localhost:8182/ --criterion pid --radius 2 --store-segment Yes --spec ~/adapt/config/segmentByPID.json --log-to-kafka --kafka localhost:9092

RSYNC = ~/adapt/trace/trace_rsync.sh

~/adapt/trace/current/ta5attack2_units.avro:
	$(RSYNC)

%.bin:
	$(RSYNC)

%.avro:
	$(RSYNC)


# Keep track in the filesystem of whether ingestd has run yet.
/tmp/1_trace_loaded.txt: ~/adapt/trace/current/$(TRACE)
	ls -Hl $<; du -Dh $<
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	Trint -fp $< | tee $@
	dstat -cdg 1 15
	csh -c 'time sync'


/tmp/2_segmented.txt: /tmp/1_trace_loaded.txt
	@echo '~/adapt/tools/label_count.py'
	time ~/adapt/tools/delete_segments.py
	cd $(SDIR) && csh -c 'time $(SEG)' 2>&1 | tee $@


/tmp/3_classified.txt: /tmp/2_segmented.txt
	csh -c 'time ../fg_classifier.py --drop' 2>&1 | tee $@


/tmp/tests.json:
	cd /tmp && ln -s ~/adapt/tests/tests.json

/tmp/test_interpreter.py: /tmp/tests.json
	cp -p ~/adapt/tests/test_interpreter.py $@
	cat test_interpreter_seg_patch.txt | (cd /tmp && patch -p2)
	@egrep -n 'cmd = [^o]' $@  # Use the alternate segmenter on big traces.


%.png: %.dot
	dot -Tpng -o $@ $<

%.pdf: %.dot
	dot -Tpdf -o $@ $<


clean:
	bash -c 'rm -f /tmp/{1_trace_loaded,2_segmented,3_classified}.txt'
