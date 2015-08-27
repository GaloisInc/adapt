/*
 * rTree.hpp
 *
 *  Created on: Aug 26, 2015
 *      Author: tadeze
 */

#ifndef RTREE_HPP_
#define RTREE_HPP_

class rTree {
	public:
		rTree *leftChild;
		rTree *rightChild;
		rTree *parent;
		int nodeSize;
		int splittingAtt;
		double splittingPoint;
		int depth;
	rTree()
	{
		leftChild = NULL;
		rightChild = NULL;
		parent = NULL;
		splittingAtt = -1;
		splittingPoint = 999;
		depth = 0;
		nodeSize = 0;
	}
	;

	virtual ~rTree()
	{	//delete *leftChild; //check if deleting the child is need.
	}
	;
	void iTree();
	double pathLength(std::vector<double> &inst);


public:
	rTree();
	virtual ~rTree();
};

#endif /* RTREE_HPP_ */








