#include "RForest.hpp"
//using namesapce Eigen;
/*
 * Class for RandomRotation Matrix
 * TODO: convert from data to Matrix class of eigen ;
 */
using namespace Eigen;

/*Take Matrix and return doubleframe
 */
doubleframe* RForest::convert_to_df(MatrixXd &m){
    doubleframe* df = new doubleframe();
    df->data = new double*[m.rows()];
    df->nrow = m.rows();
    df->ncol = m.cols();
   
    for (int i=0; i<m.rows(); ++i)
    {
     df->data[i] =&m.row(i).data()[0];//   &m(i,j);//  i.row(i).data()[j];
    }
return df;
}

/* Rotate data instance with rotation matrix
 */
double* RForest::rotateInstance(double* inst,MatrixXd &m)
{

double* rotdata	= new double[m.cols()];
int ncol = m.cols();
MatrixXd matData = Map<MatrixXd>(inst,1,ncol);
MatrixXd rotMat = matData*m;
Map<MatrixXd>(rotdata,rotMat.rows(),rotMat.cols()) = rotMat;
return rotdata;
}


/*
 * Takes matrix and empty vector data
 * Fill the vector-2d with the matrix value
 */
void RForest::convert_to_vector(MatrixXd &m, std::vector<std::vector<double> > &v){
    for (int i=0; i<m.rows(); ++i)
    {
        const double* begin = &m.row(i).data()[0];
        v.push_back(std::vector<double>(begin, begin+m.cols()));
    }
}

/*
 * Takes dataset of vector<vector<double> and return Matrix of data
 */
MatrixXd RForest::convert_to_Matrix(std::vector<std::vector<double> > &data) {
	MatrixXd mat(data.size(), data[0].size());
	for (int i = 0; i <(int)data.size(); i++)
	      mat.row(i) =VectorXd::Map(&data[i][0],(int)data[i].size());
	return mat;
    }

/* Convert struct data to Matrix
 *
 */

MatrixXd RForest::d_convert_to_Matrix(const doubleframe* data,
        std::vector<int> &sampleIndex){
	MatrixXd mat((int)sampleIndex.size(), data->ncol);
	for (int i = 0; i <(int)sampleIndex.size(); i++)
	  mat.row(i) =VectorXd::Map(&data->data[sampleIndex[i]][0],data->ncol);
	return mat;
}

/*
 * Rotation matrix generator
 * @input Matrix empty matrix
 * @input n size of matrix //will remove later and seed as well need to remove it
 */
void RForest::generate_random_rotation_matrix(MatrixXd& M,int n,int seed)
{

    std::mt19937 eng{std::random_device{}()};
    std::default_random_engine generator(time(0)+seed);
	std::normal_distribution<double> distribution(0.0, 1.0);
	MatrixXd A(n, n);
	const VectorXd ones(VectorXd::Ones(n));
	for (int i = 0; i<n; i++)
	{
		for (int j = 0; j<n; j++)
		{
			A(i, j) = distribution(eng); //generator);  //mtrand.randNorm(0, 1);

		}
	}
	const HouseholderQR<MatrixXd> qr(A);
	const MatrixXd Q = qr.householderQ();
	M = Q*(qr.matrixQR().diagonal().array() <0).select(-ones, ones).asDiagonal();
	if (M.determinant()<0)
	for (int i = 0; i<n; i++)
		M(i, 0) = -M(i, 0);

}


MatrixXd  RForest::rotateData(doubleframe* dt, MatrixXd& M){ 
    std::vector<int> sampleIndex={5,6,8,3,2,9};
    // getSample(sampleIndex,10,true);
    MatrixXd  mData = d_convert_to_Matrix(dt,sampleIndex);
    return mData*M;
}

/*
void RForest::buildForest(doubleframe* df){
    int n=df->ncol;
    //double dt[8] = { 1,2,3,4,5,6,7,8};
    MatrixXd m(n,n);  //rotation matrix
    generate_random_rotation_matrix(m,n,4);
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
   doubleframe* xdf = new doubleframe();
    xdf->data = new double*[mv.rows()];
    xdf->nrow = mv.rows();
    xdf->ncol = mv.cols();
  
 doubleframe* xdf = convert_to_df(mv);
 cout<<xdf->nrow<<" by "<<xdf->ncol<<endl;


for(int i=0;i<xdf->nrow;i++)
{
    for(int j=0;j<xdf->ncol ; j++)
        cout<<xdf->data[i][j]<<"\t";
        cout<<"\n";
}

double *vec =df->data[2];


double* rotvec = rotateInstance(vec,m);
cout<<"Finally rotated data\n";
for(int i=0;i<4;i++) cout<<rotvec[i]<<"\t";
cout<<"\nLet's do reall thing----------\n";
//dataset->data = xdf;
//dataset->ncol=4;
//dataset->nrow = 10;
cout<<" build forest\n";



}
*/
void RForest::rForest(){
    //Build the RForest model 
    std::vector<int> sampleIndex;
//cout<<"Random rotation matrix \n";
    for(int n=0;n<ntree;n++)
          {
            //Get sample datazr
            getSample(sampleIndex,nsample,rSample,dataset->nrow);
            
            MatrixXd rotMat(dataset->ncol,dataset->ncol);
            generate_random_rotation_matrix(rotMat,dataset->ncol,n);
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
     

            }
}
/*
 * overrides method of pathLength for rotated data
 */
std::vector<double> RForest::pathLength(double *inst)
{
    std::vector<double> depth;
    int i=0;
    MatrixXd rotmat;
    double* transInst=new double[rotmat.cols()];//NULL;
    for(std::vector<Tree*>::iterator it=this->trees.begin();it!=trees.end();it++)
    {
    	transInst=rotateInstance(inst,rotMatrices[i]);
        double _depth = (*it)->pathLength(transInst);
    	depth.push_back(_depth);
        i++;

    }
    delete[] transInst;
   // transInst=NULL;
    //delete transInst;
    return depth;
}

