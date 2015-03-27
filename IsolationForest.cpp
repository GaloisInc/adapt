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
using namespace std;
//build ntree forest from the data

//build the forest
IsolationForest::IsolationForest(const int ntree,Data data,int maxheight,const int nsample,bool rSample)
{

	this->trees[ntree];
	this->nsample = nsample;
	int* sampleIndex;

	for(int n=0;n<ntree;n++)
	{
		  if(rSample==true)
		   {
			  //get sample index data from the
			  sampleIndex =sampleI(0,data.nrows,nsample);
			  vector<vector<float> > tempdata;
			  for(int i=0;i<nsample;i++)
			  {

				  tempdata.push_back(data.data[sampleIndex[i]]);
			  }
			  data.data=tempdata;
			tempdata.clear();

		   }

	 this->trees.push_back(new Tree(data,0,maxheight));

	}
	buildForest(ntree,data,maxheight);
}

/*
 * Accepts single row point and return Anomaly Score
 */
float IsolationForest::instanceScore(vector<float> inst)
{
	double avgPathLength=0;
	for(int t=0;t<this->ntree;t++)
	{
	avgPathLength +=  this->trees[t]->pathLength(inst);
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

	for(int inst=0;inst<data.data.size();inst++)
	{


    scores.push_back(instanceScore(data.data[0]));

	}
	return scores;



}


