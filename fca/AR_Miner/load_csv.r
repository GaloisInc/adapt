#### this function takes as input a csv context and loads a data structure
load_csv=function(csv_file_name){
  #csv_file_name="/home/terminator2/Documents/R_Projects/AnomalyRulesMining/contexts/Context_ProcessEvent.csv"
  Incon = file(csv_file, "r")
  if(is.null(Incon))stop("error in csv_file_name     - terminating")
 # context_file=read.csv(file='./contexts/Context_ProcessEvent.csv')
  cat('loading csv')
  context_file=NULL
  result = tryCatch({
    context_file=read.csv(csv_file)
  },   error = function(e) {
    context_file=NULL
  } )
  
  if(is.null(context_file))return(NULL)
  context_file=read.csv(csv_file)
  ObjectOfAttributes=list()
  AttribuesList=as.list(names(context_file)[2:length(names(context_file))])
  AttributesOfObjects=list()
  colnames(context_file) <- NULL
  context_matrix <- as.matrix(context_file)
  ObjectList=as.list(context_matrix[,1])
  context_matrix<- context_matrix[,-1]
  cat('loading ObjectOfAttributes')
  
  for (i in 1:length(AttribuesList)){
    col= context_matrix[,i] 
    List=as.list(ObjectList[col=="1"])
    ObjectOfAttributes[[length(ObjectOfAttributes)+1]] <-  List 
  }
  cat('loading AttributesOfObjects')
  
  for (i in 1:length(ObjectList)){
    lin= context_matrix[i,] 
    List=as.list(AttribuesList[lin=="1"])
    AttributesOfObjects[[length(AttributesOfObjects)+1]] <-  List 
  }
  
  returned_list <- list("List_Objects" = ObjectList, "List_Attributes" = AttribuesList,
                        "AttributesofObject"=AttributesOfObjects,"ObjectOfAttributes"=ObjectOfAttributes)
  close(Incon)
  return (returned_list)
  
  
  
  
}