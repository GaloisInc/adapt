library("arules")
library(Matrix)
library(data.table)
library(Matrix)
library(e1071)
library(rPython)
library(dplyr)
library(tidyr)
library(glue)
require(ggplot2)
require(reshape2)
library(stringi)
library(data.table)
library(RNeo4j)
library('RCurl')
library('RJSONIO')
library('plyr')
library(jsonlite)
library(microbenchmark)
library(tictoc)

fcboscript='/home/terminator1/Documents/adapt/FCA_JSON/explore/fcbo.py'
fcboscript2='/home/terminator1/Documents/adapt/FCA_JSON/fcbo.py'
#'/home/terminator2/Documents/Adapt_Project/Repository/experimental_fca_neo4j_branche/adapt/explore/fcbo.py'
viewer = getOption("viewer")
if(!is.null(viewer)){
  viewer("http://localhost:8080")
}else {
  utils::browseURL("http://localhost:8080")
}
neo4j = startGraph("http://localhost:8080/") #opts = list(timeout=2))  http://localhost:8080/query/generic/g.V().limit(10)
browse(neo4j)
query="g.V().hasLabel('AdmNetFlowObject').as('nf').values('remoteAddress').as('ip').select('nf').inE('predicateObject','predicateObject2').outV().values('eventType').as('type').select('ip','type')"
#g.V().has('eventType').as('y').values('eventType').as('type').select('y').out().has('subjectType','SUBJECT_PROCESS').as('x').values('uuid').as('id').select('x').out().has('path').values('path').as('filepath').select('id','type','filepath').dedup()"#g.V().limit(10)"
neo4JURL="http://localhost:8080/query/json/"
query_result <- fromJSON(paste0(neo4JURL,query,sep=""))
#itemJson=unlist(query_result[4])
#repo_df <- lapply(query_result, function(x) {
#  itemJson=unlist(x)
#  df <- data_frame(id        = itemJson[1],
#                   type     = itemJson[2],
#                   filepath     = itemJson[3])
#}) %>% bind_rows()
#query_resultdf<- lapply(query_result, function(x) {
#  x[sapply(x, is.null)] <- NA
#  unlist(x)
#})
#query_resultdf=do.call("rbind", query_resultdf)
#python.load(fcboscript)
Listviews=c("ProcessEventExec",
            "ProcessNetFlow2",
              "ProcessEventExec",
              "ProcessByIPPort",
              "FileEventType",
              "FileEventExec",
              "FileEvent")
myWorkingDirectory=getwd()
AttributesDictionnary=paste0(myWorkingDirectory,"/contexts/AttributesDictionnary.txt",sep="")
ObjectssDictionnary=paste0(myWorkingDirectory,"/contexts/ObjectsDictionnary.txt",sep="")
#JsonSpecFile="/home/terminator2/Documents/Adapt_Project/Repository/experimental_fca/explore/csv/csvspec.json"
JsonSpecFile2='/home/terminator1/Documents/adapt/FCA_JSON/contextSpecFiles/neo4jspec_FileEvent.json'
ContextFile=paste0(myWorkingDirectory,"/contexts/Context.basenum",sep="")
ShiftedContextFile=paste0(myWorkingDirectory,"/contexts/ShiftedContext.basenum",sep="")
ContextFileRCF=paste0(myWorkingDirectory,"/contexts/Context.rcf",sep="")
ShiftedContextFileRCF=paste0(myWorkingDirectory,"/contexts/ShiftedContext.rcf",sep="")
pycommand = paste0("python3 ",fcboscript2,sep=" ")
pycommand = paste0(pycommand,JsonSpecFile2,sep=" ")
pycommand=paste0(pycommand,ObjectssDictionnary,sep=" ")
pycommand=paste0(pycommand,AttributesDictionnary,sep=" ")
pycommand=paste0(pycommand,ContextFile," ",ContextFileRCF,sep=" ")
fcboresult=try(system(pycommand, wait=TRUE))
# shiftContext=paste0("./tool06_shiftContext.pl ",ContextFile, "  1 > ",ShiftedContextFile)
#shiftContextRCF=paste0("./tool06_shiftContext.pl ",ShiftedContextFileRCF, "  1 > ",ShiftedContextFileRCF)
try(system(shiftContext))
#rules <- apriori(sprsM, parameter = list(support = .001))
#inspect(head(sort(rules, by = "lift"), 3))
attributes <- read.table("~/Documents/R_Projects/AnomalyRulesMining/contexts/AttributesDictionnary.txt", quote="\"", comment.char="")
objects<- read.table("~/Documents/R_Projects/AnomalyRulesMining/contexts/ObjectsDictionnary.txt", quote="\"", comment.char="")


ArgItemsets=c(" -alg:apriori",#This is the absolute basic levelwise algorithm for finding FIs. It is efficient for sparse datasets only.
              " -alg:aclose",#A levelwise alg. to find FGs and their closures. Not too efficient since it needs to do lots of intersections for computing the corresponding FCIs.
              " -alg:aprioriclose",#An extension of Apriori that finds FIs and marks FCIs. The FCI-identification gives no overhead.
              " -alg:aprioriinverse",#Finds perfectly rare itemsets. An itemset is a PRI if all its subsets are rare (with the exception of the empty set).
              " -alg:apriorirare",#Finds minimal rare itemsets. An itemset is an mRI if it is rare and all its subsets are frequent. Here you must specify an extra option:
              " -alg:arima",#Arima calls Apriori-Rare to extract mRIs. Then, from this set it restores the family of non-zero rare itemsets. This process is memory extensive.
              " -alg:charm",#A very efficient depth-first alg. to extract FCIs. More efficient on dense datasets.
              " -alg:close",#A levelwise alg. to find FCIs. Note that it was the very first alg. that were implemented in the Coron System.
              " -alg:dcharm",#A diffset implementation of Charm. A very efficient depth-first alg. to extract FCIs. More efficient on dense datasets. It is like Charm but instead of tidsets it uses diffsets. As a result, it performs better and uses less memory. Its output is the same as Charm's.
              " -alg:eclat",#A very efficient depth-first alg. to extract FIs.
              " -alg:zart" #An extension of Pascal to produce FCI/FG pairs. Zart identifies FCIs and associates the FGs to their closures.
)
#add an attribute to specify the database source
Global_Result_Dataframe =data.frame("","","","" , "")
names(Global_Result_Dataframe)=c("id",
                                 "type",#"I, ASS
                                 "algorithm",
                                 "minsup",
                                 "data")
cpt=1
for (j in 1:length(ArgItemsets)){
  MinSup=20
  ChoiceAlg=ArgItemsets[j]
  Results_Frequent_Itemsets=c()
  for (i in 1:5){
    MinSup=20*i
    result=""
    CoronOutPut=list()
    df.CoronOutPut2=data.frame()
    df.CoronOutPut=data.frame()
    opt=" "
    if(j==5)opt=" -nonzero "
    cmd=paste0(getwd(),"/coron-0.8/core01_coron.sh ",
               #ShiftedContextFile,
               ContextFileRCF,
               " ", MinSup,"% -names ", ChoiceAlg,opt,sep="") #>thisresults2.txt
    cat("\n ==================================================Itemsets Mining \n")
    cat("MinSup: ",MinSup,"\n")
    cat("Alg: ",ChoiceAlg,"\n")
    result=try(system(cmd, intern = TRUE,  wait = TRUE)) 
    #x=strsplit(as.character(result[length(result)]),":")
    back=0
    if(j==11)back=10
    NbFI=as.integer(unlist(strsplit(as.character(result[length(result)-back]),":"))[2])
    cat(result,"\n")
    Results_Frequent_Itemsets=c(Results_Frequent_Itemsets,NbFI)
    CoronOutPut=as.list(result[11:length(result)-1])
    df.CoronOutPut=as.data.frame(do.call(rbind, CoronOutPut))
    df.CoronOutPut2=setNames(do.call(rbind.data.frame, CoronOutPut), "FCIs")
    Split <- strsplit(as.character(df.CoronOutPut2$FCIs), " (", fixed = TRUE)
    Intent <- sapply(Split, "[", 1)
    Intent = as.data.frame(do.call(rbind, as.list(Intent)))
    #Intent[ Intent == NA ] <- "NULL"
    Extent <- as.data.frame(do.call(rbind, as.list(sapply(Split, "[", 2))))
    save(result,file=paste0("./Rdata/Results_Itemsets_",trimws(ChoiceAlg),"_Sup_",MinSup,".RData",sep="") )
    save(df.CoronOutPut2,file=paste0("./Rdata/Results_Itemsets_DataFramce_",trimws(ChoiceAlg),"_Sup_",MinSup,".RData",sep="") )
    save(Intent,file=paste0("./Rdata/Results_Itemsets_Antecedent_",trimws(ChoiceAlg),"_Sup_",MinSup,".RData",sep="") )
    save(Extent,file=paste0("./Rdata/Results_Itemsets_Result_",trimws(ChoiceAlg),"_Sup_",MinSup,".RData",sep="") )
    save(result,file=paste0("./Rdata/Results_Itemsets_",trimws(ChoiceAlg),"_Sup_",MinSup,".RData",sep="") )
    rm(result,df.CoronOutPut,df.CoronOutPut2,Split,Intent,Extent)
    #Global_Result_Dataframe[nrow(Global_Result_Dataframe) + 1,] = list("ItemSetsMining",trimws(ChoiceAlg),MinSup,NbFI)
    #Create an empty data frame
    #  de <- list("ItemSetsMining",trimws(ChoiceAlg),MinSup,NbFI)
    # Global_Result_Dataframe = rbind(Global_Result_Dataframe,de, stringsAsFactors=FALSE)
    de<-data.frame(as.character(cpt),"ItemSetsMining",trimws(ChoiceAlg),as.character(MinSup),as.character(NbFI))
    names(de)<-names(Global_Result_Dataframe )
    Global_Result_Dataframe  <- rbind(Global_Result_Dataframe , de)
    rm(de)
    cpt=cpt+1
    cat("==================================================\n")
  }
  
  
}
save(Results_Frequent_Itemsets,file=paste0("./Rdata/Results_Size_Itemsets_.RData",sep="") )
save(Global_Result_Dataframe,file=paste0("./Rdata/Global_Result_Dataframe.RData",sep="") )
write.csv(Global_Result_Dataframe,file=paste0("./Rdata/Global_Result_Dataframe.csv",sep="") )


########################################### AssRules

AssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all -full -examples",sep="") #>thisresults2.txt
AssRulesresult=try(system(AssRulescmd, intern = TRUE,  wait = TRUE)) 
capture.output(AssRulesresult,file=paste0("./contexts/AssociationRulesWithFullDetails_Conf_",MinConf,"_Sup_",MinSup,".txt",sep=""))
CoronOutPut=as.list(AssRulesresult)
df.AssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
save(df.AssocRulesOutPut,file="./contexts/AssociationRulesWithFullDetails.RData")
#cat(AssRulesresult,"\n")

####################################################################################display time cpu
options(max.print=10000000)
listresult=list()
Assruleslistresulttext=list()
RareAssruleslistresulttext=list()
TimeAssruleslistresulttext=list()

x <- matrix(data=NA,byrow=FALSE,ncol = 6, nrow=6)
#for (i in 1:6){for(j in 1:6){cat('\n i \n',i);cat('\n j \n',j); sx[i,j]=rnorm(1, mu, sigma)}}

y <- matrix(data=NA,byrow=TRUE,ncol = 6, nrow=6)
z <- matrix(data=NA,byrow=TRUE,ncol = 6, nrow=6)

Conf=10
Sup=10
DisplayFull=FALSE
for(i in 1:6){
  MinSup=Sup*i
  df.AssocRulesOutPut=data.frame()
  df.RareAssocRulesOutPut=data.frame()
  for(j in 1:6){
    MinConf=Conf*j
    cat('\n ##################sup \n',MinSup)
    cat('\n ###############conf \n',MinConf)
    #listresult=c(listresult,paste("Conf_",MinConf,"_Sup_",MinSup,sep=""))
    
    tic("FreqAsRules" )
    # SoftAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all",sep="") #>thisresults2.txt
    SoftAssRulesresult=try(system(SoftAssRulescmd, intern = TRUE,  wait = TRUE))     
    t=toc()
    
    if(DisplayFull)capture.output(SoftAssRulesresult,file=paste0("./contexts/AssociationRulesOnly_Conf_",MinConf,"_Sup_",MinSup,".txt",sep=""))
    CoronOutPut=as.list(SoftAssRulesresult)
    df.AssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
    NbRules= unlist(strsplit(as.character(df.AssocRulesOutPut$V1[length(df.AssocRulesOutPut$V1)]),":"))[2]
    cat('nb Association Rules',NbRules)
    #Assruleslistresulttext=c(Assruleslistresulttext,NbRules)
    
    x[i,j]=as.integer(gsub(",", '', NbRules, fixed = T))
    cputime=t$toc-t$tic
    #TimeAssruleslistresulttext=c(TimeAssruleslistresulttext,as.character(cputime))
    y[i,j]=as.double(cputime)
    if(DisplayFull){
      AssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all -full -examples",sep="") #>thisresults2.txt
      AssRulesresult=try(system(AssRulescmd, intern = TRUE,  wait = TRUE)) 
      capture.output(AssRulesresult,file=paste0("./contexts/AssociationRulesWithFullDetails_Conf_",MinConf,"_Sup_",MinSup,".txt",sep=""))
    }
    #tic("RarAsRules"  )
    RareAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:BtB -rule:rare ",sep="")
    RareAssRulesresult=try(system(RareAssRulescmd, intern = TRUE,  wait = TRUE)) 
    #toc()  
    if(DisplayFull)capture.output(RareAssRulesresult,file=paste0("./contexts/RareAssociationRules_Conf_",MinConf,"_Sup_",MinSup,".txt",sep=""))
    CoronOutPut=as.list(RareAssRulesresult)
    df.RareAssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
    NbRulesrare= unlist(strsplit(as.character(df.RareAssocRulesOutPut$V1[length(df.RareAssocRulesOutPut$V1)]),":"))[2]
    cat('nb Association Rules',NbRulesrare)
    #RareAssruleslistresulttext=c(RareAssruleslistresulttext,NbRulesrare)
    z[i,j]=as.integer(gsub(",", '', NbRulesrare, fixed = T))
  }
}

#Global_AssRules_Result_Dataframe =data.frame("","","","" , "","","","","" , "","","","","" , 
#                                            "","","","","" , "","","","","" , "","","","","" , "","","","","" , "","")
#de=as.data.frame(listresult) 
#names(de)<-names(Global_AssRules_Result_Dataframe )
#Global_AssRules_Result_Dataframe=rbind(Global_AssRules_Result_Dataframe,de)
#de=as.data.frame(Assruleslistresulttext) 
#names(de)<-names(Global_AssRules_Result_Dataframe )
#Global_AssRules_Result_Dataframe=rbind(Global_AssRules_Result_Dataframe,de)

#de=as.data.frame(TimeAssruleslistresulttext) 
#names(de)<-names(Global_AssRules_Result_Dataframe )
#Global_AssRules_Result_Dataframe=rbind(Global_AssRules_Result_Dataframe,de)

#de=as.data.frame(RareAssruleslistresulttext) 
#names(de)<-names(Global_AssRules_Result_Dataframe )
#Global_AssRules_Result_Dataframe=rbind(Global_AssRules_Result_Dataframe,de)
#write.csv(Global_AssRules_Result_Dataframe,file='./Global_AssRules_Result_Dataframe.csv')

library(gplots)

#Build the matrix data to look like a correlation matrix
#x <- matrix(rnorm(64), nrow=8)
xval <- formatC(x, format="f", digits=2)
pal <- colorRampPalette(c(rgb(0.96,0.96,1), rgb(0.1,0.1,0.9)), space = "rgb")
y_val <- formatC(y, format="f", digits=2)

#Plot the matrix
x_hm <- heatmap.2(x, Rowv=FALSE, Colv=FALSE, dendrogram="none", main="Support X Confidence Heatmap", xlab="Confidence (x10)", 
                  ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=x, notecol="black", notecex=0.8, 
                  keysize = 1.5, margins=c(5, 5))

y_hm<- heatmap.2(y, Rowv=FALSE, Colv=FALSE, dendrogram="none", main="Support X Confidence Heatmap", xlab="Confidence (x10)", 
                 ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=y_val, notecol="black", notecex=0.8, 
                 keysize = 1.5, margins=c(5, 5))

z_hm <- heatmap.2(z, Rowv=FALSE, Colv=FALSE, dendrogram="none", main="Support X Confidence Heatmap", xlab="Confidence (x10)", 
                  ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=z, notecol="black", notecex=0.8, 
                  keysize = 1.5, margins=c(5, 5))

######################################################################
########fair assoc rules
#./core02_assrulex.sh  [switches]  <database>  <min_supp>  <min_conf>  -alg:<alg>  -rule:<rule>
SoftAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all",sep="") #>thisresults2.txt
start_time <- Sys.time()
SoftAssRulesresult=try(system(SoftAssRulescmd, intern = TRUE,  wait = TRUE)) 
end_time <- Sys.time()
cat('proc time: ',end_time - start_time)
capture.output(SoftAssRulesresult,file="./contexts/AssociationRulesOnly.txt")
CoronOutPut=as.list(SoftAssRulesresult)
df.AssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
NbRules= unlist(strsplit(as.character(df.AssocRulesOutPut$V1[length(df.AssocRulesOutPut$V1)]),":"))[2]
cat('nb Association Rules',NbRules)
save(df.AssocRulesOutPut,file="./contexts/AssociationRulesOnly.RData")
cat(SoftAssRulesresult,"\n")

####Rare
RareAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:BtB -rule:rare ",sep="")
RareAssRulesresult=try(system(RareAssRulescmd, intern = TRUE,  wait = TRUE)) 
#The rule is in the FF class, i.e. both sides of the rule are frequent (frequent itemset implies frequent itemset). 
#The rule is closed, i.e. the union of the left and right side forms a closed itemset.
capture.output(RareAssRulesresult,file="./contexts/RareAssociationRules.txt")
CoronOutPut=as.list(RareAssRulesresult)
df.AssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
save(df.AssocRulesOutPut,file="./contexts/RareAssociationRules.RData")
NbRules= unlist(strsplit(as.character(df.AssocRulesOutPut$V1[length(df.AssocRulesOutPut$V1)]),":"))[2]
cat('nb Association Rules',NbRules)
cat(RareAssRulesresult,"\n")

#
data <- read.table(file="./contexts/AssociationRules2.txt", sep="\t", quote="", comment.char="")
try(system("./core03_leco.sh ./contexts/Context.rcf 1 -names -order -alg:dtouch -method:snow -dot -null -uc", intern = TRUE,  wait = TRUE)) 
try(system(" xdot ./graphviz/lattice.dot &"))
pattern="g"
try(system(" ./post01_filterRules.sh ./contexts/AssociationRules.txt \"EXECUTE\" -keep -left"))
#########################################################
ARJsonOutPut=fromJSON('/home/terminator2/Documents/Adapt_Project/Repository/experimental_fca_json/adapt/explore/jsonoutput/implication.json')
ARJsonOutPut2=fromJSON('/home/terminator2/Documents/Adapt_Project/Repository/experimental_fca_json/adapt/explore/jsonoutput/implication.json')
for (i in 1:length(ARJsonOutPut)){
  cat("processing Line \n",i)
  current_rule=ARJsonOutPut[[i]]$rules
  
}








#####display time cpu
m <- microbenchmark("FreqAsRules" = {
  SoftAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all",sep="") #>thisresults2.txt
  SoftAssRulesresult=try(system(SoftAssRulescmd, intern = TRUE,  wait = TRUE))    },
  "RarAsRules" = {
    RareAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:BtB -rule:rare ",sep="")
    RareAssRulesresult=try(system(RareAssRulescmd, intern = TRUE,  wait = TRUE)) 
    
  },times=20 )
library(ggplot2)
autoplot(m)

uq <- function(x) { fivenum(x)[4]}  
lq <- function(x) { fivenum(x)[2]}

y_min <- 0 # min(by(m$time,m$expr,lq))
y_max <- max(by(m$time,m$expr,uq)) * 1.05

p <- ggplot(m,aes(x=expr,y=time)) + coord_cartesian(ylim = c( y_min , y_max )) 
p + stat_summary(fun.y=median,fun.ymin = lq, fun.ymax = uq, aes(fill=expr))












########################################### display the lattice
#./pre02_converter.sh ShiftedContextFile  -of:./shifted.rcf 
#./core03_leco.sh sample/laszlo.rcf 1 -names -order -alg:dtouch -method:snow -dot -ext -dot:ext -null -uc
#cd graphviz/
#  ./compile_gif_leco.sh
#./view_leco.sh


########################################### display
hist(as.numeric(Global_Result_Dataframe$data))

# plot on same grid, each series colored differently -- 
# good if the series have same scale
ggplot(Global_Result_Dataframe, aes(Global_Result_Dataframe$algorithm,Global_Result_Dataframe$data)) + geom_line(aes(colour = Global_Result_Dataframe$algorithm))
w.plot <- melt(Global_Result_Dataframe$data) 

p <- ggplot(aes(x=Global_Result_Dataframe$minsup, colour=variable), data=w.plot)
p + geom_density()
plot(density(as.numeric(Global_Result_Dataframe$data)), type = "n")

#Extent[ Extent == NA ] <- "NULL"
########################################### Other 



ListIntent=as.list(Intent)



d<-as.data.frame(z[!which(z==''),])
r <- with(z, which(z=='', arr.ind=TRUE))
newd <- z[-r, ]

z=stri_list2matrix(CoronOutPut, byrow = TRUE)



out <- strsplit(as.character(CoronOutPut),'()') 
z=do.call(rbind, out)
## v 1.9.6+ 
setDT(CoronOutPut)[, paste0("type", 1:2) := tstrsplit(CoronOutPut, "_and_")]
before


df.aree <- as.data.frame(t(as.data.frame(do.call(rbind, CoronOutPut))))



#x=bind_rows(lapply(CoronOutPut, as.data.frame.list))
#y=as.data.frame(data.table::transpose(CoronOutPut), col.names = names(CoronOutPut[[1]]))
#write.csv(file="./x.csv",CoronOutPut,sep="(")

dt <- data.table(person = c('Sam','Sam','Sam','Greg','Tom','Tom','Tom','Mary','Mary'), 
                 group = c('a','b','e','a','b','c','d','b','d'))
# non-sparse, desirable output
M <- as.matrix(table(dt))
M %*% t(M)

# sparse, binary instead of integer 
rows <- sort(unique(dt$person))
cols <- sort(unique(dt$group))
dimnamesM <- list(person = rows, groups = cols)

sprsM <- sparseMatrix(i = match(dt$person, rows), 
                      j = match(dt$group, cols), dimnames = dimnamesM)


