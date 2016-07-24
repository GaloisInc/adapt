import os, sys, json, re
sys.path.append(os.path.expanduser('~/adapt/segment/segmenter'))
from bareBonesTitanDB import BareBonesTitanClient
from flask import Flask, render_template, url_for, request, Response


app = Flask(__name__)
db = BareBonesTitanClient()

@app.route('/')
def service():
    os.system("./genSegGraph.sh")
    return render_template('index.html')


@app.route('/query', methods=["POST"])
def json_query_post():
	gremlin_query_string = request.form['query']
	return json_query_get(gremlin_query_string)


@app.route('/query/<gremlin_query_string>', methods=["GET"])
def json_query_get(gremlin_query_string):
	try:
		results = db.execute(gremlin_query_string)
		return Response(response=json.dumps(results), status=200, mimetype="application/json")
	except:
		return Response(status=500, response=str(result))


@app.route('/graph')
def graph():
	return render_template('graph.html')


app.run(host='0.0.0.0', port=8180)
