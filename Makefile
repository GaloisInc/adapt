CC=gcc
CFLAGS=--std=c99 -D_GNU_SOURCE -Wall -Werror -g
PP=g++
PFLAGS=-Wall -Werror -g

all: iforest.exe

cincl.o: C/common.o C/object.o C/strfun.o C/readwrite.o C/argparse.o C/argparse_iforest.o C/frames.o
	ld -r C/common.o C/object.o C/strfun.o C/readwrite.o C/argparse.o C/argparse_iforest.o C/frames.o -o cincl.o

C/%.o: C/%.c C/%.h
	$(CC) $(CFLAGS) -c $< -o $@

IsolationForest.o: IsolationForest.cpp classes.hpp
	$(PP) $(PFLAGS) -c $< -o $@

Tree.o: Tree.cpp classes.hpp
	$(PP) $(PFLAGS) -c $< -o $@

utility.o: utility.cpp utility.h
	$(PP) $(PFLAGS) -c $< -o $@

main.o: main.cpp utility.h cincl.hpp classes.hpp
	$(PP) $(PFLAGS) -c $< -o $@

iforest.exe: cincl.o IsolationForest.o Tree.o utility.o main.o
	$(PP) $(PFLAGS) -o iforest.exe cincl.o IsolationForest.o Tree.o utility.o main.o

fresh:
	make clean
	make all

clean:
	rm -rf *.o*
	rm -rf *.exe
