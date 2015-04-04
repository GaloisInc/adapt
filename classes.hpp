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
	vector<int> nodeIndx;
	Tree() {};
    Tree(Data* data, int height,int maxHeight);
	virtual ~Tree(){
	//delete *leftChild;
	//delete *rightChild;
	};
	double pathLength(vector<float> inst);
};

class IsolationForest {
public:
    vector<Tree*> trees;
    Data *data;
    int ntree;
    bool rSample;
    int nsample;
	IsolationForest();
	IsolationForest(int ntree,Data* data,int maxheight,const int nsample,bool rSample);
	virtual ~IsolationForest()
	{
for(int i=0;i<(int)trees.size();i++)
	delete[] trees[i];
	delete data;
	}
	void readData(string filename);
	void buildForest(int ntree, Data data,int maxheight);
	float instanceScore(vector<float> inst);
	vector<float> AnomalyScore(Data* data);

};







#endif /* CLASSES_HPP_ */
