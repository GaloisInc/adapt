


args <- commandArgs()
print(args)

list.of.packages <- c('RJSONIO',"jsonlite")

new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
if(length(new.packages)) {install.packages(new.packages)}
library('RJSONIO')
 
library('jsonlite')


JsonSpecFile=args[6]
rcf_context_file=args[8]
csv_file=args[7]
nb=as.numeric(args[9])

if(is.null(JsonSpecFile))stop("error in JsonSpecFile file   - terminating")
if(is.null(rcf_context_file))stop("error in rcf_context_file file   - terminating")
if(is.null(csv_file))stop("error in csv_file file   - terminating")
OutCon<-file(rcf_context_file,"w")
OutConCsv<-file(csv_file,"w")


dataspec=fromJSON(JsonSpecFile,simplifyVector = TRUE)
if(length(dataspec)==0)stop("neo4j json spec file incorrect - terminating")
query=unlist(dataspec[1])[[1]]
objectsdata=unlist(dataspec[1])[[2]]
attributesdata=unlist(dataspec[1])[[3]]
neo4JURL="http://localhost:8080/query/json/"
query_result <- fromJSON(paste0(neo4JURL,query,sep=""),simplifyVector = TRUE) 
if(is.null(query_result))stop("neo4j server error - terminating")
cat('\n Start querying the server \n ')
query_result=unique(query_result)
names(query_result)
if(length(query_result)<=1)stop("neo4j json spec no objects or attributes detected - terminating")
cat('\n Json results successfully retrieved from server \n ')
#UniqueObjectList= as.list(query_result[objectsdata] %>% distinct())
cat('\n Start creating Object and Attributes Lists From Json \n ')
AttributesofObject<-list()
UniqueAttributeList=list()
#id_unique<-query_result[objectsdata] %>% distinct()
UniqueObjectList <- unique(query_result[, names(query_result)[1]])
#UniqueObjectList=UniqueObjectList[1:nb] 
for (i in 1:length(UniqueObjectList)){#for each object
  tmp=NULL
  for(j in 2:length(names(query_result))){ # for each column of the attributes
    tmp2=unique(as.character(query_result[query_result[objectsdata]==UniqueObjectList[[i]], names(query_result)[j]]))
    tmp=c(tmp,tmp2)
  }
  AttributesofObject[eval(UniqueObjectList[[i]])]=list(unique(tmp))
  UniqueAttributeList = append(UniqueAttributeList,as.list(tmp) )
  UniqueAttributeList=unique(UniqueAttributeList)
  
}
cat('\n Object and Attributes Lists successfully created \n ')

##################################### generating the RCF file
cat('\n Start creating RCF Context \n ')

writeLines("[Relational Context]", OutCon)
writeLines("Default Name", OutCon)
writeLines("[Binary Relation]", OutCon)
writeLines("Name_of_dataset", OutCon)
writeLines(paste(UniqueObjectList, collapse = ' | '),OutCon)
writeLines(paste(UniqueAttributeList, collapse = ' | '),OutCon)
for(i in 1:length(UniqueObjectList)){
  matching_List <-  ifelse(UniqueAttributeList %in% AttributesofObject[[i]], 1, 0)
  writeLines( paste(matching_List, collapse = ' ') ,OutCon)
  
}

writeLines("[END Relational Context]", OutCon)
cat('\n  RCF Context successfully created \n ')
##################################### generating the csv file 
cat('\n  Start creating CSV Context \n ')

binaryAttList=paste( unlist(UniqueAttributeList), collapse=',')
binaryAttList=paste(',',binaryAttList)
binaryAttList=gsub(" ", "", binaryAttList, fixed = TRUE)
writeLines(binaryAttList, OutConCsv)

for (i in 1:length(UniqueObjectList)){
  matching_List <-  ifelse(UniqueAttributeList %in% AttributesofObject[[i]], 1, 0)
  
  binaryAttList=paste( unlist(matching_List), collapse=',')
  
  binaryAttList=paste(as.character(UniqueObjectList[i]),',',binaryAttList)
  binaryAttList=gsub(" ", "", binaryAttList, fixed = TRUE)
  writeLines( binaryAttList ,OutConCsv)
}

cat('\n  CSV Context successfully created \n ')
