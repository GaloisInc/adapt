import os

from flask import Flask, render_template, url_for
app = Flask(__name__)

@app.route('/')
def service():
    os.system("./genSegGraph.sh")
    return render_template('index.html')

app.run(host='0.0.0.0', port=8180)
