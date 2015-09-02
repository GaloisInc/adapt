#include "RForest.hpp"
/*
 * Class for RandomRotation Matrix
 *
 */
using namespace Eigen;

/*Take Matrix and return doubleframe
 */
void  RForest::convertToDf(MatrixXd &m,doubleframe* df){

   
    for (int i=0; i<m.rows(); ++i)
    {
     df->data[i] =&m.row(i).data()[0];//   &m(i,j);//  i.row(i).data()[j];
    }
}

/* Rotate data instance with rotation matrix
 */
void RForest::rotateInstance(double* inst,MatrixXd &m,double* rotData)
{

int ncol = m.cols();
MatrixXd matData = Map<MatrixXd>(inst,1,ncol);
MatrixXd rotMat = matData*m;
Map<MatrixXd>(rotData,rotMat.rows(),rotMat.cols()) = rotMat;

}


/*
 * Takes matrix and empty vector data
 * Fill the vector-2d with the matrix value
 */
void RForest::convertToVector(MatrixXd &m, std::vector<std::vector<double> > &v){
    for (int i=0; i<m.rows(); ++i)
    {
        const double* begin = &m.row(i).data()[0];
        v.push_back(std::vector<double>(begin, begin+m.cols()));
    }
}

/*
 * Takes dataset of vector<vector<double> and return Matrix of data
 */
MatrixXd RForest::convertToMatrix(std::vector<std::vector<double> > &data) {
	MatrixXd mat(data.size(), data[0].size());
	for (int i = 0; i <(int)data.size(); i++)
	      mat.row(i) =VectorXd::Map(&data[i][0],(int)data[i].size());
	return mat;
    }

/* Convert struct data to Matrix
 *
 */

MatrixXd RForest::convertDfToMatrix(const doubleframe* data,
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
void RForest::generateRandomRotationMatrix(MatrixXd& M,int n,int seed)
{

    //std::mt19937 eng{std::random_device{}()}; //For production
    std::default_random_engine eng(seed); //for debugging
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
    MatrixXd  mData = convertDfToMatrix(dt,sampleIndex);
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
    std::vector<int> sampleIndex(this->nsample);
    doubleframe* sampleDf = new doubleframe();
    sampleDf->data = new double*[this->nsample];
    //logfile<<"point,tree,x1,x2\n";
    for(int n=0;n<ntree;n++)
          {
            //zret sample datazr
            getSample(sampleIndex,nsample,rsample,dataset->nrow);
            
            MatrixXd rotMat(dataset->ncol,dataset->ncol);
            generateRandomRotationMatrix(rotMat,dataset->ncol,n+1);
            //Save rotation matrix
            this->rotMatrices.push_back(rotMat);
        //    logfile<<rotMat<<"\n";
            //Rotate data and convert to doubleframe format
            MatrixXd rotData =convertDfToMatrix(dataset,sampleIndex)*rotMat;
            sampleDf->nrow = this->nsample;
            sampleDf->ncol = rotMat.cols();
            convertToDf(rotData,sampleDf);
            
            //Fill the sampleIndex with indices of the sample rotated data
            sampleIndex.clear();
            for(int i=0;i<sampleDf->nrow;i++) sampleIndex.push_back(i);
            Tree *tree  = new Tree();
            tree->iTree(sampleIndex,sampleDf,0,maxheight,stopheight);
            this->trees.push_back(tree);

            }
    delete[] sampleDf->data;
    delete sampleDf;
}
/*
 * overrides method of pathLength for rotated data
 */
std::vector<double> RForest::pathLength(double *inst)
{
    std::vector<double> depth;
    int i=0;
   // pnt++;
    //MatrixXd rotmat;
    double* transInst=new double[dataset->ncol];//NULL;
    for(std::vector<Tree*>::iterator it=this->trees.begin();it!=trees.end();it++)
    {
    	rotateInstance(inst,this->rotMatrices[i],transInst);
        double _depth = (*it)->pathLength(transInst);
    	depth.push_back(_depth);
        i++;
   //     logfile<<pnt<<","<<i<<","<<*(transInst)<<","<<*(transInst+1)<<"\n";
 /*for(int i=0;i<dataset->ncol;i++) std::cout<<transInst[i]<<"\t";
 std::cout<<"\n------------Tree---------"<<i<<std::endl;
 */   }

    delete transInst;
    //delete transInst;
    return depth;
}

