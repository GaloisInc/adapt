# Generating Concepts with FCbO and analyzing the concepts

## Brief description of the general structure of the fca directory

The AR_Miner directory contains all the R scripts developed by Sid. Details on those scripts can be found in the README file contained in that directory.

The current README describes how to use the FCA and rule mining implemented in Python and Bash and stored in the FCA_Miner repository (Python scripts for FCA/rule mining algorithms in the python_scripts directory
and bash scripts for FCA pipeline automation in the bash_scripts directory).

All other directories provide example input files or JSON specification files, mainly used for the Python and Bash but also for the R scripts (see relevant README (../AR_Miner/README.md) ):
* the contextExamples directory provides examples of all possible input context files (CXT format, FIMI format, input CSV format) all obtained by using the event query (specified in contextSpecFiles/neo4jspec_ProcessEvent.json ) on the ta1-theia-bovia-cdm17.bin file 
* the contextSpecFiles directory contains JSON specification files that describe all parameters required to extract the data necessary to produce an input context file (in either CXT, FIMI or CSV format) from the neo4j database.
 The contextSpecFiles/neo4jspec_ProcessEvent.json file can, for example, be used with any dataset to generate the default ProcessEvent context.
* the contextQueryResults directory contains the results for the CDM17 Cadets Bovia dataset of the queries specified in some of the JSON specification files from the contextSpecFiles directory
* the csvContexts directory contains input CSV contexts
	
The FCA_Miner directory also contains the FCbO/PCbO original C source code in directories fcbo-ins and pcbo-amai respectively.


## Brief description of the FCA scripts files (to be found in the fca/FCA_Miner directory)

### FCA and rule mining scripts in Python (fca/FCA_Miner/python_scripts directory)

The core FCA algorithms (currently only FCbO), rule mining algorithms and analysis are currently implemented in Python. 

The original FCbO C implementation has been ported to Python in fcbo.py. fca.py implements additional generic FCA-related functions. concepts_analysis.py implements all functions related to rule mining,
anomaly detection and the analysis of their results. input_processing.py implements all sorts of useful functions for input and output parsinf, processing and formatting.

conversion_script.py takes a JSON specification file and generates an CSV input context (see below for details). 

fcascript.py is the script needed to launch the concepts generation with FCbO and analyze the generated concepts to find anomalies.  




### FCA pipeline automation scripts in bash (fca/FCA_Miner/bash_scripts)

At this point, there are mainly two scripts: ingest_script.sh and ingest_all_script.sh .

ingest.sh automates the full pipeline for one dataset (ingest and/or ui and/or conversion to input CSV context and/or FCA analysis) for one fixed set of parameters (i.e type of context, support threshold
, rule specification)
ingest_all_script.sh generalizes ingest.sh to multiple datasets.


## How to use the conversion script conversion_script.py

In the adapt directory, the following command can be used to launch the conversion script:

```
python3 fca/FCA_Miner/python_scripts/conversion_script.py [--port PORT]
							[--specdirectory SPECDIRECTORY [SPECDIRECTORY ...]]
                            [--contextname CONTEXTNAME]
                            [--pathCSVContext PATHCSVCONTEXT]
```


The conversion script takes up to four optional arguments:
*  --port/-p PORT, which corresponds to the UI/ingest port. If not specified, the port is set to a default value of 8080.
*  --specdirectory/-d SPECDIRECTORY [SPECDIRECTORY ...], which corresponds to the list of directories to explore in search for the query specification file.
If not specified, this takes a default value of './fca/contextSpecFiles'. If specifiying multiple directories, separate the directory names with spaces e.g ```-d directory1 directory2 directory3 directory4```.
* --contextname/-n CONTEXTNAME, which specifies the name of the context (e.g ProcessEvent). If not specified, the name of the context is set to a default value of 'ProcessEvent'.
* --pathCSVContext/-cp PATHCSVCONTEXT, which specifies the path to the newly created CSV context. If not specified, the newly created CSV context is by default stored in specdirectory/contextname.csv

If the ```--pathCSVContext/-cp``` is not used and the default path to the CSV context to be created already exists, the user will be asked whether he/she wants to overwrite the existing context file or 
to provide an alternative output path if an overwrite is undesirable.


## Brief description of the core/main FCA software (i.e Python scripts)

The concept/rule generation and rule violation analysis software can take various types of input:

- context files in FIMI or CXT format

- query results from the Neo4j database

- CSV files generated from the original CDM files

It runs in 3 steps:

1. input retrieval, parsing and formatting

2. concept generation

3. rule generation followed by rule violation analysis

It is possible to run all the steps or a subset of them. If you want to run all the pipeline, set the ```--workflow/-w``` option to ```'both'```. If only the concept generation is of interest, ```--workflow/-w``` should be set 
to ```'fca'```. ```--workflow/-w``` should be set to ```'analysis'``` if only the rule generation and rule violation analysis are of interest. 
If you only want to generate context files (input files to the FCA algorithm), ```--workflow/-w``` should be set to ```'context'```. 

### Description of the FCA algorithm input

Inputs to the FCA algorithm are specified using either the ```--inputfile/-i``` or the ```--specfile/-s``` or both. 

At least one of these options (whatever the value of the ```--workflow/-w``` option) must be specified otherwise an error is thrown (in the case where ```--workflow/-w``` is set to ```'analysis'```, the arguments passed to these options are used to retrieve information (e.g names of objects and attributes)
 about the context that was used to generate the concepts to be analyzed).

If both options are specified, the specification file given as argument to ```--specfile/-s``` is used to generate the context used in concept generation/analysis and this context is saved to the file specified by the ```--inputfile/-i```
option. Whether the file is saved in FIMI or CXT format depends on the extension of the file given as argument to ```--inputfile/-i``` (no other formats than CXT or FIMI are supported at the moment)  

```--inputfile/-i``` can be specified alone. In this case, the file (in FIMI or CXT format) passed as argument to the option is used to generate the context that will subsequently be passed as input to concept generation and/or rule generation and analysis.  
The FIMI file format is described in: http://fcalgs.sourceforge.net/format.html . And a few examples of CXT files are provided in the fca directory.
It is currently recommended to use files CXT format as they are faster to parse than FIMI files.

```--specfile/-s``` can take two types of json files as argument.
The first type of files describes all the parameters (e.g query to run, data fields to extract as objects or attributes) needed to run a query on the database and generate an input context to concept generation and/or rule generation and analysis (one could also run the query specified by this type of file, save its results to a JSON file and pass this result file as parameter to the ```--queryres/-q```).
The second type of files gives the information necessary to extract an FCA/FCA analysis input context from a set of CSV files. In the case where the specification file passed as argument to the ```--specfile/-s``` option
corresponds to the second type of file, the ```--csv``` flag must be used.

In the case where ```--workflow/-w``` is set to ```'analysis'```, the ```--concept_file/-cf``` option must be used and it specifies the absolute path to the file that contains the concepts to analyze. 

The ```--disable-naming/-dn``` flag cannot be invoked with in the case where only ```--inputfile/-i``` is specified and is passed a FIMI file as argument.
It can be only invoked when json query specification files are passed as argument to ```--specfile/-s```or a file in CXT format is passed as argument to ```--inputfile/-i```.
If invoked, the objects and attributes are identified by their positions in the context matrix (as is the case for FIMI files) and not by name.

### Generation of context files (input files to FCA)

```--workflow/-w``` should be set to ```'context'``` if you want to generate context files (input files to the FCA algorithm).

You can generate files in either CXT or FIMI formats. 

The input to be converted into a context file should be specified either using ```--inputfile/-i``` (if you want to convert a CXT file into a FIMI file or vice versa) or ```--specfile/-s`` (if the input you want to convert to cxt or fimi originates from a query to the database or from CSV files that are described in json specification file).

The converted output is written to the file passed as argument to ```--outputfile/-o```. If no outputfile is specified, the input is printed on the screen in the cxt format.

To convert a CXT file to a FIMI file, the extension of the full file path/argument passed to ```--inputfile/-i``` should be '.cxt' and the extension of the full file path/argument passed to ```--outputfile/-o``` should be '.fimi'. 

Similarly, to convert a FIMI file to a CXT file, the extension of the full file path/argument passed to ```--inputfile/-i``` should be '.fimi' and the extension of the full file path/argument passed to ```--outputfile/-o``` should be '.cxt'.

If you want to convert some input described in a json specification file, the extension of the full file path/argument passed to ```--outputfile/-o``` should be that of the file format you want to convert the input into: '.cxt' for CXT files and '.fimi' for FIMI files.

### Description of the concept generation

Concept generation can be done in 3 ways:
-using the ported Python version of FCbO (not very efficient and optimized at present)
-using the FCbO executable compiled from the original FCbO authors' C code
-using the PCbO executable compiled from the original PCbO authors' C code

The ```--fca_algo/-f``` and ```--parallel/-pl``` options control which version of the concept generation is executed.

```--fca_algo/-f``` takes two possible values: ```'C'``` or ```'python'```. When ```--fca_algo/-f``` is set to ```'python'```, the ported Python FCbo version is launched, otherwise, if ```--fca_algo/-f```
is set to ```'C'```, either of original (compiled from C) FCbO or PCbO executables are launched. The original FCbO executable is launched if ```--parallel/-pl``` is set to 1 and the original PCbO executable
is launched if ```--parallel/-pl``` is set to a value greater than 1.

 ```--parallel/-pl``` corresponds to the ```-P``` option of the original PCbO code (see http://fcalgs.sourceforge.net/pcbo-amai.html) and sets the number of threads to cpus.
The recommended value for this option is the number of hardware processors (processor cores) in the system or higher (typically, two or three times the number of all CPU cores). 

The ```--fcbo_path/-p``` option specifies the location of the FCbO/PCbO C code (absolute path).

The ```--outputfile/-o``` option specifies the full path to the file where the concepts should be saved. If not specified, the concepts are printed on the screen.

The argument passed to ```--min_support/-m``` is a number between 0 and 1 that corresponds to the minimal support value for a concept to be generated by FCbO divided by the number of objects. By default, this value is set to 0, which means all concepts are generated.

### Description of the arguments needed to launch the analysis

All results of the rule generation and rule violation analysis are saved in the file passed as argument to the ```--analysis_outputfile/-oa``` option. If not specified, the analysis results are printed on the screen.

In the case where ```--workflow/-w``` is set to ```'analysis'```, the ```--concept_file/-cf``` option must be used and it specifies the absolute path to the file that contains the concepts to analyze. An example of concept
file is stored in the fca directory.

It is possible to choose the type of rules you want to generate with the ```--rules_spec/-rs``` option. The argument passed to the ```--rules_spec/-rs``` is a JSON file that specifies the types of rules to generate as the rule generation parameters (e.g for implication rules, minimum confidence threshold and number of rules to generate). 
An example of such specification file is the file fca/FCA_Miner/rulesSpecs/rules_positive_implication.json.


### Launching the concept generation and analysis


The concept generation and analysis can be launched after an avro file has been ingested (or CSV files generated from an avro file) by invoking the following command:

```
python3 fcascript.py [-h] --workflow {context,fca,analysis,both}
                    [--fca_algo {python,C}] [--fcbo_path FCBO_PATH]
                    [--inputfile INPUTFILE] [--specfile SPECFILE]
                    [--queryres QUERYRES] [--csv] [--parallel PARALLEL]
                    [--min_support MIN_SUPPORT] [--disable_naming]
                    [--outputfile OUTPUTFILE]
                    [--analysis_outputfile ANALYSIS_OUTPUTFILE]
                    [--concept_file CONCEPT_FILE] [--rules_spec RULES_SPEC]
```


To generate analysis results based on the ProcessEvent context and positive implication rules (with standard support and confidence settings), one could launch (after ADM ingestion):
```
python3 fca/FCA_Miner/python_scripts/fcascript.py -s fca/contextSpecFiles/neo4jspec_ProcessEvent.json -w both -m 0.05 --fca_algo C --fcbo_path fca/FCA_Miner/pcbo-amai/pcbo --parallel 3 -rs fca/FCA_Miner/rulesSpecs/rules_positive_implication.json
```

(if you want to run the C version of PCbO. The number provided to --parallel can be any number greater than 1, it just corresponds to the number of threads PCbO is supposed to run)

or 

```
python3 fca/FCA_Miner/python_scripts/fcascript.py -s fca/contextSpecFiles/neo4jspec_ProcessEvent.json -w both -m 0.05 --fca_algo C --fcbo_path fca/FCA_Miner/fcbo-ins/fcbo -rs fca/FCA_Miner/rulesSpecs/rules_positive_implication.json
```
(if you want to run the C version of FCbO)

or 

```
python3 fca/FCA_Miner/python_scripts/fcascript.py -s fca/contextSpecFiles/neo4jspec_ProcessEvent.json -w both -m 0.05 -rs fca/FCA_Miner/rulesSpecs/rules_positive_implication.json
```
(if you want to run the Python version of FCbO)


Alternatively, if working with a CSV input file instead of a JSON specification file, one could use the following equivalent command line:

```
python3 fca/FCA_Miner/python_scripts/fcascript.py -i fca/csvContexts/neo4jspec_ProcessEvent.csv -w both -m 0.05 -rs fca/FCA_Miner/rulesSpecs/rules_positive_implication.json
```

## How to use the bash scripts

### ingest.sh

This script can take up to 9 option flags, some of which are mutually exclusive:
* ```-I/--ingest-only``` indicates that only an ingest should be run with the script.
 ```-g|--generation_only``` indicates that only the generation of an input CSV context should be performed with the script.
 ```-f|--fca_only``` indicates that only FCA and rule mining (and all analysis related to it) should be performed with the script.
 These three options are mutually exclusive.
* ```-i|--ingest``` takes the path to the file to be ingested as a parameter. If not specified, no ingest is performed by default.
* ```-p|--port``` takes the UI/ingest port as a parameter. If not specified, the port is set to 8080 by default.
* ```-db|--dbkeyspace``` takes the UI/ingest dbkeyspace as a parameter. If not specified, the dbkeyspace is set to 'neo4j' by default.
* ```-c|--conversion``` specifies the parameters to the conversion_script.py script. It can take no parameters or up to 4 parameters.
If a parameter is given to this option, it is prefixed by the short name of the option correponding to the parameter in conversion_script.py followed by '='.
For example, ```-c p=9000 d=./fca/contextSpecFiles n=ProcessEvent cp=./fca/csvContexts/neo4j_ProcessEvent.csv``` means that the JSON specification
file corresponding to a context of name 'ProcessEvent' will be searched for in the directory './fca/contextSpecFiles', the ingest/UI port used in the conversion script will be 9000
and the output of the conversion script will be the file './fca/csvContexts/neo4j_ProcessEvent.csv'.
* ```-fp|--fca_params``` specifies the parameters to the fcascript.py script. As with ```-c|--conversion```, it can take no to multiple parameters, all prefixed by the short
name  of the option correponding to the parameter in fcascript.py followed by '='. 
For example, ```-fp w=both m=0.05 s=fca/contextSpecFiles/neo4jspec_ProcessEvent.json rs=fca/rulesSpecs/rules_positive_implication.json```
would invoke this command line:
```
python3 fca/FCA_Miner/python_scripts/fcascript.py -s fca/contextSpecFiles/neo4jspec_ProcessEvent.json -w both -m 0.05 -rs fca/FCA_Miner/rulesSpecs/rules_positive_implication.json
```
whereas:

```-fp w=both m=0.05 i=fca/csvContexts/neo4jspec_ProcessEvent.csv rs=fca/rulesSpecs/rules_positive_implication.json```
would invoke this command line:
```
python3 fca/FCA_Miner/python_scripts/fcascript.py -i fca/csvContexts/neo4jspec_ProcessEvent.csv -w both -m 0.05 -rs fca/FCA_Miner/rulesSpecs/rules_positive_implication.json
```


This script includes a section that makes sure it is executed from the ```adapt``` directory.

The full command line would be :
```
./fca/FCA_Miner/bash_scripts/ingest_script.sh -i file_to_ingest -p ingest_port -m mem -db dbkeyspace -c n=contextName d=contextSpecDir cp=path2csv p=port -fp w=workflow (i=path2csv|s=path2json) m=minsupp rs=ruleSpec oa=path2analysisOutput

```
### ingest_all_script.sh

* ```-r|--search_repository``` : directory where the files to be ingested are stored. For now, the script searches for all CDM17 CADETS, FiveDirections and Trace files in this directory.
If not specified, the default value for this option is the current directory i.e the ```adapt``` directory since the script contains a section that ensures we are positioned in the ```adapt```
directory before execution.
* ```-p|--port```: ingest/UI ports. A space separated list of ports can be provided as parameter to this option. If not specified and if a set of ingests has to performed and/or a set of UIs started,
a number of ports equal to the number of files to be ingested/UIs to be started are randomly generated with the interval \[8080,10080\]. If only one port value is specified, the random generation interval becomes
\[specified_value, specified_value+2000\].
* ```-N|--no_ingest```: indicates no ingest is to be performed (no parameter)
* ```-seq|--sequential_ingest```: indicates the ingest should be run sequentially and not in parallel (no parameter)
* ```-m|--mem```: maximum memory allocated to the ingest/UI
* ```-d|--context_directory```: path where the contexts are stored. By default set to './fca/contextSpecFiles'
* ```-n|--context_name```: context name for context generation and FCA analysis (default value: 'ProcessEvent')
* ```-w|--fca_workflow```: FCA workflow (typically ```'both'```(default value) or ```'analysis'```)
* ```-ms|--fca_minsupp```: minimal support associated with the FCA analysis (default value: 0)
* ```-rs|--fca_rule_spec```: path to the JSON rule specification file (default value: './fca/FCA_Miner/rulesSpecs/rules_positive_implication.json')
* ```-cd|--csv_dir```: path to input CSV file (if using an input CSV file or if a conversion to this input format is performed after ingest/UI) (default value: './fca/csvContexts')
* ```-oa|--fca_analysis_output_dir```: directory where the result of the FCA should be stored (default value: './fca/FCA_Miner/fcaAnalysis') 


The full command line would be :
```
./fca/FCA_Miner/bash_scripts/ingest_all_script.sh -r directory -p ingest_port1 [ingest_port2...ingest_port_n] -seq -d context_directory -n contextname -w both -ms minsupp -rs ruleSpec -oa path2analysisOutput

```


