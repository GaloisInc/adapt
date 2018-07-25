#### this function takes as input a csv context and loads a data structure
load_csv=function(csv_file){
  #csv_file_name="/home/terminator2/Documents/R_Projects/AnomalyRulesMining/contexts/Context_ProcessEvent.csv"
  Incon = file(csv_file, "r")
  if(is.null(Incon))stop("error in csv_file_name     - terminating")
  if(!file.exists(csv_file))stop(" csv file does not exist")
  
  if (file.size(csv_file) == 0)  {stop("\n csv_file is file empty \n")}
  
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
  
  if(length(ObjectList)==0)stop("\n Length of List_Objects is 0, try other configurations or databases \n")
  if(length(AttribuesList)==0)stop("\n Length of List_Attributes is 0, try other configurations or databases \n")
  
  
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