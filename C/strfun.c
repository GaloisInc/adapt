#include "strfun.h"

int count_char(const char* str,char delim) {
    int count = 0;
    forseq(i,0,strlen(str),
        if (str[i]==delim) count++;
    )
    return count;
}

d(char*)* tokenize(const char* tstr,char delim) {
    d(char*)* tkns = vecalloc(char*,count_char(tstr,delim)+1);
    char *_tstr,*_delim,*saveptr;
    _tstr = nstrdup(tstr);
    _delim = talloc(char,2);
    _delim[0]=delim;_delim[1]='\0';
    bool needfree = true;
    forveach(token,tkns,
        *token = strtok_r(_tstr,_delim,&saveptr);
        if (needfree) {
            needfree = false;
            free(_tstr);
        }
        _tstr=NULL;
    )
    free(_delim);
    return tkns;
}

#define strtol_fix(ccp,eptr) strtol(ccp,eptr,10)
#define strtoll_fix(ccp,eptr) strtoll(ccp,eptr,10)
#define CODEGEN_CONV(t,cfunc) bool str_to_##t(t* convptr,char** strptr,bool advance,bool strict) {\
    char *str,*end;\
    end=str=*strptr;\
    *convptr = cfunc(str,&end);\
    if (advance) *strptr=end;\
    return exp_as_bool((str==end)||tern(strict,*end!='\0',false));\
}
CODEGEN_CONV(int,strtol_fix)
CODEGEN_CONV(long,strtoll_fix)
CODEGEN_CONV(float,strtof)
CODEGEN_CONV(double,strtod)
#undef CODEGEN_CONV
