
all: report classify

ADAPT = $(HOME)/adapt
CFG = $(ADAPT)/config/supervisord.conf
EDGES = /tmp/add_edges.txt

# Hack: pause a few seconds while daemons startup or shutdown.
WAIT = 5

# Insert nodes into Titan, tracked in the FS to avoid re-inserting.
# The "sleep" accommodates supervisord's odd habit of exiting with status 1.
infoleak.txt:
	flake8 classify_subgraph.py node_type_report.py
	pgrep supervisord > /dev/null || cd $(ADAPT) && supervisord -c $(CFG); sleep $(WAIT)
	pstree -u | egrep 'java|supervisord'
	netstat -tan | egrep ':(8182|2181|50321|57569|9092).*LISTEN' | sort
	Trint -u -s $(ADAPT)/infoleak.provn  2> $(EDGES) | egrep -v ', src = ResultId' | tee $@

PROG1 = node_type_report.py
report: infoleak.txt
	coverage erase
	coverage run $(PROG1) | tail -15
	coverage report --include=$(PROG1) -m

PROG2 = classify_subgraph.py
classify: infoleak.txt
	coverage erase
	coverage run $(PROG2)
	coverage report --include=$(PROG2) -m

clean:
	rm -f infoleak.txt $(EDGES)
	-killall supervisord; sleep $(WAIT)
