/*
 * IsolationForest.hpp
 *
 *  Created on: May 5, 2015
 *      Author: tadeze
 */

#ifndef ISOLATIONFOREST_HPP_
#define ISOLATIONFOREST_HPP_
#include "utility.hpp"
#include "Tree.hpp"
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
	int tau;


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
	std::vector<double> ADtest(const std::vector<std::vector<double> > &pathlength, bool weighttotail);
	std::vector<double> importance(double *inst);

	//convergent iForest
	void convergeIF(int maxheight,bool stopheight, const int nsample, bool rSample,double tau);
	double getdepth(double *inst,Tree* tree);


};



#endif /* ISOLATIONFOREST_HPP_ */
