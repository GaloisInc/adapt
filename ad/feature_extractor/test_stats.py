import unittest, view_stats, os, statistics, numpy

class TestStats(unittest.TestCase):
    def setUp(self):
        self.score_file_path = "/home/vagrant/adapt/ad/test/scores/unittest.csv"
        self.feature_file_path = "/home/vagrant/adapt/ad/test/features/unittest.csv"
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
        lines.append("")  # ensure handle empty lines
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
    # feature ranges
    #
    def test_compute_feature_ranges(self):
        flines = []
        flines.append("id,f1,f2,f3")
        flines.append("001,6,2,12")
        flines.append("002,5,2,10")
        flines.append("003,4,2,11")
        flines.append("004,3,2,9")
        flines.append("005,2,2,9")
        self.create_file(self.feature_file_path, flines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_feature_ranges()
        self.assertEqual('2', stats.feature_mins['f1'])
        self.assertEqual('2', stats.feature_mins['f2'])
        self.assertEqual('9', stats.feature_mins['f3'])
        self.assertEqual('6', stats.feature_maxs['f1'])
        self.assertEqual('2', stats.feature_maxs['f2'])
        self.assertEqual('12', stats.feature_maxs['f3'])
        

    def test_compute_feature_ranges_single_node(self):
        flines = []
        flines.append("id,f1,f2,f3")
        flines.append("001,6,2,12")
        self.create_file(self.feature_file_path, flines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_feature_ranges()
        self.assertEqual('6', stats.feature_mins['f1'])
        self.assertEqual('2', stats.feature_mins['f2'])
        self.assertEqual('12', stats.feature_mins['f3'])
        self.assertEqual('6', stats.feature_maxs['f1'])
        self.assertEqual('2', stats.feature_maxs['f2'])
        self.assertEqual('12', stats.feature_maxs['f3'])
        
    '''  
    # Don't support files that only have header because using reader = csv.DictReader(csvfile)
    # won't have any rows to reference and the lookups will fail
    def test_compute_feature_ranges_no_data(self):
        flines = []
        flines.append("id,f1,f2,f3")
        self.create_file(self.feature_file_path, flines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_feature_ranges()
        self.assertEqual('noData', stats.feature_mins['f1'])
        self.assertEqual('noData', stats.feature_mins['f2'])
        self.assertEqual('noData', stats.feature_mins['f3'])
        self.assertEqual('noData', stats.feature_maxs['f1'])
        self.assertEqual('noData', stats.feature_maxs['f2'])
        self.assertEqual('noData', stats.feature_maxs['f3'])
    '''
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
        
    # formatting helpers
    def test_derive_histogram_ranges(self):
        stats = view_stats.ViewStats('foo','/somepath')
        
        ranges = []
        range_strings = stats.derive_histogram_ranges(ranges)
        self.assertEqual("-noData-", range_strings[0])
        self.assertEqual(1,len(range_strings))
        
        ranges = [0.1]
        range_strings = stats.derive_histogram_ranges(ranges)
        self.assertEqual("0.10 - 0.10", range_strings[0])
        self.assertEqual(1,len(range_strings))
        
        ranges = [0.1, 0.3]
        range_strings = stats.derive_histogram_ranges(ranges)
        self.assertEqual("0.10 - 0.30", range_strings[0])
        self.assertEqual(1,len(range_strings))
        
        ranges = [0.1, 0.3, 0.5]
        range_strings = stats.derive_histogram_ranges(ranges)
        self.assertEqual("0.10 - 0.30", range_strings[0])
        self.assertEqual("0.30 - 0.50", range_strings[1])
        self.assertEqual(2,len(range_strings))
        
        ranges = [0.1, 0.3, 0.5, 0.7]
        range_strings = stats.derive_histogram_ranges(ranges)
        self.assertEqual("0.10 - 0.30", range_strings[0])
        self.assertEqual("0.30 - 0.50", range_strings[1])
        self.assertEqual("0.50 - 0.70", range_strings[2])
        self.assertEqual(3,len(range_strings))
        
        
        ranges = [0.1, 0.3, 0.5, 0.7, 0.9]
        range_strings = stats.derive_histogram_ranges(ranges)
        self.assertEqual("0.10 - 0.30", range_strings[0])
        self.assertEqual("0.30 - 0.50", range_strings[1])
        self.assertEqual("0.50 - 0.70", range_strings[2])
        self.assertEqual("0.70 - 0.90", range_strings[3])
        self.assertEqual(4,len(range_strings))
        
    def test_get_range_widths(self):
        stats = view_stats.ViewStats('foo','/somepath')
        r = ["0.1-0.2", "0.2-0.33"]
        w = stats.get_range_widths(r)
        self.assertEqual(7, w[0])
        self.assertEqual(8, w[1])
        
    def test_get_dash_bar(self):
        stats = view_stats.ViewStats('foo','/somepath')
        widths= [6]
        db = stats.get_dash_bar(widths)
        self.assertEqual("----------", db) # | xxxxxx |  (10 chars wide)
        widths= [6,4]
        db = stats.get_dash_bar(widths)
        self.assertEqual("-----------------", db) # | xxxxxx | xxxx |  (17 chars wide)
        widths= [6,4,2]
        db = stats.get_dash_bar(widths)
        self.assertEqual("----------------------",db) # | xxxxxx | xxxx | xx |  (22 chars wide)
    
    def test_get_range_header(self):
        stats = view_stats.ViewStats('foo','/somepath')   
        r = ["0.1-0.2"]
        h = stats.get_range_header(r)
        self.assertEqual("| 0.1-0.2 |",h)
        r = ["0.1-0.2", "0.2-0.33"]
        h = stats.get_range_header(r)
        self.assertEqual("| 0.1-0.2 | 0.2-0.33 |",h)
        
    def test_round_the_bounds(self):
        stats = view_stats.ViewStats('foo','/somepath')
        self.assertEqual(['0.00'],stats.round_the_bounds([0]))
        
    def test_get_histogram_values_string(self):
        stats = view_stats.ViewStats('foo','/somepath') 
        range_widths = [7]
        values = [5]
        #import pdb; pdb.set_trace()
        s = stats.get_histogram_values_string(values,range_widths)
        self.assertEqual("|       5 |",s)
        
        range_widths = [7, 7]
        values = [5,7]
        s = stats.get_histogram_values_string(values,range_widths)
        self.assertEqual("|       5 |       7 |",s)
        
        range_widths = [7, 7]
        values = [5555555,7]
        s = stats.get_histogram_values_string(values,range_widths)
        self.assertEqual("| 5555555 |       7 |",s)
        
        range_widths = [7, 7]
        values = [55555555,7]
        s = stats.get_histogram_values_string(values,range_widths)
        self.assertEqual("| 55555555|       7 |",s)
        
        range_widths = [7, 11]
        values = [55555555,7]
        s = stats.get_histogram_values_string(values,range_widths)
        self.assertEqual("| 55555555|           7 |",s)
    
    def test_get_histogram_range_portions(self):
        stats = view_stats.ViewStats('foo','/somepath') 
        # case : 1 wide for 2 ranges
        histogram_bins_per_output_line = 1
        histogram_ranges = [ "1-2","2-3"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(2,len(range_fragments))
        self.assertEqual(1,len(range_fragments[0]))
        self.assertEqual(1,len(range_fragments[1]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[1][0])
        
        # case : 2 wide for 3 ranges
        histogram_bins_per_output_line = 2
        histogram_ranges = [ "1-2","2-3", "3-4"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(2,len(range_fragments))
        self.assertEqual(2,len(range_fragments[0]))
        self.assertEqual(1,len(range_fragments[1]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[0][1])
        self.assertEqual("3-4", range_fragments[1][0])
        
        # case : 2 wide for 4 ranges
        histogram_bins_per_output_line = 2
        histogram_ranges = [ "1-2","2-3", "3-4","4-5"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(2,len(range_fragments))
        self.assertEqual(2,len(range_fragments[0]))
        self.assertEqual(2,len(range_fragments[1]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[0][1])
        self.assertEqual("3-4", range_fragments[1][0])
        self.assertEqual("4-5", range_fragments[1][1])
        
        # case : 2 wide for 5 ranges
        histogram_bins_per_output_line = 2
        histogram_ranges = [ "1-2","2-3", "3-4","4-5","5-6"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(3,len(range_fragments))
        self.assertEqual(2,len(range_fragments[0]))
        self.assertEqual(2,len(range_fragments[1]))
        self.assertEqual(1,len(range_fragments[2]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[0][1])
        self.assertEqual("3-4", range_fragments[1][0])
        self.assertEqual("4-5", range_fragments[1][1])
        self.assertEqual("5-6", range_fragments[2][0])
        
        # case : 3 wide for 7 ranges
        histogram_bins_per_output_line = 3
        histogram_ranges = [ "1-2","2-3","3-4","4-5","5-6","6-7","7-8"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(3,len(range_fragments))
        self.assertEqual(3,len(range_fragments[0]))
        self.assertEqual(3,len(range_fragments[1]))
        self.assertEqual(1,len(range_fragments[2]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[0][1])
        self.assertEqual("3-4", range_fragments[0][2])
        self.assertEqual("4-5", range_fragments[1][0])
        self.assertEqual("5-6", range_fragments[1][1])
        self.assertEqual("6-7", range_fragments[1][2])
        self.assertEqual("7-8", range_fragments[2][0])
        
        # case : 5 wide for 10 ranges
        histogram_bins_per_output_line = 5
        histogram_ranges = [ "1-2","2-3","3-4","4-5","5-6","6-7","7-8","8-9","9-10","10-11"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(2,len(range_fragments))
        self.assertEqual(5,len(range_fragments[0]))
        self.assertEqual(5,len(range_fragments[1]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[0][1])
        self.assertEqual("3-4", range_fragments[0][2])
        self.assertEqual("4-5", range_fragments[0][3])
        self.assertEqual("5-6", range_fragments[0][4])
        self.assertEqual("6-7", range_fragments[1][0])
        self.assertEqual("7-8", range_fragments[1][1])
        self.assertEqual("8-9", range_fragments[1][2])
        self.assertEqual("9-10", range_fragments[1][3])
        self.assertEqual("10-11", range_fragments[1][4])
        
        # case : 5 wide for 11 ranges
        histogram_bins_per_output_line = 5
        histogram_ranges = [ "1-2","2-3","3-4","4-5","5-6","6-7","7-8","8-9","9-10","10-11","11-12"]
        range_fragments = stats.get_histogram_portions(histogram_ranges, histogram_bins_per_output_line)
        self.assertEqual(3,len(range_fragments))
        self.assertEqual(5,len(range_fragments[0]))
        self.assertEqual(5,len(range_fragments[1]))
        self.assertEqual(1,len(range_fragments[2]))
        self.assertEqual("1-2", range_fragments[0][0])
        self.assertEqual("2-3", range_fragments[0][1])
        self.assertEqual("3-4", range_fragments[0][2])
        self.assertEqual("4-5", range_fragments[0][3])
        self.assertEqual("5-6", range_fragments[0][4])
        self.assertEqual("6-7", range_fragments[1][0])
        self.assertEqual("7-8", range_fragments[1][1])
        self.assertEqual("8-9", range_fragments[1][2])
        self.assertEqual("9-10", range_fragments[1][3])
        self.assertEqual("10-11", range_fragments[1][4])
        self.assertEqual("11-12", range_fragments[2][0])
        
       
    '''    
    def test_full_format(self):
        slines = []
        slines.append("id,f1,f2,f3,anomaly_score")
        slines.append("001,6,2,12,0.9")
        slines.append("002,5,2,10,0.7")
        slines.append("003,4,2,11,0.5")
        slines.append("004,3,2,9,0.3")
        slines.append("005,2,2,9,0.1")
        flines = []
        flines.append("id,f1,f2,f3")
        flines.append("001,6,2,12")
        flines.append("002,5,2,10")
        flines.append("003,4,2,11")
        flines.append("004,3,2,9")
        flines.append("005,2,2,9")
        self.create_file(self.score_file_path, slines)
        self.create_file(self.feature_file_path, flines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_all_stats()
        print("{0}\n".format(stats.get_stats_info_formatted()))
    '''  
    
    def test_full_format2(self):
        slines = []
        slines.append("id,feature1,feature2,feature3,anomaly_score")
        slines.append("001,6,2,12,0.1")
        slines.append("002,5,2,10,0.2")
        slines.append("003,4,2,11,0.3")
        slines.append("004,3,2,9,0.4")
        slines.append("005,2,2,9,0.5")
        slines.append("006,2,2,9,0.6")
        slines.append("007,2,2,9,0.7")
        slines.append("008,2,2,9,0.8")
        slines.append("009,2,2,9,0.9")
        slines.append("010,2,2,9,0.1")
        slines.append("011,2,2,9,0.2")
        slines.append("012,2,2,9,0.3")
        flines = []
        flines.append("id,feature1,feature2,feature3")
        flines.append("001,6,2,12")
        flines.append("002,5,2,10")
        flines.append("003,4,2,11")
        flines.append("004,3,2,9")
        flines.append("005,2,2,9")
        flines.append("006,2,2,9")
        flines.append("007,2,2,9")
        flines.append("008,2,2,9")
        flines.append("009,2,2,9")
        flines.append("010,2,2,9")
        flines.append("011,2,2,9")
        flines.append("012,2,2,9")
        self.create_file(self.score_file_path, slines)
        self.create_file(self.feature_file_path, flines)
        stats = view_stats.ViewStats(self.view_name,self.root)
        stats.compute_all_stats()
        print("{0}\n".format(stats.get_stats_info_formatted()))
      
    def test_get_proper_length_column(self):
        stats = view_stats.ViewStats('foo','/somepath') 
        c = stats.get_proper_length_column('Features', 6)
        self.assertEqual('Features',c)
        c = stats.get_proper_length_column('Features', 8)
        self.assertEqual('Features',c)
        c = stats.get_proper_length_column('Features', 9)
        self.assertEqual('Features ',c)
        c = stats.get_proper_length_column('Features', 10)
        self.assertEqual('Features  ',c)
if __name__ == '__main__':
    unittest.main()