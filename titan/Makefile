
all: bad_ls.txt

test: generic_test bad_ls_test

# These may take a little while to run, roughly 20 sec if gremlin's up.
big: report classify

ADAPT = $(HOME)/adapt
TRACE = $(ADAPT)/trace/current
CFG = $(ADAPT)/config/supervisord.conf
EDGES = /tmp/add_edges.txt

build_trint:
	make -C $(ADAPT)/ingest

# Insert nodes into Titan, tracked in the FS to avoid re-inserting.
# Throughput is 140 node/sec (more than two minutes for 19k nodes).
infoleak.txt:
	Trint -u -s ~/example/infoleak-small.provn  2> $(EDGES) | egrep -v ', src = ResultId' | tee $@
	./coverage.sh node_type_report.py
	./coverage.sh classify_subgraph.py

clean:
	rm -f infoleak.txt $(EDGES)

TAGS: *.py
	etags `find . -name '*.py' | sort`
	flake8 classify_subgraph.py node_type_report.py */[a-z]*.py tests/*/*.py

# This completes within two minutes.
report: bad_ls.txt
	./node_type_report.py

classify: bad_ls.txt
	./classify_subgraph.py


# Without filtering, Trint would not process the bad_ls trace.
#   Error ingesting /home/vagrant/adapt/trace/current/bad-ls.provn:
#   Translation error: (1911:9-1911:227)WasGeneratedBy: Unknown operation: ftruncate

# This completes within eight minutes.
bad_ls.txt: infoleak.txt
	jps | sort -k2 | egrep -v Jps | column -t
	pstree -u | egrep 'java|supervisord'
	netstat -tan | egrep ':(8182|2181|50321|57569|9092).*LISTEN' | sort
	pgrep supervisord > /dev/null
	egrep -v 'ftruncate|recvfrom' < $(TRACE)/bad-ls-small-scenario.provn > /tmp/t.provn
	Trint -u -s /tmp/t.provn | tee $@
        # This should report 27k nodes.
	curl -s -X POST -d "{\"gremlin\" : \"g.V().count()\" }" http://localhost:8182 | jq .result.data

NOSE = nosetests3 --with-doctest

generic_test: infoleak.txt
	-$(NOSE) tests/generic/infoleak_smoke_test.py

bad_ls_test: bad_ls.txt
	$(NOSE) tests/bad_ls/$@.py
