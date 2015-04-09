/*
 * Tree.cpp
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 */
#include "classes.hpp"
#include "utility.h"
using namespace std;
void Tree::iTree(Data data, int height,int maxheight)

{   this->depth=height;
	// Set size of the node
	  nodeSize = data.nrows;
	  if(data.nrows<=1 || this->depth>maxheight)
		  return;
	  //compute min and max of the attribute
	 double minmax[2][data.ncols];
	 for(int j=0;j<data.ncols;j++)
	 {
		 minmax[0][j]=data.data[0][j];
		 minmax[1][j]=minmax[0][j];
	 }

	  for(int i=0;i<data.nrows;i++) {
	    vector<float> inst= data.data[i];
		 for(int j=0;j<data.ncols;j++)
		 {
			 if(inst[j]<minmax[0][j])
				 minmax[0][j]=inst[j];
			 if(inst[j]>minmax[1][j])
				 minmax[1][j]=inst[j];
		 }

	 }

	  //use only valid attributes
	 vector<int> attributes;
	 for(int j=0;j<data.ncols;j++)
	 {
		 if(minmax[0][j]<minmax[1][j]){
			 attributes.push_back(j);
		 }
	 }
	 if(attributes.size()==0)
		 return ;

	 //Randomly pick an attribute and a split point
	int randAtt = attributes[randomI(0,data.ncols-1)];
	this->splittingAtt=randAtt;

	this->splittingPoint = randomD(minmax[0][this->splittingAtt],minmax[1][this->splittingAtt]); //(double) rand()/(RAND_MAX+1) *(minmax[1][this->splittingAtt] - minmax[0][this->splittingAtt]);
	 vector<vector<float> > lnodeData;
	 vector<vector<float> > rnodeData;

	//Split the node into two
	 for(int j=0;j<data.nrows;j++)
	 {
		 if(data.data[j][splittingAtt] > splittingPoint)
		 {
			 lnodeData.push_back(data.data[j]);
		 }
		 else
		 {
			 rnodeData.push_back(data.data[j]);
		 }
	 }

	Data dataL ={data.ncols,lnodeData.size(),lnodeData};
	 leftChild= new Tree();//&dataL,height+1,maxheight);
	 leftChild->parent=this;

	 leftChild->iTree(dataL,this->depth+1,maxheight);
	Data dataR= {data.ncols,rnodeData.size(),rnodeData};
	 rightChild= new Tree();//&dataR,height+1,maxheight);

	 rightChild->parent=this;
	 rightChild->depth = this->depth +1;
	 rightChild->iTree(dataR,this->depth +1,maxheight);



}

double Tree::pathLength(vector<float> inst)
{

    if (this->leftChild == NULL || this->rightChild==NULL || this->splittingAtt==-1) {
      return avgPL(this->nodeSize);
    }


    if (inst[this->splittingAtt] < this->splittingPoint) {

      return this->leftChild->pathLength(inst) +1.0;

    }
    else {
      return this->rightChild->pathLength(inst) +1.0;
    }
}



