/*
 * Forest.h
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#ifndef FOREST_H_
#define FOREST_H_
#include "utility.hpp"
#include "Tree.hpp"
#include "cincl.hpp"

class Forest {
public:
	std::vector<Tree*> trees;
	int ntree;
	bool rsample;
	int nsample;
    bool stopheight;
    int maxheight;
    doubleframe* dataset;  //holds the original dataset will make private 
    Forest()
	{
		rsample = false;
		ntree = 0;
		nsample = 256;
		dataset = NULL;
	};
Forest(int _ntree,doubleframe* _dataset,int _nsample,int _maxheight, bool _stopheight,bool _rsample)
    {
	ntree=_ntree;
    dataset=_dataset;
	nsample=_nsample;
	stopheight=_stopheight;
	maxheight=_maxheight;
 	rsample = _rsample;
    };
virtual ~Forest()
	{
		for (std::vector<Tree*>::iterator it = trees.begin(); it != trees.end();
				++it)
		{
			delete (*it);
			//std::cout<<"tree deleted\n ";
		}
       
      //delete[] dataset->data;
     // delete dataset;

	}

	double instanceScore(double *inst);
	std::vector<double> AnomalyScore(doubleframe* df);
	virtual std::vector<double> pathLength(double *inst);
	std::vector<std::vector<double> > pathLength(doubleframe* data);
	std::vector<double> ADtest(const std::vector<std::vector<double> > &pathlength, bool weighttotail);
	std::vector<double> importance(double *inst);
	double getdepth(double *inst,Tree* tree);
	void getSample(std::vector<int> &sampleIndex,const int nsample,bool rSample,int nrow);



};
#endif /* FOREST_H_ */
