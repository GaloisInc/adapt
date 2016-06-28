
all: check_daemons /tmp/classified.txt


check_daemons:
        # pgrep verifies the start_daemons.sh precondition, or bails out early.
	pgrep supervisord > /dev/null


$(HOME)/adapt/trace/current/5d_youtube_ie_output.bin:
	$(HOME)/adapt/trace/trace_rsync.sh


# Keep track in the filesystem of whether ingestd has run yet.
/tmp/trace_loaded.txt: $(HOME)/adapt/trace/current/5d_youtube_ie_output.bin
	killall ingestd; sleep 1  # Background writers can interfere with drop.
	$(HOME)/adapt/tools/delete_nodes.py  # During drop supervisor respawns.
	Trint -p $< | tee $@
	time $(HOME)/adapt/tools/await_completion.py se


/tmp/segmented.txt: /tmp/trace_loaded.txt
	time ~/adapt/tools/label_histogram.py
	./query.py --query "g.V().has(label, 'Entity-NetFlow').limit(5000)" | sort -nk2 | awk '$$2 >= 10'
	time $(HOME)/adapt/segment/segmenter/simple_segmenter.py --k 3 --drop | tee $@
	time $(HOME)/adapt/tools/await_completion.py ac


/tmp/classified.txt: /tmp/segmented.txt
	true


clean:
	bash -c 'rm -f /tmp/{trace_loaded,segmented,classified}.txt'
