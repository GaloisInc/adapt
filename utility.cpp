/*
 * utitlity.h
 *
 *  Created on: Mar 27, 2015
 *      Author: Tadeze
 */

#include "utility.hpp"

using namespace std;

template<typename T>
T randomR(T min,T max,std::set<T>& exclude=NULL)
{
	T num;
	num = (T) (min + (rand() % (max - min+1)));
	return exclude.find(num)!=exclude.end()?randomI(min,max,exclude):num;
}

int randomI(int min, int max) {
	int num;
	num = (int) (min + (rand() % (max - min)));
	return num;
}
int randomI(int min,int max,set	<int>& exclude)
{
			int num;
			num = (int) (min + (rand() % (max - min+1)));
			return exclude.find(num)!=exclude.end()?randomI(min,max,exclude):num;
			
}

double randomD(double min, double max) {
	return ceil((min + ((double) rand() / (RAND_MAX)) * (max - min)) * 100)
			/ 100;
}


void sampleI(int min,int max, int nsample,vector<int> &samples)
{
int cnt=0;
int rndI;
set<int> duplicate; //check if the number is already generated

while(cnt<nsample)  //while still nsample not generated
{
rndI = randomI(min,max,duplicate);
samples.push_back(rndI);
duplicate.insert(rndI);
cnt++;

}

}

double avgPL(double n) {

	return (((n - 1) <= 0) ?
			0.0 :
			((2.0 * (log((double) (n - 1)) + 0.5772156649))
					- (2.0 * (double) (n - 1)) / (1.0 + (double) (n - 1))));

}
void swapInt(int a, int b, int* x) {
	int hold;
	hold = x[a];
	x[a] = x[b];
	x[b] = hold;
}

double mean(vector<double> points) {
	double sum = 0;
	for (int f = 0; f < (int) points.size(); f++)
		sum += points[f];
	return sum / (double) points.size();
}
/* Read csv with delimitted format */
vector<vector<double> > readcsv(const char* filename, char delim = ',',
		bool header = true) {
	vector < vector<double> > values;
	vector<double> valueline;
	ifstream fin(filename);
	string item;
	if (header)  //if header available
		getline(fin, item);

	for (string line; getline(fin, line);) {
		istringstream in(line);
		while (getline(in, item, delim)) {
			valueline.push_back(atof(item.c_str()));
		}
		values.push_back(valueline);
		valueline.clear();
	}
	return values;
}

/*
 * CDF function
 */
map<double,double> ecdf(vector<double> points) {

	map<double,double> cdfm;
	sort(points.begin(), points.end()); //sort the points

	int j= -1;
	double len = (double) points.size();
	for (unsigned i = 0; i < points.size(); ++i) {
		if (i == 0 || points[i - 1] != points[i]) {
			j++;
			cdfm.insert({points.at(i),(double) (i + 1) / len}); /* compute F(x) ={number of element less than or equal to x}/n,
																Empirical CDF calculation */
		}
		else  //if there is duplicate take one element and compute F(x)

			{
			cdfm[points.at(j)]=(double) (j + 1) / len;
			}
	}
	return cdfm;

}

/*
 * Flatten  vector<vector<T> > into single Vector<T>
 * @param v nested vector of vector<vector<T> >
 * @return vector<T>  of single dimension
 */
template <typename T>
vector<T> flatten(const vector<vector<T>>& v) {
    size_t total_size = 0;
    for (const auto& sub : v)
        total_size += sub.size();
    vector<T> result;
    result.reserve(total_size);
    for (const auto& sub : v)
        result.insert(result.end(), sub.begin(), sub.end());
    return result;
}


vector<double> ADdistance(vector<vector<double> > depths, bool weightToTail =
		false) {
	//flatten 2-d to 1-d and compute alldepth ECDF of using all points depths
	vector<double> alldepth = flatten(depths);
	map<double, double> Fxm = ecdf(alldepth); //all depth cdf

	vector<double> scores;
	/*
	 * compute score of each point
	 * sort the depth, compute its ECDF
	 */
	for (vector<double> x : depths) {
		sort(x.begin(), x.end());
		map<double, double>::iterator iter = Fxm.begin();
		map<double, double> fxn = ecdf(x);  //empirical cdf of the point
		double sum = 0;
		for (double elem : x) {
			double val;
			while (iter != Fxm.end()) {
				if (iter->first > elem) //x.at(i))
					{

					val = (--iter)->second;
					break;
				}
				++iter;
			}
			if (iter == Fxm.end())
				val = 1; //set to 1 for upper tail end of the CDF
			double cdfDiff = max((fxn[elem]-val), 0.0);
			sum += weightToTail ? cdfDiff / sqrt(val) : cdfDiff; //distance of each point in the vector


		}
		scores.push_back(sum);
	}
		return scores;

	}

/* UTITLITY_H_ */

