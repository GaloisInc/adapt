context_scoring_shell_rare_rules=function(){
  
  
  args <- commandArgs()
  print(args)
  currentview=as.character(args[6])
  csv_file=as.character(args[7])
  rcf_context_file=as.character(args[8])
  output_scoring_file=as.character(args[9])
  MinSup=as.numeric(args[10])
  MinConf=as.numeric(args[11])
  
  myWorkingDirectory=getwd()
  options(max.print=10000000)
  
  cat('\n ############### Rare Rule Mining ######################## \n')
  
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
  
  RareAssRulescmd=paste0(getwd(),"/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ", MinSup,"% ", MinConf,"% -names -alg:BtB -rule:rare  -full",sep="")  #-full -examples",sep="")
  RareAssRulesresult=try(system(RareAssRulescmd, intern = TRUE,  wait = TRUE)) 
  #toc()  
  if(DisplayFull)capture.output(RareAssRulesresult,file=paste0("./contexts/Rare_AssociationRules_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".txt",sep=""))
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
  write.csv(file=paste0("./contexts/Rares_AssociationRules_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".csv",sep=""), df.RareAssocRulesOutPut)
  
  
}