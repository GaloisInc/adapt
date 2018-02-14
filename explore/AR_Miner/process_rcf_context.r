#### this function takes as input an RCF context and generates a csv context file
process_rcf_context=function(context_file_name,csv_file){
  
  if(is.null(context_file_name))stop("error in context_file_name file   - terminating")
  if(is.null(csv_file))stop("error in csv_file file   - terminating")
  Incon = file(context_file_name, "r")
  OutCon<-file(csv_file,"w")

  
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
  writeLines(binaryAttList, OutCon)
  #BinaryAttributesofObject<-list()
 
  #mylist <- list(list1, list2)
  for (i in 1:length(List_Objects)){
  Line=readLines(Incon, n = 1)  
  Binary=as.list(as.character(strsplit(Line, split=' ',fixed=TRUE)[[1]]))  
  AttributesofObject[[length(AttributesofObject)+1]] <- as.list(List_Attributes[which(Binary==1)]) 
  binaryAttList=gsub(" ", ",", Line, fixed = TRUE)
  binaryAttList=paste(as.character(List_Objects[i]),',',binaryAttList)
  binaryAttList=gsub(" ", "", binaryAttList, fixed = TRUE)
  writeLines(binaryAttList, OutCon)
  #cat("\n line \n",i)
  #cat(as.character(List_Objects[i]))
  #BinaryAttributesofObject[[length(AttributesofObject)+1]]<-Binary
  }
  
  returned_list <- list("List_Objects" = List_Objects, "List_Attributes" = List_Attributes,"AttributesofObject"=AttributesofObject)
  close(Incon)
  close(OutCon)
  return (returned_list)
  
  
  
  
}