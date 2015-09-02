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

//rf.buildForest(df);
cout<<"Let's build reallforest on realdata\n";

RForest rf(10,df,0,7,true,true);
rf.rForest();
cout<<"Forest built\n";
//rf.AnomalyScore(df);
/*

for(int i=0;i<rf.rotMatrices.size();i++)
{
	cout<<rf.rotMatrices.at(i)<<endl;
	cout<<"-----------------\n";
}

*/

vector<double> score= rf.AnomalyScore(df);

for (int j = 0; j < (int) score.size(); j++) {
	cout<<score[j]<<"\n";
}
cout<<"Score obtained"<<endl;




/*
int _ntree,doubleframe* df,bool _rSample,int _nsample,
		   bool _stopheight,int _maxheight
*/

//delete df;

//
//for(int i=0;i<10;i++)
// delete df->data[i];
 delete[] df->data;
 delete df;

return 0;

}


