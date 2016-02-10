
OUT = \
 aide_ingest.log \
 sha256sums \

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


# NB: apt-get only succeeds if ubuntu-keyring can validate .deb signature.
UTILS = coreutils_8.21-1ubuntu5.3_amd64
DEB = /var/cache/apt/archives/$(UTILS).deb
DEB:
	sudo apt-get --reinstall install coreutils
	ls -l $(DEB)
	dpkg -c $(DEB) | egrep '\./bin/l'

control.tar.gz: $(DEB)
	ar x $(DEB) $@

md5sums: control.tar.gz
	tar --touch -zxvf $< ./$@

# For now, pretend vendor had updated (broken) md5 to (secure) sha256.
# We happen to know both hashes for selected binaries, e.g. /bin/ls.
sha256sums: md5sums
	cat $< \
          | awk '{print $$1, "/"$$2}' \
          | sed -e 's/^0a5cfeb3bb10e0971895f8899a64e816 /1959304caf1c2b4abe1546056ad71223e75027b02411dd95a5e6969a84419c27 /' \
          | tee $@ \
          | awk 'length($$1) > 32'


TAGS: *.py
	etags `find . -name '*.py'|sort`
	nosetests3 --with-doctest *.py
	flake8 *.py

clean:
	rm -f TAGS control.tar.gz md5sums $(OUT)
