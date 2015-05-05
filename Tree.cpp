/*
 * Tree.cpp
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 */
#include "Tree.hpp"
//#include "IsolationForest.hpp"
using namespace std;
void Tree::iTree(Data data, int height, int maxheight, bool stopheight)
{
	this->depth = height;
	// Set size of the node
	nodeSize = data.nrows;
	if (data.nrows <= 1 || (stopheight && this->depth > maxheight))
	{
		this->isLeaf = true;
		return;
	}
	//compute min and max of the attribute
	vector < vector<double> > minmax;
	for (int j = 0; j < data.ncols; j++)
	{
		vector<double> tmp;
		tmp.push_back(data.data[0][j]);
		tmp.push_back(data.data[0][j]);
		minmax.push_back(tmp); //initialize max and min to random value
	}

	try
	{
		for (int i = 0; i < data.nrows; i++)
		{
			vector<double> inst = data.data.at(i);
			for (int j = 0; j < data.ncols; j++)
			{
				if (inst[j] < minmax[j].at(0))
					minmax[j].at(0) = inst[j];
				if (inst[j] > minmax.at(j).at(1))
					minmax[j].at(1) = inst[j];
			}

		}

		//use only valid attributes
		vector<int> attributes;
		for (int j = 0; j < data.ncols; j++)
		{
			if (minmax[j][0] < minmax[j][1])
			{
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
		vector < vector<double> > lnodeData;
		vector < vector<double> > rnodeData;

		//Split the node into two
		for (int j = 0; j < data.nrows; j++)
		{
			if (data.data[j][splittingAtt] > splittingPoint)
			{
				lnodeData.push_back(data.data[j]);
			}
			else
			{
				rnodeData.push_back(data.data[j]);
			}
		}

		Data dataL =
		{ data.ncols, (int) lnodeData.size(), lnodeData };
		leftChild = new Tree(); //&dataL,height+1,maxheight);
		leftChild->parent = this;
		leftChild->iTree(dataL, this->depth + 1, maxheight, stopheight);

		Data dataR =
		{ data.ncols, (int) rnodeData.size(), rnodeData };
		rightChild = new Tree(); //&dataR,height+1,maxheight);
		rightChild->parent = this;
		rightChild->iTree(dataR, this->depth + 1, maxheight, stopheight);
	} catch (const exception& er)
	{
		ffile << "Error in tree building..." << er.what() << "\n";
	}

}

double Tree::pathLength(vector<double> inst)
{
//cout<<this->depth;
	if (this == NULL)
		return 0.0;

	if (this->isLeaf)
	{ ///referenced as null for some input data .
		return avgPL(this->nodeSize);
	}

	if (inst[this->splittingAtt] > this->splittingPoint)
	{

		return this->leftChild->pathLength(inst) + 1.0;

	}
	else
	{
		return this->rightChild->pathLength(inst) + 1.0;
	}
}

