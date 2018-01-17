# Generating Concepts with FCbO and analyzing the concepts

## Brief description of the files (to be found in the explore directory)

The current implementation the FCA analysis is contained in fcbo_core.py (port of the original FCbO C implementation) and FCAscript.py (script to launch the concepts generation with FCbO and analyze the generated concepts to find anomalies).  

Example input files are provided in the directory: context files (one in CXT and one in FIMI format obtained by using the event query on ta1-theia-bovia-cdm17.bin) as well as json specification files 
(event query in spec_context_event.py, netflow query in spec_context_netflow.py and file execution query in spec_context_execute.py and information for context generation (pid vs event context) from CSV files in csv/csvspec.json)

The explore directory also contains the FCbO/PCbO original C source code in directories fcbo-ins and pcbo-amai respectively.

## Brief description of the software

The concept/rule generation and rule violation analysis software can take various types of input:

- context files in FIMI or CXT format

- query results from the Titan database

- CSV files generated from the original CDM files

It runs in 3 steps:

1. input retrieval, parsing and formatting

2. concept generation

3. rule generation followed by rule violation analysis

It is possible to run all the steps or a subset of them. If you want to run all the pipeline, set the ```--workflow/-w``` option to ```'both'```. If only the concept generation is of interest, ```--workflow/-w``` should be set 
to ```'fca'```. ```--workflow/-w``` should be set to ```'analysis'``` if only the rule generation and rule violation analysis are of interest. 
If you only want to generate context files (input files to the FCA algorithm), ```--workflow/-w``` should be set to ```'context'```. 

## Description of the FCA algorithm input

Inputs to the FCA algorithm are specified using either the ```--inputfile/-i``` or the ```--specfile/-s``` or both. 

At least one of these options (whatever the value of the ```--workflow/-w``` option) must be specified otherwise an error is thrown (in the case where ```--workflow/-w``` is set to ```'analysis'```, the arguments passed to these options are used to retrieve information (e.g names of objects and attributes)
 about the context that was used to generate the concepts to be analyzed).

If both options are specified, the specification file given as argument to ```--specfile/-s``` is used to generate the context used in concept generation/analysis and this context is saved to the file specified by the ```--inputfile/-i```
option. Whether the file is saved in FIMI or CXT format depends on the extension of the file given as argument to ```--inputfile/-i``` (no other formats than CXT or FIMI are supported at the moment)  

```--inputfile/-i``` can be specified alone. In this case, the file (in FIMI or CXT format) passed as argument to the option is used to generate the context that will subsequently be passed as input to concept generation and/or rule generation and analysis.  
The FIMI file format is described in: http://fcalgs.sourceforge.net/format.html . And a few examples of CXT files are provided in the explore directory.
It is currently recommended to use files CXT format as they are faster to parse than FIMI files.

```--specfile/-s``` can take two types of json files as argument.
The first type of files describes all the parameters (e.g query to run, data fields to extract as objects or attributes) needed to run a query on the database and generate an input context to concept generation and/or rule generation and analysis (it's not advised to use this functionality at the moment as the 
queries can timeout or produce obscure Cassandra errors.)
The second type of files gives the information necessary to extract an FCA/FCA analysis input context from a set of CSV files. In the case where the specification file passed as argument to the ```--specfile/-s``` option
corresponds to the second type of file, the ```--csv``` flag must be used.

In the case where ```--workflow/-w``` is set to ```'analysis'```, the ```--concept_file/-cf``` option must be used and it specifies the absolute path to the file that contains the concepts to analyze. An example of concept
file is stored in the explore directory (cadets_csvspec_concepts_supp0_05.txt in explore/csv/cadets  generated from the CADETS Bovia file).

The ```--disable-naming/-dn``` flag cannot be invoked with in the case where only ```--inputfile/-i``` is specified and is passed a FIMI file as argument.
It can be only invoked when json query specification files are passed as argument to ```--specfile/-s```or a file in CXT format is passed as argument to ```--inputfile/-i```.
If invoked, the objects and attributes are identified by their positions in the context matrix (as is the case for FIMI files) and not by name.

## Generation of context files (input files to FCA)

```--workflow/-w``` should be set to ```'context'``` if you want to generate context files (input files to the FCA algorithm).

You can generate files in either CXT or FIMI formats. 

The input to be converted into a context file should be specified either using ```--inputfile/-i``` (if you want to convert a CXT file into a FIMI file or vice versa) or ```--specfile/-s`` (if the input you want to convert to cxt or fimi originates from a query to the database or from CSV files that are described in json specification file).

The converted output is written to the file passed as argument to ```--outputfile/-o```. If no outputfile is specified, the input is printed on the screen in the cxt format.

To convert a CXT file to a FIMI file, the extension of the full file path/argument passed to ```--inputfile/-i``` should be '.cxt' and the extension of the full file path/argument passed to ```--outputfile/-o``` should be '.fimi'. 

Similarly, to convert a FIMI file to a CXT file, the extension of the full file path/argument passed to ```--inputfile/-i``` should be '.fimi' and the extension of the full file path/argument passed to ```--outputfile/-o``` should be '.cxt'.

If you want to convert some input described in a json specification file, the extension of the full file path/argument passed to ```--outputfile/-o``` should be that of the file format you want to convert the input into: '.cxt' for CXT files and '.fimi' for FIMI files.

## Description of the concept generation

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

## Description of the arguments needed to launch the analysis

All results of the rule generation and rule violation analysis are saved in the file passed as argument to the ```--analysis_outputfile/-oa``` option. If not specified, the analysis results are printed on the screen.

In the case where ```--workflow/-w``` is set to ```'analysis'```, the ```--concept_file/-cf``` option must be used and it specifies the absolute path to the file that contains the concepts to analyze. An example of concept
file is stored in the explore directory.

The ```--min_rule_conf/-rc``` option specifies the minimum confidence of the implication rules generated. By default, this value is set to 0.95.

The ```--max_rule_conf/-mrc``` option specifies the maximum confidence of the anti-implication rules generated. This value is set to 0.05 by default.

```--num_rules/-nr``` specifies the number of rules to display. By default, this value is set to 10.  

It is possible to choose the type of rules you want to generate with the ```--analysis_type/-a``` option. By default, the value of this option is set to 'all', which means that implication rules, anti-implication rules and disjointness rules are all generated. It is possible to generate a subset of the rules. 
If you only want to generate implication rules, the value of ```--analysis_type/-a``` should be set to ```'imp'``` (the values for anti-implication rules and disjointness rules are ```'anti'``` and ```'dis'``` respectively). If you want to generate more than a type of rule than you pass the values associated with each type of rule you want to compute separated by a comma as an argument to ```--analysis_type/-a```. For example, if you want to compute implication rules and disjointness rules, you can pass ```'imp,dis'``` or ```'dis,imp'``` as argument to ```--analysis_type/-a```. 
Similarly, if you want to compute implication rules and anti-implication rules, you can pass ```'imp,anti'``` or ```'anti,imp'``` as argument to ```--analysis_type/-a```. The full set of possible values for the ```--analysis_type/-a``` option is given in the next section.



## Launching the concept generation and analysis


The concept generation and analysis can be launched after an avro file has been ingested or CSV files generated from an avro file by invoking the following command:
```
python3 fcascript.py [-h] --workflow {'context','fca','analysis','both'}
                    [--fca_algo {python,C}] [--fcbo_path FCBO_PATH]
                    [--inputfile INPUTFILE] [--specfile SPECFILE] [--csv]
                    [--parallel PARALLEL] [--min_support MIN_SUPPORT]
                    [--disable_naming] [--outputfile OUTPUTFILE]
                    [--analysis_type {'all', 'imp', 'anti', 'dis', 'imp,anti', 'imp,dis', 'anti,imp', 'anti,dis', 'dis,imp', 'dis,anti', 'imp,anti,dis', 'imp,dis,anti', 'anti,imp,dis', 'anti,dis,imp', 'dis,imp,anti', 'dis,anti,imp'}]
                    [--analysis_outputfile ANALYSIS_OUTPUTFILE]
                    [--concept_file CONCEPT_FILE]
                    [--min_rule_conf MIN_RULE_CONF]
                    [--max_rule_conf MAX_RULE_CONF] [--num_rules NUM_RULES]
```


To perform an analysis equivalent to the one obtained with ```python3 analyze.py --event```, one could launch:
```
python3 explore/fcascript.py -s explore/csv/csvspec_updated.json --csv -m 0.05 -w both --fca_algo C --fcbo_path explore/pcbo-amai/pcbo --parallel 3
```

(if you want to run the C version of PCbO. The number provided to --parallel can be any number greater than 1, it just corresponds to the number of threads PCbO is supposed to run)

or 

```
python3 explore/fcascript.py -s explore/csv/csvspec_updated.json --csv -m 0.05 -w both --fca_algo C --fcbo_path explore/fcbo-ins/fcbo 
```
(if you want to run the C version of FCbO)

or 

```
python3 explore/fcascript.py -s explore/csv/csvspec_updated.json --csv -m 0.05 -w both
```
(if you want to run the Python version of FCbO)

## Script (runFCA.py) for testing the influence of support/confidence variation (not very stable at the moment)

This script currently runs the full pipeline (i.e concept generation followed by rule generation and analysis) on the CSV files generated from several avro files (one directory per avro file) and provides lists of minimum
support and minimum rule confidence to test. The data it is supposed to run on is the explore/csv directory.




