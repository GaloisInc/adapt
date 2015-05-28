/*
 * IsolationForest.hpp
 *
 *  Created on: May 5, 2015
 *      Author: tadeze
 */

#ifndef ISOLATIONFOREST_HPP_
#define ISOLATIONFOREST_HPP_
#include "Tree.hpp"
#include "utility.hpp"
#include "cincl.hpp"
/*
 * This class represent Forest of Tree classes.
 * Members: Vector<Tree>
 *
 */
class IsolationForest
{
public:
	std::vector<Tree*> trees;
	int ntree; //number of trees used
	bool rSample;
	int nsample;
    bool stopheight;
    int maxheight;  //maximum height depth for the trees
	IsolationForest()
	{
		rSample = false;
		ntree = 0;
		nsample = 256;
	}
	;
	/* Constructor
	 * @param: ntree: number of trees to use
	 * @param: maxheight: max height to grow
	 * @param: stopheight: grow tree until isolation if false
	 * @param: nsample: nsample used if sampling used
	 * @param: rSample: if True use sampling
	 */
	IsolationForest(const int ntree,  int maxheight,bool stopheight, const int nsample, bool rSample);

	virtual ~IsolationForest()
	{
		for (std::vector<Tree*>::iterator it = trees.begin(); it != trees.end();
				++it)
			delete *it;
	}
	/* @param: takes instance as set of features
	 * @return anomaly score
	 */
	double instanceScore(double *inst);
	/* Takes the dataset and return anomaly score
	 * @param: doubleframe* df all input points
	 * @return: AnomalyScore calculated using iForest algorithm
	 */
	std::vector<double> AnomalyScore(doubleframe* df);
	/* @param: takes instance as set of features
	 * @return all depth across all trees
	 */
	std::vector<double> pathLength(double *inst);

	/* Takes the dataset and return anomaly score
	 * @param: doubleframe* of all input points
	 * @return:  PathLength across all trees
	 */
	std::vector<std::vector<double> > pathLength(doubleframe* data);
	/* @param: path lengths across all trees
	 * @param: weightedTail if true gives higher weight to the tail end of the distribution.
	 * @return: ECDF difference calculated using Anderson-Darling method extension
	 */
	std::vector<double> ADtest(std::vector<std::vector<double> > pathLength,bool weightedTail);

};




#endif /* ISOLATIONFOREST_HPP_ */
