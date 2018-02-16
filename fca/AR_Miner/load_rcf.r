#### this function takes as input an RCF context and loads a data structure
load_rcf=function(context_file_name){
  
  Incon = file(context_file_name, "r")
  if(is.null(Incon))stop("error in context_file_name file   - terminating")
  while ( TRUE ) {
    line = readLines(Incon, n = 1)
    if ( length(line) == 0 ) {
      break
    }
    print(line)
  }
  # isSeekable
  seek(Incon,origin="start",0)
  nb_lines=length(readLines(Incon))
  print(nb_lines)
  seek(Incon,origin="start",0)
  List_Objects=list()
  Line=readLines(Incon, n = 4)
  Line=readLines(Incon, n = 1)
  List_Objects=as.list(as.character(strsplit(Line, split='|',fixed=TRUE)[[1]]))
  List_Attributes=list()
  Line=readLines(Incon, n = 1)
  List_Attributes=as.list(as.character(strsplit(Line, split='|',fixed=TRUE)[[1]]))
  AttributesofObject <- list()
  binaryAttList=paste( unlist(List_Attributes), collapse=',')
  binaryAttList=paste(',',binaryAttList)
  binaryAttList=gsub(" ", "", binaryAttList, fixed = TRUE)
    
   for (i in 1:length(List_Objects)){
    Line=readLines(Incon, n = 1)  
    Binary=as.list(as.character(strsplit(Line, split=' ',fixed=TRUE)[[1]]))  
    AttributesofObject[[length(AttributesofObject)+1]] <- as.list(List_Attributes[which(Binary==1)]) 
        
  }
  
  returned_list <- list("List_Objects" = List_Objects, "List_Attributes" = List_Attributes,"AttributesofObject"=AttributesofObject)
  close(Incon)
   return (returned_list)
  
  
  
  
}