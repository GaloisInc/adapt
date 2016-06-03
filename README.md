# Adapt

ADAPT software for Transparent Computing. This integration repository holds
copies of the individual component repositories.  From here you can boot up and
run Adapt-in-a-box, which is a single VM with select Adapt components installed,
ready for pushing CDM files into the database.  This 'in-a-box' system is
similar to the ta-3 multi-VM "tc-in-a-box" setup, but less time consuming or
resource intensive.

The rest of this readme is about using the repository for either:

- Further infrastructure development (see the 'Development' section)
- Running Adapt, populating databases and other more interactive research (see
  'Research')

# Development (Enhancing adapt-in-a-box)

There are a few development activities one can perform from this repository.
Namely, pulling in commits from component repositories and change the actual
deployment scripts to install new dependencies or alter the startup.

## Pulling Commits

Update adapt files from a subtree repo with a command like:

    git subtree pull --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master --squash

A helper script exists to pull all known components in via git subtree:

    ./pull-all.sh

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
queries can use either the single-vm 'adapt-in-a-box' or the TA-3 multi-vm
framework 'tc-in-a-box' found on the BBN gitlab site.

To start adapt-in-a-box ensure you have VirtualBox and Vagrant installed then
execute `vagrant up` from the root of this repository.  Vagrant will start a new
VM and run the install script. Once all the configured components are installed,
the `start_daemons.sh` script will automatically run, providing a complete Adapt
system:

```
vagrant up
vagrant ssh
tail -f /opt/titan/log/gremlin-server.log
# look for a message at or near the end such as "up on port 8182"
# to ensure reindexing completed properly.
```

To use the Adapt in a box, ssh to the system (`vagrant ssh`) and leverage the
components' executables. You can confirm the database is ready by checking for
the notice about listening on port 8182 at or near the bottom of
`/opt/titan/log/gremlin-server.log`. Use Trint to push data from a file into the
database:

```
Trint -p $HOME/adapt/example/bad-ls.avro
```

N.B. Trint places the data on ingestd's inbound Kafka queue.  Ingestd manages
the interaction with Titan.  Once all desired data has pushed to ingestd, and
the database has stopped processing the insertions, Trint can again be used to
send the 'finished' signal to Ingestd which then propagates it to the pattern
extractor and on down the chain:

```
Trint -f
```

While it might be convenient for Trint to automatically append this signal
after reading in a trace file, such behavior would preclude many desirable mode
of operation such as 1) ingesting data from TA-3 then sending the signal
(Adapt-specific) signal  2) ingesting data from multiple Avro files 3) ingesting
data by replaying Kafka log files produce by some TA-1 performers.

## Helper Scripts for Avro Files, Titan, and More

If you want to do X then use Y:

*Use just the N-th to M-th statements from an avro (.avro or .bin) file:* For
this you can use the Python command line tool 'avroknife', which is installed in
the VM by default.  To use knife you must give it a source and destination
_directory_ along with any desired parameters (copying statements, extracting
fields, ranges, etc). For example:

```
$ mkdir /tmp/avro
$ cp example/bad-ls.avro /tmp/avro
$ avroknife --index 0-5000 --output local:/tmp/slicedData copy local:/tmp/avro
$ Trint -p /tmp/slicedData/content.avro
Sent 5001 statements to kafka[TName {_tName = KString {_kString = "ta2"}}].
```

*Counting nodes:* Numerous tools exist in `$HOME/adapt/tools` including
`node_count.py`.  Just run `python3 $HOME/adapt/tools/node_count.py` and be
aware it performs about a half dozen full database traversals.

*Restarting Everything:* If you have changed titan configurations, or something
in supervisor.d, and would like to restart services without restarting the VM
then try running `$HOME/adapt/tools/restart_services.sh`.  Similarly, you can
follow the steps in that script but insert a `/opt/titan/bin/titan clean`
command if you'd like to wipe the database at the same time.

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
