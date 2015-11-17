
OUT = \
 test/ssh_and_exfiltrate_step34_server_side.provn \

.PHONY: test create-sandbox

test:
	cabal run -- --verbose --quiet --turtle --ast ${PWD}/test/amazon_ie_template.provn

%.ttl: %.provn
	-cabal run -- --quiet --stats --turtle $<
	@echo
	@wc -l $<.ttl
	@echo
	@head -12 $< $<.ttl
	@echo

EXAMPLE='entity(ex:e68f56, [tc:entityType="network", tc:destinationAddress="13.1.136.66"])'

%.provn:
	expand -t1 < $(HOME)/trace/current/$(shell basename $@) \
	| sed \
            -e '1a \ prefix tc <http://spade.csl.sri.com/rdf/audit-tc.rdfs#>' \
            -e 's;artifact;entity;' \
            -e 's;audit:subtype;tc:entityType;' \
            -e 's;data:destination host;tc:destinationAddress;' \
            -e 's;data:source host;tc:sourceAddress;' \
            -e 's;data:destination port;tc:destinationPort;' \
            -e 's;data:source port;tc:sourcePort;' \
            -e 's;data:;ex:;' \
        | cat > $@

# A convention of foo.ttl would be more convenient than foo.provn.ttl.
exfil:	test/ssh_and_exfiltrate_step34_server_side.ttl

create-sandbox:
	cabal sandbox init
	cabal sandbox add-source ../Ingest
	cabal install --dependencies-only

clean:
	rm -rf dist $(OUT)
