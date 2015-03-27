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
using namespace std;
int main()
{
// 2-D data
const int NROW=100;
const int NCOL=10;
float data[NROW][NCOL];
vector< vector<float> > dt;
for(int i=0;i<NROW;i++)
{
	vector<float> x;
	for(int j=0;j<NCOL;j++)
	{
		data[i][j]=(j+i)*rand()/((float)RAND_MAX+1);
    	 x.push_back(data[i][j]);
	}
	dt.push_back(x);
	x.clear();
}
//Data input
Data train;
train.data = dt;
train.ncols=NCOL;
train.nrows=NROW;

int ntree=10;
int nsample=256;
bool rsample=true;
int maxheight = (int)ceil(log2(NROW));
IsolationForest *iff;
iff =new IsolationForest(ntree,train,maxheight,nsample,rsample);
Data test = train;
iff->AnomalyScore(test);

}




