CFLAGS=-Wall -pedantic
ifeq (, $(findstring Windows, $(OS)))
CFLAGS+=-ansi
endif

FOPT=-O3 -fomit-frame-pointer

FI686=-m32 -march=i686 -mtune=i686
FAMD64=-m64
FSPARC=-m64 -mcpu=niagara -mtune=niagara
ifneq (, $(findstring Windows, $(OS)))
ARCH=i686
FARCH=$(FI686)
else
ARCH=$(shell uname -m)
ifeq ($(ARCH), i686)
FARCH=$(FI686)
endif
ifeq ($(ARCH), x86_64)
FARCH=$(FAMD64)
endif
ifeq ($(ARCH), sparc64)
FARCH=$(FSPARC)
endif
endif

all: pcbo

pcbo: pcbo.c
ifneq (, $(findstring Windows, $(OS)))
	gcc $(CFLAGS) $(FOPT) $(FARCH) -o pcbo.exe $^
	strip pcbo.exe
else
	$(CC) $(CFLAGS) $(FOPT) $(FARCH) -pthread -o pcbo $^
	strip pcbo
endif

debug: pcbo.c
ifneq (, $(findstring Windows, $(OS)))
	gcc $(CFLAGS) -g $(FARCH) -o pcbo.exe $^
else
	$(CC) $(CFLAGS) -g $(FARCH) -pthread -o pcbo $^
endif

static: pcbo.c
ifneq (, $(findstring Windows, $(OS)))
	gcc $(CFLAGS) $(FOPT) $(FARCH) -static -o pcbo-windows-$(ARCH)-static.exe $^
	strip pcbo-windows-$(ARCH)-static.exe
else
	$(CC) $(CFLAGS) $(FOPT) $(FARCH) -static -o pcbo-linux-$(ARCH)-static $^ -pthread
	strip pcbo-linux-$(ARCH)-static
endif

clean:
	rm -f pcbo pcbo.exe pcbo*static*
