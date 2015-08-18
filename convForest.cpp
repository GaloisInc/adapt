/*
 * convForest.cpp
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#include "convForest.hpp"
using namespace std;

struct topscore{
	bool operator() (const pair<int,double> p1,const pair<int,double> p2)
	{
		return p1.second > p2.second;
	}
};
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

//Sequential confidence interval stopping

double variance(vector<double> x)
{
 	double sum=0.0;
	double mn=mean(x);
	for(double elem : x)
	{
	 sum +=pow(elem-mn,2);
	}
	return sum/(double)x.size();
}

void getSample(vector<int> &sampleIndex,const int nsample,bool rSample)
{
	sampleIndex.clear();
	if (rSample && nsample < dt->nrow)
		sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
	else
		sampleI(0, dt->nrow - 1, dt->nrow, sampleIndex); //shuffle all index of the data if sampling is false



}
/*
 * Stopping confidence interval width
 */
void convForest::confstop(int maxheight,bool stopheight, const int nsample, bool rSample,double alpha)
{
	this->nsample = nsample;
	double tk = ceil(alpha*2*dt->nrow);  //top k ranked scores 
	vector<int> sampleIndex;  //index for sample row 
	this->rSample = rSample;
	vector<double> totalDepth(dt->nrow,0);
	double tua = 1/(double)dt->nrow;
	vector<double> squaredDepth(dt->nrow,0);
 	priority_queue<pair<int,double>,vector<pair<int,double> >,topscore > pq;
	
	double  ntree=0.0;
	bool converged=false;
	vector<double> theta_k; //top k score 
	vector<pair<int ,double> > topk;	
	logfile<<"ntree,index,currentscore,aggscore,vardepth,scorewidth \n";
	while (!converged) {

		//Sample data for training
		topk.clear();
		//get sample data
		getSample(sampleIndex,nsample,rSample);
		//build a tree based on the sample and add to forest
		Tree *tree = new Tree();
		tree->iTree(sampleIndex, 0, maxheight, stopheight);
		this->trees.push_back(tree);
		
		ntree++;
		double d,scores,dbar,currentscore;
		for (int inst = 0; inst <dt->nrow; inst++)
		{
			d = getdepth(dt->data[inst],tree);
			totalDepth[inst] += d;
			squaredDepth[inst] +=d*d;
			dbar=totalDepth[inst]/ntree; //Current average depth 
			scores = pow(2, -dbar / avgPL(this->nsample));
			currentscore = pow(2,-d/avgPL(this->nsample));
			pq.push(pair<int, double>(inst,scores));
			topk.push_back(pair<int,double>(inst,currentscore));
			
		}
		
		//if we have 1 tree we don't have variance
		sort(topk.begin(),topk.end(),topscore());
		theta_k.push_back((topk[tk].second + topk[tk+1].second)/2);	
		if(ntree<2)
		continue;
				
	//	double maxCIWidth =0;		
	//	double mn = mean(theta_k);
		double var = variance(theta_k);
		double halfwidth=1.96*sqrt(var)/sqrt(ntree);	
		logfile<<ntree<<","<<halfwidth<<","<<mean(theta_k)<<","<<var<<"\n";
	
       		converged = halfwidth <=tua;
	}
	
	logfile<<"Current K="<<tk<<" K' = "<<mean(theta_k);

}





























