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
currentview <- as.character(args[6])
csv_file <- as.character(args[7])
rcf.context.file <- as.character(args[8])
output.scoring.file <- as.character(args[9])
MinSup <- as.numeric(args[10])
MinConf <- as.numeric(args[11])
benign.input.association.rules.file <- as.character(args[12])
violators.file <- as.character(args[13])
#gt.file=as.character(args[14])
myWorkingDirectory <- getwd()
options(max.print=10000000)

Consider.List.objects.as.GroundTruth <- TRUE
myWorkingDirectory <- getwd()
TRANSFORM.RCF.TO.CSV <- FALSE
LOAD.RCF <- FALSE
LOAD.CSV <- TRUE
Load.Data.From.Neo4j <- FALSE
Score.Simple.Processing <- FALSE
CREATE.RCF.WITH.PYTHON <- FALSE
options(max.print = 10000000)
COMPUTE.RARE <- FALSE
 
weight <- 0.002
cat('\n ############### Association Rule Mining ######################## \n')
  cat("\n processing view : ", currentview)  
   ################ Load objects, attributes and binary matrix from CSV TO suitable variables.
  cat('\n Init. System LOAD FROM  CSV... \n ')
  cat('\n Parameters are: \n ')
  cat('\n CSV \n ',csv_file)
  source("load_csv.r")
  returns.args <- load.csv(csv_file)
  if(is.null(returns.args)){stop("context file null")}
  List.Objects <- returns.args$List_Objects
  List.Attributes <- returns.args$List_Attributes
  AttributesofObject <- returns.args$AttributesofObject 
  ObjectOfAttributes <- returns.args$ObjectOfAttributes
  
  if(length(List.Objects)==0)stop("\n Length of List.Objects is 0, try other configurations or databases \n")
  if(length(List.Attributes)==0)stop("\n Length of List.Attributes is 0, try other configurations or databases \n")
  cat("\n Nb objects is this context: \n", length(List.Objects))
  cat("\n Nb attributes in this context \n", length(List.Attributes))
  
  
  DisplayFull <- FALSE ## IF YES SAVES DATA TO DISK (TXT FILES)
  ContextFileRCF <- rcf.context.file
  
  cat("\n reading benign association rules \n")
  if(!file.exists(benign.input.association.rules.file))stop(" benign.input.association.rules.file does not exist")
  
     #paste0("./contexts/",benign.database,"/data/",contextname,"/AssociationRules.Only.Conf_",contextname,"_MinConf_",MinConf,"_Sup_",MinSup,".csv",sep="")
  if (file.size(benign.input.association.rules.file) == 0)  {stop("benign file empty")}
  benign.association.rules  <- read.csv(file=benign.input.association.rules.file) 
  CondBenignRules <- benign.association.rules$CondRules
  ResBenignRules <- benign.association.rules$ResRules
  if(is.null(benign.association.rules))stop("\n length of BenignRules = 0 \n")
  CondBenignRules <- lapply(CondBenignRules, function(x)unlist(strsplit(as.character(x),split=',')))
  ResBenignRules <- lapply(ResBenignRules, function(x)unlist(strsplit(as.character(x),split=',')))
  leftListBenign <- lapply(CondBenignRules,function(x){paste(x,collapse=',')})
  rightListBenign <- lapply(ResBenignRules,function(x){paste(x,collapse=',')})
  BenignAssociationRulesList <- paste(leftListBenign,rightListBenign,sep='=>')
  
  
      cat('\n ##################sup \n',MinSup)
      cat('\n ###############conf \n',MinConf)
      
      #  cat("\n reading association rules \n")
      # input.association_rules_file =paste0("./contexts/",database,"/data/",contextname,"/AssociationRules_Only_Conf_",contextname,"_",MinConf,"_Sup_",MinSup,".csv",sep="")
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
      
       if (file.size(violators.file) == 0)  stop("\n violator objects file is empty \n")
      violators  <- read.csv(file=violators.file) 
      violatorsList <- as.list(as.character(violators$Objects))
      NbViolators <- as.integer(length(violatorsList))
      if(NbViolators==0)stop("\n Nb Violator objects = 0\n")
      association.rules <- as.character(violators$ViolatedRulesForEachObjectConfidence)
      association.rules <- lapply(association.rules,function(x){strsplit(as.character(str_trim(x)),split='|' ,fixed = T)})
      association.rules <- lapply(association.rules,function(x){str_trim(unlist(x))})
      association.rules <- lapply(association.rules,function(x)x[!x==""])
      
      
      association.rulesLift <- as.character(violators$ViolatedRulesForEachObjectLift)
      association.rulesLift <- lapply(association.rulesLift,function(x){strsplit(as.character(str_trim(x)),split='|' ,fixed = T)})
      association.rulesLift <- lapply(association.rulesLift,function(x){str_trim(unlist(x))})
      association.rulesLift <- lapply(association.rulesLift,function(x)x[!x==""])
      
      cat('\n Violator objects scores  processing \n') 
      
      AVGScoresOfObjectsConfidence  <- ""
      AVGScoresOfObjectsLift  <- ""
      ########
      for(Obj in 1:length(violatorsList)){
      cat('\n including feedback to Object:  \n',Obj)
      #  match(violatorsList[1] ,List_Objects) association_rulesLift
        MixedCurrentRulesList <-  association.rules[Obj]
        MixedCurrentRulesList <- unlist(MixedCurrentRulesList)
        CurrentListRules <- unlist(lapply(strsplit(MixedCurrentRulesList," "),"[",1))
        CurrentScoreRules <- lapply(strsplit(MixedCurrentRulesList," "),"[",3)
        CurrentScoreRules <- lapply(CurrentScoreRules, function(x)gsub("=", '', x, fixed = T))
        CurrentScoreRules <- as.vector(as.numeric(CurrentScoreRules))
        CurrentScoreRules <- replace(CurrentScoreRules, CurrentScoreRules==Inf, 100)
        weightVector  <-  as.list(CurrentListRules %in% BenignAssociationRulesList)
        weightVector <- ifelse(weightVector=="FALSE",1,weight/length(which(weightVector==TRUE)))
        # weightVector=ifelse(weightVector=="FALSE",2^length(which(weightVector==FALSE)),0.7^length(which(weightVector==TRUE)))
        
        
        ResultScoreVector <- weightVector*CurrentScoreRules
        AvgScoreOfCurrentObj <- mean(ResultScoreVector)
        
        MixedCurrentRulesListLift <-  association.rulesLift[Obj]
        MixedCurrentRulesListLift <- unlist(MixedCurrentRulesListLift)
        CurrentScoreRulesLift <- lapply(strsplit(MixedCurrentRulesListLift," "),"[",3)
        CurrentScoreRulesLift <- lapply(CurrentScoreRulesLift, function(x)gsub("=", '', x, fixed = T))
        CurrentScoreRulesLift <- as.vector(as.numeric(CurrentScoreRulesLift))
        ResultScoreVectorLift <- weightVector*CurrentScoreRulesLift
        AvgScoreLiftOfCurrentObj <- mean(ResultScoreVectorLift)
        
        AVGScoresOfObjectsConfidence <- c(AVGScoresOfObjectsConfidence,AvgScoreOfCurrentObj)
        AVGScoresOfObjectsLift <- c(AVGScoresOfObjectsLift,AvgScoreLiftOfCurrentObj)
        
        
      }#for obj
      
      AVGScoresOfObjectsLift  <- AVGScoresOfObjectsLift[-1]
      
      AVGScoresOfObjectsConfidence <- AVGScoresOfObjectsConfidence[-1]
      #cat("\n saving results in \n",output.scoring.file)
      df.ObjectsWithScores <- data.frame(matrix(ncol = 3, nrow = 0))
      
      colnames (df.ObjectsWithScores) <- c("Objects","AVGScoresOfObjectsConfidence",
                                        "AVGScoresOfObjectsLift")
      
      df.ObjectsWithScores <- do.call(rbind, Map(data.frame, 
                                              "Objects"=as.list(violatorsList), 
                                              "AVGScoresOfObjectsConfidence"=as.list(AVGScoresOfObjectsConfidence),
                                              "AVGScoresOfObjectsLift"=as.list( AVGScoresOfObjectsLift) 
      ))
      
     # save(df.ObjectsWithScores,file=paste0("./contexts/",database,"/data/",currentview,"/df.FeedBack.benign10x10_",weight,"_Objects_WithScores_Conf_",currentview,
      #                                      "_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      
      write.csv(file=output.scoring.file, 
                df.ObjectsWithScores)
      cat("\n nb processed violator object \n",length(as.list(violatorsList)))
      
      
      
      cat('\n New scores file saved in =======> \n',output.scoring.file)
      write.csv(file=output.scoring.file, df.ObjectsWithScores)
      
      
      
      
      
      #==========================================================end scores processing
   

