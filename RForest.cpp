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
struct _doubleframe 
{
    double** dt;
    int nrow;
    int ncol;
    
};
typedef struct _doubleframe doubleframe;
*/	

/*
 * Takes matrix and empty doubleframe and fill it 
 * Fill the vecto-2d matrix
 */
doubleframe* RForest::convert_to_df(MatrixXd &m){
    doubleframe* df = new doubleframe();
    df->data = new double*[m.rows()];
    df->nrow = m.rows();
    df->ncol = m.cols();
   
    for (int i=0; i<m.rows(); ++i)
    {
      //  for(int j=0;j<m.cols();j++)
     df->data[i] =&m.row(i).data()[0];//   &m(i,j);//  i.row(i).data()[j];
    // const double* begin = &m.row(i).data()[0];
     // v.push_back(std::vector<double>(begin, begin+m.cols()));
    }
return df;
}

/*
 * Takes matrix and empty vector data
 * Fill the vector-2d with the matrix value
 */
void RForest::convert_to_vector(MatrixXd &m, vector<vector<double> > &v){
    for (int i=0; i<m.rows(); ++i)
    {
        const double* begin = &m.row(i).data()[0];
        v.push_back(std::vector<double>(begin, begin+m.cols()));
    }
}

/*
 * Takes dataset of vector<vector<double> and return Matrix of data
 */
MatrixXd RForest::convert_to_Matrix(vector<vector<double> > &data) {
	MatrixXd mat(data.size(), data[0].size());
	for (int i = 0; i <(int)data.size(); i++)
	      mat.row(i) =VectorXd::Map(&data[i][0],(int)data[i].size());
	return mat;
    }

/* Convert struct data to Matrix
 *
 */

MatrixXd RForest::d_convert_to_Matrix(const doubleframe* data,
        vector<int> &sampleIndex)
{
	MatrixXd mat((int)sampleIndex.size(), data->ncol);
	for (int i = 0; i <(int)sampleIndex.size(); i++)
	  mat.row(i) =VectorXd::Map(&data->data[sampleIndex[i]][0],data->ncol);
	return mat;

}

/*
 * Rotate data
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


MatrixXd  RForest::rotateData(doubleframe* dt, MatrixXd& M)
{  std::vector<int> sampleIndex={5,6,8,3,2,9};
 //  getSample(sampleIndex,10,true);
   MatrixXd  mData = d_convert_to_Matrix(dt,sampleIndex);
    return mData*M;

}

void RForest::buildForest(doubleframe* df){
    int n=df->ncol;
    //double dt[8] = { 1,2,3,4,5,6,7,8};
    MatrixXd m(n,n);  //rotation matrix
    generate_random_rotation_matrix(m,n);
    //Map<MatrixXd> mt(dt,2,4); // = Map<MatrixXd>
    std::cout<<"The rotation matrix is \n"<<m<<"\n";  //rotation matrix
    std::vector<int> sampleIndex;//= { 1,6,7,8};
    sampleI(0,df->nrow-1,4,sampleIndex);
//   getSample(sampleIndex,4,true);
   
    MatrixXd Mdouble = d_convert_to_Matrix(df,sampleIndex);
    cout<<"convert data to matriX\n";
    cout<<Mdouble<<endl;
    //Rotate data 
   // MatrixXd rotdata = rotateData(df,m);
   // cout<<"Rotated data"<<endl<<rotdata;

    // Convert data to matrix
    vector<vector<double> > data(10, vector<double>(4,5));
    cout<<"Size "<<data.size()<<"X"<< data[0].size()<<endl;
    MatrixXd mv=convert_to_Matrix(data);
    cout<<mv<<" is the matrix"<<endl;
    cout<<" New rotated data \n";
    //cout<<mv*m; //rotate data;

    //Convert matrix to data
    cout<<"...Coverting Matrix to vector"<<endl;
    //vector<vector<double> > v;
    //convert_to_vector(mv,v);
  /*  doubleframe* xdf = new doubleframe();
    xdf->data = new double*[mv.rows()];
    xdf->nrow = mv.rows();
    xdf->ncol = mv.cols();
   */
 doubleframe* xdf = convert_to_df(mv);
 cout<<xdf->nrow<<" by "<<xdf->ncol<<endl;


for(int i=0;i<xdf->nrow;i++)
{
    for(int j=0;j<xdf->ncol ; j++)
        cout<<xdf->data[i][j]<<"\t";
        cout<<"\n";
}

}

void RForest::rForest(){
    //Build the RForest model 
    vector<int> sampleIndex;

    for(int n=0;n<ntree;n++)
          {
            //Get sample datazr
            getSample(sampleIndex,nsample,rSample,dataset->nrow);
            
            MatrixXd rotMat(dataset->ncol,dataset->ncol);
            generate_random_rotation_matrix(rotMat,dataset->ncol);
            //Save rotation matrix
            this->rotMatrices.push_back(rotMat);
            //Save rotation matrix
            MatrixXd rotData =d_convert_to_Matrix(dataset,sampleIndex)*rotMat;
            //add tree to the forest
            doubleframe *sampleDf = convert_to_df(rotData);
            
            //Fill the sampleIndex with indices of the sample
            sampleIndex.clear();
            for(int i=0;i<sampleDf->nrow;i++) sampleIndex.push_back(i);
            Tree *tree  = new Tree();
            tree->iTree(sampleIndex,sampleDf,0,maxheight,stopheight);
            this->trees.push_back(tree);
     
            //delete rotated sample data

            }

            

}
double RForest::instanceScore(double *inst)
{

}

