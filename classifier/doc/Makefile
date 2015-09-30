
OUT = \
 API.pdf \

all: $(OUT)

%.html: %.md
	pandoc -o $@ $<

%.pdf: %.md
	pandoc -o $@ $<

clean:
	rm -f $(OUT)
