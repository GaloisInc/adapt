
/*
 * main.cpp
 *
  Created on: Mar 22, 2015
 * Author: Tadeze
 */
#include "main.hpp"
using namespace std;
//log file
ofstream ffile;
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
    doubleframe* df = conv_frame(double,ntstring,csv);
    dt=df;
    /*NOTE: We convert to your data structure for now, but we will
     *want to standardize this soon. -Andrew*/
    Data train;
    train.ncols = df->ncol;
    train.nrows = df->nrow;
    vector<double> inst;
    for_each_in_frame(r,c,item,df,({
        inst.push_back(*item);
        if (c==df->ncol-1) {
            train.data.push_back(inst);
            inst.clear();
        }
    });)


srand (time(NULL));
try
{
	ffile.open("log.txt");
	ffile<<"-------Main program...data input \n";

	//forest configuration
	IsolationForest iff(ntree,dt,maxheight,stopheight,nsample,rsample);
	//traverse(iff.trees[1]);
	ffile<<"Training ends ...\n";
	Data test = train;
	test.nrows=train.data.size();
	//assuming train and test data are same.
	vector<double> scores = iff.AnomalyScore(dt);
	//iff.data = test;
	ffile<<" Anomaly Scores end \n";
	//vector<vector<double> > pathLength=iff.pathLength(test);
	//scoref

	ofstream outscore(output_name);
	outscore<<"indx,score,avgDepth,adscore\n";
	//vector<double> adscore = iff.ADtest();
	for(int j=0;j<(int)scores.size();j++)
	{
		outscore<<j<<","<<scores[j];//<<","<<mean(pathLength[j])<<","<<adscore.at(j)<<"\n";

	}
	outscore.close();
	cout<<"Finished successfully";
}
catch(const exception& e)
{   //log error
	ffile<<"Error occured: "<<e.what()<<endl;
	ffile.close();
}
	ffile.close();
	cout << "\n...Finished\n";
	return 0;
}

