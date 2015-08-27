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
#include "Forest.hpp"

class IsolationForest:public Forest{
    public:
	IsolationForest();
	IsolationForest(int ntree,doubleframe* df, int maxheight, bool stopheight, const int nsample,
			bool rSample);

	virtual ~IsolationForest()
	{
	}
	//convergent iForest

};



#endif /* ISOLATIONFOREST_HPP_ */
