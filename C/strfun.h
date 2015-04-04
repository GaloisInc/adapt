#ifndef STRFUN
#define STRFUN

#include <string.h>
#include "common.h"
#include "object.h"

#define streq(x,y) !strcmp(x,y)
#define nstrdup(s) tern(s,strdup(s),NULL)
#define str_conv(aa,t,str) str_to_##t(aa,&(str),false,false)
#define str_conv_adv(aa,t,str) str_to_##t(aa,&(str),true,false)
#define str_conv_strict(aa,t,str) str_to_##t(aa,&(str),false,true)

int count_char(const char* cstr,char delim);
d(char*)* tokenize(const char* tstr,char delim);

#define CODEGEN_CONVSIG(t) bool str_to_##t(t* convptr,char** strptr,bool advance,bool strict)
CODEGEN_CONVSIG(int);
CODEGEN_CONVSIG(long);
CODEGEN_CONVSIG(float);
CODEGEN_CONVSIG(double);
#undef CODEGEN_CONVSIG

#endif
