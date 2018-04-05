args <- commandArgs()
print(args)

list.of.packages <- c('RJSONIO',"jsonlite")

new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
if(length(new.packages)) {install.packages(new.packages)}
library('RJSONIO')
 
library('jsonlite')


JsonSpecFile <- args[6]
rcf.context.file <- args[8]
csv.file <- args[7]
nb <- as.numeric(args[9])

if(is.null(JsonSpecFile)) stop("error in JsonSpecFile    - terminating")
if(is.null(rcf.context.file)) stop("error in rcf.context.file    - terminating")
if(is.null(csv.file)) stop("error in csv.file file   - terminating")
OutCon <- file(rcf.context.file,"w")
OutConCsv <- file(csv.file,"w")

cat("\n reading the Json specification file \n")
dataspec <- fromJSON(JsonSpecFile,simplifyVector = TRUE)
if(length(dataspec)==0) stop("neo4j json spec file incorrect - terminating")
query <- unlist(dataspec[1])[[1]]
objectsdata <- unlist(dataspec[1])[[2]]
attributesdata <- unlist(dataspec[1])[[3]]
neo4JURL <- "http://localhost:8080/query/json/"
query.result <- fromJSON(paste0(neo4JURL,query,sep=""),simplifyVector = TRUE) 
if(is.null(query.result))stop("neo4j server error - terminating")
cat('\n Start querying the server \n ')
query.result <- unique(query.result)
names(query.result)
if(length(query.result)<=1) stop("neo4j json specs do not contain objects nor attributes - terminating")
cat('\n Json results successfully retrieved from the server \n ')
#UniqueObjectList= as.list(query.result[objectsdata] %>% distinct())
cat('\n Start creating Object and Attributes Lists From Json \n ')
AttributesofObject<-list()
UniqueAttributeList=list()
#id.unique<-query.result[objectsdata] %>% distinct()
UniqueObjectList <- unique(query.result[, names(query.result)[1]])
#UniqueObjectList=UniqueObjectList[1:nb] 
for (i in 1:length(UniqueObjectList)){#for each object
  tmp=NULL
  for(j in 2:length(names(query.result))){ # for each column of the attributes
    tmp2=unique(as.character(query.result[query.result[objectsdata]==UniqueObjectList[[i]], names(query.result)[j]]))
    tmp=c(tmp,tmp2)
  }
  AttributesofObject[eval(UniqueObjectList[[i]])] <- list(unique(tmp))
  UniqueAttributeList  <- append(UniqueAttributeList,as.list(tmp) )
  UniqueAttributeList <- unique(UniqueAttributeList)
  
}
cat('\n Objects and Attributes Lists successfully created \n ')

##################################### generating the RCF file
cat('\n Start creating RCF Context \n ')

writeLines("[Relational Context]", OutCon)
writeLines("Default Name", OutCon)
writeLines("[Binary Relation]", OutCon)
writeLines("Name_of_dataset", OutCon)
writeLines(paste(UniqueObjectList, collapse = ' | '),OutCon)
writeLines(paste(UniqueAttributeList, collapse = ' | '),OutCon)
for(i in 1:length(UniqueObjectList)){
  matching.List <-  ifelse(UniqueAttributeList %in% AttributesofObject[[i]], 1, 0)
  writeLines( paste(matching.List, collapse = ' ') ,OutCon)
  
}

writeLines("[END Relational Context]", OutCon)
cat('\n  RCF Context successfully created \n ')
##################################### generating the csv file 
cat('\n  Start creating CSV Context \n ')

binaryAttList <- paste( unlist(UniqueAttributeList), collapse=',')
binaryAttList <- paste(',',binaryAttList)
binaryAttList <- gsub(" ", "", binaryAttList, fixed = TRUE)
writeLines(binaryAttList, OutConCsv)

for (i in 1:length(UniqueObjectList)){
  matching.List <-  ifelse(UniqueAttributeList %in% AttributesofObject[[i]], 1, 0)
  
  binaryAttList <- paste( unlist(matching.List), collapse=',')
  
  binaryAttList <- paste(as.character(UniqueObjectList[i]),',',binaryAttList)
  binaryAttList <- gsub(" ", "", binaryAttList, fixed = TRUE)
  writeLines( binaryAttList ,OutConCsv)
}

cat('\n  CSV Context successfully created \n ')
