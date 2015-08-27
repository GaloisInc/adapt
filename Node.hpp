/*
 * Node.hpp
 *
 *  Created on: Aug 26, 2015
 *      Author: tadeze
 */

#ifndef NODE_HPP_
#define NODE_HPP_

class Node {
public:
	Node *left;
	Node *right;
	double splitValue;
	double splitAtt;
	int depth;
	Node()
	{
		depth=0;
		left=NULL;
		right=NULL;
		splitValue=999999;
		splitAtt=989898;
	};
	}
	virtual ~Node()
	{
	//delete left;
	//delete right;
	}
};

#endif /* NODE_HPP_ */
