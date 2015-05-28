/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */
#include "IsolationForest.hpp"
//build the forest
using namespace std;
IsolationForest::IsolationForest(const int ntree, int maxheight,
bool stopheight, const int nsample, bool rSample) {

	this->nsample = nsample;
	this->ntree = ntree;
	vector<int> sampleIndex;
	this->rSample = rSample;

	//build forest through all trees
	for (int n = 0; n < ntree; n++) {
		//Sample and shuffle the data.

		sampleIndex.clear();
		if (rSample && nsample < dt->nrow) //if sampling true
			sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
		else
			//otherwise shuffle all data
			sampleI(0, dt->nrow - 1, dt->nrow, sampleIndex);

/* Build tree and construct it */
		Tree *tree = new Tree();
		tree->iTree(sampleIndex, 0, maxheight, stopheight);
		this->trees.push_back(tree);

	}

}
/*
 * Accepts single point (row) and return Anomaly Score
 */
double IsolationForest::instanceScore(double *inst) {
	double avgPathLength = 0;
	vector<double> depthInTree;

	/* compute depth, score from all Trees in the forest */
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it) {

		avgPathLength += (*it)->pathLength(inst);  //compute depth in a tree

	}
	double scores;
	avgPathLength /= (double) this->ntree;
	scores = pow(2, -avgPathLength / avgPL(this->nsample)); //score calculation

	return scores;

}

/*
 * Score for all points in the dataset
 */
vector<double> IsolationForest::AnomalyScore(doubleframe* df) {
	vector<double> scores; //score for all points

	/* iterate through all points and compute individual anomaly score */
	for (int inst = 0; inst < df->nrow; inst++) {
		scores.push_back(instanceScore(df->data[inst]));
	}

	return scores;
}
/*
 * Return instance depth in all trees
 */
vector<double> IsolationForest::pathLength(double *inst) {
	vector<double> depth;
	 //Compute through all trees
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it) {

		depth.push_back(ceil((*it)->pathLength(inst)));

	}
	return depth;
}

/* PathLength for all points
 */
vector<vector<double> > IsolationForest::pathLength(doubleframe* data) {

	vector<vector<double> > depths;
	for (int r = 0; r < data->nrow; r++)
		depths.push_back(pathLength(data->data[r]));
	return depths;
}
/*
 * Anderson_Darling ECDF difference
 */

vector<double> IsolationForest::ADtest(vector<vector<double> > pathLength,
		bool weightedTail = true) {

	return ADdistance(pathLength, weightedTail);
}

