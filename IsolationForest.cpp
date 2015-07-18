/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */
#include "IsolationForest.hpp"
//build the forest
using namespace std;
IsolationForest::IsolationForest(const int ntree,  int maxheight,
		bool stopheight, const int nsample, bool rSample)
{

	this->nsample = nsample;
 	this->ntree = ntree;
  	vector<int> sampleIndex;
 	this->rSample = rSample;
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
/*
 * Accepts single point (row) and return Anomaly Score
 */
double IsolationForest::instanceScore(double *inst)
{
	double avgPathLength = 0;
	vector<double> depthInTree;
//compute depth, score from all the forest
/*
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

		  		avgPathLength += (*it)->pathLength(inst);  //depth in a tree

	}
*/	double scores;
	//avgPathLength /= (double) this->ntree;
 	avgPathLength = mean(pathLength(inst));
	scores = pow(2, -avgPathLength / avgPL(this->nsample));

	return scores;

}

/*
 * Score for all points in the dataset
 */
vector<double> IsolationForest::AnomalyScore(doubleframe* df)
{
	vector<double> scores;
	//iterate through all points
	for (int inst = 0; inst <df->nrow; inst++)
	{
		scores.push_back(instanceScore(df->data[inst]));
	}
	return scores;
}
/*
 * Return instance depth in all trees
*/
string tmpVar;
vector<double> IsolationForest::pathLength(double *inst)
{
	vector<double> depth;
	int tr=0;
	static int pnt=0;
	
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{
          	tmpVar=to_string(++tr)+ ","+ to_string(pnt);

 		 depth.push_back(ceil((*it)->pathLength(inst)));

	
	}
       pnt++;   //for logging purpose
	return depth;
}


/* PathLength for all points
*/
vector<vector<double> > IsolationForest::pathLength(doubleframe*  data)
{
	vector < vector<double> > depths;
	for (int r = 0; r < data->nrow; r++)
		depths.push_back(pathLength(data->data[r]));
	return depths;
}
/*
 * Anderson_Darling test from the pathlength
 */

vector<double> IsolationForest::ADtest()
{


	return ADdistance(pathLength(dt),true);
}
/* Compute the feature importance of a point
 * input: *inst data instance
 * output: feature importance
 */
vector<double> IsolationForest::importance(double *inst)
{
	vector<double> depth;
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

		depth.push_back(ceil((*it)->pathLength(inst)));

	}
return depth;
}
