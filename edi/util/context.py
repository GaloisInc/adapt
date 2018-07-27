import numpy

# No longer used



# This class reads csv data that is of the following form:
# header row: A0,A1,...,An
# data row: UUID, V1, ..., Vn
# Attribute A0 is ignored.  A1...An are taken to be the attribute names.
# Each row starts with an object identifier and a series of values.
# The values are assumed to be 0/1 only.
# This data is represented internally as a dictionary mapping UUIDs to
# dictionaries mapping keys (A1...An) to their 0/1 values.


class Context:

	def __init__(self,reader):
		self.header = next(reader)[1:]
		self.data = {row[0]:{h : v
							 for (h,v) in zip(self.header,map(lambda x: int(x),row[1:]))}
						for row in reader}

	def addDicts(self,x,y):
		return {k : x[k] + y[k] for k in self.header}

	def sumDicts(self):
		sum = {x: 0 for x in self.header}
		for x in self.data.keys():
			sum = self.addDicts(sum,self.data[x])
		return sum

	def probabilityMatrix(self):
		sums = self.sumDicts()
		n = len(self.data)
		return {k : (1-(sums[k]/n), (sums[k]/n)) for k in self.header}

	def entropyMatrix(self):
		probabilities = self.probabilityMatrix()
		return {k : (0-numpy.log2(probabilities[k][0]),
					 0-numpy.log2(probabilities[k][1]))
					 for k in self.header}

	# same as sntropy matrix but we ignore cost of encoding 0's
	# instead we assume a fixed cost to say "no more 1's"
	# and ignore the (fixed) cost of coding for probability across entire dataset
	# Arguably this is a hack, but it seems to match what Krimp would do without
	# itemset mining.
	def shrimpMatrix(self):
		probabilities = self.probabilityMatrix()
		return {k : (0,
					 0-numpy.log2(probabilities[k][1]))
					 for k in self.header}

	def zimpMatrix(self):
		probabilities = self.probabilityMatrix()
		return {k : (0-numpy.log2(probabilities[k][0]),
					 0)
					 for k in self.header}
