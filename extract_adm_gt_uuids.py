#! /usr/bin/env python3

import json, re, argparse, sys, requests

if __name__ == '__main__':

    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Compute ADM ground truth UUIDs')
    parser.add_argument('namespace', metavar='NAMESPACE', type=str,
                        default='', help="The namespace under which data was ingested")
    parser.add_argument('ground_truth_csv', metavar='FILE', type=str,
                        help="One of the ground truth CSVs, as in the 'labeled_ground_truth'")
    parser.add_argument('url', metavar='URL', type=str, nargs='?',
                        default='http://localhost:8080')
    args = parser.parse_args()

    # Compute post url
    post_url = args.url + "/query/remap-uuid"
    sys.stderr.write("Queries are sent to '{}'.\n".format(post_url))

    # Get the UUIDs in the CSV
    group_truth_csv = args.ground_truth_csv # "/Users/atheriault/Downloads/cadets/labeled_ground_truth/bovia_webshell.csv"
    ground_truth_csv_text = open(group_truth_csv).read()
    group_truth_uuids = set(re.findall(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[a-f0-9]{12}",
        ground_truth_csv_text,
        re.I
    ))

    # Construct the list of final ADM uuids
    prefix = "cdm_" if not args.namespace else "cdm_" + args.namespace + "_"
    ground_truth_adm = set()
    for cdm in group_truth_uuids:
        prefixed_cdm = prefix + cdm
        adm = requests.get(post_url + "/" + prefixed_cdm).json()
        ground_truth_adm.add(adm)
    json.dump(list(ground_truth_adm), sys.stdout, indent=4)




