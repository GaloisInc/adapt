/*
 * rTree.cpp
 *
 *  Created on: Aug 26, 2015
 *      Author: tadeze
 */

#include "rTree.hpp"


//void splitNode()

void rTree::iTree(vector<int> const &dIndex,
		int height, int maxheight, bool stopheight)

{


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
		this->splittingAtt = attributes[randomI(0,attributes.size()-1)]; //randx];
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



