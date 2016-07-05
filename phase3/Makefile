
all: check_daemons /tmp/classified.txt


check_daemons:
        # pgrep verifies the start_daemons.sh precondition, or bails out early.
	pgrep supervisord > /dev/null


~/adapt/trace/current/5d_youtube_ie_output.bin:
	~/adapt/trace/trace_rsync.sh


# Keep track in the filesystem of whether ingestd has run yet.
/tmp/trace_loaded.txt: ~/adapt/trace/current/5d_youtube_ie_output.bin
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	~/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	Trint -p $< | tee $@
	time ~/adapt/tools/await_completion.py se


/tmp/segmented.txt: /tmp/trace_loaded.txt
	time ~/adapt/tools/label_histogram.py
	./query.py --query "g.V().has(label, 'Entity-NetFlow').limit(5000)" | sort -nk2 | awk '$$2 >= 10'
# Uncomment exactly one of these two commands, to use background or foreground segmenter.
# time ~/adapt/tools/await_completion.py ac
	~/adapt/segment/segmenter/simple_segmenter.py --k 90 --drop | time tee $@


/tmp/classified.txt: /tmp/segmented.txt
	nosetests3 --with-doctest --doctest-tests test_precondition.py classify/*.py
	./fg_classifier.py
	nosetests3 --with-doctest --doctest-tests test_postcondition.py
	touch $@


tests:
	nosetests3 --with-doctest --doctest-tests test_postcondition.py classify/*.py
	flake8 --ignore=E402 *.py */[a-z]*.py


clean:
	bash -c 'rm -f /tmp/{trace_loaded,segmented,classified}.txt'
