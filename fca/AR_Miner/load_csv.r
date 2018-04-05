#### this function takes as input a csv context and loads a data structure
load.csv <- function(csv.file){
  #csv.file.name <- "/home/terminator2/Documents/R.Projects/AnomalyRulesMining/contexts/Context.ProcessEvent.csv"
  Incon  <-  file(csv.file, "r")
  if(is.null(Incon))stop("error in csv.file.name     - terminating")
  if(!file.exists(csv.file))stop(" csv file does not exist")
  
  if (file.size(csv.file) == 0)  {stop("\n csv.file is file empty \n")}
  
 # context.file=read.csv(file='./contexts/Context.ProcessEvent.csv')
  cat('loading csv')
  context.file <- NULL
  result = tryCatch({
    context.file=read.csv(csv_file)
  },   error = function(e) {
    context.file=NULL
  } )
  if(is.null(context.file))return(NULL)
  #context.file <- read.csv(csv.file)
  ObjectOfAttributes <- list()
  AttribuesList <- as.list(names(context.file)[2:length(names(context.file))])
  AttributesOfObjects <- list()
  colnames(context.file) <- NULL
  context.matrix <- as.matrix(context.file)
  ObjectList <- as.list(context.matrix[,1])
  context.matrix<- context.matrix[,-1]
  
  if(length(ObjectList)==0)stop("\n Length of List.Objects is 0, try other configurations or databases \n")
  if(length(AttribuesList)==0)stop("\n Length of List.Attributes is 0, try other configurations or databases \n")
  
  
  cat('loading ObjectOfAttributes')
  
  for (i in 1:length(AttribuesList)){
    col <-  context.matrix[,i] 
    List <- as.list(ObjectList[col=="1"])
    ObjectOfAttributes[[length(ObjectOfAttributes)+1]] <-  List 
  }
  cat('loading AttributesOfObjects')
  
  for (i in 1:length(ObjectList)){
    lin <-  context.matrix[i,] 
    List <- as.list(AttribuesList[lin=="1"])
    AttributesOfObjects[[length(AttributesOfObjects)+1]] <-  List 
  }
  
  returned.list <- list("List_Objects" = ObjectList, "List_Attributes" = AttribuesList,
                        "AttributesofObject"=AttributesOfObjects,"ObjectOfAttributes"=ObjectOfAttributes)
  close(Incon)
  return (returned.list)
  
  
  
  
}