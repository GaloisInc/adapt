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
######## Construct the Context

args <- commandArgs()
print(args)
currentview=as.character(args[6])
csv_file=as.character(args[7])
rcf_context_file=as.character(args[8])
output_scoring_file=as.character(args[9])
MinSup=as.numeric(args[10])
MinConf=as.numeric(args[11])
benign_input_association_rules_file=as.character(args[12])
violators_file=as.character(args[13])
#gt_file=as.character(args[14])
myWorkingDirectory=getwd()
options(max.print=10000000)

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
 
weight=0.002
cat('\n ############### Association Rule Mining ######################## \n')
  cat("\n processing view : ", currentview)  
   ################ Load objects, attributes and binary matrix from CSV TO suitable variables.
  cat('\n Init. System LOAD FROM  CSV... \n ')
  cat('\n Parameters are: \n ')
  cat('\n CSV \n ',csv_file)
  source("load_csv.r")
  returns_args=load_csv(csv_file)
  if(is.null(returns_args)){stop("context file null")}
  List_Objects=returns_args$List_Objects
  List_Attributes=returns_args$List_Attributes
  AttributesofObject=returns_args$AttributesofObject 
  ObjectOfAttributes=returns_args$ObjectOfAttributes
  
  if(length(List_Objects)==0)stop("\n Length of List_Objects is 0, try other configurations or databases \n")
  if(length(List_Attributes)==0)stop("\n Length of List_Attributes is 0, try other configurations or databases \n")
  cat("\n Nb objects is this context: \n", length(List_Objects))
  cat("\n Nb attributes in this context \n", length(List_Attributes))
  
  
  DisplayFull=FALSE ## IF YES SAVES DATA TO DISK (TXT FILES)
  ContextFileRCF=rcf_context_file
  
  cat("\n reading benign association rules \n")
  if(!file.exists(benign_input_association_rules_file))stop(" benign_input_association_rules_file does not exist")
  
     #paste0("./contexts/",benign_database,"/data/",contextname,"/AssociationRules_Only_Conf_",contextname,"_MinConf_",MinConf,"_Sup_",MinSup,".csv",sep="")
  if (file.size(benign_input_association_rules_file) == 0)  {stop("benign file empty")}
  benign_association_rules  <- read.csv(file=benign_input_association_rules_file) 
  CondBenignRules=benign_association_rules$CondRules
  ResBenignRules=benign_association_rules$ResRules
  if(is.null(benign_association_rules))stop("\n length of BenignRules = 0 \n")
  CondBenignRules=lapply(CondBenignRules, function(x)unlist(strsplit(as.character(x),split=',')))
  ResBenignRules=lapply(ResBenignRules, function(x)unlist(strsplit(as.character(x),split=',')))
  leftListBenign=lapply(CondBenignRules,function(x){paste(x,collapse=',')})
  rightListBenign=lapply(ResBenignRules,function(x){paste(x,collapse=',')})
  BenignAssociationRulesList=paste(leftListBenign,rightListBenign,sep='=>')
  
  
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
      cat("\n Reading violator objects \n")
      
       if (file.size(violators_file) == 0)  stop("\n violator objects file is empty \n")
      violators  <- read.csv(file=violators_file) 
      violatorsList=as.list(as.character(violators$Objects))
      NbViolators=as.integer(length(violatorsList))
      if(NbViolators==0)stop("\n Nb Violator objects = 0\n")
      association_rules=as.character(violators$ViolatedRulesForEachObjectConfidence)
      association_rules=lapply(association_rules,function(x){strsplit(as.character(str_trim(x)),split='|' ,fixed = T)})
      association_rules=lapply(association_rules,function(x){str_trim(unlist(x))})
      association_rules=lapply(association_rules,function(x)x[!x==""])
      
      
      association_rulesLift=as.character(violators$ViolatedRulesForEachObjectLift)
      association_rulesLift=lapply(association_rulesLift,function(x){strsplit(as.character(str_trim(x)),split='|' ,fixed = T)})
      association_rulesLift=lapply(association_rulesLift,function(x){str_trim(unlist(x))})
      association_rulesLift=lapply(association_rulesLift,function(x)x[!x==""])
      
      cat('\n Violator objects scores  processing \n') 
      
      AVGScoresOfObjectsConfidence =""
      AVGScoresOfObjectsLift =""
      ########
      for(Obj in 1:length(violatorsList)){
      cat('\n including feedback to Object:  \n',Obj)
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
        # weightVector=ifelse(weightVector=="FALSE",2^length(which(weightVector==FALSE)),0.7^length(which(weightVector==TRUE)))
        
        
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
      #cat("\n saving results in \n",output_scoring_file)
      df.ObjectsWithScores=data.frame(matrix(ncol = 3, nrow = 0))
      
      colnames (df.ObjectsWithScores)=c("Objects","AVGScoresOfObjectsConfidence",
                                        "AVGScoresOfObjectsLift")
      
      df.ObjectsWithScores=do.call(rbind, Map(data.frame, 
                                              "Objects"=as.list(violatorsList), 
                                              "AVGScoresOfObjectsConfidence"=as.list(AVGScoresOfObjectsConfidence),
                                              "AVGScoresOfObjectsLift"=as.list( AVGScoresOfObjectsLift) 
      ))
      
     # save(df.ObjectsWithScores,file=paste0("./contexts/",database,"/data/",currentview,"/df.FeedBack_benign10x10_",weight,"_Objects_WithScores_Conf_",currentview,
      #                                      "_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      
      write.csv(file=output_scoring_file, 
                df.ObjectsWithScores)
      cat("\n nb processed violator object \n",length(as.list(violatorsList)))
      
      
      
      cat('\n New scores file saved in =======> \n',output_scoring_file)
      write.csv(file=output_scoring_file, df.ObjectsWithScores)
      
      
      
      
      
      #==========================================================end scores processing
   

