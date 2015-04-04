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
#include <cmath>
#include <cstdlib>
#include "utility.h"
int randomI(int min,int max)
{
	int output;
	return min + (rand() % (int)(max - min + 1));
}
int* sampleI(int min,int max,int nsample)
{
	int samples[nsample];
	for(int i=0;i<nsample;i++)
		samples[i]=randomI(min,max);
	return samples;
}
double avgPL(double n) {

   if (n <= 1.0) return 0;
   return 2 * (log2(n - 1) + 0.5772156649) - (2 * (n - 1) / n);
 }




 /* UTITLITY_H_ */






