.PHONY: run pdf

run: env
	env/bin/python sim.py

pdf: env
	env/bin/python sim.py pdf | dot -Tpdf -o sim.pdf

env: env/bin/activate
env/bin/activate: requirements.txt
	test -d env || virtualenv env
	env/bin/pip install -r requirements.txt
	touch env/bin/activate

