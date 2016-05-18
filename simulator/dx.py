import sys
import simulator_diagnoser as sd
import logging
import random

random.seed()

log = logging.getLogger('dx-logger')
formatter = logging.Formatter('%(asctime)s\t%(levelname)s\t%(pathname)s:%(lineno)d -- %(message)s')
for handler in [logging.StreamHandler(), sd.KafkaHandler()]:
    handler.setFormatter(formatter)
    log.addHandler(handler)
log.addHandler(handler)
log.setLevel(logging.INFO)

def stub_db(db, tag='dx_phase2_stub'):
    log.info('Removing tagged instances from DB')
    db.drop_nodes(tag=tag)

    log.info('Inserting segmentation nodes')
    v1 = db.insert_node(db.generate_uuid(), vertexType='segment', tag=tag, desc='v1')
    v2 = db.insert_node(db.generate_uuid(), vertexType='segment', tag=tag, desc='v2')
    v3 = db.insert_node(db.generate_uuid(), vertexType='segment', tag=tag, desc='v3')
    v4 = db.insert_node(db.generate_uuid(), vertexType='segment', tag=tag, desc='v4')
    v5 = db.insert_node(db.generate_uuid(), vertexType='segment', tag=tag, desc='v5')
    v6 = db.insert_node(db.generate_uuid(), vertexType='segment', tag=tag, desc='v6')

    log.info('Inserting segmentation edges')
    db.insert_edge(v1['id'], v2['id'], 'segmentEdge', tag=tag)
    db.insert_edge(v2['id'], v1['id'], 'segmentEdge', tag=tag) # can contain cycles
    db.insert_edge(v2['id'], v3['id'], 'segmentEdge', tag=tag)
    db.insert_edge(v2['id'], v4['id'], 'segmentEdge', tag=tag)
    db.insert_edge(v4['id'], v5['id'], 'segmentEdge', tag=tag)
    db.insert_edge(v5['id'], v6['id'], 'segmentEdge', tag=tag)

def diagnose(db, tag='dx_phase2_stub'):
    log.info('Diagnosing segmentation graph')
    nodes = db.get_nodes(tag=tag)
    starting_symptom = random.choice(nodes)
    nodes = db.get_transitive_successors(starting_symptom['id'])
    if nodes == None:
        path = [starting_symptom]
    else:
        path = random.choice(nodes)['objects']

    log.info('Inserting APT node')
    apt_node = db.insert_node(db.generate_uuid(), vertexType='apt', tag=tag)

    for node in path:
        db.insert_edge(apt_node['id'], node['id'], 'aptContains', tag=tag)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == 'fakemsg':
        messaging = sd.Messenger()
    else:
        messaging = sd.KafkaMessenger()

    log.info('Using messenger: ' + type(messaging).__name__)
    db = sd.DBClient()

    for _ in messaging.receive():
        stub_db(db)
        diagnose(db)

        messaging.send()
        break
