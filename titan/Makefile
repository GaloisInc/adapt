
all: generic_test bad_ls_test

# These may take a little while to run.
big: report classify

ADAPT = $(HOME)/adapt
CFG = $(ADAPT)/config/supervisord.conf
EDGES = /tmp/add_edges.txt

# Hack: pause a few seconds while daemons startup or shutdown.
WAIT = 5

# Insert nodes into Titan, tracked in the FS to avoid re-inserting.
# The "sleep" accommodates supervisord's odd habit of exiting with status 1.
infoleak.txt:
	cd $(ADAPT)/ingest && make
	pgrep supervisord > /dev/null || cd $(ADAPT) && supervisord -c $(CFG); sleep $(WAIT)
	pstree -u | egrep 'java|supervisord'
	netstat -tan | egrep ':(8182|2181|50321|57569|9092).*LISTEN' | sort
	Trint -u -s $(ADAPT)/infoleak.provn  2> $(EDGES) | egrep -v ', src = ResultId' | tee $@

report: infoleak.txt
	./coverage.sh node_type_report.py

classify: infoleak.txt
	./coverage.sh classify_subgraph.py


clean:
	rm -f infoleak.txt $(EDGES)
	-killall supervisord; sleep $(WAIT)

TAGS:
	etags `find . -name '*.py' | sort`
	flake8 classify_subgraph.py node_type_report.py tests/*/*.py


bad-ls.txt:
	pstree -u | egrep 'java|supervisord'
	netstat -tan | egrep ':(8182|2181|50321|57569|9092).*LISTEN' | sort
	Trint -u -s $(ADAPT)/trace/bad-ls.provn  2> $(EDGES) \
          | egrep -v 'Unrecognized attribute for UnitOfExecution: http://spade.csl.sri.com/#:(commandLine|cwd|group|pid|ppid|programName|source|uid)' \
          | egrep -v 'Unrecognized attribute for artifact: http://spade.csl.sri.com/#:(entityType|hasVersion|path)' \
          | cat -s \
          | tee $@ \
        # This should report eight thousand nodes.
	curl -s -X POST -d "{\"gremlin\" : \"g.V().count()\" }" http://localhost:8182 | jq .result.data

NOSE = nosetests3 --with-doctest

generic_test: infoleak.txt
	-$(NOSE) tests/generic/infoleak_smoke_test.py

bad_ls_test: bad-ls.txt
	$(NOSE) tests/bad_ls/$@.py
