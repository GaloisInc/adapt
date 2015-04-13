/*
 * Tree.h
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 */

#ifndef TREE_H_
#define TREE_H_



class Tree {
	Tree *leftChild;
	Tree *rightChild;
	Tree *parent;
	int splittingAtt;
	double splittingVal;
	int depth;
	Tree();
    Tree(double)
	virtual ~Tree();
};

 /* namespace iForest */

#endif /* TREE_H_ */
