/*
 * convForest.cpp
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#include "convForest.hpp"
using namespace std;
/*
struct topscore{
	bool operator() (const pair<int,double> p1,const pair<int,double> p2)
	{
		return p1.second > p2.second;
	}
};*/
struct larger
{
	bool operator()(pair<int,int> p1,pair<int,int> p2)
	{
		return p1.second >p2.second;
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

void convForest::convergeIF(int maxheight,bool stopheight, const int nsample, bool rSample,double tau,double alpha)
{
	this->nsample = nsample;
	double tk = ceil(alpha*2*dt->nrow);
	vector<int> sampleIndex;
	this->rSample = rSample;
	
	vector<double> totalDepth(dt->nrow,0);

	vector<double> squaredDepth(dt->nrow,0);
 	priority_queue<pair<int,double>,vector<pair<int,double> >, larger> pq;
	double  ntree=0.0;
	bool converged=false;

	vector<pair<int ,int> > topk;
	logfile<<"ntree,index,currentscore,aggscore,vardepth,scorewidth \n";
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
			pq.push( pair<int, double>(inst,scores));


		}
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

		for(int i=0;i<tk;i++)  //where tk is top 2*\alpha * N index element
		{ 	//int index=0;

	                    //pq.top().first;
			inserTopK(topk,pq.top().first);
			int index =topk.at(i).first;
			double mn = totalDepth[index]/ntree;
			double var = squaredDepth[index]/ntree -(mn*mn);
			double halfwidth = 1.96*sqrt(var)/sqrt(ntree);
			double scoreWidth = pow(2, -(mn-halfwidth) / avgPL(this->nsample)) -pow(2, -(mn+halfwidth)/ avgPL(this->nsample));
			maxCIWidth=max(maxCIWidth,scoreWidth);
	logfile<<ntree<<","<<topk.at(i).first<<","<<pq.top().second<<","<<pow(2,-mn/avgPL(this->nsample))<<","<<var<<","<<scoreWidth<<"\n";
				pq.pop();
		}

		sort(topk.begin(),topk.end(),larger());
	// logfile <<"Tree number "<<ntree<<" built with confidence\t"<<maxCIWidth<<endl;

//	 logfile <<"Tree number "<<ntree<<" built with confidence\t"<<maxCIWidth<<endl;
       		converged = maxCIWidth <=tau;
	}

//return this;
}

