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
	convForest(int _ntree,doubleframe* _df,const int _nsample,int _maxheight,bool _stopheight,
            bool _rsample,double _tau,double _alpha):Forest(_ntree,_df,_nsample,_maxheight,_stopheight, _rsample)
	{tau=_tau;alpha=_alpha;}
	virtual ~convForest()=default;

	void convergeIF(double tau,double alpha);
//Sequential confidence interval stopping 
	void confstop(double alpha);


};
#endif /* CONVFOREST_H_ */
