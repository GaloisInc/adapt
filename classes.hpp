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

using namespace std;

struct Data{
int ncols;
int nrows;
vector<vector<float> > data;
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
	vector<int> nodeIndx;
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
	virtual ~Tree(){
	//delete *leftChild;
	//delete *rightChild;
	};
	void iTree(Data data, int height,int maxHeight);
	double pathLength(vector<float> inst);
};

class IsolationForest {
public:
    vector<Tree*> trees;
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
for(vector<Tree*>::iterator it=trees.begin();it!=trees.end();++it)
	delete *it;
//    trees.clear();
//delete data;
	}
//	void readData(string filename);
	void buildForest(int ntree, Data data,int maxheight);
	float instanceScore(vector<float> inst);
	vector<float> AnomalyScore(Data data);
	vector<float> pathLength(vector<float> inst);
    vector<vector<float> > pathLength(Data data);
};
#endif /* CLASSES_HPP_ */
