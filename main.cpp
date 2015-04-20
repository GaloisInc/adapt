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

/*TODO: check on real csv file and compare with implemented algorithms
 * Check the pathLength with anomaly points
 * TODO: Recheck the forest construction, looks there is a broken code in the iTree method

 */
if(argc<=2)
{
cout<<"No input and output file given,";
exit(1);
}
//cout<<"fz-- "<<fz<<endl;
	srand(time(NULL));
	try
	{
		//string logfile=strcat("log_",argv[1]);
		ffile.open("log.csv");
		ffile<<"-------Main program...data input \n";

		int ntree=100;
		int nsample=256;
		bool rsample=true;//true;
//Prepare synthetic data
/*		const int NROW=1000;
		const int NCOL=10;
		float data[NROW][NCOL];
*/
		//read input file
		string filename(argv[1]);
		char* fname=&filename[0];
		vector<vector<double> > dt=readcsv(fname, ',', true);


/*

vector<vector<double> > dt;
for(int i=0;i<NROW;i++)
{
	vector<double> x;
	for(int j=0;j<NCOL;j++)
	{
		if(i%100==0)
		 data[i][j]=(float)randomD(5*j,i*j+1);
		else
		data[i][j]=(float)randomD(-3,10);//(10)*rand()/((float)RAND_MAX+1);
    	 x.push_back(data[i][j]);
  //  myfile<<data[i][j]<<"\t";
	}
//	myfile<<"\n";
	dt.push_back(x);
	x.clear();
}

*/
//myfile.close();




		//Data input
		Data train;
		train.data = dt;
		train.ncols=(int)dt[0].size();//NCOL;
		train.nrows = (int)dt.size();//(int)dt.size();//

		//forest configuration
		int maxheight =(int)ceil(log2(nsample));
		IsolationForest iff(ntree,train,maxheight,nsample,rsample);
		//traverse(iff.trees[1]);
		ffile<<"Training ends ...\n";

		//Scores
		//ofstream scoref("result.csv");
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
//scoref.close();
cout<<"\nFinished\n";
return 0;
}





