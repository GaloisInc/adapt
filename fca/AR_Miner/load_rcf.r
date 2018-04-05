#### this function takes as input an RCF context and loads a data structure
load.rcf <- function(context.file.name){
  
  Incon <- file(context.file.name, "r")
  if(is.null(Incon))stop("error in context.file.name file   - terminating")
  while ( TRUE ) {
    line <- readLines(Incon, n = 1)
    if ( length(line) == 0 ) {
      break
    }
    print(line)
  }
  # isSeekable
  seek(Incon,origin="start",0)
  nb.lines <- length(readLines(Incon))
  print(nb.lines)
  seek(Incon,origin="start",0)
  List.Objects=list()
  Line <- readLines(Incon, n = 4)
  Line <- readLines(Incon, n = 1)
  List.Objects <- as.list(as.character(strsplit(Line, split='|',fixed=TRUE)[[1]]))
  List.Attributes <- list()
  Line <- readLines(Incon, n = 1)
  List.Attributes <- as.list(as.character(strsplit(Line, split='|',fixed=TRUE)[[1]]))
  AttributesofObject <- list()
  binaryAttList <- paste( unlist(List.Attributes), collapse=',')
  binaryAttList <- paste(',',binaryAttList)
  binaryAttList <- gsub(" ", "", binaryAttList, fixed = TRUE)
    
   for (i in 1:length(List.Objects)){
    Line <- readLines(Incon, n = 1)  
    Binary <- as.list(as.character(strsplit(Line, split=' ',fixed=TRUE)[[1]]))  
    AttributesofObject[[length(AttributesofObject)+1]] <- as.list(List.Attributes[which(Binary==1)]) 
        
  }
  
  returned.list <- list("List_Objects" = List.Objects, "List_Attributes" = List.Attributes,"AttributesofObject"=AttributesofObject)
  close(Incon)
   return (returned.list)
  
  
  
  
}