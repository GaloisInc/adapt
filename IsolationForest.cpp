/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */
#include<iostream>
#include<string>
#include<fstream>
#include<vector>
#include<cmath>
#include "classes.hpp"
#include "utility.h"

//build the forest
IsolationForest::IsolationForest(const int ntree,Data data,int maxheight,const int nsample,bool rSample)
{

this->nsample = nsample;
this->ntree=ntree;
Data sampleData;
int sampleIndex[nsample];
for(int n=0;n<ntree;n++)
{
    	if(rSample==true && nsample<data.nrows)
		   {  //sample index data and construct sample data
			  sampleI(0,data.nrows-1,nsample,sampleIndex);
			  vector<vector<float> > tempdata;
			  for(int ind=0;ind<nsample;ind++)
				  tempdata.push_back(data.data[sampleIndex[ind]]);
			   sampleData.data=tempdata;
			 sampleData.nrows = nsample;
			 sampleData.ncols=data.ncols;
			 //tempdata.clear();

		 }

	else
	  sampleData=data;
  this->data= sampleData;
 //cout<<data.nrows<<" Rows"<<this->data->ncols;
Tree *tree =new Tree();
tree->iTree(sampleData,0,maxheight);
this->trees.push_back(tree);

}

}

/*
 * Accepts single row point and return Anomaly Score
 */
float IsolationForest::instanceScore(vector<float> inst)
{
	double avgPathLength=0;
	//pointer based
	for(vector<Tree*>::iterator it=this->trees.begin();it!=trees.end();++it)
	{
	avgPathLength += (*it)->pathLength(inst);

	}
	float scores;
	avgPathLength /=(double) this->ntree;
   scores= pow(2,-avgPathLength/avgPL(this->nsample));
    return scores;
}

/*
 * Score for all points
 */
vector<float> IsolationForest::AnomalyScore(Data data){
  vector<float> scores;

	for(int inst=0;inst<(int)data.data.size();inst++)
	{


    scores.push_back(instanceScore(data.data[inst]));

	}
	return scores;



}



