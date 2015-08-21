/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */

#include "IsolationForest.hpp"
using namespace std;
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
		getSample(sampleIndex,nsample,rSample);	
	       
	//build tree	
		Tree *tree = new Tree(); 
		tree->iTree(sampleIndex, 0, maxheight, stopheight);
		this->trees.push_back(tree);

	 }

}


