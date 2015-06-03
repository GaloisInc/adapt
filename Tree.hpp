/*
 * Tree.h
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 * Tree: class build using the training data

 */

#ifndef TREE_H_
#define TREE_H_
#include "utility.hpp"
#include "cincl.hpp"
class Tree
{
/*
 * @param: leftChild: reference to leftChild Tree
 * @param: rightChild: reference to right child
 * @param: parent: parent tree ..not very import
 * @param: nodeSize: number dataset arrived at the node.
 * @param: splittingAtt: Splitting attribute for the node
 * @param: splittingPoint: Splitting point for the node
 * @param: depth: depth of the tree
 * @param: isLeaf: checks if the node is Leaf node.
 */


public:
	Tree *leftChild;
	Tree *rightChild;
	Tree *parent;
	int nodeSize;
	int splittingAtt;
	double splittingPoint;
	int depth;
	bool isLeaf;
/* Constructor */
	Tree()
	{
		leftChild = NULL;
		rightChild = NULL;
		parent = NULL;
		splittingAtt = -1;
		splittingPoint = 999;
		depth = 0;
		isLeaf = false;
		nodeSize = 0;
	}
	;



/*
 * Destructor
 */
	virtual ~Tree()
	{  delete leftChild;
	   delete rightChild;
	}
	;
	/*
	 * iTree method: build the tree from the training dataset with specified parameters
	 * @param: &dIndex  index of the data to run isolation
	 * @param: height height of the tree
	 * @param: maxHeight max height of the tree to grow
	 * @param: stopHeight if tree stop after maxheight
	 */
	void iTree(std::vector<int> const &dIndex, int height, int maxHeight, bool stopheight);
	/*
	 * pathLength method
	 * @param: *inst of double feature vector of
	 * @return pathLength of the instance
	 */
	double pathLength(double *inst);

};

#endif /* TREE_H_ */
