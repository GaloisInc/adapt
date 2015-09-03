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

    std::mt19937 eng{std::random_device{}()}; //For production
    //std::default_random_engine eng(seed); //for debugging
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

