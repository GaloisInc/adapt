#include "RForest.hpp"
#include<iostream>
#include<random>
#include<vector>
//using namesapce Eigen;
/*
 * Class for RandomRotation Matrix
 * TODO: convert from data to Matrix class of eigen ;
 */
using namespace std;
using namespace Eigen;
/*
 * Takes matrix and empty vector data
 * Fill the vector-2d with the matrix value
 */
void RForest::convert_to_vector(MatrixXd &m, vector<vector<double> > &v)
{
for (int i=0; i<m.rows(); ++i)
{
    const double* begin = &m.row(i).data()[0];
    v.push_back(std::vector<double>(begin, begin+m.cols()));
}

}

/*
 * Takes dataset of vector<vector<double> and return Matrix of data
 */
MatrixXd RForest::convert_to_Matrix(vector<vector<double> > &data)
{
	MatrixXd mat(data.size(), data[0].size());
	for (int i = 0; i <(int)data.size(); i++)
	  mat.row(i) =VectorXd::Map(&data[i][0],(int)data[i].size());
	return mat;

}

/*
 * Generate random rotation matrix
 */

void RForest::generate_random_rotation_matrix(MatrixXd& M,int n)
{

    std::default_random_engine generator(9090);
	std::normal_distribution<double> distribution(0.0, 1.0);
	MatrixXd A(n, n);
	const VectorXd ones(VectorXd::Ones(n));
	for (int i = 0; i<n; i++)
	{
		for (int j = 0; j<n; j++)
		{
			A(i, j) = distribution(generator);  //mtrand.randNorm(0, 1);

		}
	}
	const HouseholderQR<MatrixXd> qr(A);
	const MatrixXd Q = qr.householderQ();
	M = Q*(qr.matrixQR().diagonal().array() <0).select(-ones, ones).asDiagonal();
	if (M.determinant()<0)
	for (int i = 0; i<n; i++)
		M(i, 0) = -M(i, 0);


}

/*
 * Build forest using rotated data
 */

void RForest::buildForest()
{

vector<int> sampleIndex(this->nsample);

//build trees 
for(int n=0;n<ntree;n++)
{
    sampleIndex.clear();
    getSample(sampleIndex,this->nsample,this->rSample);
    //generate rotation matrix the data from 
    MatrixXd rotmat(dt->ncol,dt->ncol);
    generate_random_rotation_matrix(rotmat,dt->col);
    rotMatrix.push_back(rotmat); //add to list of rot matrix

    vector<vector<double> >tempdata(nsample);
    for(int i : sampleIndex)
    {
tempdata.push_back(
    }


}
    
int n=4;
double dt[8] = { 1,2,3,4,5,6,7,8};
MatrixXd m(n,n);  //rotation matrix
generate_random_rotation_matrix(m,n);
Map<MatrixXd> mt(dt,2,4); // = Map<MatrixXd>
std::cout<<"The matrix is "<<m<<"\n";  //rotation matrix

//std::cout<<" Determinant is "<<m.determinant()<<"\n";
//std::cout<<"Data to matrix "<<mt<<endl;//
//std::cout<<mt*m<<" Dot product"<<endl;


// Convert data to matrix
vector<vector<double> > data(10, vector<double>(4,5));
cout<<"Size "<<data.size()<<"X"<< data[0].size()<<endl;
MatrixXd mv=convert_to_Matrix(data);
cout<<mv<<" is the matrix"<<endl;
cout<<" New rotated data \n";
//cout<<mv*m; //rotate data;

//Convert matrix to data
cout<<"...Coverting Matrix to vector"<<endl;
vector<vector<double> > v;
convert_to_vector(mv,v);
cout<<v.size()<<" by "<<v[0].size()<<endl;
for(vector<double> vv : v)
{
for(double dd : vv)
cout<<dd<<"\t";
cout<<"\n";
}



}



/*
MatrixXd  RForest::rotateData(double column,int size, MatrixXd& M)
{

//MatrixXd eigenX= Map<MatrixXd>(column,1,size);
return Onces(); //;eigenX;
}
*/


