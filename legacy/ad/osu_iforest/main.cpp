/*
 * main.cpp
 *
 @Created on: Mar 22, 2015
 * @Author: Tadeze
 * Main entry: accepts the the
 * @param argv

 Usage: iforest [Options]
 Options:
        -i FILE, --infile=FILE
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
        -w N, --windowsize=N
                specify window size.
                Default value is 512.
        -c N, --columns=N
                specify number of columns to use (0 indicates to use all columns).
				Default value is 0.
        -h, --help
                Print this help message and exit.
 */

#include "main.hpp"
using namespace std;
//log file
//ofstream logfile("treepath.csv");

void deletedoubleframe(doubleframe *df) {
	for (int ii = 0; ii < df->nrow; ++ii) {
		delete[] df->data[ii];
	}
	delete[] df->data;
	delete df;
}

doubleframe *copyNormalInstances(const doubleframe *dtOrg, const ntstringframe* metadata) {
	int cntNormal = 0, cnt = 0;
	for(int i = 0; i < metadata->nrow; ++i){
		if(!strcmp(metadata->data[i][0], "nominal"))
			++cntNormal;
	}
	std::cout << "Number of normals: " << cntNormal << std::endl;
	doubleframe *dtOn = new doubleframe();
//	dtOn->colnames = dtOrg->colnames;
	dtOn->column_major = dtOrg->column_major;
	dtOn->ncol = dtOrg->ncol;
	dtOn->nrow = cntNormal;
//	dtOn->rownames = dtOrg->rownames;
	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	for (int ii = 0; ii < metadata->nrow; ++ii) {
		if(!strcmp(metadata->data[ii][0], "nominal")){
			for (int jj = 0; jj < dtOn->ncol; ++jj) {
				dtOn->data[cnt][jj] = dtOrg->data[ii][jj];
			}
			++cnt;
		}
	}
	return dtOn;
}

doubleframe *copyAnomalyInstances(const doubleframe *dtOrg, const ntstringframe* metadata) {
	int cntAnomaly = 0, cnt = 0;
	for(int i = 0; i < metadata->nrow; ++i){
		if(!strcmp(metadata->data[i][0], "anomaly"))
			++cntAnomaly;
	}
	std::cout << "Number of anomaly: " << cntAnomaly << std::endl;
	doubleframe *dtOn = new doubleframe();
//	dtOn->colnames = dtOrg->colnames;
	dtOn->column_major = dtOrg->column_major;
	dtOn->ncol = dtOrg->ncol;
	dtOn->nrow = cntAnomaly;
//	dtOn->rownames = dtOrg->rownames;
	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	for (int ii = 0; ii < metadata->nrow; ++ii) {
		if(!strcmp(metadata->data[ii][0], "anomaly")){
			for (int jj = 0; jj < dtOn->ncol; ++jj) {
				dtOn->data[cnt][jj] = dtOrg->data[ii][jj];
			}
			++cnt;
		}
	}
	return dtOn;
}

doubleframe *copySelectedRows(const doubleframe *dtOrg, std::vector<int> idx, int from, int to) {
	doubleframe *dtOn = new doubleframe();
//	dtOn->colnames = dtOrg->colnames;
	dtOn->column_major = dtOrg->column_major;
	dtOn->ncol = dtOrg->ncol;
	dtOn->nrow = to - from + 1;
//	dtOn->rownames = dtOrg->rownames;
	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		for (int jj = 0; jj < dtOn->ncol; ++jj) {
			dtOn->data[ii][jj] = dtOrg->data[idx[from + ii]][jj];
		}
	}
	return dtOn;
}


doubleframe *copyRows(const doubleframe *dtOrg, int from, int to) {
	doubleframe *dtOn = new doubleframe();
//	dtOn->colnames = dtOrg->colnames;
	dtOn->column_major = dtOrg->column_major;
	dtOn->ncol = dtOrg->ncol;
	dtOn->nrow = to - from + 1;
//	dtOn->rownames = dtOrg->rownames;
	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		for (int jj = 0; jj < dtOn->ncol; ++jj) {
			dtOn->data[ii][jj] = dtOrg->data[from + ii][jj];
		}
	}
	return dtOn;
}

doubleframe *copyCols(const doubleframe *dtOrg, int from, int to) {
	doubleframe *dtOn = new doubleframe();
//	dtOn->colnames = dtOrg->colnames;
	dtOn->column_major = dtOrg->column_major;
	dtOn->ncol = to - from + 1;
	dtOn->nrow = dtOrg->nrow;
//	dtOn->rownames = dtOrg->rownames;
	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		for (int jj = 0; jj < dtOn->ncol; ++jj) {
			dtOn->data[ii][jj] = dtOrg->data[ii][from + jj];
		}
	}
	return dtOn;
}

std::vector<int> getRandomIdx(int nsample, int nrow) {
	std::vector<int> sampleIndex;
	if(nsample > nrow || nsample <= 0)
		nsample = nrow;
	int *rndIdx = new int[nrow];
	for(int i = 0; i < nrow; ++i)
		rndIdx[i] = i;
	for(int i = 0; i < nsample; ++i){
		int r = std::rand() % nrow;
		int t = rndIdx[i];
		rndIdx[i] = rndIdx[r];
		rndIdx[r] = t;
	}
	for(int i = 0; i < nsample; ++i){
		sampleIndex.push_back(rndIdx[i]);
	}
	delete []rndIdx;
	return sampleIndex;
}

doubleframe *combine(doubleframe *dt1, doubleframe *dt2){
	doubleframe *dtOn = new doubleframe();

	dtOn->column_major = dt1->column_major;
	dtOn->ncol = dt1->ncol;
	dtOn->nrow = dt1->nrow + dt2->nrow;

	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	int i = 0, j = 0;
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		for (int jj = 0; jj < dtOn->ncol; ++jj) {
			if(ii < dt1->nrow)
				dtOn->data[ii][jj] = dt1->data[i][jj];
			else
				dtOn->data[ii][jj] = dt2->data[j][jj];
		}
		if(ii < dt1->nrow) i++;
		else j++;
	}
	return dtOn;
}

doubleframe *createTrainingSet(doubleframe *dtTrainNorm, doubleframe *dtTrainAnom, int numNorm, int numAnom){
	doubleframe *dtOn = new doubleframe();

	dtOn->column_major = dtTrainNorm->column_major;
	dtOn->ncol = dtTrainNorm->ncol;
	dtOn->nrow = numNorm + numAnom;

	dtOn->data = new double *[dtOn->nrow];
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		dtOn->data[ii] = new double[dtOn->ncol];
	}
	std::vector<int> nidx = getRandomIdx(numNorm, dtTrainNorm->nrow);
	std::vector<int> aidx = getRandomIdx(numAnom, dtTrainAnom->nrow);
	int i = 0, j = 0;
	for (int ii = 0; ii < dtOn->nrow; ++ii) {
		int normal = std::rand() % (numNorm + numAnom);
		if(normal >= numAnom && i >= (int)nidx.size())
			normal = 0;
		if(normal < numAnom && j >= (int)aidx.size())
			normal = numAnom;
		for (int jj = 0; jj < dtOn->ncol; ++jj) {
			if(normal >= numAnom)
				dtOn->data[ii][jj] = dtTrainNorm->data[nidx[i]][jj];
			else
				dtOn->data[ii][jj] = dtTrainAnom->data[aidx[j]][jj];
		}
		if(normal >= numAnom) ++i;
		else ++j;
	}
	return dtOn;
}

std::vector<int> getRestIdx(std::vector<int> idx, int nrow){
	std::vector<int> restidx;
	bool *track = new bool[nrow];
	for(int i = 0; i < nrow; ++i)
		track[i] = true;
	for(int i = 0; i < (int)idx.size(); ++i)
		track[idx[i]] = false;
	for(int i = 0; i < nrow; ++i){
		if(track[i] == true)
			restidx.push_back(i);
	}
	delete []track;
	return restidx;
}

int sidx = 0;
struct sItem {
	double *p;
	ntstring *q;
};
int cmp(sItem *a, sItem *b) {
	if (a->p[sidx] > b->p[sidx])
		return 1;
	if (a->p[sidx] < b->p[sidx])
		return -1;
	return 0;
}

void printData(int fid, doubleframe* dt, ntstringframe* metadata) {
	char fname[100];
	sprintf(fname, "data%d.csv", fid + 1);
	std::ofstream out(fname);
	for (int i = 0; i < dt->nrow; ++i) {
		if(metadata)
			out << metadata->data[i][0];
		for (int j = 0; j < dt->ncol; ++j) {
			out << "," << dt->data[i][j];
		}
		out << std::endl;
	}
	out.close();
}
void sortByFeature(int fid, doubleframe* dt, ntstringframe* metadata) {
	sidx = fid;
	sItem *tp = new sItem[dt->nrow];
	for (int i = 0; i < dt->nrow; ++i) {
		tp[i].p = dt->data[i];
		tp[i].q = metadata->data[i];
	}
	qsort(tp, dt->nrow, sizeof(tp[0]),
			(int (*)(const void *, const void *))cmp);
    for(int i = 0; i < dt->nrow; ++i) {
		dt->data[i] = tp[i].p;
		metadata->data[i] = tp[i].q;
	}
	delete[] tp;
}

// randomly shuffle dt along with metadata
void randomShuffle(doubleframe* dt, ntstringframe* metadata) {
	for (int ii = 0; ii < dt->nrow; ++ii) {
		int ridx = rand() % dt->nrow;
		if (metadata) {
			ntstring *t = metadata->data[ii];
			metadata->data[ii] = metadata->data[ridx];
			metadata->data[ridx] = t;
		}
		double *t = dt->data[ii];
		dt->data[ii] = dt->data[ridx];
		dt->data[ridx] = t;
	}
}

void printScoreToFile(vector<double> &scores, const ntstringframe* metadata,
		char fName[]) {
	ofstream outscore;
	outscore.open(fName);
	outscore << "groundtruth,score\n";
	for (int j = 0; j < (int) scores.size(); j++) {
		if (metadata) {
			outscore << metadata->data[j][0] << ",";
		}
		outscore << scores[j] << "\n";
	}
	outscore.close();
}

struct Obj{
    int idx;
    double score;
};
int ObjCmp(Obj *a, Obj *b){
    if(a->score < b->score) return 1;
    if(a->score > b->score) return -1;
    return 0;
}

void printScoreToFile(const vector<double> &scores, ntstringframe* csv,
		const ntstringframe* metadata, const doubleframe *dt, char fName[]) {
	ofstream outscore;
	outscore.open(fName);
	// print header
	for(int i = 0; i < metadata->ncol; ++i)
		outscore << metadata->colnames[i] << ",";
	for(int i = 0; i < csv->ncol; ++i)
        outscore << csv->colnames[i] << ",";

	outscore << "anomaly_score\n";

    Obj *idx = new Obj[scores.size()];
	for (int i = 0; i < (int) scores.size(); i++) {
        idx[i].idx = i;
        idx[i].score = scores[i];
    }
    qsort(idx, scores.size(), sizeof(idx[0]),
			(int (*)(const void *, const void *))ObjCmp);

	for (int i = 0; i < (int) scores.size(); i++) {
		if(scores[idx[i].idx] <= 0) continue;
		for(int j = 0; j < metadata->ncol; ++j)
			outscore << metadata->data[idx[i].idx][j] << ",";
		for(int j = 0; j < dt->ncol; ++j)
			outscore << dt->data[idx[i].idx][j] << ",";
		outscore << scores[idx[i].idx] << "\n";
	}
	delete []idx;
	outscore.close();
}

int main(int argc, char* argv[]) {
	std::time_t st = std::time(nullptr);
	srand(0); //randomize for random number generator.
	// parse argument from command line
	parsed_args* pargs = parse_args(argc, argv);
	ntstring input_name = pargs->input_name;
	ntstring output_name = pargs->output_name;
	d(int)* metacol = pargs->metacol;
	int ntree = pargs->ntrees;
	int nsample = pargs->sampsize;
	int maxheight = pargs->maxdepth;
	bool header = pargs->header;
//	bool verbose = pargs->verbose;
	bool rsample = nsample != 0;
	ntstring test_file_name = pargs->test_file_name;
	int checkRange = pargs->check_range;
	int normType = pargs->normalization_type;
	int skipLimit = pargs->skip_limit;
	int thParam = pargs->th_param;
	int sepAlarms = pargs->sep_alarm;
	if(checkRange > 0){
		Tree::checkRange = true;
	}
//	int useColumns = 0;
//	int trainsampIdx = pargs->columns;//c for train sample size option
//	std::cout << useColumns << std::endl;
//	int windowSize = pargs->window_size;

	ntstringframe* csv = read_csv(input_name, header, false, false);
	ntstringframe* metadata = split_frame(ntstring, csv, metacol, true);
	doubleframe* dt = conv_frame(double, ntstring, csv); //read data to the global variable
	nsample = nsample == 0 ? dt->nrow : nsample;

	// read test data
	ntstringframe* testcsv = read_csv(test_file_name, header, false, false);
	ntstringframe* testmetadata = split_frame(ntstring, testcsv, metacol, true);
	doubleframe* testdt = conv_frame(double, ntstring, testcsv); //read data to the global variable

	// perform normalization
	if(normType == 1){//convert all features to binary i.e. all non-zero to 1
		for(int i = 0; i < dt->nrow; i++){
			for(int j = 0; j < dt->ncol; j++){
				if(dt->data[i][j] != 0)
					dt->data[i][j] = 1;
			}
		}
		for(int i = 0; i < testdt->nrow; i++){
			for(int j = 0; j < testdt->ncol; j++){
				if(testdt->data[i][j] != 0)
					testdt->data[i][j] = 1;
			}
		}
	}else if(normType == 2){// normalize each row to sum to 1
		for(int i = 0; i < dt->nrow; i++){
			double sum = 0;
			for(int j = 0; j < dt->ncol; j++){
				sum += dt->data[i][j];
			}
			if(sum > 0){
				for(int j = 0; j < dt->ncol; j++){
					dt->data[i][j] /= sum;
				}
			}
		}
		for(int i = 0; i < testdt->nrow; i++){
			double sum = 0;
			for(int j = 0; j < testdt->ncol; j++){
				sum += testdt->data[i][j];
			}
			if(sum > 0){
				for(int j = 0; j < testdt->ncol; j++){
					testdt->data[i][j] /= sum;
				}
			}
		}
	}

//	for(int i = 0; i < 10; i++){
//		for(int j = 0; j < dt->ncol; j++){
//			std::cout << dt->data[i][j] << ", ";
//		}
//		std::cout << endl;
//	}
//	std::cout << endl;
//	for(int i = 0; i < 10; i++){
//		for(int j = 0; j < dt->ncol; j++){
//			std::cout << testdt->data[i][j] << ", ";
//		}
//		std::cout << endl;
//	}

	std::cout << "# Trees     = " << ntree << std::endl;
	std::cout << "# Samples   = " << nsample << std::endl;
	std::cout << "# MaxHeight = " << maxheight << std::endl;
	std::cout << "Check Range = " << Tree::checkRange << std::endl;
	std::cout << "Norm. Type  = " << normType << std::endl;
	std::cout << "Skip Limit  = " << skipLimit << std::endl;
	std::cout << "TH param    = " << thParam << std::endl;
	std::cout << "Sep. Alarms = " << sepAlarms << std::endl;
	std::cout << "Train Data Dimension: " << dt->nrow << "," << dt->ncol << std::endl;
	std::cout << " Test Data Dimension: " << testdt->nrow << "," << testdt->ncol << std::endl;
	std::cout << "Meta cols: " << metadata->ncol << std::endl;
	std::cout << "input_name: " << input_name << std::endl;
	std::cout << "test_name:  " << test_file_name << std::endl;
	std::cout << "output_name:" << output_name << std::endl;

	char cmd[1000];
	std::map<std::string,OnlineIF *> IFModels;
	std::map<std::string,bool> used;
	std::map<std::string,double> TH;
	for(int nCmd = 0; nCmd < metadata->nrow; nCmd++){
		strcpy(cmd, metadata->data[nCmd][0]);
		if(used.find(cmd) != used.end())
			continue;
		used[cmd] = true;
		std::cout << "\nCMD: " << cmd;

		std::vector<int> cmdIdx;
		for(int i = 0; i < metadata->nrow; i++){
			if(strcmp(cmd, metadata->data[i][0]) == 0)
				cmdIdx.push_back(i);
		}
		std::cout << " -> " << cmdIdx.size() << std::endl;
		if((int)cmdIdx.size() < skipLimit)
			continue;
		std::vector<int> restidx = getRestIdx(cmdIdx, dt->nrow);

		doubleframe *dtTrain = copySelectedRows(dt, cmdIdx,  0,  cmdIdx.size()-1);
		doubleframe *dtRest =  copySelectedRows(dt, restidx, 0, restidx.size()-1);

		int nOther = (int)ceil(30.0 * dtTrain->nrow / 70.0);
		if(dtTrain->nrow < nsample){
			nOther = nsample - dtTrain->nrow;
		}
		std::vector<int> oidx = getRandomIdx(nOther, dtRest->nrow);
		doubleframe *dtOther =  copySelectedRows(dtRest, oidx, 0, oidx.size()-1);
		doubleframe *dtInitTrain = combine(dtTrain, dtOther);

		// create tree structure
		OnlineIF *iff = new OnlineIF(ntree, dtInitTrain, nsample, maxheight, rsample, dtInitTrain->nrow);
//		char f1[100], f2[100];
//		sprintf(f1, "%s/%sInitTree.txt", output_name, cmd);
//		sprintf(f2, "%s/%sModTree.txt", output_name,  cmd);
//		ofstream o1(f1), o2(f2);
//		iff.printStat(o1);
		// update counts from the training data
		iff->setWindowSize(dtTrain->nrow);
		for(int i = 0; i < dtTrain->nrow; i++)
			iff->update(dtTrain->data[i]);
		// change nsample size to use a # of training data to normalize iforest score
		iff->setnsample(dtTrain->nrow);
//		iff.printStat(o2);

		// get score for training data
		std::vector<double> scores = iff->AnomalyScore(dtTrain);
		std::sort(scores.begin(), scores.end());
		// compute threshold
		int rnk = 1;
		double th = scores[scores.size()-1];
		for(int i = scores.size()-1; i >= (thParam-1); i--){
			th = scores[i];
			bool found = true;
			for(int j = 1; j < thParam; j++){
				if(th != scores[i-j]){
					found = false;
					break;
				}
			}
			if(found){
				rnk = scores.size() - i;
				break;
			}
		}
		std::cout << "TH = " << th << "(" << rnk << ")" << std::endl;
		TH[cmd] = th;

		// delete the data frames
		deletedoubleframe(dtInitTrain);
		deletedoubleframe(dtTrain);
		deletedoubleframe(dtOther);
		deletedoubleframe(dtRest);

		iff->printNumLeafandDepths();
		IFModels[cmd] = iff;
	}
	std::cout << "\nTotal Models built: " << IFModels.size() << std::endl;
	std::cout << "Time to create IF Models: " << std::time(nullptr) - st << " seconds\n";

	if(sepAlarms == 0){
		std::vector<double> scores;
		for(int i = 0; i < testdt->nrow; i++){
			strcpy(cmd, testmetadata->data[i][0]);
			double curScore = 1;
			if(IFModels.find(cmd) == IFModels.end()){
				for(std::map<std::string,OnlineIF *>::iterator iff = IFModels.begin();
						iff != IFModels.end(); iff++){
					double tscore = iff->second->instanceScore(testdt->data[i]);
					if(tscore > TH[iff->first] && tscore < curScore)
						curScore = tscore;
				}
				if(curScore == 1)
					curScore = 0;
			}else{
				curScore = IFModels[cmd]->instanceScore(testdt->data[i]);
				if(curScore <= TH[cmd])
					curScore = 0;
			}
			scores.push_back(curScore);
		}

		printScoreToFile(scores, testcsv, testmetadata, testdt, output_name);
	}else{
		std::vector<double> scoresRel;
		std::vector<double> scoresIrrel;
		for(int i = 0; i < testdt->nrow; i++){
			strcpy(cmd, testmetadata->data[i][0]);
			double curScore = 1;
			if(IFModels.find(cmd) == IFModels.end()){
				for(std::map<std::string,OnlineIF *>::iterator iff = IFModels.begin();
						iff != IFModels.end(); iff++){
					double tscore = iff->second->instanceScore(testdt->data[i]);
					if(tscore > TH[iff->first] && tscore < curScore)
						curScore = tscore;
				}
				if(curScore == 1)
					curScore = 0;
				scoresRel.push_back(0);
				scoresIrrel.push_back(curScore);
			}else{
				curScore = IFModels[cmd]->instanceScore(testdt->data[i]);
				if(curScore <= TH[cmd])
					curScore = 0;
				scoresRel.push_back(curScore);
				scoresIrrel.push_back(0);
			}
		}
		char tmp1[1000], tmp2[1000];
		string s(output_name);
		if(s.find_last_of('.') != string::npos){
			sprintf(tmp1, "%s_relevent.csv", s.substr(0, s.find_last_of('.')).c_str());
			sprintf(tmp2, "%s_irrelevent.csv", s.substr(0, s.find_last_of('.')).c_str());
		}
		else{
			sprintf(tmp1, "%s_relevent.csv", output_name);
			sprintf(tmp2, "%s_irrelevent.csv", output_name);
		}
		printScoreToFile(scoresRel, testcsv, testmetadata, testdt, tmp1);
		printScoreToFile(scoresIrrel, testcsv, testmetadata, testdt, tmp2);
	}

	// delete the model
	for(std::map<std::string,OnlineIF *>::iterator iff = IFModels.begin(); iff != IFModels.end(); iff++){
		delete iff->second;
	}

	std::cout << "Total Time Elapsed: " << std::time(nullptr) - st << " seconds\n";
	return 0;
}
