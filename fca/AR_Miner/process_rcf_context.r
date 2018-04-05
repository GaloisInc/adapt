#### this function takes as input an RCF context and generates a csv context file
process.rcf.context <-function(context.file.name,csv.file){
  
  if(is.null(context.file.name))stop("error in context.file.name file   - terminating")
  if(is.null(csv.file))stop("error in csv.file file   - terminating")
  Incon  <- file(context.file.name, "r")
  OutCon<-file(csv.file,"w")

  
   while ( TRUE ) {
    line  <- readLines(Incon, n = 1)
    if ( length(line) == 0 ) {
      break
    }
    print(line)
  }
  # isSeekable
  seek(Incon,origin ="start",0)
  nb.lines <-length(readLines(Incon))
  print(nb.lines)
  seek(Incon,origin="start",0)
  List.Objects <-list()
  Line <-readLines(Incon, n = 4)
  Line <-readLines(Incon, n = 1)
  List.Objects <-as.list(as.character(strsplit(Line, split='|',fixed=TRUE)[[1]]))
  List.Attributes <-list()
  Line <-readLines(Incon, n = 1)
  List.Attributes <-as.list(as.character(strsplit(Line, split='|',fixed=TRUE)[[1]]))
  AttributesofObject <- list()
  binaryAttList <-paste( unlist(List.Attributes), collapse=',')
  binaryAttList <-paste(',',binaryAttList)
  binaryAttList <-gsub(" ", "", binaryAttList, fixed = TRUE)
  writeLines(binaryAttList, OutCon)
  #BinaryAttributesofObject<-list()
 
  #mylist <- list(list1, list2)
  for (i in 1:length(List.Objects)){
  Line <-readLines(Incon, n = 1)  
  Binary <-as.list(as.character(strsplit(Line, split=' ',fixed=TRUE)[[1]]))  
  AttributesofObject[[length(AttributesofObject)+1]] <- as.list(List.Attributes[which(Binary==1)]) 
  binaryAttList <-gsub(" ", ",", Line, fixed = TRUE)
  binaryAttList <-paste(as.character(List.Objects[i]),',',binaryAttList)
  binaryAttList <-gsub(" ", "", binaryAttList, fixed = TRUE)
  writeLines(binaryAttList, OutCon)
  #cat("\n line \n",i)
  #cat(as.character(List.Objects[i]))
  #BinaryAttributesofObject[[length(AttributesofObject)+1]]<-Binary
  }
  
  returned.list <- list("List_Objects" = List.Objects, "List_Attributes" = List.Attributes,"AttributesofObject"=AttributesofObject)
  close(Incon)
  close(OutCon)
  return (returned.list)
  
  
  
  
}