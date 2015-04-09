/*
 * main.cpp
 *
 *  Created on: Mar 22, 2015
 *      Author: Tadeze
 */
#include<iostream>
#include<fstream>
#include<cstdlib>
#include "classes.hpp"
#include "utility.h"
using namespace std;
int main()
{
srand(time(NULL));
//input parameters
int ntree=1000;
int nsample=256;
bool rsample=true;

//Prepare synthetic data
const int NROW=10000;
const int NCOL=40;
float data[NROW][NCOL];
vector< vector<float> > dt;
//save data
ofstream myfile;
myfile.open("data.csv");

for(int i=0;i<NROW;i++)
{
	vector<float> x;
	for(int j=0;j<NCOL;j++)
	{
		data[i][j]=(10)*rand()/((float)RAND_MAX+1);
    	 x.push_back(data[i][j]);
    myfile<<data[i][j]<<",";
	}
	myfile<<"\n";
	dt.push_back(x);
	x.clear();
}
myfile.close();
//Data input
Data train;
train.data = dt;
train.ncols=NCOL;
train.nrows=NROW;

//forest configuration
int maxheight =(int)ceil(log2(NROW));
IsolationForest iff(ntree,train,maxheight,nsample,rsample);

//Scores
ofstream scoref;
scoref.open("scores.csv");
Data test = train; //assuming train and test data are same.
vector<float> scores = iff.AnomalyScore(test);
for(int j=0;j<(int)scores.size();j++)
{	cout<<"Score "<<j<<"= "<<scores[j]<<endl;
    scoref<<j+1<<","<<scores[j]<<"\n";
}
scoref.close();

return 0;
}





