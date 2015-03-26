#ifndef OBJMACRO
#define OBJMACRO

#include <stdlib.h>

#define PTRSZ (sizeof(void*))
typedef struct dimmeta dimmeta;

#define dim(obj) ((dimmeta*)bytewalk(obj,-sizeof(dimmeta)))
#define ncol(obj) (dim(obj)->ncol)
#define length(obj) ncol(obj)
#define nrow(obj) (dim(obj)->nrow)
#define ndep(obj) (dim(obj)->ndep)
#define ndim(obj) ((ncol(obj)>1?1:0)+(nrow(obj)>1?1:0)+(ndep(obj)>1?1:0))
#define size(obj) (ncol(obj)*nrow(obj)*ndep(obj))
#define bytewalk(ptr,val) (((char*)(ptr))+(val))

#define cons(t,...) (t[]){__VA_ARGS__}
#define ints(...) cons(int,__VA_ARGS__)
#define dubs(...) cons(double,__VA_ARGS__)
#define flts(...) cons(float,__VA_ARGS__)

#define vecalloc(t,len) ((t*)_vecalloc_(sizeof(t),len,NULL))
#define matalloc(t,nr,nc) ((t**)_matalloc_(sizeof(t),nr,nc,NULL,NULL))
#define dim3alloc(t,nd,nr,nc) ((t***)_dim3alloc_(sizeof(t),nd,nr,nc))
#define dimfree(ptr) _confree_((dimmeta*)ptr)

dimmeta* _vecalloc_(size_t elsz,int length,dimmeta* vec);
dimmeta** _matalloc_(size_t elsz,int nrow,int ncol, dimmeta** mat, dimmeta* vec);
dimmeta*** _dim3alloc_(size_t elsz,int ndep,int nrow,int ncol);
void _dimfree_(dimmeta* ptr);

#endif
