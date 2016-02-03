
OUT = aide_ingest.log

all: $(OUT)

# A nightly cron job scans filesystems to update aide.db.
aide_ingest.log: /var/lib/aide/aide.db
	./aide_ingest.py --input-file $<  > $@

clean:
	rm -f $(OUT)
