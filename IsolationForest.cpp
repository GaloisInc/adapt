/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */
#include<iostream>
#include<string>
#include<fstream>
#include<vector>
using namespace std;

class DataTable {

	// private class variables

	// private methods

public:
	// public class variables
	vector<string> labels;
	vector<vector<float>> data;
	vector<string> dataClasses;

	// public methods

}; // class DataTable

class IsolationForest {

public:
	IsolationForest();
	virtual ~IsolationForest();
	void readData(string filename);
	void trainTree();
};


