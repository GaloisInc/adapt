/*
 * utility.cpp
 *
 *  Created on: Mar 30, 2015
 *      Author: Tadeze
 */
/*
 * utitlity.h
 *
 *  Created on: Mar 27, 2015
 *      Author: Tadeze
 */

#include "utility.h"
int randomI(int min,int max)
{ //srand(time(NULL));
int output;

return (int)ceil(min + ((double)rand() / (RAND_MAX))*(max - min ));
}
double randomD(double min, double max)
{
	float output;

	return ceil((min + ((double)rand() / (RAND_MAX))*(max - min ) )*100)/100;
}
void sampleI(int min,int max,int nsample,int* samples)
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
			 { duplicate=true;
			   break;
			 }

	}
if(!duplicate)
	samples[cnt++]=rndI;
duplicate=false;


 }


}
double avgPL(double n) {

   return (((n-1) <= 0) ? 0.0 : (( 2.0 * (log((double)(n-1)) + 0.5772156649)) - ( 2.0 * (double)(n-1))/( 1.0 + (double)(n-1))));

 }
void swapInt(int a,int b,int* x)
{
	int hold;
	hold=x[a];
	x[a]=x[b];
	x[b]=hold;
}





 /* UTITLITY_H_ */






