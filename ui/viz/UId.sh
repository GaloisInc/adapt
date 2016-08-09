#!/bin/sh

export FLASK_APP=server.py

cd $1; exec python3 -m flask run
