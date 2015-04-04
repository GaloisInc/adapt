#include "test.h"

int main(int argc, char** argv) {
    parsed_args* pargs = parse_args(argc,argv);
    printf(
        "%s\n%s\n%d - %d\n%d - %d - %d\n%s - %s\n",
        pargs->input_name,
        pargs->output_name,
        tern(pargs->metacol,pargs->metacol[0],0),
        tern(pargs->metacol,length(pargs->metacol),0),
        pargs->ntrees,
        pargs->sampsize,
        pargs->maxdepth,
        bool_as_string(pargs->header),
        bool_as_string(pargs->verbose)
    );
    return 0;
}
