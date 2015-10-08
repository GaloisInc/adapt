/*
 * IsolationForest.cpp
 *
 *  Created on: Mar, 2015
 *      Author: Tadeze
 */

#include "IsolationForest.hpp"
IsolationForest::IsolationForest(int _ntree, doubleframe* _df, int _nsample,
		int _maxheight, bool _rsample) :
		Forest(_ntree, _nsample, _maxheight, _rsample) {

	std::vector<int> sampleIndex;

	for (int n = 0; n < ntree; n++) {

		//Sample and shuffle the data.
		sampleIndex.clear();
		getSample(sampleIndex, nsample, rsample, _df->nrow);

		Tree::initialezeLBandUB(_df, sampleIndex);

		//build tree
		Tree *tree = new Tree();
		tree->iTree(sampleIndex, _df, maxheight);
		this->trees.push_back(tree); //add tree to forest
	}
}
