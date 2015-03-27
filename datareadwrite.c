#include "datareadwrite.h"

struct linelist {
    char* line;
    linelist* next;
};

float** read_csv(char* fname,bool header,bool rownames) {
    float** ret;
    FILE* file = safe_fopen(fname,"rb");
    linelist* head = talloc(linelist,1);
    linelist* cur = head;
    int linecount = 0;
    char* line;
    do {
        cur->line = read_alloc_line(file);
        line = cur->line;
        if (strlen(line)>0) {
            cur->next = talloc(linelist,1);
            cur = cur->next;
            linecount++;
        } else {
            cur->next = NULL;
        }
    } while (strlen(line)>0);
    if (header==true) linecount--;
    cur = head;
    int ncols = count_fields(cur->line,',');
    if (rownames==true) ncols--;
    ret = matalloc(float,linecount,ncols);
    int r,c;
    char *token,*saveptr;
    for (r=0;r<nrow(ret);r++) {
        if (r==0&&header==true) {
            cur = cur->next;
        }
        line = cur->line;
        for (c=0;c<ncol(ret);c++) {
            token = strtok_r(line,",",&saveptr);
            line = NULL;
            if (c==0&&rownames==true) {
                token = strtok_r(line,",",&saveptr);
            }
            float f = strtof(token,NULL);
            ret[r][c]=f;
        }
        cur = cur->next;
    }
    return ret;
}

char* read_alloc_line(FILE* file) {
    int n = 0;
    char c;
    do {
        c=(char)fgetc(file);
        if (c!=EOF) n++;
    } while(c!=EOF&&c!='\n'&&c!='\0');
    fseek(file,-n,SEEK_CUR);
    char* line = talloc(char,n+1);
    line = fgets(line,n+1,file);
    return line;
}

int count_fields(const char* str,char delim) {
    int count = 1;
    int i;
    for (i=0; i < strlen(str); i++) {
        if (str[i]==delim) count++;
    }
    return count;
}
