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
	Data data;
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
	IsolationForest(int ntree, Data data, int maxheight, bool stopheight, const int nsample,
			bool rSample);

	virtual ~IsolationForest()
	{
		for (std::vector<Tree*>::iterator it = trees.begin(); it != trees.end();
				++it)
			delete *it;
	}
	double instanceScore(std::vector<double> inst);
	std::vector<double> AnomalyScore(Data data);
	std::vector<double> pathLength(std::vector<double> inst);
	std::vector<std::vector<double> > pathLength(Data data);
	std::vector<double> ADtest();

};




#endif /* ISOLATIONFOREST_HPP_ */
