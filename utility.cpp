/*
 * utitlity.h
 *
 *  Created on: Mar 27, 2015
 *      Author: Tadeze
 */

#include "utility.hpp"


using namespace std;

int randomI(int min, int max) {
	int num;
	num = (int) (min + (rand() % (max - min)));
	return num;
}
/*unsigned randomi(int min,int max)
 {
 uniform_int_distribution<unsigned> u(min,max);
 default_random_engine e;
 return u(e);
 }*/
double randomD(double min, double max) {
	return ceil((min + ((double) rand() / (RAND_MAX)) * (max - min)) * 100)
			/ 100;
}
void sampleI(int min, int max, int nsample, vector<int> &samples) {
	int cnt = 0;
	bool duplicate = false;
	int rndI;
	while (cnt < nsample) {
		rndI = randomI(min, max);
		for (int i = 0; i < cnt; i++) {
			if (samples[i] == rndI) {
				duplicate = true;
				break;
			}

		}
		if (!duplicate) {
			samples.push_back(rndI);
			cnt++;
		}
		duplicate = false;
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
/*
 * Read csv file into vector,
 *just used for testing
 */
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
 * Taken from http://www.johndcook.com/
 */
double phi(double x)
{
// constants
     double a1 =  0.254829592;
     double a2 = -0.284496736;
     double a3 =  1.421413741;
     double a4 = -1.453152027;
     double a5 =  1.061405429;
     double p  =  0.3275911;

      // Save the sign of x
      int sign = 1;
      if (x < 0)
      sign = -1;
     x = fabs(x)/sqrt(2.0);

  // A&S formula 7.1.26
   double t = 1.0/(1.0 + p*x);
   double y = 1.0 - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*exp(-x*x);

   return 0.5*(1.0 + sign*y);
 }




/*
 * Compute the Anderson Darling normality test of vector X
 */
/*
double ADtest(double x[],int size)
{
	double mean,sd;


	if(size<8)
	{
		exit(0);
	}
	else
	{
	//	zscore(x,size,&mean,&sd);
int x=2;




	}


}*/
/*
 * Z-score
 */
void zscore(double x[],const int size,double* mean,double* sd)
	{
	int n=size;
	double sum=0.0,sum_deviation=0.0;
	for(int i=0;i<n;i++)
		sum +=x[i];
	*mean= (sum/n);
	for( int j=0;j<n;j++)
		sum_deviation += (x[j]-*mean)*(x[j]-*mean);
	*sd=sqrt(sum_deviation/n);
	for(int k=0;k<n;k++)
	 x[k]=(x[k]-*mean)/ *sd;

	}





/*
 * takes Vector of points and returns empirical CDF
 */
/*vector<double> ecdf(double* points)
{


}*/
/* UTITLITY_H_ */

