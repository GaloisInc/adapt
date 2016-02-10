
OUT = aide_ingest.log

all: $(OUT)

DB = /var/lib/aide/aide.db

# A nightly cron job scans filesystems to update aide.db.
aide_ingest.log: $(DB)
	./aide_ingest.py --input-file $<  > $@

# Ignoring deeply nested directories reduces 26k node to a more
# manageable one thousand, which gremlin can easily store in RAM.
count_dirs:
	zless < $(DB) | awk '$$4 ~ /^4/ {print $$4, $$1}' | awk -F/ 'length($$5) == 0' | cat -n | tail
	zless < $(DB) | awk '$$4 ~ /^4/ {print $$4, $$1}' | cat -n | tail

TAGS: *.py
	etags `find . -name '*.py'|sort`
	nosetests3 --with-doctest *.py
	flake8 *.py

clean:
	rm -f TAGS $(OUT)
