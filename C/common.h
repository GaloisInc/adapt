#ifndef COMMON
#define COMMON

#include <stdlib.h>
#include <stdio.h>

#define tern(c,t,e) ((c)?(t):(e))
#define assign(x,y) __typeof__(y) x = (y)
#define nullop(n,o) ({\
    assign(_n,n);\
    tern(_n,_n,o);\
})

typedef long* ptref;
#define PTRSZ (sizeof(ptref))
#define t64binary(x) ((ptref)(x))
#define f64binary(t,x) ((t)((long)x))

typedef unsigned char byte;
#define bytewalk(ptr,val) (((byte*)(ptr))+(val))

typedef int bool;
#define true 1
#define false 0
#define exp_as_bool(x) tern(x,true,false)
#define bool_as_string(x) tern(x,"true","false") 

#define talloc(t,n) ((t*)calloc(sizeof(t),n))


#define cons(t,...) (t[]){__VA_ARGS__}
#define ints(...) cons(int,__VA_ARGS__)
#define longs(...) cons(long,__VA_ARGS__)
#define doubles(...) cons(double,__VA_ARGS__)
#define floats(...) cons(float,__VA_ARGS__)

#define max(a,b) ({\
    assign(_a_,a);\
    assign(_b_,b);\
    tern(_a_>_b_,_a_,_b_);\
})
#define min(a,b) ({\
    assign(_a_,a);\
    assign(_b_,b);\
    tern(_a_<_b_,_a_,_b_);\
})
#define abs(x) ({\
    assign(_x_,x);\
    tern(_x_<0,-_x_,_x_);\
})

#define forstepseq(idx,beg,end,step,code) {\
    __typeof__((beg)+(end)+(step)) idx;\
    __typeof__(idx) _beg_ = beg;\
    __typeof__(idx) _end_ = end;\
    __typeof__(idx) _step_ = step;\
    _step_*=tern(_beg_<_end_,1,-1);\
    for (idx=_beg_;tern(_beg_<_end_,idx-_end_,_end_-idx)<0;idx+=_step_) {\
        code\
    }\
}

#define forseq(idx,beg,end,code) forstepseq(idx,beg,end,1,code)


#define err_and_exit(c,...) ({\
    fprintf(stderr,__VA_ARGS__);\
    exit(c);\
})

#endif
