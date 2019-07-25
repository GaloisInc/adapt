load.csv <- function(csv.file){

  if(!file.exists(csv.file)) {
        stop(" csv file does not exist \n")
  }

  if (file.size(csv.file) == 0)  {
      stop("\n csv.file is empty \n")
  }

  Incon  <-  file(csv.file, "r")
  if(is.null(Incon)) {
      stop("error in csv file name \n")
  }

  context.file <- NULL
  result = tryCatch({
    context.file=read.csv(csv.file)
  },   error = function(e) {
    context.file=NULL
  } )
  if(is.null(context.file)) {
      stop("error in csv file name \n")
  }
  AttributesList <- as.list(names(context.file)[2:length(names(context.file))])
  colnames(context.file) <- NULL
  context.matrix <- as.matrix(context.file)
  ObjectList <- as.list(context.matrix[,1])
  context.matrix<- context.matrix[,-1]


  returned.list <- list("Objects" = ObjectList,
                        "Attributes" = AttributesList,
                        "Matrix" = context.matrix)
  close(Incon)
  return (returned.list)
}

csv.to.rcf <- function(csv.file,output.rcf.file) {
  if(is.null(output.rcf.file)) {
      stop("error in rcf_context_file file   - terminating")
  }
  OutCon <- file(output.rcf.file,"w")

  ctx = load.csv(csv.file)

  ##################################### generating the RCF file

  writeLines("[Relational Context]", OutCon)
  writeLines("Default Name", OutCon)
  writeLines("[Binary Relation]", OutCon)
  writeLines("Name_of_dataset", OutCon)
  writeLines(paste(ctx$Objects, collapse = ' | '),OutCon)
  writeLines(paste(ctx$Attributes, collapse = ' | '),OutCon)
  for(i in 1:length(ctx$Objects)) {
    matching_List <- ctx$Matrix[i,]
    writeLines(paste(matching_List, collapse = ' '), OutCon)
  }
  writeLines("[END Relational Context]", OutCon)
  ##################################### generating the csv file 
  close(OutCon)
}
