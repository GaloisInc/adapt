
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
library(data.table)
library(stringi)
library(stringr)

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
             "Cadets_benign",
             "5dir-benign",
             "5dir-bovia",
             "5dir-pandex",
             "clearscope-benign",
             "clearscope-bovia",
             "clearscop-pandex",
            "trace-bovia" ,
            "trace-pandex",
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



database=as.character(dbkeyspace[11])
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
    List_Objects=returns_args$List_Objects
    List_Attributes=returns_args$List_Attributes
    AttributesofObject=returns_args$AttributesofObject 
    ObjectOfAttributes=returns_args$ObjectOfAttributes
    
    
 
  ###############################################  generate the rules and display the running cpu time
  x <- matrix(data=NA,byrow=FALSE,ncol = 12, nrow=12)
  y <- matrix(data=NA,byrow=TRUE,ncol = 12, nrow=12)
  z <- matrix(data=NA,byrow=TRUE,ncol = 12, nrow=12)
  
  
  if(Consider_List_objects_as_GroundTruth==TRUE){
    gt_file= fromJSON("/home/terminator2/Documents/Adapt_Project/Database/Engagement_2/ground_truth/groundtruthadmuuids/cadets_pandex_webshell.json",simplifyVector = TRUE) 
    gt_Objects=as.list(gt_file)
    gt_Objects=as.character(unlist(gt_Objects))
    cat("processing Objects in GT file Only")
    indx= intersect(  gt_Objects,  List_Objects)
    
    indx=which(List_Objects %in% indx)
    
    
  }

    
  Conf=c(10,20,30,40,50,60,70,80,90,95,97,100)
  Sup=c(10,20,30,40,50,60,70,80,90,95,97,100)
  DisplayFull=FALSE ## IF YES SAVES DATA TO DISK (TXT FILES)
  ContextFileRCF=rcf_context_file
  for(i in 5:11){
    MinSup=Sup[i]
    
    for(j in 5:11){
      #if (i==j) next
      MinConf=Conf[j]
      cat('\n ##################sup \n',MinSup)
      cat('\n ###############conf \n',MinConf)
      #listresult=c(listresult,paste("Conf_",MinConf,"_Sup_",MinSup,sep=""))
      ##-----------------------------------------LESS DETAILS OF RULES
     tic("FreqAsRules" )
      SoftAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all -full ",sep="") #>thisresults2.txt
      SoftAssRulesresult=try(system(SoftAssRulescmd, intern = TRUE,  wait = TRUE))     
      t=toc()
      
      if(DisplayFull)capture.output(SoftAssRulesresult,file=paste0("./contexts/",database,"/data/",currentview,"/AssociationRulesOnly_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".txt",sep=""))
      CoronOutPut=as.list(SoftAssRulesresult)
      CoronOutPut=lapply(CoronOutPut,function(x)x[!is.na(x)])
      CoronOutPut=lapply(CoronOutPut,function(x)x[!x==""])
      CoronOutPut=Filter(length,CoronOutPut)
      CoronOutPut=CoronOutPut[14:length(CoronOutPut)-2]
      CondRules=lapply(strsplit(as.character(CoronOutPut),">"),"[",1)
      CondRules=lapply(CondRules, function(x)gsub("{", '', x, fixed = T))
      CondRules=lapply(CondRules, function(x)gsub("}", '', x, fixed = T))
      CondRules=lapply(CondRules, function(x)gsub("=", '', x, fixed = T))
      CondRules=lapply(CondRules, function(x)gsub(" ", '', x, fixed = T))
      NbRules=as.integer(length(CondRules))
     # cat("\014")  
      cat('nb Association Rules',NbRules) 
      x[i,j]=as.integer(gsub(",", '', NbRules, fixed = T))
      if(x[i,j]==0)next
      cputime=t$toc-t$tic
      #TimeAssruleslistresulttext=c(TimeAssruleslistresulttext,as.character(cputime))
      y[i,j]=as.double(cputime)
      
      ResRules=lapply(strsplit(as.character(CoronOutPut),">"),"[",2)
      values=lapply(strsplit(as.character(ResRules),"}"),"[",2)
      cat('\n saving support \n') 
      Support=lapply(strsplit(as.character(values),";"),"[",1)
      Support=lapply(strsplit(as.character(Support),"\\["),"[",2)
      Support=lapply(Support, function(x)gsub("%", '', x, fixed = T))
      Support=lapply(Support, function(x)gsub("]", '', x, fixed = T))
      Support=as.numeric(Support)
      cat('\n saving confidence \n') 
      
      Confidence=lapply(strsplit(as.character(values),";"),"[",2)   
      Confidence=lapply(strsplit(as.character(Confidence),"\\["),"[",2)
      Confidence=lapply(strsplit(as.character(Confidence),"%"),"[",1)
      Confidence=as.numeric(Confidence)
      #Confidence=lapply(Confidence, function(x)gsub("%", '', x, fixed = T))
      #Confidence=lapply(Confidence, function(x)gsub("\\]", '', x, fixed = T))
      ResRules=lapply(strsplit(as.character(ResRules),"}"),"[",1)
      ResRules=lapply(ResRules, function(x)gsub("{", '', x, fixed = T))
      ResRules=lapply(ResRules, function(x)gsub(" ", '', x, fixed = T))
      
      Lift=lapply(strsplit(as.character(CoronOutPut),">"),"[",2)
      Lift=lapply(strsplit(as.character(Lift),";"),"[",5)
      Lift=lapply(strsplit(as.character(Lift),"="),"[",2)
      Lift=as.numeric(Lift)
      
      cat('\n saving rules db \n') 
      
      df.AssocRulesOutPut=data.frame()
    df.AssocRulesOutPut=do.call(rbind, Map(data.frame, "CondRules"=CondRules, "ResRules"=ResRules,"Support"=Support,"Confidence"=Confidence,"Lift"=Lift))
  write.csv(file=paste0("./contexts/",database,"/data/",currentview,"/AssociationRules_Only_Conf_",currentview,"_MinConf_",MinConf,"_Sup_",MinSup,".csv",sep=""), df.AssocRulesOutPut)
      #
      #
      #min(df.AssocRulesOutPut$Confidence)
      #=======================================================================scoring
      #ent = 0-sum([numpy.log2(1-rules[e]) for e in rules.keys() if e[0]<=setS and not(e[1]<=setS)])
 #     ScoresOfObjects=""
#if(! (Score_Simple_Processing)){
  #      cat('\n sapply \n') 
 #       CondRules=lapply(CondRules, function(x)unlist(strsplit(x,split=',')))
 #       ResRules=lapply(ResRules, function(x)unlist(strsplit(x,split=',')))
        
#FinalScoreList<-lapply(AttributesofObject,function(x){
          
   #       indicator=mapply(function(y,z){any(x %in% y) & !any(x %in%z ) },y=CondRules,z=ResRules)
          
  #        sum(log2(1-Confidence[indicator]/100))
          
 #       }
 #       )
        
 #       unlist(FinalScoreList)
  #      FinalScoreList=lapply(FinalScoreList,function(x)0-x)
#}
      ######     
      ##     # Dif=setdiff(CondRules2,ResRules2)
      #  for(Obj in 1:length(List_Objects)){
      #   score=0.0
      #  cat('Obj \n',Obj)
      # for(Rule in 1:length(CoronOutPut)){
      #  if(any(AttributesofObject[[Obj]] %in% CondRules2[Rule])
      #    &
      #   !(any(AttributesofObject[[Obj]] %in% ResRules2[Rule]))
      #  )
      #  { score=score+(log2(1-Confidence[Rule]/100))  }
      #  
      #}
      #  score=0-score
      #  ScoresOfObjects=c(ScoresOfObjects,score)
      #}
      #ScoresOfObjects=ScoresOfObjects[-1]
      #######

    
      Score_Simple_Processing=TRUE
      if(Score_Simple_Processing){
        cat('\n Score_Simple_Processing \n') 
        TopViolatedRulesForEachObjectConfidence=""
        TopViolatedRulesForEachObjectLift=""
        ViolatedRulesForEachObjectConfidence=""
        ViolatedRulesForEachObjectLift=""
        TopScoreConfidence=""
        TopScoreLift=""
        AVGScoresOfObjectsConfidence =""
        AVGScoresOfObjectsLift =""
        ViolatorObjectList=""
        CondRules=lapply(CondRules, function(x)unlist(strsplit(as.character(x),split=',')))
        ResRules=lapply(ResRules, function(x)unlist(strsplit(as.character(x),split=',')))
        
        for(Obj in 1: length(List_Objects)){
        #if(Consider_List_objects_as_GroundTruth==TRUE) if(Obj !in indx) next
          GlobalscoreConf=0.0
          GlobalscoreLift=0.0
          Max_ScoreConf=0.0
          Max_ScoreLift=0.0
          Max_RuleConf=""
          Max_RuleLift=""
          RuleListConf=""
          RuleListLift=""
          nbViolatedRules=0
          cat('Obj \n',Obj)
          for(Rule in 1:length(CoronOutPut)){
            RulescoreConf=0.0
            RulescoreLift=0.0
            if(length(intersect(AttributesofObject[[Obj]], as.list(unlist(CondRules[Rule]))))>0 & length(intersect(AttributesofObject[[Obj]],as.list(unlist( ResRules[Rule]))) )==0        )
            {##Violation ============================================= stackoverflow
              ##violation with score based on confidence
              nbViolatedRules=nbViolatedRules+1
              RulescoreConf=log2(1-as.double(Confidence[Rule])/100)
              GlobalscoreConf=GlobalscoreConf+RulescoreConf
              left= paste(CondRules[[Rule]],collapse=',' )
              rigth= paste(ResRules[[Rule]],collapse=',' )
              violatedrule=paste(as.character(left),as.character(rigth),sep="=>")
              sc=paste(" Score: ",as.character(-1*RulescoreConf),sep="=")
              violatedrule=paste(violatedrule,sc,sep="")
              RuleListConf=paste(RuleListConf,violatedrule,sep = " | ")
              
              if(-1*RulescoreConf>Max_ScoreConf){
                Max_ScoreConf=-1*RulescoreConf
                Max_RuleConf=violatedrule
              }
              
              
              ##violation with score based on lift
              RulescoreLift= log2(1-as.double(Lift[Rule])/100) 
              GlobalscoreLift=GlobalscoreLift+RulescoreLift
              
              violatedrule=paste(as.character(left),as.character(rigth),sep="=>")
              sc=paste(" Score: ",as.character(-1*RulescoreLift),sep="=")
              violatedrule=paste(violatedrule,sc,sep="")
              RuleListLift=paste(RuleListLift,violatedrule,sep = " | ")
              
              if(-1*RulescoreLift>Max_ScoreLift){
                Max_ScoreLift=-1*RulescoreLift
                Max_RuleLift=violatedrule
              }
              
              
              
            }###================================================endif violation
            
          }##for rules
          if(nbViolatedRules>0) 
          {
            GlobalscoreConf=(0-GlobalscoreConf)/nbViolatedRules
            AVGScoresOfObjectsConfidence=c(AVGScoresOfObjectsConfidence,GlobalscoreConf)
            TopViolatedRulesForEachObjectConfidence=c(TopViolatedRulesForEachObjectConfidence,Max_RuleConf)
            TopScoreConfidence=c(TopScoreConfidence,Max_ScoreConf)
            ViolatedRulesForEachObjectConfidence=c(ViolatedRulesForEachObjectConfidence,RuleListConf)
            
            GlobalscoreLift=(0-GlobalscoreLift)/nbViolatedRules
            AVGScoresOfObjectsLift=c(AVGScoresOfObjectsLift,GlobalscoreLift)
            TopViolatedRulesForEachObjectLift=c(TopViolatedRulesForEachObjectLift,Max_RuleLift)
            TopScoreLift=c(TopScoreLift,Max_ScoreLift)
            ViolatedRulesForEachObjectLift=c(ViolatedRulesForEachObjectLift,RuleListLift)
            ViolatorObjectList=c(ViolatorObjectList,List_Objects[[Obj]])
          }
          
        }#for obj
        
        AVGScoresOfObjectsLift =AVGScoresOfObjectsLift[-1]
        TopViolatedRulesForEachObjectLift=TopViolatedRulesForEachObjectLift[-1]
        TopScoreLift=TopScoreLift[-1]
        ViolatedRulesForEachObjectLift= ViolatedRulesForEachObjectLift[-1]
        
        AVGScoresOfObjectsConfidence=AVGScoresOfObjectsConfidence[-1]
        TopViolatedRulesForEachObjectConfidence=TopViolatedRulesForEachObjectConfidence[-1]
        TopScoreConfidence=TopScoreConfidence[-1]
        ViolatedRulesForEachObjectConfidence= ViolatedRulesForEachObjectConfidence[-1]
        ViolatorObjectList=ViolatorObjectList[-1]
        
      }
      
      if(!Score_Simple_Processing)ScoresOfObjects=FinalScoreList
      # save(AVGScoresOfObjectsLift,file=paste0("./contexts/AVGScoresOfObjectsLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(TopViolatedRulesForEachObjectLift,file=paste0("./contexts/TopViolatedRulesForEachObjectLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(TopScoreLift,file=paste0("./contexts/TopScoreLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(ViolatedRulesForEachObjectLift,file=paste0("./contexts/ViolatedRulesForEachObjectLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(AVGScoresOfObjectsConfidence,file=paste0("./contexts/AVGScoresOfObjectsConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(TopViolatedRulesForEachObjectConfidence,file=paste0("./contexts/TopViolatedRulesForEachObjectConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(TopScoreConfidence,file=paste0("./contexts/TopScoreConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      #  save(ViolatedRulesForEachObjectConfidence,file=paste0("./contexts/ViolatedRulesForEachObjectConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      
      df.ObjectsWithScores=data.frame(matrix(ncol = 9, nrow = 0))
     # colnames (df.ObjectsWithScores)=c("Objects","ViolatedRulesForEachObjectConfidence","AVGScoresOfObjectsConfidence",
                #                        "TopViolatedRulesForEachObjectConfidence","TopScoreConfidence",
                 #                       "ViolatedRulesForEachObjectLift", "AVGScoresOfObjectsLift","TopViolatedRulesForEachObjectLift",
                  #                      "TopScoreLift")
      
      if(length(ViolatorObjectList)==0) {colnames (df.ObjectsWithScores)=c("Objects","ViolatedRulesForEachObjectConfidence","AVGScoresOfObjectsConfidence",
                                                                           "TopViolatedRulesForEachObjectConfidence","TopScoreConfidence",
                                                                           "ViolatedRulesForEachObjectLift", "AVGScoresOfObjectsLift","TopViolatedRulesForEachObjectLift",
                                                                           "TopScoreLift")}
      else{
      df.ObjectsWithScores=do.call(rbind, Map(data.frame, 
                                              "Objects"=as.list(ViolatorObjectList), 
                                              "ViolatedRulesForEachObjectConfidence"=as.list(ViolatedRulesForEachObjectConfidence),
                                              "AVGScoresOfObjectsConfidence"=as.list(AVGScoresOfObjectsConfidence),
                                              "TopViolatedRulesForEachObjectConfidence"= as.list(TopViolatedRulesForEachObjectConfidence),
                                              "TopScoreConfidence"=as.list(TopScoreConfidence),
                                              #
                                              "ViolatedRulesForEachObjectLift"=as.list(ViolatedRulesForEachObjectLift),
                                              "AVGScoresOfObjectsLift"=as.list( AVGScoresOfObjectsLift),
                                              "TopViolatedRulesForEachObjectLift"=as.list(TopViolatedRulesForEachObjectLift),
                                              "TopScoreLift"= as.list(TopScoreLift)
      ))
      }
      
      save(df.ObjectsWithScores,file=paste0("./contexts/",database,"/data/",currentview,"/df.Objects_WithScores_Conf_",currentview,
                                            "_",MinConf,"_Sup_",MinSup,".RData",sep=""))
      
      write.csv(file=paste0("./contexts/",database,"/data/",currentview,"/Objects_With_Scores_",currentview,
                            "_Conf_",MinConf,"_Sup_",MinSup,".csv",sep=""), 
                df.ObjectsWithScores)
      cat("\n nb violator object \n",length(as.list(ViolatorObjectList)))
      
      
      #      Confidence=as.numeric(Confidence)
      #      FinalScoreList<-lapply(AttributesofObject,function(x){
      #          indicator=sapply(CondRules2,function(y){(x %in% y) })
      #          sum(log2(1- Confidence[indicator] /100))
      #  
      #}      )
      #      indicator=mapply(function(y,z){setdiff(CondRules2,ResRules2) },y=CondRules2,z=ResRules2)
      # CondRules2=lapply(CondRules, function(x)unlist(strsplit(x,split=',')))
      #ResRules2=lapply(ResRules, function(x)unlist(strsplit(x,split=',')))
      # Dif=setdiff(CondRules2,ResRules2)
      #for(Obj in 1:length(List_Objects)){
      # score=0.0
      #cat('Obj \n',Obj)
      #for(Rule in 1:length(CoronOutPut)){
      #  if(any(AttributesofObject[[Obj]] %in% CondRules2[Rule])
      #     &
      #     !(any(AttributesofObject[[Obj]] %in% ResRules2[Rule]))
      #  )
      #  { score=score+(log2(1-Confidence[Rule]/100))  }
      #  
      #}
      #score=0-score
      #  ScoresOfObjects=c(ScoresOfObjects,score)
      #}
      #
      
      #
      #
      #
      #=========
      #    List1=AttributesofObject
      #    List2=CondRules
      #    List3=ResRules
      #    List4=Confidence
      # Make a list of lists
      #   All_Lists = list(
      #    "List1" = List1,
      #   "List2" = List2,
      #  "List3" = List4
      #)
      #
      # Create a dataframe listing all pairwise combinations of the lists
      #intersect_df <- data.frame(t(combn(names(All_Lists), 2)))
      #
      # Add a new column to this dataframe indicating the length of the intersection
      # between each pair of lists
      #intersect_df$count <- apply(intersect_df, 1, function(r) length(intersect(
      #
      #All_Lists[[r[1]]] ,
      #  as.list(unlist(strsplit(as.character(All_Lists[[r[2]]]),',')) )
      #  
      #)))
      #
      #
      #
      #
      #
      #
      #
      #
      #
      #
      # l=lapply(AttributesofObject,function(x){
      #  score=0.0
      # mapply(function(y,z,zz){
      #  Reduce(intersect, list(x,,c))
      #
      #  if(length(intersect(x,as.list(unlist(strsplit(as.character(y))))))>0
      #    &
      #    length(intersect(x,as.list(unlist(strsplit(as.character(z))))))==0
      #  )
      #    score=score+(log2(1-as.double(zz)/100))          
      #        },y=CondRules,z=ResRules,zz=Confidence)
      
      #})
      #
      #
      #
      #
      #
      #df.AssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
      #NbRules= unlist(strsplit(as.character(df.AssocRulesOutPut$V1[length(df.AssocRulesOutPut$V1)]),":"))[2]
      #Assruleslistresulttext=c(Assruleslistresulttext,NbRules)
      #
      ##----------------------------------------------------------------------------------------------------------------------FULL DETAILS OF RULES
      if(DisplayFull){
        AssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all -full -examples",sep="") #>thisresults2.txt
        AssRulesresult=try(system(AssRulescmd, intern = TRUE,  wait = TRUE)) 
        capture.output(AssRulesresult,file=paste0("./contexts/",database,"/data/",currentview,"/AssociationRulesWithFullDetails_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".txt",sep=""))
      }
      #CoronOutPut=as.list(AssRulesresult)
      #
      #--------------------------------------------------------------------------------------------------------------RARE RULES
      #tic("RarAsRules"  )
      if(COMPUTE_RARE){
      RareAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:BtB -rule:rare  -full",sep="")  #-full -examples",sep="")
      RareAssRulesresult=try(system(RareAssRulescmd, intern = TRUE,  wait = TRUE)) 
      #toc()  
      if(DisplayFull)capture.output(RareAssRulesresult,file=paste0("./contexts/",database,"/data/",currentview,"/Rare_AssociationRules_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".txt",sep=""))
      #####################
      CoronOutPut=as.list(RareAssRulesresult)
      CoronOutPut=lapply(CoronOutPut,function(x)x[!is.na(x)])
      CoronOutPut=lapply(CoronOutPut,function(x)x[!x==""])
      CoronOutPut=Filter(length,CoronOutPut)
      CoronOutPut=CoronOutPut[14:length(CoronOutPut)-2]
      CondRules=lapply(strsplit(as.character(CoronOutPut),">"),"[",1)
      CondRules=lapply(CondRules, function(x)gsub("{", '', x, fixed = T))
      CondRules=lapply(CondRules, function(x)gsub("}", '', x, fixed = T))
      CondRules=lapply(CondRules, function(x)gsub("=", '', x, fixed = T))
      CondRules=lapply(CondRules, function(x)gsub(" ", '', x, fixed = T))
      NbRulesrare=as.integer(length(CondRules))
    cat("\014")  
      cat('nb Association Rules',NbRulesrare) 
      ResRules=lapply(strsplit(as.character(CoronOutPut),">"),"[",2)
      values=lapply(strsplit(as.character(ResRules),"}"),"[",2)
      Support=lapply(strsplit(as.character(values),";"),"[",1)
      Support=lapply(strsplit(as.character(Support),"\\["),"[",2)
      Support=lapply(Support, function(x)gsub("%", '', x, fixed = T))
      Support=lapply(Support, function(x)gsub("]", '', x, fixed = T))
      Confidence=lapply(strsplit(as.character(values),";"),"[",2)   
      Confidence=lapply(strsplit(as.character(Confidence),"\\["),"[",2)
      Confidence=lapply(strsplit(as.character(Confidence),"%"),"[",1)
      
      Lift=lapply(strsplit(as.character(CoronOutPut),">"),"[",2)
      Lift=lapply(strsplit(as.character(Lift),";"),"[",5)
      Lift=lapply(strsplit(as.character(Lift),"="),"[",2)
      Lift=as.numeric(Lift)
      Confidence=as.numeric(Confidence)   
      Support=as.numeric(Support)
      
      #Confidence=lapply(Confidence, function(x)gsub("%", '', x, fixed = T))
      #Confidence=lapply(Confidence, function(x)gsub("\\]", '', x, fixed = T))
      ResRules=lapply(strsplit(as.character(ResRules),"}"),"[",1)
      ResRules=lapply(ResRules, function(x)gsub("{", '', x, fixed = T))
      ResRules=lapply(ResRules, function(x)gsub(" ", '', x, fixed = T))
      df.RareAssocRulesOutPut=data.frame()  
      df.RareAssocRulesOutPut=do.call(rbind, Map(data.frame, "CondRules"=CondRules, "ResRules"=ResRules,"Support"=Support,"Confidence"=Confidence,"Lift"=Lift))
      write.csv(file=paste0("./contexts/",database,"/",currentview,"/Rares_AssociationRules_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".csv",sep=""), df.RareAssocRulesOutPut)
      
      
      
      #CoronOutPut=as.list(RareAssRulesresult)
      #df.RareAssocRulesOutPut=as.data.frame(do.call(rbind, CoronOutPut))
      #NbRulesrare= unlist(strsplit(as.character(df.RareAssocRulesOutPut$V1[length(df.RareAssocRulesOutPut$V1)]),":"))[2]
      #cat('nb Association Rules',NbRulesrare)
      #RareAssruleslistresulttext=c(RareAssruleslistresulttext,NbRulesrare)
   #   z[i,j]=as.integer(gsub(",", '', NbRulesrare, fixed = T))
      }#rare
    } #for j
  }# for i
  
  save(x,file=paste0("./contexts/",database,"/data/",currentview,"/NB_AssociationRules_",currentview,"_Conf_",MinConf,"_Sup_",MinSup,".RData",sep=""))
  
  save(y,file=paste0("./contexts/",database,"/data/",currentview,"/CPU_AssociationRules_",currentview,"_Conf_",MinConf,"_Sup_",MinSup,".RData",sep=""))
  
}  ## for views
  
   ###############################---------------------------------------------------------------------------###display the results
  #Build the matrix data to look like a correlation matrix
  #x <- matrix(rnorm(64), nrow=8)
  xval <- formatC(x, format="f", digits=2)
  pal <- colorRampPalette(c(rgb(0.96,0.96,1), rgb(0.1,0.1,0.9)), space = "rgb")
  y_val <- formatC(y, format="f", digits=2)
  library(gplots)
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
  

