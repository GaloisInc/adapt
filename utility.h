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
int randomI(int min,int max);
void sampleI(int min,int max,int nsample,std::vector<int> &sampleIndx);
double avgPL(double n);
double randomD(double min, double max);
void swapInt(int a,int b,int* x);
double  mean(std::vector<double> points);
std::vector<std::vector<double> > readcsv(const char* filename,char delim,bool header);
extern std::ofstream ffile;//("log.txt");
#endif
 /* UTITLITY_H_ */


