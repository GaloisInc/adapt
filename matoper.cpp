/*
 * matoper.cpp
 *
 *  Created on: Aug 26, 2015
 *      Author: tadeze
 */
#include "RForest.hpp"
#include<iostream>
using namespace std;
/*
struct _doubleframe 
{
    double** dt;
    int nrow;
    int ncol;
    
};
typedef struct _doubleframe doubleframe;
*/
int main()
{
std::cout<<" Matrix rotation "<<std::endl;

RForest rf;
//rf.buildForest();

doubleframe* df=new doubleframe();


//double** dt;
df->data = new double*[10];
df->nrow = 10;
df->ncol = 4;
for(int i=0;i<10;i++)
    df->data[i] = new double[4];
for(int i=0;i<10;i++)
 for(int j=0;j<4;++j)
    df->data[i][j]=i*j; 
//df->nrow=10;
//df->ncol=4;
std::cout<<"Content of the frame is ";
for(int i=0;i<10;i++)
{
    for(int j=0;j<4;++j)
        std::cout<<df->data[i][j]<<"\t";
    std::cout<<"\n";
}

rf.buildForest(df);






for(int i=0;i<10;i++)
 delete df->data[i];
 delete[] df->data;
 delete df;

return 0;

}


