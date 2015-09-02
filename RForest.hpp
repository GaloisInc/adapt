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
#include "Eigen/Dense"
#include "Eigen/QR"
#include "Forest.hpp"
class RForest: public Forest {
public:
	std::vector<Eigen::MatrixXd> rotMatrices;
	 int pnt;
	void convertToDf(Eigen::MatrixXd &m, doubleframe* df);
	void rotateInstance(double* inst, Eigen::MatrixXd &m,double* rotatedData);
	void buildForest(doubleframe* df);
	void generateRandomRotationMatrix(Eigen::MatrixXd& M, int n, int seed);
	void convertToVector(Eigen::MatrixXd &m,
			std::vector<std::vector<double> > &v);
	Eigen::MatrixXd convertToMatrix(std::vector<std::vector<double> > &data);
	Eigen::MatrixXd rotateData(doubleframe* dt, Eigen::MatrixXd& M);
	Eigen::MatrixXd convertDfToMatrix(const doubleframe* data,
			std::vector<int> &sampleIndex);
	std::vector<double> pathLength(double *inst);
	void rForest();

	//Constructor

	RForest(int _ntree,doubleframe* _df,int _nsample,int _maxheight,
			bool _stopheight,bool _rsample) :
			Forest(_ntree, _df, _nsample, _maxheight, _stopheight, _rsample) {
		pnt=0;}
	;

	RForest() {
	}
	;
	virtual ~RForest() {

	}
	;
};
#endif /* RFOREST_H_ */

