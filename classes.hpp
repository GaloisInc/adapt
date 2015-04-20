/*
 * classes.hpp
 *
 *  Created on: Mar 26, 2015
 *      Author: Tadeze
 */

#ifndef CLASSES_HPP_
#define CLASSES_HPP_
#include<string>
#include<iostream>
#include<stdlib.h>
#include<cmath>
#include<vector>
#include <fstream>



struct Data
{
  int ncols;
  int nrows;
 std::vector<std::vector<double> > data;
};

class Tree {
public:
    Tree *leftChild;
	Tree *rightChild;
	Tree *parent;
	int nodeSize;
	int splittingAtt;
	double splittingPoint;
	int depth;
	bool isLeaf;
	std::vector<int> nodeIndx;

	Tree(){
	leftChild=NULL;
	rightChild=NULL;
	parent=NULL;
	splittingAtt=-1;
	splittingPoint=999;
	depth=0;
	isLeaf=false;
	nodeSize=0;
	};
    
Tree(Data data, int height,int maxHeight);
virtual ~Tree(){	//delete *leftChild;
	};
void iTree(Data data, int height,int maxHeight);
double pathLength(std::vector<double> inst);

};

class IsolationForest {
public:
    std::vector<Tree*> trees;
    Data data;
    int ntree;
    bool rSample;
    int nsample;
   IsolationForest()
  {
    rSample=false;
    ntree=0;
    nsample=256;
   };
IsolationForest(int ntree,Data data,int maxheight,const int nsample,bool rSample);

virtual ~IsolationForest()
 {
    for(std::vector<Tree*>::iterator it=trees.begin();it!=trees.end();++it)
	delete *it;
  }
double instanceScore(std::vector<double> inst);
std::vector<double> AnomalyScore(Data data);
std::vector<double> pathLength(std::vector<double> inst);
std::vector<std::vector<double> > pathLength(Data data);
int countleft(Tree* tree);
};
#endif /* CLASSES_HPP_ */
