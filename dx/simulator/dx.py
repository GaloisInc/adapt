#! /usr/bin/env python3

import argparse
import simulator_diagnoser as sd
import logging
import random
import os

def set_log_handler(log, handler=logging.StreamHandler()):
    handler.setFormatter(formatter)
    log.addHandler(handler)

logging.basicConfig(level=0)
log = logging.getLogger('dx-logger')
formatter = logging.Formatter('%(asctime)s -- %(message)s')
log.setLevel(logging.INFO)
set_log_handler(log)
log.propagate = 0

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

    scriptdir = os.path.dirname(os.path.abspath(__file__))
    configs = sd.ConfigParser(scriptdir + '/dx.yml')

    for _ in messaging.receive():
        graph = sd.DBGraph()
        graph.clear_diagnoses()
        matcher = sd.StatelessMatcher(configs.get_grammar())

        log.info('Starting diagnoser')
        diagnoser = sd.APTDiagnoser(graph, matcher, avoid_cycles=True)
        log.info('Diagnoser has finished')

        if args.single_run:
            messaging.send()
            break
