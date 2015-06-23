/*
 * Tree.cpp
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 */
#include "Tree.hpp"
using namespace std;
void Tree::iTree(vector<int> const &dIndex, int height, int maxheight, bool stopheight)
{
	/*TODO:  Check the randomizing algorithm/ or try to use the c++11 ;
	 * random function and compare the results. 
	 *
	 *
	 */
	
	
	this->depth = height; //Tree height
	// Set size of the node
	nodeSize = dIndex.size();
	//stop growing if condition
	if (dIndex.size() <= 1 || (stopheight && this->depth > maxheight))
	{
		//this->isLeaf = true;
		return;
	}
	//*** Need modification
	//Initialize minmax array for holding max and min of an attributes
	vector<vector<double> > minmax;
	for (int j = 0; j < dt->ncol; j++)
	{
		vector<double> tmp;
		tmp.push_back(dt->data[0][j]);
		tmp.push_back(dt->data[0][j]);
		minmax.push_back(tmp); //initialize max and min to random value
	}
  
       //Compute max and min of each attribute
	for (unsigned i = 0; i <dIndex.size() ; i++)
	{
		//vector<double> inst = data->data[i];
		for (int j = 0; j < dt->ncol; j++)
		{
			if (dt->data[dIndex.at(i)][j] < minmax[j].at(0))
				minmax[j].at(0) =dt->data[dIndex.at(i)][j];
			if (dt->data[dIndex.at(i)][j] > minmax.at(j).at(1))
				minmax[j].at(1) = dt->data[dIndex.at(i)][j];
		}

	}

		//use only valid attributes

        vector<int> attributes;
	for (int j = 0; j < dt->ncol; j++)
	{      
		if (minmax[j][0] < minmax[j][1])
		{
			attributes.push_back(j);
		}
	}
	//return if no valid attribute found
	if (attributes.size() == 0)
		return;
	//Randomly pick an attribute and a split point
	
	//int randx = randomI(0, attributes.size());
	this->splittingAtt = attributes[randomI(0,attributes.size())]; //randx];
	this->splittingPoint = randomD(minmax[this->splittingAtt][0],minmax[this->splittingAtt][1]);
	
	vector <int> lnodeData;
	vector < int> rnodeData;
	//Split the node into two
	for (unsigned i = 0; i < dIndex.size(); i++)
	{
		if ( dt->data[dIndex.at(i)][splittingAtt] > splittingPoint)
		{
			lnodeData.push_back(i);
		}
		else
		{
			rnodeData.push_back(i);
		}
	}

	leftChild = new Tree(); //&dataL,height+1,maxheight);
	leftChild->parent = this;
	leftChild->iTree(lnodeData, this->depth + 1, maxheight, stopheight);

	rightChild = new Tree(); //&dataR,height+1,maxheight);
	rightChild->parent = this;
	rightChild->iTree(rnodeData, this->depth + 1, maxheight, stopheight);

}


/*
 * takes instance as vector of double
 */
double Tree::pathLength(double *inst)
{

 	if (this->leftChild==NULL||this->rightChild==NULL)
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

