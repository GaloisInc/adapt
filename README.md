# Adapt

ADAPT software for Transparent Computing. This integration repository holds
copies of the individual component repositories.  From here you can boot up and
run Adapt-in-a-box, which is a single VM with select Adapt components installed,
ready for pushing CDM files into the database.  This 'in-a-box' system is
similar to the ta-3 multi-VM "tc-in-a-box" setup, but less time consuming or
resource intensive.

The rest of this readme is about using the repository for either:

- Further infrastructure development (see the 'Devleopment' section)
- Running Adapt, populating databases and other more interactive research (see
  'Research')

# Development

There are a few development activities one can perform from this repository.
Namely, pulling in commits from component repositories and change the actual
deployment scripts to install new dependencies or alter the startup.

## Pulling Commits

Update adapt files from a subtree repo with a command like:

    git subtree pull --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master --squash

or push adapt files to a subtree repo with a command like:

    git subtree push --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master

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

# Research

Users who wish to execute Adapt components, put data into the database, and run
queries can use either run the single-vm 'adapt-in-a-box' or the TA-3 multi-vm
framework 'tc-in-a-box' found on the BBN gitlab site.

To start adapt-in-a-box ensure you have VirtualBox and Vagrant installed then
execute `vagrant up` from the root of this repository.  Vagrant will start a new
VM and run the install script. Once all the configured components are installed,
the `start_daemons.sh` script will automatically run, providing a complete Adapt
system.

To use the Adapt in a box, ssh to the system (`vagrant ssh`) and leverage the
components' executables. For example, to push data from a file into the database
via Ingestd, you can use Trint to put the data on ingestd's inbound queue:

```
Trint -p ./adapt/example/bad-ls.avro
```

# Directory Descriptions

- ad: Anomaly Detection
- classifier: Classifier
- config: System configuration files for primitive services including the
  database and communication queues.
- dashboard: A primitive web interface for displaying messages from each
  component.
- dx: Diagnostics
- example: Basic ProvN examples which will eventually be replaced by CDM.
- ingest: The ingester, which takes data from its source (TA-1) and insert it
  into the database after some basic checks and transformations.
- install: Scripts to install all available tools in a single virtual machine.
- kb: Knowledge base (adversarial models etc)
- px: Pattern Extractor
- Segment: Graph segmentation
- trace: Script for working with the data store on seaside.galois.com
- ui: A fake user interface that was once useful (trash now?)
