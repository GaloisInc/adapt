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
class IsolationForest
{
public:
	std::vector<Tree*> trees;
	int ntree;
	bool rSample;
	int nsample;
    	bool stopheight;
    	int maxheight;
	IsolationForest()
	{
		rSample = false;
		ntree = 0;
		nsample = 256;
	}
	;
	IsolationForest(int ntree, int maxheight, bool stopheight, const int nsample,
			bool rSample);

	virtual ~IsolationForest()
	{
		for (std::vector<Tree*>::iterator it = trees.begin(); it != trees.end();
				++it)
			delete *it;
	}
	double instanceScore(double *inst);
	std::vector<double> AnomalyScore(doubleframe* df);
	std::vector<double> pathLength(double *inst);
	std::vector<std::vector<double> > pathLength(doubleframe* data);
	std::vector<double> ADtest();

};




#endif /* ISOLATIONFOREST_HPP_ */
