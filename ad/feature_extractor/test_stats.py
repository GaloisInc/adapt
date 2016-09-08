import unittest, view_stats

class TestStats(unittest.TestCase):

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
        
    def test_feature_mean(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.compute_feature_means()
        f1_mean = stats.feature_means['f1']
        f2_mean = stats.feature_means['f2']
        f3_mean = stats.feature_means['f3']
        self.assertEqual(f1_mean, '4.50')
        self.assertEqual(f2_mean, '2.00')
        self.assertEqual(f3_mean, '5.00')
        
    def test_feature_stddev(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.compute_feature_stdevs()
        f1_stdev = stats.feature_stdevs['f1']
        f2_stdev = stats.feature_stdevs['f2']
        f3_stdev = stats.feature_stdevs['f3']
        self.assertEqual(f1_stdev, '2.45')
        self.assertEqual(f2_stdev, '0.00')
        self.assertEqual(f3_stdev, '2.14')

    def test_feature_variance(self):
        stats = view_stats.ViewStats('statsTest','/home/vagrant/adapt/ad/test')
        stats.compute_feature_variances()
        f1_variance = stats.feature_variances['f1']
        f2_variance = stats.feature_variances['f2']
        f3_variance = stats.feature_variances['f3']
        self.assertEqual(f1_variance, '6.00')
        self.assertEqual(f2_variance, '0.00')
        self.assertEqual(f3_variance, '4.57')
        
    def test_feature_mean_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_means()
        f1_mean = stats.feature_means['f1']
        f2_mean = stats.feature_means['f2']
        f3_mean = stats.feature_means['f3']
        self.assertEqual(f1_mean, '1.00')
        self.assertEqual(f2_mean, '2.00')
        self.assertEqual(f3_mean, '3.00')
        
    def test_feature_stddev_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_stdevs()
        f1_stdev = stats.feature_stdevs['f1']
        f2_stdev = stats.feature_stdevs['f2']
        f3_stdev = stats.feature_stdevs['f3']
        self.assertEqual(f1_stdev, '0.00')
        self.assertEqual(f2_stdev, '0.00')
        self.assertEqual(f3_stdev, '0.00')

    def test_feature_variance_single_node(self):
        stats = view_stats.ViewStats('statsTestSingleNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_variances()
        f1_variance = stats.feature_variances['f1']
        f2_variance = stats.feature_variances['f2']
        f3_variance = stats.feature_variances['f3']
        self.assertEqual(f1_variance, '0.00')
        self.assertEqual(f2_variance, '0.00')
        self.assertEqual(f3_variance, '0.00')
        
        
    def test_feature_mean_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_means()
        self.assertEqual({}, stats.feature_means)
        
    def test_feature_stddev_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_stdevs()
        self.assertEqual({}, stats.feature_stdevs)

    def test_feature_variance_zero_node(self):
        stats = view_stats.ViewStats('statsTestZeroNode','/home/vagrant/adapt/ad/test')
        stats.compute_feature_variances()
        self.assertEqual({}, stats.feature_variances)
        
if __name__ == '__main__':
    unittest.main()