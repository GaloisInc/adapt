
#include "objmacro.h"

struct dimmeta {
    size_t elsz;
    int ncol;
    int nrow;
    int ndep;
};

dimmeta* _vecalloc_(size_t elsz,int length,dimmeta* vec) {
    if (vec==NULL) {
        vec = (dimmeta*)calloc(sizeof(dimmeta)+(elsz*length),1);
    }
    vec[0].elsz = elsz;
    vec[0].ncol = length;
    vec[0].nrow = 1;
    vec[0].ndep = 1;
    return vec+1;
}

dimmeta** _matalloc_(size_t elsz,int nrow,int ncol,dimmeta** mat,dimmeta* vec) {
    if (vec==NULL) {
        vec = (dimmeta*)calloc(sizeof(dimmeta)+(elsz*ncol),nrow);
    }
    if (mat==NULL) {
        mat = (dimmeta**)malloc(sizeof(dimmeta)+(PTRSZ*nrow));
    }
    dimmeta** ret =  (dimmeta**)bytewalk(mat,sizeof(dimmeta));
    dim(ret)[0].elsz = elsz;
    dim(ret)[0].ncol = ncol;
    dim(ret)[0].nrow = nrow;
    dim(ret)[0].ndep = 1;
    int i;
    for (i=0;i<nrow;i++) {
        dimmeta* vecp = (dimmeta*)bytewalk(vec,i*(sizeof(dimmeta)+elsz*ncol));
        ret[i] = _vecalloc_(elsz,ncol,vecp);
    }
    return ret;
}

dimmeta*** _dim3alloc_(size_t elsz,int ndep,int nrow,int ncol) {
    dimmeta* vec = (dimmeta*)calloc(sizeof(dimmeta)+(elsz*ncol),nrow*ndep);
    dimmeta** mat = (dimmeta**)malloc((sizeof(dimmeta)+(PTRSZ*nrow))*ndep);
    dimmeta*** dim3 = (dimmeta***)malloc(sizeof(dimmeta)+(PTRSZ*ndep));
    dimmeta*** ret =  (dimmeta***)bytewalk(ret,sizeof(dimmeta));
    dim(ret)[0].elsz = elsz;
    dim(ret)[0].ncol = ncol;
    dim(ret)[0].nrow = nrow;
    dim(ret)[0].ndep = ndep;
    int i;
    for (i=0;i<ndep;i++) {
        dimmeta** matp = (dimmeta**)bytewalk(mat,i*(sizeof(dimmeta)+(PTRSZ*nrow)));
        dimmeta* vecp = (dimmeta*)bytewalk(vec,i*((sizeof(dimmeta)+(elsz*ncol))*nrow));
        ret[i] = _matalloc_(elsz,nrow,ncol,matp,vecp);
    }
    return ret;
}

void _dimfree_(dimmeta* ptr) {
}
