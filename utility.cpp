
/*
 * utitlity.h
 *
 *  Created on: Mar 27, 2015
 *      Author: Tadeze
 */

#include "utility.h"

using namespace std;

int randomI(int min,int max)
{
	 int num;
	num=(int)(min + (rand() % (max - min)));
	return num;
}
/*unsigned randomi(int min,int max)
{
	uniform_int_distribution<unsigned> u(min,max);
	default_random_engine e;
	return u(e);
}*/
double randomD(double min, double max)
{
	return ceil((min + ((double)rand() / (RAND_MAX))*(max - min ) )*100)/100;
}
void sampleI(int min,int max,int nsample,vector<int> &samples)
{
	int cnt=0;
 	bool duplicate=false;
	int rndI;
 	while(cnt<nsample)
 	{
	   rndI=randomI(min,max);
	   for(int i=0;i<cnt;i++)
	     {
		 if(samples[i]==rndI)
		   {
		   duplicate=true;
		   break;
	       }

	     }
	   if (!duplicate)
	   {
		   samples.push_back(rndI);
		   cnt++;
	   }
	  duplicate=false;
   }
}
double avgPL(double n)
 {

   return (((n-1) <= 0) ? 0.0 : (( 2.0 * (log((double)(n-1)) + 0.5772156649)) - ( 2.0 * (double)(n-1))/( 1.0 + (double)(n-1))));

 }
void swapInt(int a,int b,int* x)
{
	int hold;
	hold=x[a];
	x[a]=x[b];
	x[b]=hold;
}

double mean(vector<double> points)
{
	double sum=0;
	for(int f=0;f<(int)points.size();f++)
	  sum+=points[f];
	return sum/(double)points.size();
}
/*
* Read csv file into vector, 
*Will check either pointer or the vaues is better 
*/
vector<vector<double> >  readcsv(const char* filename,char delim=',',bool header=true)
{
	vector<vector<double> > values;
	vector<double> valueline;
	ifstream fin(filename);
	string item;
	if(header)  //if header available
  	  getline(fin,item);

	for(string line;getline(fin,line);)
	 {
	   istringstream in(line);
	 while(getline(in,item,delim))
 	   {
            valueline.push_back(atof(item.c_str()));
           }
       values.push_back(valueline);
       valueline.clear();
  }
return values;
}



 /* UTITLITY_H_ */






