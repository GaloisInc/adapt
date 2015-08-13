/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */

#include "IsolationForest.hpp"
using namespace std;
//build the forest

IsolationForest::IsolationForest(const int ntree,  int maxheight,
		bool stopheight, const int nsample, bool rSample):Forest(ntree,nsample,maxheight,stopheight,rSample)
{

	/*nsample = nsample;
 	this->ntree = ntree;
  	*/
	vector<int> sampleIndex;
// 	this->rSample = rSample;
  //build forest through all trees
	for (int n = 0; n < ntree; n++)
	{
		//if sampling is true
		//Sample and shuffle the data.
		sampleIndex.clear();
		if(rSample && nsample<dt->nrow)
			sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
		else
			sampleI(0, dt->nrow-1, dt->nrow, sampleIndex);   //shuffle all index of the data if sampling is false
 
	       //build tree	
		Tree *tree = new Tree(); 
		tree->iTree(sampleIndex, 0, maxheight, stopheight);
		this->trees.push_back(tree);

	 }

}


