/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */
#include "IsolationForest.hpp"
//build the forest
using namespace std;
IsolationForest::IsolationForest(const int ntree, doubleframe* data/*Data data*/, int maxheight,
		bool stopheight, const int nsample, bool rSample)
{

	this->nsample = nsample;
	this->ntree = ntree;
	Data sampleData;  //for holding the sample data/ all data
	vector<int> sampleIndex;
	this->rSample = rSample;
	//build forest through all trees
	for (int n = 0; n < ntree; n++)
	{
		//if sampling is true
	//Sample and shuffle the data.
	sampleIndex.clear();
	if(rSample && nsample<data->nrow)
	sampleI(0, data->nrow - 1, nsample, sampleIndex); //sample nsample
	else
	sampleI(0, data->nrow - 1, data->nrow-1, sampleIndex);   //shuffle all index of the data if sampling is false


	/*
	  if (rSample == true && nsample < data->nrow)
		{ //sample index data and construct sample data
			sampleI(0, data->nrow - 1, nsample, sampleIndex); //sample nsample index from all index.

			vector < vector<double> > tempdata;
			for (int ind = 0; ind < nsample; ind++)
				tempdata.push_back(data.data[sampleIndex[ind]]);
				sampleData.data = tempdata;
			sampleData.nrows = nsample;
			sampleData.ncols = data.ncols;
			tempdata.clear();
			sampleIndex.clear();

		}

		else
			sampleData = data;
*/

  //  this->data = data;  //sampleData;
Tree *tree = new Tree();
tree->iTree(sampleIndex, 0, maxheight, stopheight);
this->trees.push_back(tree);

	}

}
/*
 * Accepts single point (row) and return Anomaly Score
 */
double IsolationForest::instanceScore(double* inst)
{
	double avgPathLength = 0;
	vector<double> depthInTree;

	//compute depth, score from all trees in the forest
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

	avgPathLength += (*it)->pathLength(inst);  //depth in a tree

	}
	double scores;
	avgPathLength /= (double) this->ntree;
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

vector<double> IsolationForest::pathLength(vector<double> inst)
{
	vector<double> depth;
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

		depth.push_back(ceil((*it)->pathLength(&inst)));

	}
	return depth;
}


 * PathLength for all points

vector<vector<double> > IsolationForest::pathLength(Data data)
{
	vector < vector<double> > depths;
	for (int r = 0; r < data.nrows; r++)
		depths.push_back(pathLength(data.data[r]));
	return depths;
}*/
/*
 * Anderson_Darling test from the pathlength
 */
/*
vector<double> IsolationForest::ADtest()
{


	return ADdistance(pathLength(this->data),true);
}
*/
