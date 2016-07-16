#!/bin/sh

export FLASK_APP=server.py

cd $1; python3 -m flask run
