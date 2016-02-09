To automatically setup a virtual machine for Adapt, use vagrant and execute
'vagrant up'.  This will provision a Debian system, install tooling including
Java 8, titan, zookeeper, and kafka, then compile the 'trint' command line tool
useful for Ingest.

Other components of Adapt will be included as their maturity allows.

Manual setup is possible by mirroring the actions of the bootstrap.sh script.
