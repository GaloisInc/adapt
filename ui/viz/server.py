import os

from flask import Flask
app = Flask(__name__, static_url_path='')

@app.route('/')
def service():
    os.system("./genSegGraph.sh")
    return render_template('index.html')


app.run(host='0.0.0.0', port=8180)
