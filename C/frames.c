#include "frames.h"

#define CODEGEN_INITFRAME(t) CODEGEN_INITFRAMESIG(t) {\
    nrow=choice(mat,choice(column_major,ncol(mat),nrow(mat)),nrow);\
    ncol=choice(mat,choice(column_major,nrow(mat),ncol(mat)),ncol);\
    t##frame* frame = talloc(t##frame,1);\
    frame->column_major = column_major;\
    frame->data = nulloption(mat,matalloc(t,choice(column_major,ncol,nrow),choice(column_major,nrow,ncol)));\
    frame->nrow = nrow;\
    frame->ncol = ncol;\
    return frame;\
}

#define CODEGEN_CUTFRAME(t) CODEGEN_CUTFRAMESIG(t) {\
    d(t*)* cutmajors = vecalloc(t*,length(majors));\
    for_each_in_vec(newidx,oldidxptr,majors,cutmajors[newidx]=tocut->data[*oldidxptr];)\
    *dim(cutmajors)=*dim(tocut->data);\
    nrow(cutmajors)=length(majors);\
    return frame(t,cutmajors,tocut->column_major);\
}

#define CODEGEN_SPLITFRAME(t) CODEGEN_SPLITFRAMESIG(t) {\
    if (!majors) return NULL;\
    t##frame* oldframe = *tosplit;\
    t##frame* postcut = _cut_##t##frame_(oldframe,majors);\
    hash_table* umaj = make_hash_table(nrow(oldframe->data));\
    for_each_in_vec(i,ref,majors,hash_set(umaj,*ref);)\
    int oldpos = 0;\
    forseq(newpos,0,nrow(oldframe->data),{\
        if (!hash_contains(umaj,newpos)) {\
            oldframe->data[oldpos]=oldframe->data[newpos];\
            oldpos++;\
        }\
    })\
    nrow(oldframe->data)=oldpos;\
    choice(oldframe->column_major,oldframe->ncol=oldpos,oldframe->nrow=oldpos);\
    return postcut;\
}

#define CODEGEN_CONVFRAME(t,u,cfunc) CODEGEN_CONVFRAMESIG(t,u) {\
    t##frame* tf = init_frame(t,uf->nrow,uf->ncol,uf->column_major);\
    for_each_in_frame(m,n,ta,tf,cfunc(ta,uf->data[m][n]);)\
    return tf;\
}\

#define CODEGEN_DEFINEFRAME(t)\
CODEGEN_INITFRAME(t)\
CODEGEN_CUTFRAME(t)\
CODEGEN_SPLITFRAME(t)

CODEGEN_DEFINEFRAME(ntstring)
CODEGEN_DEFINEFRAME(int)
CODEGEN_DEFINEFRAME(long)
CODEGEN_DEFINEFRAME(float)
CODEGEN_DEFINEFRAME(double)

#define str_to_double_fix(fa,str) str_conv_strict(fa,double,str)
CODEGEN_CONVFRAME(double,ntstring,str_to_double_fix)
#undef str_to_float_fix

