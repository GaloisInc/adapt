/*
 * RotationForest.hpp
 *
 *  Created on: Aug 26 2015
 *      Author: tadeze
 */

#ifndef RFOREST_H_
#define RFOREST_H_
//#include "utility.hpp"
#include "Tree.hpp"
//#include "cincl.hpp"

class RForest {
public:
	std::vector<MatrixXd> rotMatrix;
	int ntree;
	bool rSample;
	int nsample;
    	bool stopheight;
    	int maxheight;

    Forest()
	{
		rSample = false;
		ntree = 0;
		nsample = 256;
	};
Forest(int _ntree,int _nsample,int _maxheight, bool _stopheight,bool _rSample)
    {
	ntree=_ntree;
	nsample=_nsample;
	stopheight=_stopheight;
	maxheight=_maxheight;
 	rSample = _rSample;
    };
virtual ~Forest()
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
	double getdepth(double *inst,Tree* tree);
	void getSample(std::vector<int> &sampleIndex,const int nsample,bool rSample);



};
#endif /* FOREST_H_ */
