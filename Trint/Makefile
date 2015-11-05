
OUT = \
 test/ssh_and_exfiltrate_step34_server_side.provn \

.PHONY: test create-sandbox

test:
	cabal run -- --verbose --quiet --turtle --ast ${PWD}/test/amazon_ie_template.provn

%.ttl: %.provn
	cabal run -- --quiet --stats --turtle $<
	@echo
	@wc -l $<.ttl
	@echo
	@head -99 $< $<.ttl
	@echo

%.provn:
	sed -e '3a \ entity(ex:e68f56, [tc:artifactType="network", tc:destinationAddress="13.1.136.66"])' \
            -e '1a \ prefix ex <http://example.org/>' \
            -e '1a \ prefix tc <http://adapt.galois.com/>' \
            -e '1a \ prefix foaf <http://xmlns.com/foaf/0.1/>' \
            -e '1a \ prefix dc <http://purl.org/dc/elements/1.1/>' \
            -e '1a \ prefix nfo <http://www.semanticdesktop.org/ontologies/2007/03/22/nfo/v1.2/>' \
            -e 's;process;entity;' \
            -e 's;data:destination host;tc:destinationAddress;' \
            -e 's;data:source host;tc:sourceAddress;' \
            -e 's;data:destination port;tc:destinationPort;' \
            -e 's;data:source port;tc:sourcePort;' \
            -e 's;data:;ex:;' \
            -e 's;endDocument;end document;' \
            < $(HOME)/trace/current/$(shell basename $@) \
        | expand -t1 \
        | egrep 'document|prefix|entity.ex.(bd33a|e68f)'  > $@

# A convention of foo.ttl would be more convenient than foo.provn.ttl.
exfil:	test/ssh_and_exfiltrate_step34_server_side.ttl

create-sandbox:
	cabal sandbox init
	cabal sandbox add-source ../Ingest
	cabal install --dependencies-only

clean:
	rm -rf dist $(OUT)
