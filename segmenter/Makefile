
SEG1 =  testprov_out_segmented_694638006_1389213733.dot

OUT = \
 $(SEG1) \

all: $(OUT)

$(SEG1):
	cd $(HOME)/.virtualenvs/segmenter || mkvirtualenv segmenter
	if [ _`basename $(VIRTUAL_ENV)` != _segmenter ]; then bash -c 'echo Please run: workon segmenter; exit 1'; fi
	python -c 'import SPARQLWrapper' || pip install -r requirements.txt
	./segmenter.py     test/prov_out.ttl 694575727
	./segmented2dot.py test/prov_out.ttl
	./dot2viz.py

clean:
	rm -f $(OUT) *.dot *.dot.pdf test/prov_out_segmented_*.ttl
