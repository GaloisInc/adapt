import os, sys, json, re, logging
sys.path.append(os.path.expanduser('~/adapt/pylib'))
from titanDB import TitanClient as BareBonesTitanClient
from flask import Flask, render_template, url_for, request, Response
from logging import FileHandler
from time import localtime, strftime

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
