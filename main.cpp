/*
  TODO: implement the reading csv file operation
 * main.cpp
 *
 *  Created on: Mar 22, 2015
 *      Author: Tadeze
 */
#include<iostream>
#include<fstream>
#include<cstdlib>
#include<ctime>
#include "classes.hpp"
#include "utility.h"
using namespace std;

int main()
{

/*TODO: check on real csv file and compare with implemented algorithms
 * Check the pathLength with anomaly points
 */

srand(time(NULL));
/*//input parameters
for(int j=0;j<100;j++)
	cout<<randomI(0,10)<<endl;*/



int ntree=100;
int nsample=100;
bool rsample=true;

//Prepare synthetic data
const int NROW=1000;
const int NCOL=10;
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
		if(i%100==0)
		 data[i][j]=(float)randomD(5*j,i*j+1);
		else
		data[i][j]=(float)randomD(-3,10);//(10)*rand()/((float)RAND_MAX+1);
    	 x.push_back(data[i][j]);
    myfile<<data[i][j]<<"\t";
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
int maxheight =(int)ceil(log2(nsample));
IsolationForest iff(ntree,train,maxheight,nsample,rsample);
//Scores
ofstream scoref;
scoref.open("scores.csv");
Data test = train; //assuming train and test data are same.
vector<float> scores = iff.AnomalyScore(test);
vector<vector<float> > pathLength=iff.pathLength(test);
for(int j=0;j<(int)scores.size();j++)
{	cout<<"Score "<<j<<"= "<<scores[j]<<"\t"<<mean(pathLength[j])<<endl;

}
scoref.close();
cout<<"finished process";



return 0;
}





