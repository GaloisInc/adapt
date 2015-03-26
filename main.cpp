/*
 * main.cpp
 *
 *  Created on: Mar 22, 2015
 *      Author: Tadeze
 */
#include<iostream>
#include<fstream>
#include<cstdlib>
using namespace std;
int main()
{
///Assume I have data in  two dimensional matrix
const int NROW=100;
const int NCOL=10;
double data[NROW][NCOL];
for(int i=0;i<NROW;i++)
{
	for(int j=0;j<NCOL;j++)
	{
		data[i][j]=(j+i)*rand()/((float)RAND_MAX+1);
       // cout<<data[i][j]<<"\t";
	}
	//cout<<endl;
}




}




