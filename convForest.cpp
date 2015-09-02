/*
 * convForest.cpp
 *
 *  Created on: Aug 12, 2015
 *      Author: tadeze
 */

#include "convForest.hpp"
using namespace std;

struct topscore
{
	bool operator() (const pair<int,double> p1,const pair<int,double> p2)
	{
		return p1.second < p2.second;
	}
};
struct larger
{
	bool operator()(const pair<int,double> p1,const pair<int,double> p2)

	{
		return p1.second <p2.second;
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
//Sample data from the datset
/*
void getSample(vector<int> &sampleIndex,const int nsample,bool rSample)
{
	sampleIndex.clear();
	if (rSample && nsample < dt->nrow)
		sampleI(0, dt->nrow - 1, nsample, sampleIndex); //sample nsample
	else
		sampleI(0, dt->nrow - 1, dt->nrow, sampleIndex); //shuffle all index of the data if sampling is false



}*/
/*
 * @input two vector v1 and v2 a
 * @return proporation of intersection ,[0,1]
 */
double topcommonK(vector<int> &v1,vector<int> &v2)
{
	vector<int> v3;
	sort(v1.begin(),v1.end());
	sort(v2.begin(),v2.end());
	set_intersection(v1.begin(),v1.end(),v2.begin(),v2.end(),back_inserter(v3));
	return (double)v3.size()/(double)v1.size();
}


void convForest::convergeIF(double tau,double alpha)
{
//	this->nsample = nsample;
	double tk = ceil(alpha*4*dataset->nrow);
	vector<int> sampleIndex;
//	this->rSample = rSample;
	
	vector<double> totalDepth(dataset->nrow,0);
    int conv_cnt =0;  //convergence counter

	vector<double> squaredDepth(dataset->nrow,0);
 	priority_queue<pair<int,double>,vector<pair<int,double> >, larger> pq;
	double  ntree=0.0;
	bool converged=false;

	vector<pair<int ,int> > topk;
	logfile<<"ntree,index,currentscore,probinter \n";
	vector<int> topIndex(tk);
	vector<int> prevIndex;
	double prob =0;
	while (!converged) {
     pq= priority_queue<pair<int,double>,vector<pair<int,double> >,larger >();


		//Sample data for training
		getSample(sampleIndex,nsample,rsample,dataset->nrow);
		//build a tree based on the sample and add to forest
		Tree *tree = new Tree();
		tree->iTree(sampleIndex,dataset, 0, maxheight, stopheight);
		this->trees.push_back(tree);
		ntree++;
		double d,scores,dbar;
		topIndex.clear();
		for (int inst = 0; inst <dataset->nrow; inst++)
		{
			d = getdepth(dataset->data[inst],tree);
			totalDepth[inst] += d;
			squaredDepth[inst] +=d*d;
			dbar=totalDepth[inst]/ntree;
			scores = pow(2, -dbar / avgPL(this->nsample));
			pq.push( pair<int, double>(inst,scores));
			

		}
		//if we have 1 tree we don't have variance
		if(ntree<2)
		{	
			for(int i=0;i<tk;i++)
			 {
				inserTopK(topk,pq.top().first);
				topIndex.push_back(pq.top().first);
			 	 pq.pop();

			 }
		prevIndex = topIndex;
			continue;

		}

	    /*	
         *	double maxCIWidth =0;
		

		for(int i=0;i<tk;i++)  //where tk is top 2*\alpha * N index element
		{ 	//int index=0;

	              int index= pq.top().first;
		      //inserTopK(topk,pq.top().first);
		//	topIndex.push_back(pq.top().first);
		//	int index =topk.at(i).first;
			double mn = totalDepth[index]/ntree;
			double var = squaredDepth[index]/ntree -(mn*mn);
			double halfwidth = 1.96*sqrt(var)/sqrt(ntree);
			double scoreWidth = pow(2, -(mn-halfwidth) / avgPL(this->nsample)) -pow(2, -(mn+halfwidth)/ avgPL(this->nsample));
			maxCIWidth=max(maxCIWidth,scoreWidth);
	    //		logfile<<ntree<<","<<pq.top().first<<","<<pq.top().second<<","<<prob<<"\n";
	    //	logfile<<i<<ntree<<","<<index<<","<<pq.top().second<<","<<","<<var<<","<<scoreWidth<<"\n";
				pq.pop();
		}
	
        */


		for(int i=0;i<tk;i++){
            //where tk is top 2*\alpha * N index element
			
            //int index=0;
            //pq.top().first;
		    //	inserTopK(topk,pq.top().first);
			topIndex.push_back(pq.top().first);
		    //	int index =topk.at(i).first;
		    //	double mn = totalDepth[index]/ntree;
		    //	double var = squaredDepth[index]/ntree -(mn*mn);
		    //	double halfwidth = 1.96*sqrt(var)/sqrt(ntree);
		    //	double scoreWidth = pow(2, -(mn-halfwidth) / avgPL(this->nsample)) -pow(2, -(mn+halfwidth)/ avgPL(this->nsample));
		    //	maxCIWidth=max(maxCIWidth,scoreWidth);
			logfile<<ntree<<","<<pq.top().first<<","<<pq.top().second<<","<<prob<<"\n";
			//logfile<<ntree<<","<<topk.at(i).first<<","<<pq.top().second<<","<<pow(2,-mn/avgPL(this->nsample))<<","<<var<<","<<scoreWidth<<"\n";
			pq.pop();
		}
	
	prob=topcommonK(topIndex,prevIndex);
	prevIndex = topIndex;
    if(prob==1)
        conv_cnt++;
    else
        conv_cnt=0;
    converged = conv_cnt>5;
	//	sort(topk.begin(),topk.end(),larger());
	// logfile <<"Tree number "<<ntree<<" built with confidence\t"<<maxCIWidth<<endl;

    //	 logfile <<"Tree number "<<ntree<<" built with confidence\t"<<maxCIWidth<<endl;
    //   		converged = prob>0.99 && ntree >100;     //maxCIWidth <=tau;
	}

    //return this;
}

//Sequential confidence interval stopping

double variance(vector<double> x){
 	double sum=0.0;
	double mn=mean(x);
	for(double elem : x)
	{
	 sum +=pow(elem-mn,2);
	}
	return sum/(double)(x.size()-1);
}

/*
 * Stopping confidence interval width on \theta (k)
 */
void convForest::confstop(double alpha)
{
//	this->nsample = nsample;
	double tk = ceil(alpha*2*dataset->nrow);  //top k ranked scores 
	vector<int> sampleIndex;  //index for sample row 
//	this->rSample = rSample;
	vector<double> totalDepth(dataset->nrow,0);
	double tua =0.008; // 1/(double)dt->nrow;   //need to be changed 
	vector<double> squaredDepth(dataset->nrow,0);
    //priority_queue<pair<int,double>,vector<pair<int,double> >,topscore > pq;
	double hm=0.0 ; //inflation factor will be used later 	
	double  ntree=0.0;
	bool converged=false;
	//double theta_es;
	vector<double> theta_k; //top k score
	vector<pair<int,double> > topk_ac; 
	vector<pair<int ,double> > topk;	
    logfile<<"point,ntree,depth\n";
    //logfile<<"tree,thetain,thetaacc\n";

    priority_queue<pair<int,double>,vector<pair<int,double> >, larger> pq;
	while (!converged) {
	    pq= priority_queue<pair<int,double>,vector<pair<int,double> >,larger >();

		//Sample data for training
		topk.clear();
	    //	topk_ac.clear();
		//get sample data
		getSample(sampleIndex,nsample,rsample,dataset->nrow);
		//build a tree based on the sample and add to forest
		Tree *tree = new Tree();
		tree->iTree(sampleIndex,dataset, 0, maxheight, stopheight);
		this->trees.push_back(tree);
		
		ntree++;
		double d,score,dbar;//,currentscore;
		for (int inst = 0; inst <dataset->nrow; inst++)
		{
			d = getdepth(dataset->data[inst],tree);
			totalDepth[inst] += d;
			squaredDepth[inst] +=d*d;
			dbar=totalDepth[inst]/ntree; //Current average depth 
			score = pow(2, -dbar / avgPL(this->nsample));
            //currentscore = pow(2,-d/avgPL(this->nsample));
			pq.push(pair<int, double>(inst,score));
			topk.push_back(pair<int,double>(inst,d));
		    //topk_ac.push_back(pair<int,double>(inst,scores));
            logfile<<inst<<","<<ntree<<","<<d<<"\n";   
		}
		
	    //Sort shallowest at the top 
		sort(topk.begin(),topk.end(),topscore());
	    //sort(topk_ac.begin(),topk_ac.end(),topscore());
        
        //depth threshold
		double thetabar = (topk[tk].second + topk[tk+1].second)/2.0;
	    //double thetabar_ac =(topk_ac[tk].second + topk_ac[tk+1].second)/2.0;
		
        theta_k.push_back(thetabar);
		//double thetaacc = (
		
        if(ntree<2) //No variance for tree<2
		    continue;

        //logfile<<ntree<<","<<thetabar<<","<<thetabar_ac<<"\n";				
	    //double maxCIWidth =0;		
		//double mn = mean(theta_k);
		
        double var = variance(theta_k);
		double halfwidth=1.96*sqrt(var)/sqrt(ntree);	
	

        //logfile<<ntree<<","<<halfwidth<<","<<mean(theta_k)<<","<<var<<"\n";
 
	    converged=(halfwidth+hm)<=tua;
        converged = false;
        //converged = ntree>1;
	  //converged = ntree>300; //    halfwidth <=tua;
      if(ntree>400) break;  	
	}
	
 //   theta_es=mean(theta_k);
//    std::cout<<theta_es<<endl;
//    double avgScore = score(theta_es,this->nsample);
    //logfile<<"Current K="<<tk<<" K' = "<<mean(theta_k);
   // logfile<<"index,score\n";

}

























                                                                                                  
