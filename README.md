# Adapt

The ingester library, and associated command line tool 'Trint', parses Prov-N
run Adapt-in-a-box, which is a single VM with select Adapt components installed,
ready for pushing CDM files into the database.  This 'in-a-box' system is
similar to the ta-3 multi-VM "tc-in-a-box" setup, but less time consuming or
resource intensive.
This Prov-N is then translated to our CDM-inspired internal data format.  The
to individual component repos.
tool is able to produce AST print-outs of the CDM, upload CDM to gremlin-server
# Pulling Commits
via websockets, compute statistics about the provenance, and syntax check the
  'Research')

# Development (Enhancing adapt-in-a-box)

There are a few development activities one can perform from this repository.
Namely, pulling in commits from component repositories and change the actual
input file to produce localized error messages.



First install [stack](https://github.com/commercialhaskell/stack/releases) build

tool then run `make` from the top level.
# Directory Descriptions
    git subtree push --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master

A helper script exists to pull all known components in via git subtree:
- ad: Anomaly Detection
    ./pull-all.sh
- classifier: Classifier
The infrastructure files of interest are:

- Vagrantfile: Defines the VM and installation scripts.
- install/boostrap.sh: Invoked by vagrant (first `vagrant up` call and any
  `vagrant provision` call) to install all dependencies and select Adapt
  components.
- start_daemons.sh: Executes infrastructure via supervisord and sets up Kafka
  topics.
- config/supervisord.conf.{adapt,tc}inabox: Configuration files for the two VM
  configurations.

# Research (Using Adapt-in-a-box)

Users who wish to execute Adapt components, put data into the database, and run
Trint command line is in the form "Trint [Flags] Parameters" where
framework 'tc-in-a-box' found on the BBN gitlab site.
  database and communication queues.
To start adapt-in-a-box ensure you have VirtualBox and Vagrant installed then
execute `vagrant up` from the root of this repository.  Vagrant will start a new
VM and run the install script. Once all the configured components are installed,
the `start_daemons.sh` script will automatically run, providing a complete Adapt
system.

To use the Adapt in a box, ssh to the system (`vagrant ssh`) and leverage the
components' executables. You can confirm the database is ready by checking for
the notice about listening on port 8182 at or near the bottom of
`/opt/titan/log/gremlin-server.log`. Use Trint to push data from a file into the
database:

- dx: Diagnostics
Parameters:

N.B. Trint places the data on ingestd's inbound Kafka queue.  Ingestd manages
  -l                 --lint                    Check the given file for syntactic and type issues.
  -q                 --quiet                   Quiet linter warnings
- install: Scripts to install all available tools in a single virtual machine.
  -v                 --verbose                 Verbose debugging messages
  -a                 --ast                     Produce a pretty-printed internal CDM AST
  -u[Database host]  --upload[=Database host]  Uploads the data by inserting it into a Titan database using gremlin.
send the 'finished' signal to Ingestd which then propogates it to the pattern
extractor and on down the chain:
Trint -f
```
- Segment: Graph segmentation
While it might be convenient for Trint to automatically append this signal
The 'upload' command has been tested with a gremlin server configured with the
of operation such as 1) ingesting data from TA-3 then sending the signal
(Adapt-specific) signal  2) ingesting data from multiple Avro files 3) ingesting
data by replaying Kafka log files produce by some TA-1 performers.
default websockets channelizer.
# Directory Descriptions

- ad: Anomaly Detection
- classifier: Classifier
- config: System configuration files for primitive services including the
  database and communication queues.
- dashboard: A primitive web interface for displaying messages from each
  component.
- dx: Diagnostics
- example: Basic CDM/Avro container file examples.
- ingest: The ingester, which takes data from its source (TA-1) and insert it
  into the database after some basic checks and transformations.
- install: Scripts to install all available tools in a single virtual machine.
- kb: Knowledge base (adversarial models etc)
- px: Pattern Extractor
- Segment: Graph segmentation
- trace: Script for working with the data store on seaside.galois.com
- ui: A fake user interface that was once useful (trash now?)
