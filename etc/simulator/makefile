.PHONY: run pdf dxpdf upreqs check

ENV = env
REQS = requirements.txt
MODULE = simulator_diagnoser

run: $(ENV)
	$(ENV)/bin/python sim.py

pdf: $(ENV)
	$(ENV)/bin/python sim.py pdf | dot -Tpdf -o sim.pdf

dxpdf: $(ENV)
	$(ENV)/bin/python sim.py dxpdf | dot -Tpdf -o sim.pdf

$(ENV): $(ENV)/bin/activate
$(ENV)/bin/activate: $(REQS)
	test -d $(ENV) || virtualenv $(ENV)
	$(ENV)/bin/pip install -r $(REQS)
	touch $(ENV)/bin/activate

upreqs:
	$(ENV)/bin/pip freeze > $(REQS)

check: $(ENV)
	$(ENV)/bin/pep8 --config=.pep8 $(MODULE) sim.py