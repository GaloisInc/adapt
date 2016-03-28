.PHONY: run test test-coverage coverage coverage-html pdf dxpdf upreqs check help

MODULE = simulator_diagnoser

ENV = env
REQS = config/requirements.txt

PYTHON = $(ENV)/bin/python
PIP = $(ENV)/bin/pip
PEP8 = $(ENV)/bin/pep8
COVERAGE = $(ENV)/bin/coverage
COVERAGERC = --rcfile=config/coveragerc

TEST_PATTERN = -p "*_test.py"

## Default target. Same as 'make run'.
all: run

## Run entrypoint: sim.py.
run: $(ENV)
	$(PYTHON) sim.py

## Run tests.
test: $(ENV)
	$(PYTHON) -m unittest discover -v $(TEST_PATTERN)

test-coverage: $(ENV)
	$(COVERAGE) run --source=$(MODULE) $(COVERAGERC) -m unittest discover $(TEST_PATTERN)

## Generate coverage report.
coverage: test-coverage
	$(COVERAGE) report $(COVERAGERC)

## Generate HTML coverage report.
coverage-html: test-coverage
	$(COVERAGE) html $(COVERAGERC)

## Run PEP8 style check.
check: $(ENV)
	$(PEP8) --config=config/pep8 $(MODULE) sim.py

pdf: $(ENV)
	$(PYTHON) sim.py pdf | dot -Tpdf -o sim.pdf

dxpdf: $(ENV)
	$(PYTHON) sim.py dxpdf | dot -Tpdf -o sim.pdf


## Generate data to d3 viz: open viz/index.html to see the report.
d3: $(ENV)
		$(PYTHON) sim.py json > $(MODULE)/viz/data.json

$(ENV): $(ENV)/bin/activate
$(ENV)/bin/activate: $(REQS)
	test -d $(ENV) || virtualenv $(ENV)
	$(PIP) install -r $(REQS)
	touch $(ENV)/bin/activate

## Update requirements.txt file by running 'pip freeze'.
upreqs:
	$(PIP) freeze > $(REQS)

## Display this help text.
help:
	$(info Available targets:)
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "%-15s %s\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)
