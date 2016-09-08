import unittest, view_stats, os, statistics, numpy

class TestStats(unittest.TestCase):
    def setUp(self):
        self.score_file_path = "/home/vagrant/adapt/ad/test/scores/unittest.csv"
        self.view_name = "unittest"
        self.root = "/home/vagrant/adapt/ad/test"
    
    def create_file(self, path, lines):
        if os.path.isfile(path):
            os.remove(path)
        f = open(path,'w')
        for line in lines:
            f.write("{0}\n".format(line))
        f.close()
    #
    #  histogram for scores
    #
    def test_score_histogram(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("002,1,0,10,0.7")
        lines.append("003,1,0,11,0.5")
        #lines.append("")  # ensure handle empty lines
        lines.append("004,1,0,9,0.3")
        lines.append("005,1,0,9,0.1")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_histogram()
        #print("\nscore histogram : {0}".format(stats.histogram_for_scores))
        #print("\ncounts {0}".format(stats.histogram_for_scores[0]))
        #print("\nranges {0}".format(stats.histogram_for_scores[1]))
        counts = [2,0,2,1]
        ranges = [0.1, 0.3, 0.5, 0.7, 0.9]
        self.assertTrue(len(stats.histogram_for_scores[0]) > 0)
        self.assertTrue(len(stats.histogram_for_scores[1]) > 0)
        #d = [1,3,5,7,9]
        #print("hist for {0} is {1}\n\n".format(d, numpy.histogram(d,'auto', None, False, None, None)))
        #d = [0.1,0.3,0.5,0.7,0.9]
        #print("hist for {0} is {1}\n\n".format(d, numpy.histogram(d,'auto', None, False, None, None)))
    #
    # mean score
    #
    def test_score_mean(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("002,1,0,10,0.7")
        lines.append("003,1,0,11,0.5")
        lines.append("")  # ensure handle empty lines
        lines.append("004,1,0,9,0.3")
        lines.append("005,1,0,9,0.1")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_mean()
        self.assertEqual('0.50', stats.score_mean)
        
    def test_score_mean_single_node(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("")  # ensure handle empty lines
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_mean()
        self.assertEqual('0.90', stats.score_mean)
        
    def test_score_mean_zero_node(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_mean()
        self.assertEqual('noData', stats.score_mean)
        
    def test_score_mean_empty_file(self):
        lines = []
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_mean()
        self.assertEqual('noData', stats.score_mean)
    #
    # score variance
    #
    def test_score_variance(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("002,1,0,10,0.7")
        lines.append("003,1,0,11,0.5")
        lines.append("")  # ensure handle empty lines
        lines.append("004,1,0,9,0.3")
        lines.append("005,1,0,9,0.1")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_variance()
        self.assertEqual('0.10', stats.score_variance)
        
    def test_score_variance_single_node(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("")  # ensure handle empty lines
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_variance()
        self.assertEqual('0.00', stats.score_variance)
        
        
    def test_score_variance_same_score_nodes(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("002,0,0,6,0.9")
        lines.append("")  # ensure handle empty lines
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_variance()
        self.assertEqual('0.00', stats.score_variance)
        
    def test_score_variance_zero_node(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_variance()
        self.assertEqual('noData', stats.score_variance)
        
    def test_score_variance_empty_file(self):
        lines = []
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_variance()
        self.assertEqual('noData', stats.score_variance)    
        
    #
    # score stddev
    #
    def test_score_stdev(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("002,1,0,10,0.7")
        lines.append("003,1,0,11,0.5")
        lines.append("")  # ensure handle empty lines
        lines.append("004,1,0,9,0.3")
        lines.append("005,1,0,9,0.1")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        #import pdb; pdb.set_trace()
        stats.compute_score_stdev()
        self.assertEqual('0.32', stats.score_stdev)
        
    def test_score_stdev_single_node(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("")  # ensure handle empty lines
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_stdev()
        self.assertEqual('0.00', stats.score_stdev)
        
        
    def test_score_stdev_same_score_nodes(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        lines.append("001,1,0,12,0.9")
        lines.append("002,0,0,6,0.9")
        lines.append("")  # ensure handle empty lines
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_stdev()
        self.assertEqual('0.00', stats.score_stdev)
        
    def test_score_stdev_zero_node(self):
        lines = []
        lines.append("id,f1,f2,f3,anomaly_score")
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_stdev()
        self.assertEqual('noData', stats.score_stdev)
        
    def test_score_stdev_empty_file(self):
        lines = []
        self.create_file(self.score_file_path, lines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_score_stdev()
        self.assertEqual('noData', stats.score_stdev)     
    #
    # score range
    #   
    def test_score_range(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.set_score_range()
        self.assertEqual('0.10', stats.score_range_min)
        self.assertEqual('0.90', stats.score_range_max)
        
    def test_score_range_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.set_score_range()
        self.assertEqual('0.90', stats.score_range_min)
        self.assertEqual('0.90', stats.score_range_max)
        
    def test_score_range_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.set_score_range()
        self.assertEqual('noData', stats.score_range_min)
        self.assertEqual('noData', stats.score_range_max)
        
    #
    # mean value for feature
    #
    def test_feature_mean(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.compute_feature_means()
        f1_mean = stats.feature_means['f1']
        f2_mean = stats.feature_means['f2']
        f3_mean = stats.feature_means['f3']
        self.assertEqual(f1_mean, '4.50')
        self.assertEqual(f2_mean, '2.00')
        self.assertEqual(f3_mean, '5.00')
        
    def test_feature_mean_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_means()
        f1_mean = stats.feature_means['f1']
        f2_mean = stats.feature_means['f2']
        f3_mean = stats.feature_means['f3']
        self.assertEqual(f1_mean, '1.00')
        self.assertEqual(f2_mean, '2.00')
        self.assertEqual(f3_mean, '3.00')  
          
    def test_feature_mean_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_means()
        self.assertEqual({}, stats.feature_means)
    
    #
    # stdev feature values
    #
    def test_feature_stddev(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.compute_feature_stdevs()
        f1_stdev = stats.feature_stdevs['f1']
        f2_stdev = stats.feature_stdevs['f2']
        f3_stdev = stats.feature_stdevs['f3']
        self.assertEqual(f1_stdev, '2.45')
        self.assertEqual(f2_stdev, '0.00')
        self.assertEqual(f3_stdev, '2.14')
        
    def test_feature_stddev_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_stdevs()
        f1_stdev = stats.feature_stdevs['f1']
        f2_stdev = stats.feature_stdevs['f2']
        f3_stdev = stats.feature_stdevs['f3']
        self.assertEqual(f1_stdev, '0.00')
        self.assertEqual(f2_stdev, '0.00')
        self.assertEqual(f3_stdev, '0.00')
 
    def test_feature_stddev_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_stdevs()
        self.assertEqual({}, stats.feature_stdevs)
        
    #
    # feature value variance
    #
    def test_feature_variance(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.compute_feature_variances()
        f1_variance = stats.feature_variances['f1']
        f2_variance = stats.feature_variances['f2']
        f3_variance = stats.feature_variances['f3']
        self.assertEqual(f1_variance, '6.00')
        self.assertEqual(f2_variance, '0.00')
        self.assertEqual(f3_variance, '4.57')
        


    def test_feature_variance_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_variances()
        f1_variance = stats.feature_variances['f1']
        f2_variance = stats.feature_variances['f2']
        f3_variance = stats.feature_variances['f3']
        self.assertEqual(f1_variance, '0.00')
        self.assertEqual(f2_variance, '0.00')
        self.assertEqual(f3_variance, '0.00')
        
    

    def test_feature_variance_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_variances()
        self.assertEqual({}, stats.feature_variances)
        
if __name__ == '__main__':
    unittest.main()