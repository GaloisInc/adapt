
OUT = \
 test/ssh_and_exfiltrate_step34_server_side.provn \

.PHONY: test create-sandbox

test:
	cabal run -- --verbose --quiet --turtle --ast ${PWD}/test/amazon_ie_template.provn

%.ttl: %.provn
	cabal run -- --turtle $<

%.provn:
	sed -e 's; host;_host;g' \
            -e 's; port;_port;g' \
            < $(HOME)/trace/current/$(shell basename $@)  > $@

# A convention of foo.ttl would be more convenient than foo.provn.ttl.
exfil:	test/ssh_and_exfiltrate_step34_server_side.ttl

create-sandbox:
	cabal sandbox init
	cabal sandbox add-source ../Ingest
	cabal install --dependencies-only

clean:
	rm -rf dist $(OUT)
