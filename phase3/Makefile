
#
# Typical usage, faster than "make clean all",
# is to start with the segmentation phase:
#
#     make clean && touch /tmp/1_trace_loaded.txt && time make
#


all: check_daemons TAGS /tmp/3_classified.txt


check_daemons:
        # pgrep verifies the start_daemons.sh precondition, or bails out early.
	pgrep supervisord > /dev/null


TAGS: *.py */*.py
	etags *.py */*.py ../../../adapt/tools/*/[a-z]*.py


RSYNC = ~/adapt/trace/trace_rsync.sh

~/adapt/trace/current/5d_youtube_ie_output.bin:
	$(RSYNC)

~/adapt/trace/current/infoleak_small_units.avro:
	$(RSYNC)

~/adapt/trace/current/ta5attack1_units.avro:
	$(RSYNC)

~/adapt/trace/current/gather.avro.bin:
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
	time ~/adapt/tools/await_completion.py se


/tmp/2_segmented.txt: /tmp/1_trace_loaded.txt
	time ~/adapt/tools/label_count.py
	./report.py --report=netflow | sort -nk2 | awk '$$2 >= 1'
# Uncomment exactly one of these two commands, to use background or foreground segmenter.
# time ~/adapt/tools/await_completion.py ac
	~/adapt/segment/segmenter/simple_segmenter.py --k 30 --drop | time tee $@


/tmp/3_classified.txt: /tmp/2_segmented.txt
	nosetests3 --with-doctest --doctest-tests test_precondition.py classify/*.py
	./fg_classifier.py
	nosetests3 --with-doctest --doctest-tests test_postcondition.py
	touch $@


tests:
	nosetests3 --with-doctest --doctest-tests test_postcondition.py classify/*.py
	flake8 --ignore=E402 *.py */[a-z]*.py


clean:
	bash -c 'rm -f TAGS /tmp/{1_trace_loaded,2_segmented,3_classified}.txt'
