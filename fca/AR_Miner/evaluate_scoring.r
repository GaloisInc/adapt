
######## Construct the Context
context_name='ProcessEvent'
csv_file='./contexts/Context_ProcessEvent.csv'
rcf_file='./contexts/Context_ProcessEvent.rcf'
output_scoring_file='./contexts/Scoring_Of_Context_ProcessEvent.csv'
MinSup='90'
MinConf='90'

args <- commandArgs()
print(args)
currentview='ProcessEvent'
csv_file='./contexts/Context_ProcessEvent.csv'
rcf_context_file=as.character(args[8])
output_scoring_file=as.character(args[9])
MinSup=as.numeric(args[10])
MinConf=as.numeric(args[11])

myWorkingDirectory=getwd()
options(max.print=10000000)

cat('\n ############### Association Rule Mining ######################## \n')

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

ContextFileRCF=rcf_context_file
cat('\n ################## sup \n',MinSup)
cat('\n ############### conf \n',MinConf)
##-----------------------------------------LESS DETAILS OF RULES
cat('\n ----> Calculating Rules \n')
SoftAssRulescmd=paste0("./coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:zart -rule:all -full ",sep="") #>thisresults2.txt
cat(SoftAssRulescmd)
SoftAssRulesresult=try(system(SoftAssRulescmd, intern = TRUE,  wait = TRUE))     
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

cat('nb Association Rules',NbRules) 


ResRules=lapply(strsplit(as.character(CoronOutPut),">"),"[",2)
values=lapply(strsplit(as.character(ResRules),"}"),"[",2)
Support=lapply(strsplit(as.character(values),";"),"[",1)
Support=lapply(strsplit(as.character(Support),"\\["),"[",2)
Support=lapply(Support, function(x)gsub("%", '', x, fixed = T))
Support=lapply(Support, function(x)gsub("]", '', x, fixed = T))
Support=as.numeric(Support)
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


df.AssocRulesOutPut=data.frame()
df.AssocRulesOutPut=do.call(rbind, Map(data.frame, "CondRules"=CondRules, "ResRules"=ResRules,"Support"=Support,"Confidence"=Confidence,"Lift"=Lift))
write.csv(file=paste0("./contexts/AssociationRules_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".csv",sep=""), df.AssocRulesOutPut)
cat('\n Saving Rules \n')
cat('\n Rules file in =======> \n',paste0("./contexts/AssociationRules_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".csv",sep=""))



cat('\n Calculating Scores of the Rules \n')
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

for(Obj in 1:length(List_Objects)){
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
    {##Violation =============================================
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

AVGScoresOfObjectsLift=AVGScoresOfObjectsLift[-1]
TopViolatedRulesForEachObjectLift=TopViolatedRulesForEachObjectLift[-1]
TopScoreLift=TopScoreLift[-1]
ViolatedRulesForEachObjectLift= ViolatedRulesForEachObjectLift[-1]

AVGScoresOfObjectsConfidence=AVGScoresOfObjectsConfidence[-1]
TopViolatedRulesForEachObjectConfidence=TopViolatedRulesForEachObjectConfidence[-1]
TopScoreConfidence=TopScoreConfidence[-1]
ViolatedRulesForEachObjectConfidence= ViolatedRulesForEachObjectConfidence[-1]
ViolatorObjectList=ViolatorObjectList[-1]




# save(AVGScoresOfObjectsLift,file=paste0("./contexts/AVGScoresOfObjectsLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(TopViolatedRulesForEachObjectLift,file=paste0("./contexts/TopViolatedRulesForEachObjectLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(TopScoreLift,file=paste0("./contexts/TopScoreLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(ViolatedRulesForEachObjectLift,file=paste0("./contexts/ViolatedRulesForEachObjectLift_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(AVGScoresOfObjectsConfidence,file=paste0("./contexts/AVGScoresOfObjectsConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(TopViolatedRulesForEachObjectConfidence,file=paste0("./contexts/TopViolatedRulesForEachObjectConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(TopScoreConfidence,file=paste0("./contexts/TopScoreConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))
#  save(ViolatedRulesForEachObjectConfidence,file=paste0("./contexts/ViolatedRulesForEachObjectConfidence_Only_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".RData",sep=""))

df.ObjectsWithScores=data.frame(matrix(ncol = 9, nrow = 0))
colnames (df.ObjectsWithScores)=c("Objects","ViolatedRulesForEachObjectConfidence","AVGScoresOfObjectsConfidence",
                                  "TopViolatedRulesForEachObjectConfidence","TopScoreConfidence",
                                  "ViolatedRulesForEachObjectLift", "AVGScoresOfObjectsLift","TopViolatedRulesForEachObjectLift",
                                  "TopScoreLift")


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

cat('\n Saving Scores of the Rules \n')

write.csv(file=paste0("./contexts/Objects_With_Scores_",currentview,"_",MinConf,"_Sup_",MinSup,".csv",sep=""), df.ObjectsWithScores)
#  write.csv(file=output_scoring_file, df.ObjectsWithScores)
 



