all:
	cd IngestDaemon ; stack install

test:
	cd IngestDaemon ; stack test IngestDaemon
