/*
 * RotationForest.hpp
 *
 *  Created on: Aug 26 2015
 *      Author: tadeze
 */

#ifndef RFOREST_H_
#define RFOREST_H_

#include <Dense>
#include <QR>
#include "Forest.hpp"
//#include "utility.hpp"
//#include "cincl.hpp"
class RForest: public Forest {
public:
	std::vector<Eigen::MatrixXd> rotMatrix;
/*	int ntree;
	bool rSample;
	int nsample;
    bool stopheight;
    int maxheight;
*/
    void buildForest();

	void generate_random_rotation_matrix(Eigen::MatrixXd& M,int n);
    Eigen::MatrixXd rotate_data(std::vector<int> &sampleIndex,Eigen::MatrixXd& rotM);
    void convert_to_vector(Eigen::MatrixXd &m, std::vector<std::vector<double> > &v);
    Eigen::MatrixXd convert_to_Matrix(std::vector<std::vector<double> > &data);

    RForest(int _ntree,bool _rSample,int _nsample,bool _stopheight,int _maxheight):Forest(_ntree,_nsample,_maxheight,_stopheight,_rSample)
    {
    };
    RForest(){};
    virtual ~RForest()
	{

	};



};
#endif /* RFOREST_H_ */




//double instanceScore(double *inst);
//std::vector<double> AnomalyScore(doubleframe* df);
//std::vector<double> pathLength(double *inst);
//std::vector<std::vector<double> > pathLength(doubleframe* data);
//std::vector<double> ADtest(const std::vector<std::vector<double> > &pathlength, bool weighttotail);
//std::vector<double> importance(double *inst);
//double getdepth(double *inst,Tree* tree);
//void getSample(std::vector<int> &sampleIndex,const int nsample,bool rSample);
