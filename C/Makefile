CC=gcc
CFLAGS=-Wall -Werror -g

all: test.exe

test.exe: common.o object.o strfun.o readwrite.o argparse.o argparse_iforest.o test.o
	$(CC) $(CFLAGS) -o test.exe test.o common.o object.o strfun.o readwrite.o argparse.o argparse_iforest.o 

%.o: %.c %.h
	$(CC) $(CFLAGS) -c $<

test: test.exe
	./test.exe -ivHm potato 1,2-10,99-99 --header=true --outfile=potahto

fresh:
	make clean
	make all

clean:
	rm -rf *.o
	rm -rf *.exe
