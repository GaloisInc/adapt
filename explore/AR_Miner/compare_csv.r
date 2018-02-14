
args <- commandArgs()
#print(args)
A <- read.csv(args[6])
B<- read.csv(args[7])
indx <- colSums(A==B)
set1=as.list(colnames(A))
set2=as.list(colnames(B))
if(length(setdiff(set1, set2))>0)cat("\n ==================>the files do not have the same colnames \n")  else cat(" similar col names")
context_matrix1 <- as.matrix(A)
set1=as.list(context_matrix1[,1])
context_matrix2 <- as.matrix(B)
set2=as.list(context_matrix2[,1])
if(length(setdiff(set1, set2))>0)cat("\n ==================>the files do not have the same rownames \n")  else cat(" similar rownames names")

A=A[ , order(names(A))]
B=B[ , order(names(A))]
 
row.names(A)=set1
row.names(B)=set2
A=A[   order(as.character(set1)),]
B=B[   order(as.character(set1)),]
l=mapply(function(x, y) all(x==y), A, B)
if(all(l==TRUE))cat("\n ==================>the files are similar \n")  else cat(" The files are differents")



list1<-readLines('./contexts/ProcessEventSample.csv')
list2<-readLines('./contexts/ProcessEventSample2.csv')

 