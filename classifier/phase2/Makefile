
all: fg-test bg-test bad-ls-short

# This can be integrated with upstream deps after phase2 commits are available.
fg-test:
	nosetests3 --with-doctest --doctest-tests phase2_test.py classify/*.py
	flake8 *.py */[a-z]*.py

# classifyd.py should already be running in the background under supervisord.
# If it is not running, then
#   1) it will not produce a "recvd msg" log entry, and
#   2) as a feature of kafka, the foreground client won't notice any trouble.
bg-test:
	@echo; echo; echo Testing the classifier daemon...
	./faux_seg.py


/tmp/dst/schema.json:
	bash -c 'rm -rf /tmp/{src,dst}'
	bash -c 'mkdir  /tmp/{src,dst}'
	cp -p ~/adapt/example/bad-ls.avro /tmp/src/content.avro
	avroknife getschema local:/tmp/src/  > $@
	wc -l $@

N = 1000
bad-ls-short: /tmp/dst/schema.json
	avroknife tojson --index 0-$(N) local:/tmp/src/ | jq --compact-output .datum  > /tmp/dst/content.json
	avroknife copy   --index 0-$(N) local:/tmp/src/ --output local:/tmp/dst/
