
all: test

ADAPT = $(HOME)/adapt
CFG = $(ADAPT)/config/supervisord.conf

# Insert nodes into Titan, tracked in the FS to avoid re-inserting.
infoleak.txt:
	pgrep supervisord > /dev/null && supervisord -c $(CFG)
	Trint -u -s $(ADAPT)/infoleak.provn | tee $@

test: infoleak.txt
	flake8 classify_subgraph.py node_type_report.py
	coverage erase
	coverage run node_type_report.py
	coverage report --include=node_type_report.py -m
