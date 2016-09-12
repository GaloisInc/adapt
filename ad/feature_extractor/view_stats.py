import sys, csv, math, statistics, numpy

class ViewStats:
    def __init__(self, vt, run_time_dir):
        self.view_type = vt
        #view issues
        self.number_nodes = 0
        self.number_nodes_attached = 0
        #feature issues
        self.feature_file_path = run_time_dir + '/features/' + self.view_type + '.csv'
        self.value_list_for_features = {}
        self.histograms_for_features = {}
        self.features = []
        self.feature_mins = {}
        self.feature_maxs = {}
        self.feature_means = {}
        self.feature_stdevs = {}
        self.feature_variances = {}
        #score issues
        self.scores_file_path  = run_time_dir + '/scores/'   + self.view_type + '.csv'
        self.score_range_min = -1
        self.score_range_max = -1
        self.score_mean = '?'
        self.score_stdev = '?'
        self.score_variance = '?'
        self.scores = []
        self.histogram_for_scores = {}

    def compute_all_stats(self):
        self.compute_feature_ranges()
        self.compute_feature_means()
        self.compute_feature_variances()
        self.compute_feature_stdevs()
        self.compute_feature_histograms()
        self.set_score_range()
        self.compute_score_mean()
        self.compute_score_variance()
        self.compute_score_stdev()
        self.compute_score_histogram()
        
    def nonblank_lines(self,lines):
        nonblanks = []
        for l in lines:
            if l == '\n':
                pass
            else:
                nonblanks.append(l)
        return nonblanks
                

     
    def load_scores(self):
        with open(self.scores_file_path, 'r') as csvfile:
            self.scores = []
            reader = csv.DictReader(csvfile)
            for row in reader:
                score = float(row['anomaly_score'])
                self.scores.append(score)
            self.scores.sort()

    def load_features(self):
        #print('setting value list for features...')
        with open(self.feature_file_path, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                features = row.keys()
                #print(features)
                #print(row)
                for feature in features:
                    if feature != 'id':
                        if not(feature in self.value_list_for_features):
                            self.value_list_for_features[feature] = []
                        list_for_feature = self.value_list_for_features[feature]
                        list_for_feature.append(float(row[feature]))
            self.features = list(self.value_list_for_features.keys())
            self.features.sort()
            #print('values for features {0}'.format(self.value_list_for_features))
            
    def get_stats_info(self):
        INFO="###########################################\nstatistics for view "+self.view_type+'\n'
        INFO+="###########################################\n"
        INFO+="# nodes         " + "{0}".format(self.number_nodes) + '\n'
        INFO+="# nodes attached" + "{0}".format(self.number_nodes_attached) + '\n'
        INFO+="features: " 
        #INFO+=', '.join(self.features) + '\n'
        for f in self.features:
            INFO+="\n\t" + f + "\tmean " + self.feature_means[f] + "\tstdev " + self.feature_stdevs[f]
        INFO+="\n\nFEATURE HISTOGRAMS"
        for f in self.features:
            INFO+="\n\t" + f + "\t:  " + "{0}".format(self.histograms_for_features[f])
        INFO+="\n\nscore range - min {0} , max {1}\n".format(self.score_range_min, self.score_range_max)
        INFO+="\n"
        return INFO
        
    def derive_histogram_ranges(self, range_list):
        range_strings = []
        if len(range_list) == 0:
            range_strings.append("-noData-")
        elif len(range_list) == 1:
            range_strings.append("{0:.2f} - {0:.2f}".format(float(range_list[0])))
        else:
            for i in range(len(range_list) - 1):
                range_strings.append("{0:.2f} - {1:.2f}".format(float(range_list[i]), float(range_list[i+1])))
        return range_strings
            
    def get_range_widths(self, histogram_ranges):
        w = []
        for hr in histogram_ranges:
            w.append(len(hr))
        return w
    
    def get_dash_bar(self, range_widths):
        dash = ""
        for rw in range_widths:
            dash+="--"
            for i in range(rw):
               dash+='-'
            dash+= "-"
        dash+="-" # one for above the final pipe
        return dash
        
    def get_range_header(self, histogram_ranges):
        h = ""
        for range in histogram_ranges:
            h+="| {0} ".format(range)
        h+="|"
        return h
        
    def get_histogram_values_string(self, values, range_widths):
        v = ""
        for i in range(len(values)):
            value_string = "{0}".format(values[i])
            v+="| "
            r_length = range_widths[i]
            v_length = len(value_string)
            if r_length > v_length:
                diff = r_length - v_length
                for j in range(diff):
                    v+=' '
                v+="{0} ".format(value_string)
            elif r_length == v_length:
                v+="{0} ".format(value_string)
            else : # r_length < v_length
                v+="{0}".format(value_string)
        v+='|'
        return v
            
    def get_feature_string_max_length(self, features):
        max_length = 0
        for f in features:
            if len(f) > max_length:
                max_length = len(f)
        return max_length
        
    def get_proper_length_column(self, s, maxlen):
        result = s
        if len(s) > maxlen:
            return s
        elif len(s) == maxlen:
            return s
        else: #len(s) < maxlen
            diff = maxlen - len(s)
            for i in range(diff):
                result+=' '
            return result
        
    #   format histogram this way:
    #     -------------------------
    #     | 0. - 0.5  | 0.5 - 1.0 |
    #     -------------------------
    #     |        6  |         0 |
    #     -------------------------
    #
    #
    def get_stats_info_formatted(self):
        INFO= "\n\n###########################################\nstatistics for view "+self.view_type+'\n'
        INFO+="###########################################\n"
        INFO+="# nodes found     " + "{0}".format(self.number_nodes) + '\n'
        INFO+="# nodes annotated " + "{0}".format(self.number_nodes_attached) + '\n'
        INFO+="\n"
        INFO+="Anomaly score:\n"
        INFO+="min\tmax\tmean\tstd\tvar\n"
        INFO+="{0}\t{1}\t{2}\t{3}\t{4}\n".format(self.score_range_min, self.score_range_max,self.score_mean, self.score_stdev, self.score_variance)
        INFO+="\n" 
        histogram_ranges              = self.derive_histogram_ranges(self.histogram_for_scores[1])
        histogram_range_string_widths = self.get_range_widths(histogram_ranges)
        histogram_dash_bar            = self.get_dash_bar(histogram_range_string_widths)
        histogram_range_header        = self.get_range_header(histogram_ranges)
        histogram_values_line         = self.get_histogram_values_string(self.histogram_for_scores[0], histogram_range_string_widths)
        INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="    {0}\n".format(histogram_range_header)
        INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="    {0}\n".format(histogram_values_line)
        INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="\n"
        feature_string_max_length = self.get_feature_string_max_length(self.features)
        feature_header_string = self.get_proper_length_column('Feature',feature_string_max_length)
        INFO+="{0}\tmin\tmax\tmean\tstd\tvar\n".format(feature_header_string)
        for f in self.features:
            feature_string = self.get_proper_length_column(f,feature_string_max_length)
            INFO+="{0}\t{1}\t{2}\t{3}\t{4}\t{5}\n".format(feature_string, self.feature_mins[f], self.feature_maxs[f], self.feature_means[f], self.feature_stdevs[f],self.feature_variances[f])
        
        for f in self.features:
            INFO+="\n"
            INFO+="{0}:\n".format(f)
            histogram_values              = self.histograms_for_features[f][0]
            histogram_bounds              = self.histograms_for_features[f][1]
            histogram_ranges              = self.derive_histogram_ranges(histogram_bounds)
            bins_per_line                 = 5
            
            # break the bins into sub-sequences so they can fit on the line without wrapping
            portion_info                  = self.partition_histogram(histogram_ranges, histogram_values ,bins_per_line)
            range_portions = portion_info['range_portions']
            value_portions = portion_info['value_portions']
            
            # print out as many bins as bins_per_row, then move to next line
            for i in range(0,len(range_portions)):
                histogram_range_portion = range_portions[i]
                histogram_value_portion = value_portions[i]
                range_string_widths           = self.get_range_widths(histogram_range_portion)
                histogram_dash_bar            = self.get_dash_bar(range_string_widths)
                histogram_range_header        = self.get_range_header(histogram_range_portion)
                histogram_values_line         = self.get_histogram_values_string(histogram_value_portion,range_string_widths)
                INFO+="    {0}\n".format(histogram_dash_bar)
                INFO+="    {0}\n".format(histogram_range_header)
                INFO+="    {0}\n".format(histogram_dash_bar)
                INFO+="    {0}\n".format(histogram_values_line)
                INFO+="    {0}\n".format(histogram_dash_bar)
            INFO+="\n"
        INFO+="\n"
        return INFO
                  
    def partition_histogram(self, histogram_ranges, histogram_values, bins_per_line):
        h_range_portions = self.get_histogram_portions(histogram_ranges, bins_per_line)
        h_value_portions = self.get_histogram_portions(histogram_values, bins_per_line)
        return {'range_portions':h_range_portions, 'value_portions':h_value_portions}
            
    def get_histogram_portions(self,bin_item_list, bins_per_line):
        result = []
        for portion in self.sublist_generator(bin_item_list, bins_per_line):
            result.append(portion)
        return result
            
    def sublist_generator(self, l, n):
        """Yield successive n-sized chunks from l."""
        for i in range(0, len(l), n):
            yield l[i:i + n]   
    #
    # score functions
    #
    '''
    def note_anomaly_score(self, score):
        score_as_float = float(score)
        if (self.score_range_min == -1):
            self.score_range_min = score_as_float
        else:
            if (score_as_float < self.score_range_min):
                self.score_range_min = score_as_float
        if (self.score_range_max == -1):
            self.score_range_max = score_as_float
        else:
            if (score_as_float > self.score_range_max):
                self.score_range_max = score_as_float
    '''            
    def set_score_range(self):
        if (not(self.scores_loaded())):
            self.load_scores()
        if len(self.scores) == 0:
            # no data present
            self.score_range_min = "noData"
            self.score_range_max = "noData"
        elif len(self.scores) == 1:        
            self.score_range_min = "{0:.2f}".format(float(self.scores[0]))
            self.score_range_max = self.score_range_min
        else:
            floats = [float(x) for x in self.scores]
            self.score_range_min = "{0:.2f}".format(min(floats))
            self.score_range_max = "{0:.2f}".format(max(floats))
                
    def compute_score_mean(self):
        if (not(self.scores_loaded())):
            self.load_scores()
        if len(self.scores) == 0:
            self.score_mean = 'noData'
        else:
            mean = statistics.mean(self.scores)
            self.score_mean = "{0:.2f}".format(mean)
                
    def compute_score_variance(self):
        if (not(self.scores_loaded())):
            self.load_scores()
        if len(self.scores) == 0:
            self.score_variance = 'noData'
        elif len(self.scores) == 1:
            self.score_variance = '0.00'
        else:
            variance = statistics.variance(self.scores)
            self.score_variance = "{0:.2f}".format(variance)
            
    def compute_score_stdev(self):
        if (not(self.scores_loaded())):
            self.load_scores()     
        if len(self.scores) == 0:
            self.score_stdev = 'noData'
        elif len(self.scores) == 1:
            self.score_stdev = '0.00'
        else:
            stdev = statistics.stdev(self.scores)
            self.score_stdev = "{0:.2f}".format(stdev)
    
    def compute_score_histogram(self):
        if (not(self.scores_loaded())):
            self.load_scores()
        histogram = numpy.histogram(self.scores,'auto', None, False, None, None)
        histogram_refined = []
        histogram_refined.append(histogram[0])
        histogram_refined.append(self.round_the_bounds(histogram[1]))
        self.histogram_for_scores = histogram_refined 
         
    def scores_loaded(self):
        return bool(self.scores)
        
    def round_the_bounds(self, list_of_float_strings):
        result = []
        for s in list_of_float_strings:
            f = float(s)
            rounded = "{0:.2f}".format(f)
            result.append(rounded)
        return result
        
    #
    # feature functions
    #   
    def features_loaded(self):
        return bool(self.value_list_for_features)
           
    def compute_feature_ranges(self):
        if (not(self.features_loaded())):
            self.load_features()
        for feature in self.features:
            # initialize to no data and revise if data present
            self.feature_mins[feature] = 'noData'
            self.feature_maxs[feature] = 'noData'
            values = self.value_list_for_features[feature]
            if len(values) == 0:
                # no data present
                pass
            elif len(values) == 1:        
                self.feature_mins[feature] = "{0}".format(int(values[0]))
                self.feature_maxs[feature] = self.feature_mins[feature]
            else:
                ints = [int(x) for x in values]
                self.feature_mins[feature] = "{0}".format(min(ints))
                self.feature_maxs[feature] = "{0}".format(max(ints))
            
    def compute_feature_means(self):
        if (not(self.features_loaded())):
            self.load_features()
        for feature in self.features:
            values = self.value_list_for_features[feature]
            # should never be zero, but just in case
            if len(values) == 0:
                self.feature_means[feature] = 'noData'
            else:
                mean = statistics.mean(values)
                self.feature_means[feature] = "{0:.2f}".format(mean)
            
    def compute_feature_variances(self):
        if (not(self.features_loaded())):
            self.load_features()
        for feature in self.features:
            values = self.value_list_for_features[feature]
            # should never be zero, but just in case
            if len(values) == 0:
                self.feature_variances[feature] = 'noData'
            elif len(values) == 1:
                self.feature_variances[feature] = '0.00'
            else:
                variance = statistics.variance(values)
                self.feature_variances[feature] = "{0:.2f}".format(variance)
        
    def compute_feature_stdevs(self):
        if (not(self.features_loaded())):
            self.load_features()
        for feature in self.features:
            values = self.value_list_for_features[feature]
            # should never be zero, but just in case
            if len(values) == 0:
                self.feature_stdevs[feature] = 'noData'
            elif len(values) == 1:
                self.feature_stdevs[feature] = '0.00'
            else:
                stdev = statistics.stdev(values)
                self.feature_stdevs[feature] = "{0:.2f}".format(stdev)
        
    def compute_feature_histograms(self):
        if (not(self.features_loaded())):
            self.load_features()
        for feature in self.features:
            values = self.value_list_for_features[feature]
            histogram = numpy.histogram(values,'auto', None, False, None, None)
            self.histograms_for_features[feature] = histogram


if __name__ == '__main__':
    #import pdb; pdb.set_trace()
    view_stats = ViewStats('statsTest','/home/vagrant/adapt/ad/test')
    view_stats.compute_feature_means()
    view_stats.compute_feature_stdevs()
    view_stats.compute_feature_histograms()
    view_stats.set_score_range()
    print(view_stats.get_stats_info())

