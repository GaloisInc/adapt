
all: fg-test bg-test

# This can be integrated with upstream deps after phase2 commits are available.
fg-test:
	nosetests3 --with-doctest --doctest-tests phase2_test.py classify/*.py
	flake8 *.py */[a-z]*.py

# classifyd.py should already be running in the background under supervisord.
# If it is not running, then
#   1) it will not produce a "recvd msg" log entry, and
#   2) as a feature of kafka, the foreground client won't notice any trouble.
bg-test:
	@echo; echo; echo Testing the classifier daemon...
	./faux_seg.py
