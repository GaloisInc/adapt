/*
 * utitlity.h
 *
 *  Created on: Mar 27, 2015
 *      Author: Tadeze
 */
#ifndef UTILITY_HPP_
#define UTILITY_HPP_
#include<iostream>
#include<fstream>
#include<sstream>
#include <cmath>
#include <cstdlib>
#include<ctime>
#include<algorithm>
#include<string>
#include<iterator>
#include<vector>
#include<algorithm>
#include<map>
#include<set>
#include "cincl.hpp"
/* @param dt global doubleframe to whole the dataset */
extern doubleframe* dt;


struct Data
{
	int ncols;
	int nrows;
	std::vector<std::vector<double> > data;
};

/* random number generators between min and max */
template<typename T>
T randomR(T min,T max,std::set<T>& exclude);
int randomI(int min, int max);
double randomD(double min, double max);
int randomI(int min,int max,std::set<int>& exclude);


/* Method for sampling N unique points  between range of min and max value
 * @param min,max  range of the points
 * @param nsample: number of samples to generate
 * @param empty vector index to fill the generated number.
 */
void sampleI(int min, int max, int nsample, std::vector<int> &sampleIndx);
/* swap two integers */
void swapInt(int a, int b, int* x);
/* @param number of points in node
 * @return average estimate of Binary Search Tree c(n)=2*H(n-1)-(2(n-1)/n)
 */
double avgPL(double n);

/*
 * Compute mean of vector double points
 * @param vector of points
 * @return mean of the points
 */
double mean(std::vector<double> points);
/*
 * Alternate way to the current C implementation for reading csv file
 */
std::vector<std::vector<double> > readcsv(const char* filename, char delim,
		bool header);
/* empirical CDF function
 * @param  points vector of data points
 * @return returns ecdf of each points in the vector
 */
std::map<double,double> ecdf(std::vector<double> points);
/* Anderson-Darling distance method
 * @param depth 2-d depth of all points across all trees
 * @param weightToTail: if true gives more weight to tail
 * @return vector score computed.
 */
std::vector<double> ADdistance(std::vector<std::vector<double> > depths,bool weightToTail);

#endif
/* UTITLITY_H_ */

