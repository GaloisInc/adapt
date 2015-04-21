/*
 TODO: Debug the source of the problem in concret dataset
 *TODO: output log to file
 * main.cpp
 *
 *  Created on: Mar 22, 2015
 *      Author: Tadeze
 */
#include "classes.hpp"
#include "utility.h"
using namespace std;
//log file
ofstream ffile;

int main(int argc, char* argv[])
{

	if (argc <= 2)
	{
		cout << "No input and output file given, \n"
				"iForest <inputfile> <outputfile> "<<endl;
		exit(1);
	}
	srand (time(NULL));try
{	//string logfile=strcat("log_",argv[1]);
	ffile.open("log.csv");
	ffile<<"-------Main program...data input \n";

	int ntree=100;
	int nsample=256;
	bool rsample=true;//true;
	string filename(argv[1]);
	char* fname=&filename[0];
	vector<vector<double> > dt=readcsv(fname, ',', true);
	Data train;
	train.data = dt;
	train.ncols=(int)dt[0].size();//NCOL;
	train.nrows = (int)dt.size();//(int)dt.size();//
	//forest configuration
	int maxheight =(int)ceil(log2(nsample));
	IsolationForest iff(ntree,train,maxheight,nsample,rsample);
	//traverse(iff.trees[1]);
	ffile<<"Training ends ...\n";
	Data test = train;
	test.nrows=dt.size();
	//assuming train and test data are same.
	vector<double> scores = iff.AnomalyScore(test);
	ffile<<" Anomaly Scores end \n";
	vector<vector<double> > pathLength=iff.pathLength(test);
	//scoref
	ofstream outscore(argv[2]);
	outscore<<"indx,score,avgDepth\n";
	for(int j=0;j<(int)scores.size();j++)
	{
		outscore<<j<<","<<scores[j]<<","<<mean(pathLength[j])<<"\n";
	}
	outscore.close();
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

