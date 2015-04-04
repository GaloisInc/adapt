#include "readwrite.h"

char* read_next_line(FILE* file) {
    int n = 0;
    char c;
    do {
        c=(char)fgetc(file);
        if (c!=EOF) n++;
    } while(c!=EOF&&c!='\n'&&c!='\0');
    fseek(file,-n,SEEK_CUR);
    char* line = talloc(char,n+1);
    line = fgets(line,n+1,file);
    if (strlen(line)<=0) {
        free(line);
        return NULL;
    }
    return line;
}

stringframe* read_csv(char* fname,bool header,bool rownames) {
    stringframe* csv = talloc(stringframe,1);
    FILE* file = ee_fopen(fname,"rb");
    list* lines = talloc(list,1);
    char* line = read_next_line(file);
    while (line) {
        list_add(lines,line);
        line = read_next_line(file);
    }
    int linecount = lines->size;
    if (header) linecount--;
    line = list_next(char*,lines);
    line = list_next(char*,lines);
    int ncols = count_char(line,',');
    line = list_prev(char*,lines);
    line = list_prev(char*,lines);
    if (rownames) {
        csv->rownames = vecalloc(char*,linecount);
    } else {
        ncols++;
    }
    csv->data = matalloc(char*,linecount,ncols);
    d(char*)* tokens;
    forseq(r,0,nrow(csv->data),
        line = list_consume(char*,lines);
        tokens = tokenize(line,',');
        if (r==0&&header) {
            csv->colnames = tokens;
            line = list_consume(char*,lines);
            tokens = tokenize(line,',');
        }
        if (rownames) {
            csv->rownames[r] = tokens[0];
        }
        forvindex(c,csv->data[r],
            csv->data[r][c] = tokens[c+tern(rownames,1,0)];
        )
        dimfree(tokens);
    )
    return csv;
}

//End Public Definitions
