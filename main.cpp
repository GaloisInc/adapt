
/*
 * main.cpp
 *
  Created on: Mar 22, 2015
 * Author: Tadeze
 */
#include "main.hpp"
using namespace std;
//log file
doubleframe* dt;
int main(int argc, char* argv[])
{
    parsed_args* pargs = parse_args(argc,argv);
    ntstring input_name = pargs->input_name;
    ntstring output_name = pargs->output_name;
    d(int)* metacol = pargs->metacol;
    int ntree = pargs->ntrees;
    int nsample = pargs->sampsize;
    int maxheight = pargs->maxdepth;
    bool header = pargs->header;
    bool verbose = pargs->verbose;
    verbose=verbose;//Clears warning for now.
    bool rsample=nsample!=0;
    bool stopheight = maxheight!=0;


    ntstringframe* csv = read_csv(input_name,header,false,false);
   ntstringframe* metadata = split_frame(ntstring,csv,metacol);
    metadata = metadata;//Clears warning for now.
     dt = conv_frame(double,ntstring,csv);
     /*NOTE: We convert to your data structure for now, but we will
     *want to standardize this soon. -Andrew*/


srand (time(NULL));

	IsolationForest iff(ntree,maxheight,stopheight,nsample,rsample);
	//traverse(iff.trees[1]);
	vector<double> scores = iff.AnomalyScore(dt);
	//iff.data = test;
	vector<vector<double> > pathLength=iff.pathLength(dt);
	//scoref

	ofstream outscore(output_name);
	outscore<<"indx,score,avgDepth,adscore\n";
	vector<double> adscore = iff.ADtest();
	for(int j=0;j<(int)scores.size();j++)
	{
		outscore<<j<<","<<scores[j]<<","<<mean(pathLength[j])<<","<<adscore.at(j)<<"\n";

	}
	outscore.close();
	cout << "\n...Finished\n";
	return 0;
}

