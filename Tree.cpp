/*
 * Tree.cpp
 *
 *  Created on: Mar 24, 2015
 *      Author: Tadeze
 */

#include "Tree.hpp"

bool Tree::checkRange = false;
bool Tree::useVolumeForScore = false;
std::vector<double> Tree::LB;
std::vector<double> Tree::UB;

void Tree::iTree(std::vector<int> const &dIndex, const doubleframe *dt, int &maxheight){
	this->nodeSize = dIndex.size(); // Set size of the node
	if (dIndex.size() <= 1 || (maxheight > 0 && this->depth > maxheight))
		return;

	double min, max, temp;
	int attCnt = dt->ncol, attribute, rIdx;
	int *attPool = new int[attCnt];
	for(int i = 0; i < attCnt; ++i)
		attPool[i] = i;
	while(attCnt > 0){
		rIdx = randomI(0, attCnt - 1);
		attribute = attPool[rIdx];
		min = max = dt->data[dIndex[0]][attribute];
		for(unsigned int i = 1; i < dIndex.size(); ++i){
			temp = dt->data[dIndex[i]][attribute];
			if (min > temp) min = temp;
			if (max < temp) max = temp;
		}
		if(min < max) break; // found attribute
		--attCnt;
		attPool[rIdx] = attPool[attCnt];
	}
	delete []attPool;
	if(attCnt <= 0) return; // no valid attribute found

	this->minAttVal = min;
	this->maxAttVal = max;
	this->splittingAtt = attribute;
	this->splittingPoint = randomD(min, max);

	//Split the node into two
	std::vector<int> lnodeData, rnodeData;
	for (unsigned int i = 0; i < dIndex.size(); i++) {
		temp = dt->data[dIndex[i]][this->splittingAtt];
		if (temp <= this->splittingPoint && temp != max)
			lnodeData.push_back(dIndex[i]);
		else
			rnodeData.push_back(dIndex[i]);
	}

	leftChild = new Tree();
	leftChild->parent = this;
	leftChild->depth = this->depth + 1;
//	leftChild->volume = this->volume + log(this->splittingPoint - Tree::LB[attribute])
//									 - log(Tree::UB[attribute] - Tree::LB[attribute]);
	leftChild->volume = this->volume + log(this->splittingPoint - min) - log(max - min);
	temp = Tree::UB[attribute];
	Tree::UB[attribute] = this->splittingPoint;
	leftChild->iTree(lnodeData, dt, maxheight);
	Tree::UB[attribute] = temp;

	rightChild = new Tree();
	rightChild->parent = this;
	rightChild->depth = this->depth + 1;
//	rightChild->volume = this->volume + log(Tree::UB[attribute] - this->splittingPoint)
//									  - log(Tree::UB[attribute] - Tree::LB[attribute]);
	rightChild->volume = this->volume + log(max - this->splittingPoint) - log(max - min);
	temp = Tree::LB[attribute];
	Tree::LB[attribute] = this->splittingPoint;
	rightChild->iTree(rnodeData, dt, maxheight);
	Tree::LB[attribute] = temp;
}

double Tree::getScoreAtDepth(double *inst, int depLim){
	double ret = 0;
	Tree *cur = this;
	while(cur->leftChild != NULL || cur->rightChild != NULL){
		if (inst[cur->splittingAtt] <= cur->splittingPoint)
			cur = cur->leftChild;
		else
			cur = cur->rightChild;
		ret = cur->depth + avgPL(cur->nodeSize);
		if(cur->depth == depLim)
			return ret;
	}
	return ret;
}

double Tree::getPatternScoreAtDepth(double *inst, int depLim){
	double ret = 0;
	Tree *cur = this;
	while(cur->leftChild != NULL || cur->rightChild != NULL){
		if (inst[cur->splittingAtt] <= cur->splittingPoint)
			cur = cur->leftChild;
		else
			cur = cur->rightChild;
//		if(cur->nodeSize == 0)
//			std::cout << "error0" << std::endl;
		ret = log(cur->nodeSize) - cur->volume;
		if(cur->depth == depLim)
			return ret;
	}
	return ret;
}


std::vector<double> Tree::getPatternScores(double *inst){
	std::vector<double> ret;
	Tree *cur = this;
	int cnt = 0;
	while(cur->leftChild != NULL || cur->rightChild != NULL){
		if (inst[cur->splittingAtt] <= cur->splittingPoint)
			cur = cur->leftChild;
		else
			cur = cur->rightChild;
//		if(cur->nodeSize <= 0) break;
		ret.push_back(log(cur->nodeSize) - cur->volume);
		++cnt;
		if(cnt >= 10)
			break;
	}
//	if(ret.size() == 0) std::cout << "error";
	while(cnt < 10){
		ret.push_back(log(cur->nodeSize) - cur->volume);
		cnt++;
	}

	return ret;
}

/*
 * takes an instance as vector of double
 */
double Tree::pathLength(double *inst) {
	Tree *cur = this;
	double min, max, instAttVal, temp;
	while(cur->leftChild != NULL || cur->rightChild != NULL){
		if(cur->nodeSize <= 1) break;
		instAttVal = inst[cur->splittingAtt];
		if(Tree::checkRange == true){
			min = cur->minAttVal;
			max = cur->maxAttVal;
			if(instAttVal < min){
				temp = randomD(instAttVal, max);
				if(temp < min){
					if(Tree::useVolumeForScore == true)
						return ((cur->depth + 1) *
							   -(cur->volume + log(temp - instAttVal) - log(max-instAttVal)));
					else
						return (cur->depth + 1);
				}
			}
			if(instAttVal > max){
				temp = randomD(min, instAttVal);
				if(temp > max){
					if(Tree::useVolumeForScore == true)
						return ((cur->depth + 1) *
							   -(cur->volume + log(instAttVal-temp) - log(instAttVal-min)));
					else
						return (cur->depth + 1);
				}
			}
		}

		if (instAttVal <= cur->splittingPoint)
			cur = cur->leftChild;
		else
			cur = cur->rightChild;
	}
	if(Tree::useVolumeForScore == true){
//		if(cur->nodeSize <= 1)
//			return (-cur->nodeSize/cur->volume);
//		return (-1/(cur->volume - log(cur->nodeSize)));
		double d = (cur->depth + avgPL(cur->nodeSize));
		double vol = cur->volume;
		if(cur->nodeSize > 1)
			vol -= log(cur->nodeSize);
		return (d * -vol);
//		return (-cur->nodeSize/cur->volume);
	}
	return (cur->depth + avgPL(cur->nodeSize));
}

// for online IF
void Tree::renewNodeSize(){
	nodeSize = newNodeSize;
	newNodeSize = 1;
	if(leftChild == NULL && rightChild == NULL)
		return;
	leftChild->renewNodeSize();
	rightChild->renewNodeSize();
}

void Tree::update(const double inst[]){
	++newNodeSize;
	if(leftChild == NULL && rightChild == NULL)
		return;
	if(inst[splittingAtt] <= splittingPoint)
		leftChild->update(inst);
	else
		rightChild->update(inst);
}

void Tree::printDepthAndNodeSize(std::ofstream &out){
	for(int i = 0; i < depth; ++i)
		out << "-";
	out << "(" << depth
		<< ", " << nodeSize << ", " << newNodeSize
		<< ", " << exp(volume)
		<< ", " << splittingAtt
		<< ", " << minAttVal
		<< ", " << splittingPoint
		<< ", " << maxAttVal
		<< ")" << std::endl;
	if(leftChild != NULL)
		leftChild->printDepthAndNodeSize(out);
	if(rightChild != NULL)
		rightChild->printDepthAndNodeSize(out);

	//	if(this->leftChild == NULL && this->rightChild == NULL){
	//		out << this->depth << "," << this->nodeSize << std::endl;
	//	}else{
	//		this->leftChild->printDepthAndNodeSize(out);
	//		this->rightChild->printDepthAndNodeSize(out);
	//	}

//	if(this->leftChild == NULL && this->rightChild == NULL){
//		out << this->depth << "," << this->nodeSize << std::endl;
//	}else{
//		this->leftChild->printDepthAndNodeSize(out);
//		this->rightChild->printDepthAndNodeSize(out);
//	}
}

void Tree::printPatternFreq(double inst[], int &tid, int &iid, std::ofstream &out){
	Tree *cur = this;
//	out << (iid+1) << ", "
//		<< (tid+1) << ", "
//		<< cur->depth << ", "
//		<< cur->nodeSize << ", "
//		<< exp(cur->volume) << ", "
//		<< cur->nodeSize/exp(cur->volume) << std::endl;
	while(cur->leftChild != NULL || cur->rightChild != NULL){
		if (inst[cur->splittingAtt] <= cur->splittingPoint)
			cur = cur->leftChild;
		else
			cur = cur->rightChild;
		out << (iid+1) << ", "
			<< (tid+1) << ", "
			<< cur->depth << ", "
			<< cur->nodeSize << ", "
			<< exp(cur->volume) << ", "
			<< log(cur->nodeSize) - cur->volume << std::endl;
	}
}


void Tree::initialezeLBandUB(const doubleframe* _df, std::vector<int> &sampleIndex){
	// initialize LBs and UBs
	Tree::LB.clear();
	Tree::UB.clear();
	double min, max, temp;
	for(int j = 0; j < _df->ncol; ++j){
		temp = _df->data[sampleIndex[0]][j];
		min = max = temp;
		for(unsigned int i = 1; i < sampleIndex.size(); ++i){
			temp = _df->data[sampleIndex[i]][j];
			if(min > temp) min = temp;
			if(max < temp) max = temp;
		}
		Tree::LB.push_back(min);
		Tree::UB.push_back(max);
	}
}
