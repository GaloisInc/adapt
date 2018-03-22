
######## Construct the Context
source("check_packages.r") 
check_packages()
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
library(stringr)
library(data.table)
library(RNeo4j)
library('RCurl')
library('RJSONIO')
library('plyr')
library(jsonlite)
library(tictoc)
library(gplots)
library(sqldf)
library("tm")
library("SnowballC")
library("wordcloud")
library("RColorBrewer")


Listviews=c( "ProcessEvent",
             "ProcessEventExec",
             "ProcessNetflow",
             "ProcessByIPPort",
             "FileEventType",
             "FileEventExec",
             "FileEvent"
)


dbkeyspace=c("Cadet_Pandex",
             "Cadet-Bovia",
              "5dir-bovia",
             "5dir-pandex",
             "clearscope-bovia",
             "clearscop-pandex",
             "trace-bovia" ,
             "trace-pandex" 
            
)


benign_dbkeyspace=c( 
  "Cadets_benign",
  "5dir-benign",
  
  "clearscope-benign",
  
  "Trace_Benign"
)


Consider_List_objects_as_GroundTruth=TRUE
myWorkingDirectory=getwd()
TRANSFORM_RCF_TO_CSV=FALSE
LOAD_RCF=FALSE
LOAD_CSV=TRUE
Load_Data_From_Neo4j=FALSE
Score_Simple_Processing=FALSE
CREATE_RCF_WITH_PYTHON=FALSE
options(max.print=10000000)
COMPUTE_RARE=FALSE


benign_database=as.character(benign_dbkeyspace[1])
database=as.character(dbkeyspace[1])
weight=0.002
cat('\n ############### Association Rule Mining ######################## \n')
for (k in 1:length(Listviews)){
  
  cat("\n processing view 1: ", as.character(Listviews[k]))  
  
  
  
  currentview=as.character(Listviews[k])
  JsonSpecFile =paste0('../contextSpecFiles/neo4jspec_',currentview,'.json',sep="")
  rcf_context_file=paste0(myWorkingDirectory,"/contexts/",database,"/Context_",currentview,".rcf",sep="")
  csv_file=paste0(myWorkingDirectory,"/contexts/",database,"/Context_",currentview,".csv",sep="")
  
  mainDir=paste0(myWorkingDirectory,"/contexts/",database  ,sep="")
  subDir="data"
  ifelse(!dir.exists(file.path(mainDir, subDir)), dir.create(file.path(mainDir, subDir)), FALSE)
  mainDir=paste0(mainDir,"/",subDir  ,sep="")
  
  subDir=  currentview 
  ifelse(!dir.exists(file.path(mainDir, subDir)), dir.create(file.path(mainDir, subDir)), FALSE)
  #############EXTRACT DATA FROM SERVER AND save context as RCF AND csv files
  if(Load_Data_From_Neo4j){
    cat('\n Init System NEO4J=>RCF... \n ')
    cat('\n Parameters are: \n ')
    cat('\n JSON  \n ',JsonSpecFile)  
    cat('\n RCF \n ',rcf_context_file)
    cat('\n CSV \n ',csv_file)
    source("get_query.r")
    tic("Querying the NEO4J Json DB")
    returns_args=get_query(JsonSpecFile,rcf_context_file,csv_file)
    toc()
    cputime=t$toc-t$tic
    cat('\n Querying the NEO4J DB took \n',as.double(cputime))
    List_Objects=returns_args$List_Objects
    List_Attributes=returns_args$List_Attributes
    AttributesofObject=returns_args$AttributesofObject
  }
  ############Load objects, attributes and binary matrix from RCF TO suitable variables.
  if(LOAD_RCF){
    cat('\n Init System LOAD FROM RCF... \n ')
    cat('\n Parameters are: \n ')
    cat('\n RCF \n ',rcf_context_file)
    source("load_rcf.r")
    returns_args=load_rcf(rcf_context_file)
    List_Objects=returns_args$List_Objects
    List_Attributes=returns_args$List_Attributes
    AttributesofObject=returns_args$AttributesofObject 
    
    
  }
  ############## TRANSFORM RCF TO CSV CONTEXT 
  if(TRANSFORM_RCF_TO_CSV){
    cat('\n Init System RCF=>CSV ... \n ')
    cat('\n Parameters are: \n ')
    cat('\n RCF \n ',rcf_context_file)
    cat('\n CSV \n ',csv_file)
    source("process_rcf_context.r")
    returns_args=process_rcf_context(rcf_context_file,
                                     csv_file)
    List_Objects=returns_args$List_Objects
    List_Attributes=returns_args$List_Attributes
    AttributesofObject=returns_args$AttributesofObject
  }
  
  ####################################################################
  if(CREATE_RCF_WITH_PYTHON){
    myWorkingDirectory=getwd()
    AttributesDictionnary=paste0(myWorkingDirectory,"/contexts/AttributesDictionnary.txt",sep="")
    ObjectssDictionnary=paste0(myWorkingDirectory,"/contexts/ObjectsDictionnary.txt",sep="")
    #JsonSpecFile="/home/terminator2/Documents/Adapt_Project/Repository/experimental_fca/explore/csv/csvspec.json"
    JsonSpecFile2=paste0('./contextSpecFiles/neo4jspec_',currentview,'.json',sep="")
    ContextFile=paste0(myWorkingDirectory,"/contexts/Context.basenum",sep="")
    ShiftedContextFile=paste0(myWorkingDirectory,"/contexts/ShiftedContext.basenum",sep="")
    ContextFileRCF=paste0(myWorkingDirectory,"/contexts/Context_",currentview,".rcf",sep="")
    ShiftedContextFileRCF=paste0(myWorkingDirectory,"/contexts/ShiftedContext.rcf",sep="")
    fcboscript2='./fcbo.py'
    pycommand = paste0("python3 ",fcboscript2,sep=" ")
    pycommand = paste0(pycommand,JsonSpecFile2,sep=" ")
    pycommand=paste0(pycommand,ObjectssDictionnary,sep=" ")
    pycommand=paste0(pycommand,AttributesDictionnary,sep=" ")
    pycommand=paste0(pycommand,ContextFile," ",ContextFileRCF,sep=" ")
    fcboresult=try(system(pycommand, wait=TRUE))
    
    
  }
  
  ################ Load objects, attributes and binary matrix from CSV TO suitable variables.
  
  cat('\n Init System LOAD FROM  CSV... \n ')
  cat('\n Parameters are: \n ')
  cat('\n CSV \n ',csv_file)
  source("load_csv.r")
  returns_args=load_csv(csv_file)
  if(is.null(returns_args)){cat("context file null");next}
  List_Objects=returns_args$List_Objects
  List_Attributes=returns_args$List_Attributes
  AttributesofObject=returns_args$AttributesofObject 
  ObjectOfAttributes=returns_args$ObjectOfAttributes
  
 
  
  Conf=c(10,20,30,40,50,60,70,80,90,95,97,98,100)
  Sup=c(10,20,30,40,50,60,70,80,90,95,97,98,100)
  DisplayFull=FALSE ## IF YES SAVES DATA TO DISK (TXT FILES)
  ContextFileRCF=rcf_context_file
  
  cat("\n reading benign association rules \n")
  benign_input_association_rules_file ="/home/terminator2/Documents/Adapt_Project/Repository/AR_Rule_Mining_Coron/adapt/fca/AR_Miner/contexts/Cadets_benign/data/ProcessEvent/AssociationRules_Only_Conf_ProcessEvent_MinConf_10_Sup_10.csv"
  
  #paste0("./contexts/",benign_database,"/data/",contextname,"/AssociationRules_Only_Conf_",contextname,"_MinConf_",MinConf,"_Sup_",MinSup,".csv",sep="")
  if (file.size(benign_input_association_rules_file) == 0)  next
  benign_association_rules  <- read.csv(file=benign_input_association_rules_file) 
  CondBenignRules=benign_association_rules$CondRules
  ResBenignRules=benign_association_rules$ResRules
  CondBenignRules=lapply(CondBenignRules, function(x)unlist(strsplit(as.character(x),split=',')))
  ResBenignRules=lapply(ResBenignRules, function(x)unlist(strsplit(as.character(x),split=',')))
  leftListBenign=lapply(CondBenignRules,function(x){paste(x,collapse=',')})
  rightListBenign=lapply(ResBenignRules,function(x){paste(x,collapse=',')})
  BenignAssociationRulesList=paste(leftListBenign,rightListBenign,sep='=>')
  
  
  
  for(i in c(5,11)){
    MinSup=Sup[i]
    
    for(j in c(5,11)){
      #if (i==j) next
      MinConf=Conf[j]
      cat('\n ##################sup \n',MinSup)
      cat('\n ###############conf \n',MinConf)
    
    #  cat("\n reading association rules \n")
     # input_association_rules_file =paste0("./contexts/",database,"/data/",contextname,"/AssociationRules_Only_Conf_",contextname,"_",MinConf,"_Sup_",MinSup,".csv",sep="")
      #if (file.size(input_association_rules_file) == 0)  next
      #association_rules  <- read.csv(file=input_association_rules_file) 
      #NbRules=as.integer(length(association_rules$X))
      # cat("\014")  
    #  cat('nb Association Rules',NbRules) 
    #  if(NbRules==0)next
     # CondRules=as.list(association_rules$CondRules)
    #  ResRules=as.list(association_rules$ResRules)
     # Support=as.list(association_rules$Support)
    #  Confidence=as.list(association_rules$Confidence)
     # Lift=as.list(association_rules$Lift)
    #  CondRules=lapply(CondRules, function(x)unlist(strsplit(as.character(x),split=',')))
     # ResRules=lapply(ResRules, function(x)unlist(strsplit(as.character(x),split=',')))
      
   
     
      
      
      
      
      ##=====================================================Processing Scores
      
      #######loading attack objects
      cat("\n reading violator objects \n")
      
      violators_file =paste0("./contexts/",database,"/data/",currentview,"/Objects_With_Scores_",currentview,"_",MinConf,"_Sup_",MinSup,".csv",sep="")
      if (file.size(violators_file) == 0)  next
      violators  <- read.csv(file=violators_file) 
      violatorsList=as.list(as.character(violators$Objects))
      NbViolators=as.integer(length(violatorsList))
      if(NbViolators==0)next
      association_rules=as.character(violators$ViolatedRulesForEachObjectConfidence)
      association_rules=lapply(association_rules,function(x){strsplit(as.character(str_trim(x)),split='|' ,fixed = T)})
      association_rules=lapply(association_rules,function(x){str_trim(unlist(x))})
      association_rules=lapply(association_rules,function(x)x[!x==""])
      
      
      association_rulesLift=as.character(violators$ViolatedRulesForEachObjectLift)
      association_rulesLift=lapply(association_rulesLift,function(x){strsplit(as.character(str_trim(x)),split='|' ,fixed = T)})
      association_rulesLift=lapply(association_rulesLift,function(x){str_trim(unlist(x))})
      association_rulesLift=lapply(association_rulesLift,function(x)x[!x==""])
      
      
     # tmp2=lapply(strsplit(unlist(tmp)," "),"[",1)
      
      
      #
      #tmp2=lapply(tmp,function(x){strsplit(unlist(x),split=' ' ,fixed = T)[[1]][1]})
      
      
      #lst <- list("| EVENT_READ=>EVENT_EXIT Score: =8.28771237954946 | EVENT_READ=>EVENT_FORK Score: =8.0397848661059 | EVENT_CLOSE=>EVENT_EXIT Score: =8.07825901392049 | EVENT_CLOSE=>EVENT_FORK Score: =7.93016037493137 | EVENT_OPEN=>EVENT_EXIT Score: =8.24331826019101 | EVENT_OPEN=>EVENT_FORK Score: =8.0023101606872 | EVENT_LSEEK=>EVENT_EXIT Score: =8.48035745749183 |", 
       #           "| EVENT_READ,EVENT_LSEEK,EVENT_FORK=>EVENT_EXIT Score: =8.96578428466209 | EVENT_READ,EVENT_LSEEK,EVENT_EXIT=>EVENT_FORK Score: =8.42973138442187 |", 
        #          "| EVENT_READ,EVENT_LSEEK,EVENT_FORK=>EVENT_EXIT", "| EVENT_READ,EVENT_LSEEK,EVENT_FORK=>EVENT_EXIT")
      
    #  l=lapply(sapply(lst, function(x)strsplit(x,split='\\|')),
     #          function(x) grep("^.((?!Score:).)*$", x, value=TRUE, perl = TRUE))
     
      
      
      
      
      
     # tmp2=lapply(strsplit(tmp," "),"[",1)
      
      #tmp=lapply(tmp, function(x){trimws(x, "b")})
      #tmp2=lapply(tmp,function(x){grep("^.((?!Score:).)*$", x, value=TRUE, perl = TRUE)})
      
    #  tmp=lapply(sapply(association_rules, function(x)strsplit(x,split='\\|')),
     #                   function(x) grep("^.((?!Score:).)*$", x, value=TRUE, perl = TRUE))
      
      cat('\n Scores  Processing \n') 
  
      AVGScoresOfObjectsConfidence =""
      AVGScoresOfObjectsLift =""
       
      
     # 
      #leftList=lapply(CondRules,function(x){paste(x,collapse=',')})
      #rightList=lapply(ResRules,function(x){paste(x,collapse=',')})
      #originalAssociationRulesList=paste(leftList,rightList,sep='=>')
      
      
      
      
      ########
      for(Obj in 1:length(violatorsList)){
        #if(Consider_List_objects_as_GroundTruth==TRUE) if(Obj !in indx) next
   
        cat('\n Obj \n',Obj)
        
        
      #  match(violatorsList[1] ,List_Objects) association_rulesLift
       MixedCurrentRulesList= association_rules[Obj]
       MixedCurrentRulesList=unlist(MixedCurrentRulesList)
       CurrentListRules=unlist(lapply(strsplit(MixedCurrentRulesList," "),"[",1))
       CurrentScoreRules=lapply(strsplit(MixedCurrentRulesList," "),"[",3)
       CurrentScoreRules=lapply(CurrentScoreRules, function(x)gsub("=", '', x, fixed = T))
       CurrentScoreRules=as.vector(as.numeric(CurrentScoreRules))
       CurrentScoreRules=replace(CurrentScoreRules, CurrentScoreRules==Inf, 100)
       weightVector = as.list(CurrentListRules %in% BenignAssociationRulesList)
       weightVector=ifelse(weightVector=="FALSE",1,weight/length(which(weightVector==TRUE)))
    #   weightVector=ifelse(weightVector=="FALSE",weight*2^length(which(weightVector==FALSE)),weight*0.9^length(which(weightVector==TRUE)))
         # weightVector=ifelse(weightVector=="FALSE",1,weight*0.2*0.09^length(which(weightVector==TRUE)))
      
       ResultScoreVector=weightVector*CurrentScoreRules
       AvgScoreOfCurrentObj=mean(ResultScoreVector)
       
       MixedCurrentRulesListLift= association_rulesLift[Obj]
       MixedCurrentRulesListLift=unlist(MixedCurrentRulesListLift)
       CurrentScoreRulesLift=lapply(strsplit(MixedCurrentRulesListLift," "),"[",3)
       CurrentScoreRulesLift=lapply(CurrentScoreRulesLift, function(x)gsub("=", '', x, fixed = T))
       CurrentScoreRulesLift=as.vector(as.numeric(CurrentScoreRulesLift))
       ResultScoreVectorLift=weightVector*CurrentScoreRulesLift
       AvgScoreLiftOfCurrentObj=mean(ResultScoreVectorLift)
       
       AVGScoresOfObjectsConfidence=c(AVGScoresOfObjectsConfidence,AvgScoreOfCurrentObj)
       AVGScoresOfObjectsLift=c(AVGScoresOfObjectsLift,AvgScoreLiftOfCurrentObj)
       
        
      }#for obj
      
      AVGScoresOfObjectsLift =AVGScoresOfObjectsLift[-1]

      AVGScoresOfObjectsConfidence=AVGScoresOfObjectsConfidence[-1]

    
     cat("\n saving results \n")
    df.ObjectsWithScores=data.frame(matrix(ncol = 3, nrow = 0))
    
    colnames (df.ObjectsWithScores)=c("Objects","AVGScoresOfObjectsConfidence",
                                                                         "AVGScoresOfObjectsLift")
    
      df.ObjectsWithScores=do.call(rbind, Map(data.frame, 
                                              "Objects"=as.list(violatorsList), 
                                              "AVGScoresOfObjectsConfidence"=as.list(AVGScoresOfObjectsConfidence),
                                              "AVGScoresOfObjectsLift"=as.list( AVGScoresOfObjectsLift) 
      ))
  
    
    save(df.ObjectsWithScores,file=paste0("./contexts/",database,"/data/",currentview,"/df.FeedBack_Objects_WithScores_3Formula_",currentview,"_",weight,
                                          "_Conf_",MinConf,"_Sup_",MinSup,".RData",sep=""))
    
    write.csv(file=paste0("./contexts/",database,"/data/",currentview,"/Objects_With_Scores_FeedBack_3Formula_",currentview,"_Conf_",MinConf,"_Sup_",MinSup,".csv",sep=""), 
              df.ObjectsWithScores)
    cat("\n nb violator object \n",length(as.list(violatorsList)))
    
    
      
      
      
      
      
      
      
      #==========================================================end scores processing
    } #for j
  }# for i
  
  
 }  ## for views

 