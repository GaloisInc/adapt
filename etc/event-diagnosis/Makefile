.PHONY: run upreqs check start-daemons bad-ls infoleak

ENV = env
REQS = requirements.txt

ADAPT = $(HOME)/adapt

run: $(ENV)
	$(ENV)/bin/python main.py

$(ENV): $(ENV)/bin/activate
$(ENV)/bin/activate: $(REQS)
	test -d $(ENV) || virtualenv $(ENV)
	$(ENV)/bin/pip install -r $(REQS) > /dev/null
	touch $(ENV)/bin/activate

upreqs:
	$(ENV)/bin/pip freeze > $(REQS)

check: $(ENV)
	$(ENV)/bin/flake8 main.py

start-daemons:
	$(ADAPT)/install/start_daemons.sh

bad-ls:
	$(HOME)/.local/bin/Trint -u $(ADAPT)/bad-ls.provn

infoleak:
	$(HOME)/.local/bin/Trint -u $(ADAPT)/infoleak.provn
