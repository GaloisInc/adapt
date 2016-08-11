import os, sys, json, re
sys.path.append(os.path.expanduser('~/adapt/segment/segmenter'))
from bareBonesTitanDB import BareBonesTitanClient
from flask import Flask, render_template, url_for, request, Response
import logging
from logging import FileHandler

from showSegmentsSubGraph import renderSegments


app = Flask(__name__)
db = BareBonesTitanClient()

@app.route('/')
def service():
    renderSegments()
    return render_template('index.html')


@app.route('/query', methods=["POST"])
def json_query_post():
	gremlin_query_string = request.form['query']
	return json_query_get(gremlin_query_string)


@app.route('/query/<gremlin_query_string>', methods=["GET"])
def json_query_get(gremlin_query_string):
	app.logger.info("Received query: %s" % gremlin_query_string)
	result = "db.execute(%s) never returned a good result!" % gremlin_query_string
	try:
		result = db.execute(gremlin_query_string)
		return Response(response=json.dumps(result), status=200, mimetype="application/json")
	except:
		return Response(status=500, response=str(result))


@app.route('/graph')
def graph():
	return render_template('graph.html')


log_handler = FileHandler(os.path.expanduser('~/flask.log'), delay=True)  # https://docs.python.org/dev/library/logging.handlers.html#logging.FileHandler
log_handler.setLevel(logging.WARNING)
app.logger.addHandler(log_handler)

app.run(host='0.0.0.0', port=8180, processes=4)
