/*
 * main.cpp
 *
 @Created on: Mar 22, 2015
 * @Author: Tadeze
 * Main entry: accepts the the
 * @param argv
 *    Usage: iforest [options]
 *      -i FILE, --infile=FILE
 Specify path to input data file. (Required).
 -o FILE, --outfile=FILE
 Specify path to output results file. (Required).
 -m COLS, --metacol=COLS
 Specify columns to preserve as meta-data. (Separated by ',' Use '-' to specify ranges).
 -t N, --ntrees=N
 Specify number of trees to build.
Default value is 100.
 -s S, --sampsize=S
 Specify subsampling rate for each tree. (Value of 0 indicates to use entire data set).
 Default value is 2048.
 -d MAX, --maxdepth=MAX
 Specify maximum depth of trees. (Value of 0 indicates no maximum).
 Default value is 0.
 -H, --header
 Toggle whether or not to expect a header input.
 Default value is true.
 -v, --verbose
 Toggle verbose ouput.
 Default value is false.
 -h, --help
 Print this help message and exit.
 */

#include "main.hpp"
using namespace std;
//doubleframe* dt; /* global variable doubleframe to hold the data, to be accessed by all classes */
//Data* dt;

//log file
ofstream logfile("treepath.csv");

int main(int argc, char* argv[]) {
	srand(time(NULL)); //randomize for random number generator.
	/*parse argument from command line*/
	parsed_args* pargs = parse_args(argc, argv);
	ntstring input_name = pargs->input_name;
	ntstring output_name = pargs->output_name;
	d(int)* metacol = pargs->metacol;
	int ntree = pargs->ntrees;
	int nsample = pargs->sampsize;
	int maxheight = pargs->maxdepth;
	bool header = pargs->header;
	bool verbose = pargs->verbose;
	verbose = verbose; //Clears warning for now.
	bool rsample = nsample != 0;
	bool stopheight = maxheight != 0;
    // logfile<<"tree,index,spAttr,spValue"<<"\n";
	
	//	bool weightedTailAD=true; //weighed tail for Anderson-Darling test
	ntstringframe* csv = read_csv(input_name, header, false, false);
	ntstringframe* metadata = split_frame(ntstring, csv, metacol,true);
	doubleframe* dt = conv_frame(double, ntstring, csv); //read data to the global variable
   
    /* 	Basic IsolationForest  */

   IsolationForest iff(ntree,dt,nsample,maxheight,stopheight,rsample); //build iForest
  
   RForest rff(ntree,dt,nsample,maxheight,stopheight,rsample);
   rff.rForest();     //build Rotation Forest

   /*
    * .........Parameters for convergent Forest..............
	*/
	//double tau=0.05;
	//double alpha=0.01;
 	//convForest iff(tau,alpha);

    //iff.convergeIF(maxheight,stopheight,nsample,rsample,tau,alpha);
    //iff.confstop(maxheight,stopheight,nsample,rsample,alpha);
	//ntree= iff.trees.size();
	//std::cout<<"Number of trees required="<<ntree<<std::endl;
	
	vector<double> scores = iff.AnomalyScore(dt); //generate anomaly score
	vector<double> rscores = rff.AnomalyScore(dt);
	//vector<vector<double> > pathLength = iff.pathLength(dt); //generate Depth all points in all trees
	//vector<double> adscore = iff.ADtest(pathLength,weightedTailAD); //generate Anderson-Darling difference.

	//Output file for score, averge depth and AD score
	ofstream outscore(output_name);
   
     /*if (metadata!=NULL) {
        if (header) {
            for_each_in_vec(i,cname,metadata->colnames,{
                outscore << *cname << ",";
            })
        } else {
            forseq(c,0,metadata->ncol,{
                outscore << "meta" << c << ",";
            })
        }
         }
    */	outscore << "indx,ifscore,rfscore\n";
	for (int j = 0; j < (int) scores.size(); j++) {
        if (metadata) {
            forseq(m,0,metadata->ncol,{
                 //outscore << metadata->data[j][m] << ",";
                })
            }
		
		outscore << j << "," << scores[j]<<","<<rscores[j]<<"\n"; // << "," << mean(pathLength[j]) << "\n";
    	}
	outscore.close();
    logfile.close();
	return 0;
}

