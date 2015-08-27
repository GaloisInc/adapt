#include <iostream>
#include <Dense>
#include <QR>
#include<random>
//#include "../MersenneTwister.h"
using namespace Eigen;

void random_rotation_matrix(MatrixXd& M, int n)
{
	//unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
	std::default_random_engine generator(9090);
	std::normal_distribution<double> distribution(0.0, 1.0);
	MatrixXd A(n, n);
	const VectorXd ones(VectorXd::Ones(n));
	for (int i = 0; i<n; i++)
	{
		for (int j = 0; j<n; j++)
		{
			A(i, j) = distribution(generator);      //mtrand.randNorm(0, 1);

		}
	}
	const HouseholderQR<MatrixXd> qr(A);
	const MatrixXd Q = qr.householderQ();
	M = Q*(qr.matrixQR().diagonal().array() <0).select(-ones, ones).asDiagonal();
	if (M.determinant()<0)
	for (int i = 0; i<n; i++)
		M(i, 0) = -M(i, 0);


}
int main()
{
	MatrixXd m(5, 5);
	int n = 5;
	random_rotation_matrix(m, n);


	/*m(0, 0) = 3;
	m(1, 0) = 2.5;
	m(0, 1) = -1;
	m(1, 1) = m(1, 0) + m(0, 1);*/
	std::cout << m << std::endl;
	std::cout << "This is determinant" << m.determinant() << std::endl;
	std::cin.get();
}
