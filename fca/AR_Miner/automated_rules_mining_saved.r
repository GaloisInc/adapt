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
 library(tictoc)
library(gplots)


######## Construct the Context
fcboscript2='./fcbo.py'
 
Listviews=c("ProcessEventExec",
            "ProcessNetFlow",
            "ProcessEventExec",
            "ProcessByIPPort",
            "FileEventType",
            "FileEventExec",
            "FileEvent")
for (k in 1:length(Listviews)){
  cat("\n processing \n", as.character(Listviews[k]))  
currentview=as.character(Listviews[k])
myWorkingDirectory=getwd()
AttributesDictionnary=paste0(myWorkingDirectory,"/contexts/AttributesDictionnary.txt",sep="")
ObjectssDictionnary=paste0(myWorkingDirectory,"/contexts/ObjectsDictionnary.txt",sep="")
#JsonSpecFile="/home/terminator2/Documents/Adapt_Project/Repository/experimental_fca/explore/csv/csvspec.json"
JsonSpecFile2=paste0('./contextSpecFiles/neo4jspec_',currentview,'.json',sep="")
ContextFile=paste0(myWorkingDirectory,"/contexts/Context.basenum",sep="")
ShiftedContextFile=paste0(myWorkingDirectory,"/contexts/ShiftedContext.basenum",sep="")
ContextFileRCF=paste0(myWorkingDirectory,"/contexts/Context_",currentview,".rcf",sep="")
ShiftedContextFileRCF=paste0(myWorkingDirectory,"/contexts/ShiftedContext.rcf",sep="")
pycommand = paste0("python3 ",fcboscript2,sep=" ")
pycommand = paste0(pycommand,JsonSpecFile2,sep=" ")
pycommand=paste0(pycommand,ObjectssDictionnary,sep=" ")
pycommand=paste0(pycommand,AttributesDictionnary,sep=" ")
pycommand=paste0(pycommand,ContextFile," ",ContextFileRCF,sep=" ")
fcboresult=try(system(pycommand, wait=TRUE))
 
############## Load objects, attributes and binary matrix in suitable variables.
source("process_rcf_context.r")
returns_args=process_rcf_context(paste0(myWorkingDirectory,"/contexts/Context_",currentview,".rcf",sep=""),
                                 paste0(myWorkingDirectory,'/contexts/CSV_context_',currentview,'.csv',sep=""))
List_Objects=returns_args$List_Objects
List_Attributes=returns_args$List_Attributes
AttributesofObject=returns_args$AttributesofObject
#############save context as csv file
source("get_query.r")
get_query()


###############################################display time cpu
options(max.print=10000000)
 

x <- matrix(data=NA,byrow=FALSE,ncol = 9, nrow=9)
 
y <- matrix(data=NA,byrow=TRUE,ncol = 9, nrow=9)
z <- matrix(data=NA,byrow=TRUE,ncol = 9, nrow=9)

Conf=10
Sup=10
DisplayFull=FALSE
ContextFileRCF='./contexts/rcf_context_file.rcf'
for(i in 1:9){
  MinSup=Sup*i
  df.AssocRulesOutPut=data.frame()
  df.RareAssocRulesOutPut=data.frame()
  for(j in 1:9){
    MinConf=Conf*j
    cat('\n ##################sup \n',MinSup)
    cat('\n ###############conf \n',MinConf)
    #listresult=c(listresult,paste("Conf_",MinConf,"_Sup_",MinSup,sep=""))
    
    tic("FreqAsRules" )
    SoftAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all",sep="") #>thisresults2.txt
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

 


#Build the matrix data to look like a correlation matrix
#x <- matrix(rnorm(64), nrow=8)
xval <- formatC(x, format="f", digits=2)
pal <- colorRampPalette(c(rgb(0.96,0.96,1), rgb(0.1,0.1,0.9)), space = "rgb")
y_val <- formatC(y, format="f", digits=2)

#Plot the matrix
jpeg(file=paste0("./images/im_assocrules_",currentview,".jpg",sep=""))

x_hm <-try(heatmap.2(x, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=paste0("Supp X Conf Heatmap FCRules ",currentview,sep=""), xlab="Confidence (x10)", 
                  ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=x, notecol="black", notecex=0.8, 
                  keysize = 1.5, margins=c(5, 5)))
dev.off() 

jpeg(file=paste0("./images/im_cpu_",currentview,".jpg",sep=""))
y_hm<- try(heatmap.2(y, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=paste0("Supp X Conf Heatmap CPU ",currentview,sep=""), xlab="Confidence (x10)", 
                 ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=y_val, notecol="black", notecex=0.8, 
                 keysize = 1.5, margins=c(5, 5)))
dev.off() 
jpeg(file=paste0("./images/im_rarerules_",currentview,".jpg",sep=""))
z_hm <- try(heatmap.2(z, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=paste0("Supp X Conf Heatmap RareRules ",currentview,sep=""), xlab="Confidence (x10)", 
                  ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=z, notecol="black", notecex=0.8, 
                  keysize = 1.5, margins=c(5, 5)))
dev.off()
 
}

