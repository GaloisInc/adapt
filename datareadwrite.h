#ifndef DATARW
#define DATARW

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "objmacro.h"

#define safe_fopen(fname,mode) ({\
    FILE* ret = fopen(fname,mode);\
    ret!=NULL?ret:({fprintf(stderr,"Could not read file: %s\n",fname);exit(1);ret;});\
})

typedef struct linelist linelist;

float** read_csv(char* fname,bool header,bool rownames);
char* read_alloc_line(FILE* file);
int count_fields(const char* cstr,char delim);

#endif
