import os
import sys
import json
import re
import flask
import logging

import networkx

sys.path.append(os.path.expanduser('/vagrant/pylib'))
sys.path.append(os.path.expanduser('/vagrant/classifier'))
from titanDB import TitanClient as BareBonesTitanClient

from flask import Flask, render_template, url_for, request, Response
from logging import FileHandler
from time import localtime, strftime

from showSegmentsSubGraph import renderSegments

from ace.provenance_graph import ProvenanceGraph

app = Flask(__name__)
db = BareBonesTitanClient()

provenanceGraph = ProvenanceGraph()

@app.route('/')
def service():
    return render_template('index.html')

@app.route('/segmentation')
def segmentation():
    renderSegments()
    return render_template('segmentation.html')

@app.route('/classification')
def classification():
    data = []
    segmentNodes = provenanceGraph.segmentNodes()
    for segmentNode in segmentNodes:
        segmentId = segmentNode['id']
        activityId, activityType, suspicionScore = provenanceGraph.getActivity(segmentId)
        data.append({ 'segmentId' : segmentId,
                      'activityId' : activityId,
                      'activityType' : activityType,
                      'suspicionScore' : suspicionScore })

    return render_template('classification.html', rows = data)

@app.route('/edit_activity/<id>', methods = ['GET'])
def edit_activity(id):
    id = int(id)
    name = flask.request.args['name']
    value = flask.request.args['value']
    value = value.replace('<br>', '')
    if name == 'type':
        provenanceGraph.changeActivityType(id, value)
    elif name == 'score':
        provenanceGraph.changeActivitySuspicionScore(id, value)
    else:
        return "FAIL"
    
    return "OK"

@app.route('/show_segment/<id>')
def show_segment(id):
    G = provenanceGraph.getSegment(id)
    H = networkx.to_pydot(G)
    H.write_png('static/segment' + str(id) + '.png')
    return render_template('show_segment.html', id = id)

@app.route('/query', methods=["POST"])
def json_query_post():
    gremlin_query_string = request.form['query']
    return json_query_get(gremlin_query_string)

@app.route('/query/<gremlin_query_string>', methods=["GET"])
def json_query_get(gremlin_query_string):
    timestamp = strftime('%Y-%m-%d %H:%M:%S', localtime())
    app.logger.info("%s -- Received query: %s <br/>\n" % (timestamp, gremlin_query_string))
    result = "db.execute(%s) never returned a good result! <br/>\n" % gremlin_query_string
    try:
        result = db.execute(gremlin_query_string)
        app.logger.info("%s -- Retrieved results: %i <br/>\n" % (timestamp, len(result)))
        return Response(response=json.dumps(result), status=200, mimetype="application/json")
    except:
        app.logger.info("%s -- Could not retrieve result with message: %s <br/>\n" % (timestamp, result))
        return Response(status=500, response=str(result))


@app.route('/graph')
def graph():
    return render_template('graph.html')


@app.route("/log")
def log():
    return render_template('flask.log')


log_handler = FileHandler(os.path.expanduser('~/adapt/ui/viz/templates/flask.log'), "w")  # https://docs.python.org/dev/library/logging.handlers.html#logging.FileHandler
log_handler.setLevel(logging.INFO)
app.logger.addHandler(log_handler)

app.run(host='0.0.0.0', port=8180, processes=4)
