/*
 * Forest.cpp
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#include "Forest.hpp"
using namespace std;
string tmpVar;

double Forest::getdepth(double* inst,Tree* tree)
{
	return tree->pathLength(inst);
}

/*
 * Accepts single point (row) and return Anomaly Score
 */
double Forest::instanceScore(double *inst)
{
	double avgPathLength = 0;
	vector<double> depthInTree;
	double scores;
	//avgPathLength /= (double) this->ntree;
 	avgPathLength = mean(pathLength(inst));
	scores = pow(2, -avgPathLength / avgPL(this->nsample));

	return scores;

}

/*
 * Score for  a set of dataframe in the dataset
 */
vector<double> Forest::AnomalyScore(doubleframe* df)
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

vector<double> Forest::pathLength(double *inst)
{
	vector<double> depth;
	//int tr=0;
	//static int pnt=0;

	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{
          	//tmpVar=to_string(++tr)+ ","+ to_string(pnt);

 		 depth.push_back((*it)->pathLength(inst));


	}
      // pnt++;   //for logging purpose
	return depth;
}


/* PathLength for all points
*/
vector<vector<double> > Forest::pathLength(doubleframe*  data)
{
	vector < vector<double> > depths;
	for (int r = 0; r < data->nrow; r++)
		depths.push_back(pathLength(data->data[r]));
	return depths;
}
/*
 * Anderson_Darling test from the pathlength
 */
/*
vector<double> IsolationForest::ADtest(const vector<vector<double> > &pathlength,bool weighttotail)
{

//Will fix later
	return ADdistance(pathlength,weighttotail);
}*/
/* Compute the feature importance of a point
 * input: *inst data instance
 * output: feature importance
 */
vector<double> Forest::importance(double *inst)
{
	vector<double> depth;
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

		depth.push_back(ceil((*it)->pathLength(inst)));

	}
return depth;
}
//Sample data from the datset
void Forest::getSample(vector<int> &sampleIndex,const int nsample,bool rSample,int nrow)
{
	sampleIndex.clear();
	if (rSample && nsample < nrow)
		sampleI(0, nrow - 1, nsample, sampleIndex); //sample nsample
	else
		sampleI(0, nrow - 1, nrow, sampleIndex); //shuffle all index of the data 



}

