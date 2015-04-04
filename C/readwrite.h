#ifndef READWRITE
#define READWRITE

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "common.h"
#include "object.h"
#include "strfun.h"

#define ee_fopen(fname,mode) ({\
    FILE* ret = fopen(fname,mode);\
    tern(ret!=NULL,ret,({err_and_exit(1,"Could not open file: %s\n",fname);ret;}));\
})

typedef struct stringframe {
    d(char*)* colnames;
    d(char*)* rownames;
    d(char*)** data;
} stringframe;

stringframe* read_csv(char* fname,bool header,bool rownames);
char* read_next_line(FILE* file);

#endif
