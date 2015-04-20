/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */
#include "classes.hpp"
#include "utility.h"
//build the forest
using namespace std;
IsolationForest::IsolationForest(const int ntree,Data data,int maxheight,const int nsample,bool rSample)
{

this->nsample = nsample;
this->ntree=ntree;
Data sampleData;
vector<int> sampleIndex;
this->rSample=rSample;
for(int n=0;n<ntree;n++)
  {
ffile<<"-----Tree number----------- "<<n+1<<"\n";
   if(rSample==true && nsample<data.nrows)
        {  //sample index data and construct sample data
	    sampleI(0,data.nrows-1,nsample,sampleIndex);
	    vector<vector<double> > tempdata;
	    for(int ind=0;ind<nsample;ind++)
	       tempdata.push_back(data.data[sampleIndex[ind]]);
	    sampleData.data=tempdata;
	    sampleData.nrows = nsample;
	    sampleData.ncols=data.ncols;
		 tempdata.clear();
		 sampleIndex.clear();
           }

	else
	  sampleData=data;
  this->data= sampleData;
  Tree *tree =new Tree();
  tree->iTree(sampleData,0,maxheight);
  this->trees.push_back(tree);
//ffile<<n<<"\t " splitt at "<<tree->splittingAtt<<"\t depth "<<countleft(tree)<<endl;

  }

}

int IsolationForest::countleft(Tree *tree)
{
	if(tree->isLeaf)
		return 1;
	else
	 return	countleft(tree->leftChild)+1;

}

/*
 * Accepts single row point and return Anomaly Score
 */
double IsolationForest::instanceScore(vector<double> inst)
{
	double avgPathLength=0;
	vector<double> depthInTree;
	//pointer based
	for(vector<Tree*>::iterator it=this->trees.begin();it!=trees.end();++it)
	{
		avgPathLength +=(*it)->pathLength(inst);
	}
	double scores;
	avgPathLength /=(double) this->ntree;
       scores= pow(2,-avgPathLength/avgPL(this->nsample));

      return scores;

}

/*
 * Score for all points
 */
vector<double> IsolationForest::AnomalyScore(Data data)
{
        vector<double> scores;

	for(int inst=0;inst<(int)data.data.size();inst++)
	{


           scores.push_back(instanceScore(data.data[inst]));

	}
	return scores;
}
/*
 * Return instance depth in all trees
 */
vector<double> IsolationForest::pathLength(vector<double> inst)
{
   vector<double> depth;
   for(vector<Tree*>::iterator it=this->trees.begin();it!=trees.end();++it)
     {

	depth.push_back(ceil((*it)->pathLength(inst)));

     }
return depth;
}

/*
 * PathLength for all points
 */
vector<vector<double> > IsolationForest::pathLength(Data data)
{
    vector<vector<double> > depths;
    for(int r=0;r<data.nrows;r++)
	depths.push_back(pathLength(data.data[r]));
    return depths;
}

/* Idea to traverse the deepest Node.
int IsolationForest::Maxheight(Tree* tree)
{
	if(!tree->isLeaf)
		return Maxheight(tree->leftChild);
	else
		return tree->depth;
}
*/


