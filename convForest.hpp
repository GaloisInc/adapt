/*
 * convForest.h
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#ifndef CONVFOREST_H_
#define CONVFOREST_H_

#include "Forest.hpp"
class convForest: public Forest {
public:
	int tau;
	int alpha;
	convForest(double _tau,double _alpha)
	{tau=_tau;alpha=_alpha;}
	virtual ~convForest()=default;

	void convergeIF(int maxheight,bool stopheight, const int nsample, bool rSample,double tau,double alpha);
//Sequential confidence interval stopping 
	void confstop(int maxheight,bool stopheight, const int nsample, bool rSample,double alpha);


};
#endif /* CONVFOREST_H_ */
