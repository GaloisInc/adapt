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
		Node root;
	rTree()
	{
	}
	;

	virtual ~rTree()
	{	//delete *leftChild; //check if deleting the child is need.
	}
	;
	void iTree(std::vector<int> const &dIndex,
			int height, int maxheight, bool stopheight);

	double pathLength(std::vector<double> &inst);


};

#endif /* RTREE_HPP_ */








