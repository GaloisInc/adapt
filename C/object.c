
#include "object.h"

//Dim functions
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
    dimmeta*** ret =  (dimmeta***)bytewalk(dim3,sizeof(dimmeta));
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

void _dimfree_(ptref ptr) {
    ptref old;
    switch (ndim(ptr)) {
        case 3:
            old = ptr;
            ptr = (ptref)(*ptr);
            free(dim(old));
        case 2:
            old = ptr;
            ptr = (ptref)(*ptr);
            free(dim(old));
        default:
            free(dim(ptr));
    }
}
//End dim functions

//List functions
struct listnode {
    ptref el;
    listnode* next;
    listnode* prev;
};
#define node_attach(p,n) ({\
    if (p) p->next = n;\
    if (n) n->prev = p;\
})

int _list_in_(list* lst,ptref el,bool head) {
    listnode* inc = talloc(listnode,1);
    inc->el = el;
    if (lst->size==0) {
        lst->ptr = inc;
        node_attach(inc,inc);
    } else {
        node_attach(lst->ptr->prev,inc);
        node_attach(inc,lst->ptr);
        if (head) lst->ptr = inc;
    }
    return ++(lst->size);
}

ptref _list_out_(list* lst, bool head, bool remove, bool iterate) {
    if (lst->size == 0) return NULL;
    listnode* target = tern(head,lst->ptr,lst->ptr->prev);
    ptref el = target->el;
    if (remove) {
        lst->ptr = target->next;
        node_attach(lst->ptr,target->prev);
        if (lst->size == 1) lst->ptr = NULL;
        free(target);
        (lst->size)--;
    } else if (iterate) {
        lst->ptr = tern(head,target->next,target->prev);
    }
    return el;
}

void list_free(list* lst,bool free_els) {
    ptref ptr;
    while (lst->size>0) {
        ptr = list_consume(ptref,lst);
        if (free_els) free(ptr);
    }
    free(lst);
}
//End list functions
