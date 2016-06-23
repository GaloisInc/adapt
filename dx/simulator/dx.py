#! /usr/bin/env python3

import argparse
import simulator_diagnoser as sd
import logging
import random
import os

random.seed()

def set_log_handler(log, handler=logging.StreamHandler()):
    handler.setFormatter(formatter)
    log.addHandler(handler)

log = logging.getLogger('dx-logger')
formatter = logging.Formatter('%(asctime)s\t%(levelname)s\t%(pathname)s:%(lineno)d -- %(message)s')
log.setLevel(logging.INFO)
set_log_handler(log)

def stub_db(db, configs, tag='dx_phase2_stub'):
    log.info('Removing tagged instances from DB')
    db.drop_edges(tag=tag)
    db.drop_nodes(tag=tag)

    log.info('Inserting segmentation graph')
    segmentation_graph = configs.get_graph()
    segmentation_graph.store(db, tag=tag)

def diagnose(db, configs, tag='dx_phase2_stub'):
    segmentation_graph = sd.SegmentationGraph.read(db, tag=tag)
    symptoms = configs.get_symptoms(segmentation_graph)

    log.info('Diagnosing segmentation graph')
    dx = sd.SimpleDiagnoser(configs.get_grammar())
    dxs = dx.diagnose(segmentation_graph, symptoms)

    log.info('Inserting APT nodes')
    for path, matcher in dxs.iterate():
        apt_node = db.insert_node(db.generate_uuid(), vertexType='apt', tag=tag)
        for index, label in matcher.matches:
            dbnode = segmentation_graph.get_node_data(path[index], 'dbid')
            db.insert_edge(apt_node['id'], dbnode, 'aptContains', aptLabel=label, tag=tag)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Diagnose APT campaigns from segmented graph.')
    parser.add_argument('-n', '--no-kafka', action='store_true', help='disable kafka messaging.')
    parser.add_argument('-s', '--single-run', action='store_true', help='set DX to run only once.')
    args = parser.parse_args()

    if args.no_kafka:
        messaging = sd.Messenger()
    else:
        set_log_handler(log, sd.KafkaHandler())
        messaging = sd.KafkaMessenger()

    log.info('Arguments: ' + str(args))
    log.info('Using messenger: ' + type(messaging).__name__)
    db = sd.DBClient()

    scriptdir = os.path.dirname(os.path.abspath(__file__))
    configs = sd.ConfigParser(scriptdir + '/dx.yml')

    for _ in messaging.receive():
        # stub_db(db, configs)
        # diagnose(db, configs)

        if args.single_run:
            messaging.send()
            break
