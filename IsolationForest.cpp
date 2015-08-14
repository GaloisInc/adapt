/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */

#include "IsolationForest.hpp"
//build the forest
using namespace std;
IsolationForest::IsolationForest(const int ntree,  int maxheight,
		bool stopheight, const int nsample, bool rSample)
{

	this->nsample = nsample;
 	this->ntree = ntree;
  	vector<int> sampleIndex;
 	this->rSample = rSample;
  //build forest through all trees
	for (int n = 0; n < ntree; n++)
	{
		//if sampling is true
		//Sample and shuffle the data.
		sampleIndex.clear();
		if(rSample && nsample<dt->nrow)
			sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
		else
			sampleI(0, dt->nrow-1, dt->nrow, sampleIndex);   //shuffle all index of the data if sampling is false
 
	       //build tree	
		Tree *tree = new Tree(); 
		tree->iTree(sampleIndex, 0, maxheight, stopheight);
		this->trees.push_back(tree);

	 }

}


struct topscore{
	bool operator() (const pair<int,double> p1,const pair<int,double> p2)
	{
		return p1.second > p2.second;
	}
};

void inserTopK(vector<pair<int,int> > &sl,int b)
{
for(vector<pair<int,int> >::iterator it=sl.begin();it!=sl.end();++it)
{

if(it->first==b)
{
it->second++;
return;
}
}
sl.push_back(pair<int,int>(b,1));
}

void IsolationForest::convergeIF(int maxheight,bool stopheight, const int nsample, bool rSample,double tau,double alpha)
{
	this->nsample = nsample;
	double tk = ceil(alpha*2*dt->nrow);
	vector<int> sampleIndex;
	this->rSample = rSample;
	vector<double> totalDepth(dt->nrow,0);

	vector<double> squaredDepth(dt->nrow,0);
 	priority_queue<pair<int,double>,vector<pair<int,double> >,topscore > pq,pqtmp;
	
	double  ntree=0.0;
	bool converged=false;
	
	vector<pair<int ,int> > topk;	
	logfile<<"rank,ntree,index,currentscore,aggscore,vardepth,scorewidth \n";
	while (!converged) {

		//Sample data for training
		sampleIndex.clear();
		if (rSample && nsample < dt->nrow)
			sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
		else
			sampleI(0, dt->nrow - 1, dt->nrow, sampleIndex); //shuffle all index of the data if sampling is false

		sampleIndex.clear();
		if (rSample && nsample < dt->nrow)
			sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
		else
			sampleI(0, dt->nrow - 1, dt->nrow, sampleIndex); //shuffle all index of the data if sampling is false

		//build a tree based on the sample and add to forest
		Tree *tree = new Tree();
		tree->iTree(sampleIndex, 0, maxheight, stopheight);
		this->trees.push_back(tree);
		ntree++;
		double d,scores,dbar;
	
		
		for (int inst = 0; inst <dt->nrow; inst++)
		{
			d = getdepth(dt->data[inst],tree);
			totalDepth[inst] += d;
			squaredDepth[inst] +=d*d;
			dbar=totalDepth[inst]/ntree;
			scores = pow(2, -dbar / avgPL(this->nsample));
			pq.push(pair<int, double>(inst,scores));


		}
		pqtmp =pq;
		//if we have 1 tree we don't have variance
		if(ntree<2)
		{	for(int i=0;i<tk;i++)
			 {
				inserTopK(topk,pq.top().first);		
			 	 pq.pop();

			 }
			
			continue;
		
		}
		
		double maxCIWidth =0;		
	
		for(int i=0;i<tk;i++)
		{	
			inserTopK(topk,pqtmp.top().first);
			pqtmp.pop();
		}
		sort(topk.begin(),topk.end(),topscore());
		for(int i=0;i<tk;i++)  //where tk is top 2*\alpha * N index element
		{ 	//int index=0;

	                    //pq.top().first;
			int index =topk.at(i).first;
			double mn = totalDepth[index]/ntree;
			double var = squaredDepth[index]/ntree -(mn*mn);
			double halfwidth = 1.96*sqrt(var)/sqrt(ntree);
			double scoreWidth = pow(2, -(mn-halfwidth) / avgPL(this->nsample)) -pow(2, -(mn+halfwidth)/ avgPL(this->nsample));
			maxCIWidth=max(maxCIWidth,scoreWidth);
			logfile<<i<<","<<ntree<<","<<topk.at(i).first<<","<<pq.top().second<<","<<pow(2,-mn/avgPL(this->nsample))<<","<<var<<","<<scoreWidth<<"\n";
			pq.pop();
		}

			// logfile <<"Tree number "<<ntree<<" built with confidence\t"<<maxCIWidth<<endl;

//	 logfile <<"Tree number "<<ntree<<" built with confidence\t"<<maxCIWidth<<endl;
       		converged = maxCIWidth <=tau;
	}

//return this;
}

double IsolationForest::getdepth(double* inst,Tree* tree)
{
	return tree->pathLength(inst);
}





/*
 * Accepts single point (row) and return Anomaly Score
 */
double IsolationForest::instanceScore(double *inst)
{
	double avgPathLength = 0;
	vector<double> depthInTree;
//compute depth, score from all the forest
/*
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

		  		avgPathLength += (*it)->pathLength(inst);  //depth in a tree

	}
*/	double scores;
	//avgPathLength /= (double) this->ntree;
 	avgPathLength = mean(pathLength(inst));
	scores = pow(2, -avgPathLength / avgPL(this->nsample));

	return scores;

}

/*
 * Score for all points in the dataset
 */
vector<double> IsolationForest::AnomalyScore(doubleframe* df)
{
	vector<double> scores;
	//iterate through all points
	for (int inst = 0; inst <df->nrow; inst++)
	{
		scores.push_back(instanceScore(df->data[inst]));
	}
	return scores;
}
/*
 * Return instance depth in all trees
*/
string tmpVar;
vector<double> IsolationForest::pathLength(double *inst)
{
	vector<double> depth;
	int tr=0;
	static int pnt=0;
	
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{
          	tmpVar=to_string(++tr)+ ","+ to_string(pnt);

 		 depth.push_back(ceil((*it)->pathLength(inst)));

	
	}
       pnt++;   //for logging purpose
	return depth;
}


/* PathLength for all points
*/
vector<vector<double> > IsolationForest::pathLength(doubleframe*  data)
{
	vector < vector<double> > depths;
	for (int r = 0; r < data->nrow; r++)
		depths.push_back(pathLength(data->data[r]));
	return depths;
}
/*
 * Anderson_Darling test from the pathlength
 */
/*
vector<double> IsolationForest::ADtest(const vector<vector<double> > &pathlength,bool weighttotail)
{

//Will fix later
	return ADdistance(pathlength,weighttotail);
}*/
/* Compute the feature importance of a point
 * input: *inst data instance
 * output: feature importance
 */
vector<double> IsolationForest::importance(double *inst)
{
	vector<double> depth;
	for (vector<Tree*>::iterator it = this->trees.begin(); it != trees.end();
			++it)
	{

		depth.push_back(ceil((*it)->pathLength(inst)));

	}
return depth;
}
