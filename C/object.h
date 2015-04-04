#ifndef OBJECT
#define OBJECT

#include "common.h"

//dim-style memory management

//A notational macro used to indicate an obj was/should be
//allocated with dimension meta data.
#define d(x) x

typedef struct {
    size_t elsz;
    int ncol;
    int nrow;
    int ndep;
} dimmeta;

#define dim(obj) ((dimmeta*)bytewalk(obj,-sizeof(dimmeta)))
#define ncol(obj) (dim(obj)->ncol)
#define length(obj) ncol(obj)
#define nrow(obj) (dim(obj)->nrow)
#define ndep(obj) (dim(obj)->ndep)
#define ndim(obj) ({\
    assign(_obj,obj);\
    tern(ncol(_obj)>1,1,0)+tern(nrow(_obj)>1,1,0)+tern(ndep(_obj)>1,1,0);\
})
#define size(obj) ({\
    assign(_obj,obj);\
    ncol(_obj)*nrow(_obj)*ndep(_obj);\
})
#define vecalloc(t,len) ((t*)_vecalloc_(sizeof(t),len,NULL))
#define matalloc(t,nr,nc) ((t**)_matalloc_(sizeof(t),nr,nc,NULL,NULL))
#define dim3alloc(t,nd,nr,nc) ((t***)_dim3alloc_(sizeof(t),nd,nr,nc))
#define dimfree(ptr) _dimfree_((ptref)(ptr))
dimmeta* _vecalloc_(size_t elsz,int length,dimmeta* vec);
dimmeta** _matalloc_(size_t elsz,int nrow,int ncol, dimmeta** mat, dimmeta* vec);
dimmeta*** _dim3alloc_(size_t elsz,int ndep,int nrow,int ncol);
void _dimfree_(ptref ptr);
#define forvindex(i,vobj,code) {\
    int i;\
    for (i=0;i<length(vobj);i++) {\
        code\
    }}
#define forveach(each,vobj,code) {\
    assign(_vobj,vobj);\
    __typeof__(_vobj) each = _vobj;\
    forvindex(_i_,_vobj,{\
        code\
    } each++;)}
#define vseqcopy(dest,dbeg,dend,src,sbeg,send) ({\
    assign(_dbeg_,dbeg);\
    assign(_dend_,dend);\
    assign(_sbeg_,sbeg);\
    assign(_send_,send);\
    assign(_dest_,dest);\
    assign(_src_,src);\
    int _d_,_s_,_ds_,_ss_;\
    _ds_=tern(_dbeg_<_dend_,1,-1);\
    _ss_=tern(_sbeg_<_send_,1,-1);\
    for (_d_=_dbeg_,_s_=_sbeg_;\
        (tern(_ds_>0,_d_-_dend_,_dend_-_d_)<0)&&(tern(_ss_>0,_s_-_send_,_send_-_s_)<0);\
        _d_+=_ds_,_s_+=_ss_) {\
            _dest_[_d_]=_src_[_s_];\
    }\
})
#define vcopy(dest,src) vseqcopy(dest,0,length(dest),src,0,length(src))

//End dim-style

//Circular list managment
typedef struct listnode listnode;
typedef struct {
    int size;
    listnode* ptr;
} list;

#define list_push(lst,el) _list_in_(lst,t64binary(el),true)
#define list_add(lst,el) _list_in_(lst,t64binary(el),false)
#define list_in(lst,el,head) _list_in_(lst,t64binary(el),(head))
#define list_next(t,lst) f64binary(t,_list_out_(lst,true,false,true))
#define list_prev(t,lst) f64binary(t,_list_out_(lst,false,false,true))
#define list_peek(t,lst) f64binary(t,_list_out_(lst,true,false,false))
#define list_head(t,lst) list_peek(t,lst)
#define list_tail(t,lst) f64binary(t,_list_out_(lst,false,false,false))
#define list_pop(t,lst) f64binary(t,_list_out_(lst,true,true,false))
#define list_consume(t,lst) list_pop(t,lst)
#define list_remove(t,lst) f64binary(t,_list_out_(lst,false,true,false))
#define list_out(t,lst,head,remove,iterate) f64binary(t,_list_out_(lst,head,remove,iterate))

int _list_in_(list* lst,ptref el,bool head);
ptref _list_out_(list* lst,bool head,bool remove,bool iterate);
void list_free(list* lst,bool free_els);

//End circular list

#endif
