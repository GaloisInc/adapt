/*
 * RotationForest.hpp
 *
 *  Created on: Aug 26 2015
 *      Author: tadeze
 */
/*struct _doubleframe 
{
    double** dt;
    int nrow;
    int ncol;
    
};
typedef struct _doubleframe doubleframe;
*/	


#ifndef RFOREST_H_
#define RFOREST_H_
//#include "utility.hpp"
#include <Dense>
#include <QR>
//#include<vector>
#include "Forest.hpp"
//#include "utility.hpp"
//#include "cincl.hpp"
class RForest:public Forest {
public:
	std::vector<Eigen::MatrixXd> rotMatrices;
/*	int ntree;
	bool rSample;
	int nsample;
    bool stopheight;
    int maxheight;
*/
    void buildForest(doubleframe* df);
    void rForest();
	//Data rotation operations
    void generate_random_rotation_matrix(Eigen::MatrixXd& M,int n);
    Eigen::MatrixXd rotateData(doubleframe* df,Eigen::MatrixXd& M);
    void convert_to_vector(Eigen::MatrixXd &m, std::vector<std::vector<double> > &v);
    doubleframe* convert_to_df(Eigen::MatrixXd &m);
    Eigen::MatrixXd convert_to_Matrix(std::vector<std::vector<double> > &data);
   Eigen::MatrixXd d_convert_to_Matrix(const doubleframe* data,std::vector<int> &sampleIndex);
    RForest(int _ntree,doubleframe* df,bool _rSample,int _nsample,bool _stopheight,int _maxheight):Forest(_ntree,df,_nsample,_maxheight,_stopheight,_rSample){
    	
       /* ntree=_ntree;
    	rSample= _nsample;
    	stopheight= _stopheight;
    	maxheight= _maxheight;*/
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
