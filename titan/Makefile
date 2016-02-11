
all: bad_ls.txt

test: generic_test bad_ls_test

# These may take a little while to run, roughly 20 sec if gremlin's up.
big: report classify

ADAPT = $(HOME)/adapt
TRACE = $(ADAPT)/trace/current
CFG = $(ADAPT)/config/supervisord.conf
EDGES = /tmp/add_edges.txt

# Hack: pause a few seconds while daemons startup or shutdown.
WAIT = 5

# Insert nodes into Titan, tracked in the FS to avoid re-inserting.
# The "sleep" accommodates supervisord's odd habit of exiting with status 1.
infoleak.txt:
	cd $(ADAPT)/ingest && make
	pgrep supervisord > /dev/null || (cd $(ADAPT) && supervisord -c $(CFG); sleep $(WAIT))
	pstree -u | egrep 'java|supervisord'
	netstat -tan | egrep ':(8182|2181|50321|57569|9092).*LISTEN' | sort
	Trint -u -s $(ADAPT)/infoleak.provn  2> $(EDGES) | egrep -v ', src = ResultId' | tee $@
	./coverage.sh node_type_report.py
	./coverage.sh classify_subgraph.py


clean:
	rm -f infoleak.txt $(EDGES)
	-killall supervisord; sleep $(WAIT)

TAGS: *.py
	etags `find . -name '*.py' | sort`
	flake8 classify_subgraph.py node_type_report.py */[a-z]*.py tests/*/*.py


# This completes within two minutes.
report: bad_ls.txt
	./node_type_report.py

classify: bad_ls.txt
	./classify_subgraph.py

# This completes within eight minutes.
bad_ls.txt: infoleak.txt
	pstree -u | egrep 'java|supervisord'
	netstat -tan | egrep ':(8182|2181|50321|57569|9092).*LISTEN' | sort
	pgrep supervisord > /dev/null
	make -C ../ingest
	test `stat -L -c%s $(TRACE)/bad-ls.provn` = 4897610
	test `sha256sum <  $(TRACE)/bad-ls.provn | cut -c1-7` = 696215a
	Trint -u -s $(TRACE)/bad-ls.provn  2> $(EDGES) \
          | egrep -v 'Unrecognized attribute for UnitOfExecution: http://spade.csl.sri.com/#:(commandLine|cwd|group|pid|ppid|programName|source|uid)' \
          | egrep -v 'Unrecognized attribute for artifact: http://spade.csl.sri.com/#:(entityType|hasVersion|path)' \
          | cat -s \
          | tee $@ \
        # This should report eight thousand nodes.
	curl -s -X POST -d "{\"gremlin\" : \"g.V().count()\" }" http://localhost:8182 | jq .result.data

NOSE = nosetests3 --with-doctest

generic_test: infoleak.txt
	-$(NOSE) tests/generic/infoleak_smoke_test.py

bad_ls_test: bad_ls.txt
	$(NOSE) tests/bad_ls/$@.py
