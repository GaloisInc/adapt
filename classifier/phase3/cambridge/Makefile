
#
# Typical usage, faster than "make clean all",
# is to start with the segmentation phase:
#
#     make clean && touch /tmp/1_trace_loaded.txt && time make
#


all: check_daemons /tmp/3_classified.txt


check_daemons:
        # pgrep verifies the start_daemons.sh precondition, or bails out early.
	pgrep supervisord > /dev/null


RSYNC = ~/adapt/trace/trace_rsync.sh

~/adapt/trace/current/ta5attack2_units.avro:
	$(RSYNC)

%.bin:
	$(RSYNC)

%.avro:
	$(RSYNC)


# Keep track in the filesystem of whether ingestd has run yet.
/tmp/1_trace_loaded.txt: ~/adapt/trace/current/ta5attack2_units.avro
	ls -Hl $<; du -Dh $<
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	Trint -p $< | tee $@
	dstat -cdg 1 15
	csh -c 'time sync'


/tmp/2_segmented.txt: /tmp/1_trace_loaded.txt
	@echo '~/adapt/tools/label_count.py'
	csh -c 'time ~/adapt/classifier/phase3/simple_segments_by_pid.py --debug --drop'
	touch $@


/tmp/3_classified.txt: /tmp/2_segmented.txt
	csh -c 'time ../fg_classifier.py'
	touch $@


/tmp/test_interpreter.py:
	cp -p ~/adapt/tests/test_interpreter.py /tmp
	cat test_interpreter_seg_patch.txt | (cd /tmp && patch -p2)
	@egrep -n 'cmd = [^o]' $@

clean:
	bash -c 'rm -f /tmp/{1_trace_loaded,2_segmented,3_classified}.txt'
