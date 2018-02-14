
args <- commandArgs()
#print(args)
A <- read.csv(args[6])
B<- read.csv(args[7])
 
set1=as.list(colnames(A))
set2=as.list(colnames(B))
cat("\n checking col names \n")
if(length(setdiff(set1, set2))>0)stop("\n ==================>the files do not have the same colnames \n")  else cat("\n similar col names \n")
cat("\n checking row names \n")

context_matrix1 <- as.matrix(A)
set1=as.list(context_matrix1[,1])
context_matrix2 <- as.matrix(B)
set2=as.list(context_matrix2[,1])
if(length(setdiff(set1, set2))>0)stop("\n ==================>the files do not have the same rownames \n")  else cat(" similar rownames names")

x=colnames(A)
A=A[ ,order(x)]
B=B[ , order(x)]
A=A[   order(as.character(set1)),]
B=B[   order(as.character(set1)),]
l=mapply(function(x, y) all(x==y), A, B)
if(all(l==TRUE))cat("\n ==================>the files are similar \n")  else cat(" The files are differents")

 

 