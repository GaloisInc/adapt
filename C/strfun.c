#include "strfun.h"

int count_char(const char* str,char delim) {
    int count = 0;
    forseq(i,0,strlen(str),{
        if (str[i]==delim) count++;
    })
    return count;
}

d(char*)* tokenize(const char* tstr,char delim) {
    d(char*)* tkns = vecalloc(char*,count_char(tstr,delim)+1);
    char *_tstr,*_delim,*saveptr;
    _tstr = nstrdup(tstr);
    _delim = talloc(char,2);
    _delim[0]=delim;_delim[1]='\0';
    for_each_in_vec(i,token,tkns,{
        *token = nstrdup(strtok_r(choice(i==0,_tstr,NULL),_delim,&saveptr));
    })
    free(_delim);
    free(_tstr);
    return tkns;
}

#define strtol_fix(ccp,eptr) strtol(ccp,eptr,10)
#define strtoll_fix(ccp,eptr) strtoll(ccp,eptr,10)
#define CODEGEN_STRCONV(t,cfunc) CODEGEN_STRCONVSIG(t) {\
    char *str,*end;\
    end=str=*strptr;\
    *convptr = cfunc(str,&end);\
    if (advance) *strptr=end;\
    return (str==end)||choice(strict,*end!='\0',false);\
}
CODEGEN_STRCONV(int,strtol_fix)
CODEGEN_STRCONV(long,strtoll_fix)
CODEGEN_STRCONV(float,strtof)
CODEGEN_STRCONV(double,strtod)
#undef strol_fix
#undef stroll_fix
