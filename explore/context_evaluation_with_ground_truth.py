#!/usr/bin/env python3

import csv, json
from sys import argv
from functools import reduce
from pprint import pprint


# Usage:
# Edit these constants:
cdm2admCsv_path = "/home/gberrada/adaptNew/adapt/explore/cdm2admCsv.csv"  # Alec made this. It's a constant mapping of UUIDs from CDM (for ground truth labels) to their corresponding ADM uuid. Can download from here: https://owncloud-tng.galois.com/index.php/s/RzqNzscSCcnGvDn
implication_file_path = "/home/gberrada/adaptNew/adapt/explore/implication.json"  # this is the "implication.json" file produced by Ghita's script.

# Then use this script at the command line with something like:
# `context_evaluation_with_ground_truth.py ~/cadets/labeled_ground_truth/bovia_webshell.csv`
# You can optionally provide one or more space-separated arguments: [s, p, o, prov] after the csv path argument. This will constrain the results considered from the ground truth to only UUIDs for [subjects, predicates, objects, provenance node], respectively.

# Example command used to produce "implication.json":
# `python3 fcascript.py -w both -m 0.05 -s contextSpecFiles/neo4jspec_FileEventType.json -q /Users/ryan/Desktop/fca/cadets_bovia/file_event_type.json -rs rulesSpecs/rules_implication_all.json`

fd = open(cdm2admCsv_path, "r")
reader = csv.reader(fd)
reader.__next__()  # Throw away header row
uuid_map = {}
for row in reader:
	uuid_map[row[0]] = row[1].replace("AdmUUID(","").replace(")","")
fd.close()

def extract_uuids(csv_file_path):
	fd = open(csv_file_path, "r")
	reader = csv.reader(fd)
	reader.__next__()  # Throw away header row
	subjects = []
	predicates = []
	objects = []
	provenances = []
	for row in reader:
		if len(row[4]) > 0:
			subjects.append(row[4])
		if len(row[5]) > 0:
			predicates = predicates + row[5].split(";")
		if len(row[6]) > 0:
			objects.append(row[6])
		if len(row[7]) > 0:
			provenances = provenances + row[7].split(";")
	fd.close()
	args = argv[2:] if len(argv[2:]) > 0 else ["all"]
	def arg_return(key):
		return {
			"s": subjects,
			"p": predicates,
			"o": objects,
			"prov": provenances
		}.get(key, subjects + predicates + objects + provenances)
	uuids = reduce(lambda a,b: a + b, [arg_return(a) for a in args], [])

	should_resolve_to_adm = True

	if should_resolve_to_adm:
		acc = []
		for u in uuids:
			resolved = resolve_to_adm_uuid(u)
			if len(resolved) > 0:
				acc.append([resolved])
			else:
				acc.append([])
		return_uuids = list(set(reduce(list.__add__, acc)))
	else:
		return_uuids = list(set(uuids))

	print("\nADM UUIDs for ground truth of the specified type:")
	[print(u) for u in return_uuids]
	print("\ng.V().has('uuid',within(["+",".join(return_uuids)+"])).hasLabel('Node')\n")
	return return_uuids


def resolve_to_adm_uuid(uuid):
	if uuid not in uuid_map:
		print("Could not remap the UUID: ", uuid)
		return ""
	else:
		return uuid_map[uuid]


with open(implication_file_path, "r") as fd:
	implications = json.loads(fd.read())
	results = {}
	uuids = extract_uuids(argv[1])
	for u in uuids:
		if u in implications:
			results[u] = implications[u]
		else:
			results[u] = {}

	sorted_implications = sorted(list(implications.items()), key=lambda x: x[1]['score'] if "score" in x[1] else 0.0, reverse=True)
	uuid_rank_list = [x[0] for x in sorted_implications]
	sorted_results = sorted(list(results.items()), key=lambda x: x[1]['score'] if "score" in x[1] else 0.0, reverse=True)

	print("Total size of rule breakers:", len(uuid_rank_list),"\n")

	print("Ground truth UUIDs sorted by score:")
	for r in sorted_results:
		if r[0] in uuid_rank_list:
			rank = uuid_rank_list.index(r[0])
		else:
			rank = 0
		print("\n", r[0], "==> Rank:", rank, " Percent from top:", "{:.2f}".format(rank / float(len(uuid_rank_list))*100)+"%")
		pprint(r[1])
