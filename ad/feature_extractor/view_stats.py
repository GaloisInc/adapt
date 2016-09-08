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

    def compute_all_stats():
        self.compute_feature_means()
        self.compute_feature_variances()
        self.compute_feature_stdevs()
        self.compute_feature_histograms()
        self.set_score_range()
        self.compute_score_mean()
        
    def nonblank_lines(self,lines):
        nonblanks = []
        for l in lines:
            if l == '\n':
                pass
            else:
                nonblanks.append(l)
        return nonblanks
                
    def set_score_range(self):
        #print('setting score range...')
        with open(self.scores_file_path, 'r') as csvfile:
            lines = csvfile.readlines()
            lines = self.nonblank_lines(lines)
            if len(lines) == 2:
                parts = lines[1].split(',') #line[0] is header
                only_score = parts[len(parts) - 1]
                self.score_range_min = "{0:.2f}".format(float(only_score))
                self.score_range_max = self.score_range_min
            elif len(lines) < 2:
                # no data present
                self.score_range_min = "noData"
                self.score_range_max = "noData"
            else:
                i = 0;
                for line in lines:
                    if i == 0:
                        pass
                        #print('skipping header {0}'.format(line))
                    else:
                        parts = line.split(',')
                        score = parts[len(parts) - 1]
                        self.note_anomaly_score(score)
                    i = i + 1
                self.score_range_min = "{0:.2f}".format(self.score_range_min)
                self.score_range_max = "{0:.2f}".format(self.score_range_max)
                #print('range of scores {0} - {1}'.format(self.score_range_min, self.score_range_max))
     
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
        
    def get_stats_info_formatted(self):
        INFO= "###########################################\nstatistics for view "+self.view_type+'\n'
        INFO+="###########################################\n"
        INFO+="# nodes found     " + "{0}".format(self.number_nodes) + '\n'
        INFO+="# nodes annotated " + "{0}".format(self.number_nodes_attached) + '\n'
        INFO+="\n"
        INFO+="Anomaly score:\n"
        INFO+="min\tmax\tmean\tstd\tvar\n"
        INFO+="{0}\t{1}\t{2}\t{3}\t{4}\n\n".format(self.score_range_min, self.score_range_max,self.score_mean, self.score_stdev, self.score_variance)
        INFO+="\n" 
        histogram_ranges              = derive_histogram_ranges(self.histogram_for_scores)
        histogram_range_string_widths = get_range_widths(histogram_ranges)
        histogram_dash_bar            = get_dash_bar(histogram_range_string_widths)
        histogram_range_header        = get_range_header(histogram_ranges)
        histogram_values              = get_histogram_values_string(self.histogram_for_scores)
        INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="    {0}\n".format(histogram_range_header)
        INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="    {0}\n".format(histogram_values)
        INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="\n"
        feature_string_max_length = get_feature_string_max_length(self.features)
        feature_header_string = get_proper_length_column('Feature',feature_string_max_length)
        INFO+="{0}\tmin\tmax\tmean\tstd\tvar".format(feature_header_string)
        for f in self.features:
            feature_string = get_proper_length_column(f,feature_string_max_length)
            INFO+="{0}\t{1}\t{2}\t{3}\t{4}\t{5}".format(feature_string, self.feature_mins[f], self.feature_maxs[f], self.feature_means[f], self.feature_stdevs[f],self.feature_variances[f])
        
        for f in self.features:
            INFO+="\n"
            INFO+="{0}:\n".format(f)
            histogram_ranges              = derive_histogram_ranges(self.histogram_for_features)
            histogram_range_string_widths = get_range_widths(histogram_ranges)
            histogram_dash_bar            = get_dash_bar(histogram_range_string_widths)
            histogram_range_header        = get_range_header(histogram_ranges)
            histogram_values              = get_histogram_values_string(self.histogram_for_features)
            INFO+="    {0}\n".format(histogram_dash_bar)
            INFO+="    {0}\n".format(histogram_range_header)
            INFO+="    {0}\n".format(histogram_dash_bar)
            INFO+="    {0}\n".format(histogram_values)
            INFO+="    {0}\n".format(histogram_dash_bar)
        INFO+="\n"
        return INFO
                  
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

    #
    # score functions
    #
    def compute_score_mean(self):
        if (not(bool(self.scores))):
            self.load_scores()
        
        if len(self.scores) == 0:
            self.score_mean = 'noData'
        else:
            mean = statistics.mean(self.scores)
            self.score_mean = "{0:.2f}".format(mean)
                
    def compute_score_variance(self):
        if (not(bool(self.scores))):
            self.load_scores()
        
        if len(self.scores) == 0:
            self.score_variance = 'noData'
        elif len(self.scores) == 1:
            self.score_variance = '0.00'
        else:
            variance = statistics.variance(self.scores)
            self.score_variance = "{0:.2f}".format(variance)
            
    def compute_score_stdev(self):
        if (not(bool(self.scores))):
            self.load_scores()
            
        if len(self.scores) == 0:
            self.score_stdev = 'noData'
        elif len(self.scores) == 1:
            self.score_stdev = '0.00'
        else:
            stdev = statistics.stdev(self.scores)
            self.score_stdev = "{0:.2f}".format(stdev)
      
    #
    # feature functions
    #      
    def compute_feature_means(self):
        #print('computing means...')
        if (not(bool(self.value_list_for_features))):
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
        if (not(bool(self.value_list_for_features))):
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
        #print('computing standard deviation')
        if (not(bool(self.value_list_for_features))):
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
        #print('computing feature histograms')
        if (not(bool(self.value_list_for_features))):
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

