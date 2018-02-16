csv_to_rcf<-function(csv_file,Out_put_rcf_file){
 
  
  
  if(is.null(Out_put_rcf_file))stop("error in rcf_context_file file   - terminating")
  if(is.null(csv_file))stop("error in csv_file file   - terminating")
  OutCon<-file(Out_put_rcf_file,"w")
   
  
  cat('\n Init System LOAD FROM  CSV... \n ')
  cat('\n Parameters are: \n ')
  cat('\n CSV \n ',csv_file)
  source("load_csv.r")
  returns_args=load_csv(csv_file)
  List_Objects=returns_args$List_Objects
  List_Attributes=returns_args$List_Attributes
  AttributesofObject=returns_args$AttributesofObject 
  ObjectOfAttributes=returns_args$ObjectOfAttributes
  
  ##################################### generating the RCF file
  cat('\n Start creating RCF Context \n ')
  
  writeLines("[Relational Context]", OutCon)
  writeLines("Default Name", OutCon)
  writeLines("[Binary Relation]", OutCon)
  writeLines("Name_of_dataset", OutCon)
  writeLines(paste(List_Objects, collapse = ' | '),OutCon)
  writeLines(paste(List_Attributes, collapse = ' | '),OutCon)
  for(i in 1:length(List_Objects)){
    matching_List <-  ifelse(List_Attributes %in% AttributesofObject[[i]], 1, 0)
    writeLines( paste(matching_List, collapse = ' ') ,OutCon)
    
  }
  
  writeLines("[END Relational Context]", OutCon)
  cat('\n  RCF Context successfully created \n ')
  ##################################### generating the csv file 

}