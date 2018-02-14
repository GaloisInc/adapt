check_packages=function(){
  list.of.packages <- c("arules",
                        "Matrix",
                        "data.table",
                        "Matrix",
                        "e1071",
                        "rPython",
                        "dplyr",
                        "tidyr",
                        "glue",
                        "ggplot2",
                        "reshape2",
                        "stringi",
                        "data.table",
                        "RNeo4j",
                        'RCurl',
                        'RJSONIO',
                        'plyr',
                        "jsonlite",
                        "tictoc",
                        "gplots",
                        "sqldf"
                        )
  new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
  if(length(new.packages)) install.packages(new.packages)
  else print('all packages are installed ')
}