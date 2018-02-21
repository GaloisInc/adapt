#! /usr/bin/env python3

import json, re, argparse, sys, ijson

if __name__ == '__main__':

    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Compute ADM ground truth UUIDs')
    parser.add_argument('adm2cdm_mapping', metavar='FILE', type=str,
                        help="Produce this file with './cmdline_query.py server-url --endpoint=cypher --query=\"MATCH (n) RETURN n.uuid, n.originalCdmUuids\" > mapping.json'")
    parser.add_argument('ground_truth_csv', metavar='FILE', type=str,
                        help="One of the ground truth CSVs, as in the 'labeled_ground_truth'")
    args = parser.parse_args()


    # Some mapping JSON files are >2GB so we can't just use `json.loads`
    mapping_file = args.adm2cdm_mapping # "/Users/atheriault/Code/adapt/cadets_bovia_adm_mapping.json"
    cdm2adm = {}
    with open(mapping_file, 'r') as fd:
        parser = ijson.items(fd, 'item')
        for node in parser:
            adm = node["n.uuid"]
            cdms = node["n.originalCdmUuids"].split(";")
            for cdm in cdms:
                cdm2adm[cdm] = adm

    group_truth_csv = args.ground_truth_csv # "/Users/atheriault/Downloads/cadets/labeled_ground_truth/bovia_webshell.csv"
    ground_truth_csv_text = open(group_truth_csv).read()
    group_truth_uuids = set(re.findall(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[a-f0-9]{12}",
        ground_truth_csv_text,
        re.I
    ))

    # Construct the list of final ADM uuids
    ground_truth_adm = list({ cdm2adm[cdm] for cdm in group_truth_uuids })
    json.dump(ground_truth_adm, sys.stdout, indent=4)




