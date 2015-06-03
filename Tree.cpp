/*
 * Tree.cpp
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 *TODO: modify the max and min attribute value
 */

#include "Tree.hpp"
using namespace std;
void Tree::iTree(vector<int> const &dIndex, int height, int maxheight,
		bool stopheight) {
	this->depth = height; //Tree height
	nodeSize = dIndex.size();

	//stop growing if condition
	if (dIndex.size() <= 1 || (stopheight && this->depth > maxheight)) {
		//this->isLeaf = true;
		return;
	}

	//*** Need modification
	//Initialize minmax array for holding max and min of an attributes
	vector<vector<double> > minmax;
	for (int j = 0; j < dt->ncol; j++) {
		vector<double> tmp;
		tmp.push_back(dt->data[0][j]);
		tmp.push_back(dt->data[0][j]);
		minmax.push_back(tmp); //initialize max and min to random value
	}

	/* compute max and min of the attribute of the data in the current node*/
	for (unsigned i = 0; i < dIndex.size(); i++) {

		for (int j = 0; j < dt->ncol; j++) {
			if (dt->data[dIndex.at(i)][j] < minmax[j].at(0))
				minmax[j].at(0) = dt->data[dIndex.at(i)][j];
			if (dt->data[dIndex.at(i)][j] > minmax.at(j).at(1))
				minmax[j].at(1) = dt->data[dIndex.at(i)][j];
		}

	}
  /* use only valid attributes */
	vector<int> attributes;
	for (int j = 0; j < dt->ncol; j++) {
		if (minmax[j][0] < minmax[j][1]) {
			attributes.push_back(j);
		}
	}
	if (attributes.size() == 0)
		return;

	//Randomly pick an attribute and a split point
	int randx = randomI(0, attributes.size());
	this->splittingAtt = attributes[randx];

	this->splittingPoint = randomD(minmax[this->splittingAtt][0],
			minmax[this->splittingAtt][1]);

	vector<int> lnodeData;  //collect left node data
	vector<int> rnodeData;

	//Split the node into two
	for (unsigned i = 0; i < dIndex.size(); i++) {
		if (dt->data[dIndex.at(i)][splittingAtt] > splittingPoint) {
			lnodeData.push_back(i);
		} else {
			rnodeData.push_back(i);
		}
	}
    /* build the left and righ child trees */
	leftChild = new Tree();
	leftChild->parent = this;
	leftChild->iTree(lnodeData, this->depth + 1, maxheight, stopheight);

	rightChild = new Tree();
	rightChild->parent = this;
	rightChild->iTree(rnodeData, this->depth + 1, maxheight, stopheight);
}

/*
 * Compute the pathLength of testing data
 */
double Tree::pathLength(double *inst) {

	/* if the node is Leaf just compute the avg function in the nodes*/
	if (this->leftChild == NULL || this->rightChild == NULL) { ///referenced as null for some input data .
		return avgPL(this->nodeSize);
	}

	if (inst[this->splittingAtt] > this->splittingPoint) {

		return this->leftChild->pathLength(inst) + 1.0;

	} else {
		return this->rightChild->pathLength(inst) + 1.0;
	}
}

