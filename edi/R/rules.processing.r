library("stringr") # load str_split
library("ppls")
rules.processing <- function(AssociationRulesresult){

  rules.nonempty = (length(AssociationRulesresult) > 13)
  CoronOutPut=as.list(AssociationRulesresult)
  CoronOutPut=lapply(CoronOutPut,function(x)x[!is.na(x)])
  CoronOutPut=lapply(CoronOutPut,function(x)x[!x==""])
  CoronOutPut=Filter(length,CoronOutPut)
  CoronOutPut=CoronOutPut[15:length(CoronOutPut)-3]


  Rules.List <- list() 
  
  ### extract rules==
  if (rules.nonempty) {
      for (i in seq(1,  length(CoronOutPut), by=6)){
      Rules.List=c(Rules.List,CoronOutPut[i]);
    }
  }
  
  ###extract positive examples
  
  Positives.List <- list()
  if (rules.nonempty) {
    for (i in seq(3,  length(CoronOutPut), by=6)){
      Positives.List=c(Positives.List,CoronOutPut[i]);
    }
  }
  
  ### extraact objectsOf.Rules
  
  objectsOf.Rules <- Positives.List
  objectsOf.Rules <- str_split(as.character(objectsOf.Rules), ",")
  # Finally use list() with unique() and unlist()
  objectsOf.Rules=lapply(objectsOf.Rules, function(x)gsub(" ", '', x, fixed = T))
  objectsOf.Rules=lapply(objectsOf.Rules, function(x)gsub("[", '', x, fixed = T))
  objectsOf.Rules=lapply(objectsOf.Rules, function(x)gsub("]", '', x, fixed = T))
  
  Positives.List <- str_split(as.character(Positives.List), ",")
  # Finally use list() with unique() and unlist()
  Positives.List=lapply(Positives.List, function(x)gsub(" ", '', x, fixed = T))
  Positives.List=lapply(Positives.List, function(x)gsub("[", '', x, fixed = T))
  Positives.List=lapply(Positives.List, function(x)gsub("]", '', x, fixed = T))
  Positives.List=unique(unlist(Positives.List))
  
  Cond.Rules=lapply(strsplit(as.character(Rules.List),">"),"[",1)
  Cond.Rules=lapply(Cond.Rules, function(x)gsub("{", '', x, fixed = T))
  Cond.Rules=lapply(Cond.Rules, function(x)gsub("}", '', x, fixed = T))
  Cond.Rules=lapply(Cond.Rules, function(x)gsub("=", '', x, fixed = T))
  Cond.Rules=lapply(Cond.Rules, function(x)gsub(" ", '', x, fixed = T))
  NbRulesrare=as.integer(length(Cond.Rules))
  #cat("\014")
  cat('\n nb Rare Association Rules ',NbRulesrare) 
  
  
  Res.Rules=lapply(strsplit(as.character(Rules.List),">"),"[",2)
  values=lapply(strsplit(as.character(Res.Rules),"}"),"[",2)
  Support=lapply(strsplit(as.character(values),";"),"[",1)
  Support=lapply(strsplit(as.character(Support),"\\["),"[",2)
  Support=lapply(Support, function(x)gsub("%", '', x, fixed = T))
  Support=lapply(Support, function(x)gsub("]", '', x, fixed = T))
  Confidence.sc=lapply(strsplit(as.character(values),";"),"[",2)   
  Confidence.sc=lapply(strsplit(as.character(Confidence.sc),"\\["),"[",2)
  Confidence.sc=lapply(strsplit(as.character(Confidence.sc),"%"),"[",1)
  
  Lift.sc=lapply(strsplit(as.character(Rules.List),">"),"[",2)
  Lift.sc=lapply(strsplit(as.character(Lift.sc),";"),"[",5)
  Lift.sc=lapply(strsplit(as.character(Lift.sc),"="),"[",2)
  Lift.sc=lapply(Lift.sc, function(x)gsub(",", '', x, fixed = T))
  Lift.sc=as.numeric(Lift.sc)
  Confidence.sc=as.numeric(Confidence.sc)   
  Support=as.numeric(Support)
  
  
  
  SuppLeft=lapply(strsplit(as.character(Rules.List),">"),"[",2)
  SuppLeft=lapply(strsplit(as.character(SuppLeft),";"),"[",3)
  SuppLeft=lapply(strsplit(as.character(SuppLeft),"\\["),"[",2)
  SuppLeft=lapply(SuppLeft, function(x)gsub("%", '', x, fixed = T))
  SuppLeft=lapply(SuppLeft, function(x)gsub("]", '', x, fixed = T))
  SuppLeft=as.numeric(SuppLeft)
  
  
  
  SuppRight=lapply(strsplit(as.character(Rules.List),">"),"[",2)
  SuppRight=lapply(strsplit(as.character(SuppRight),";"),"[",4)
  SuppRight=lapply(strsplit(as.character(SuppRight),"\\["),"[",2)
  SuppRight=lapply(SuppRight, function(x)gsub("%", '', x, fixed = T))
  SuppRight=lapply(SuppRight, function(x)gsub("]", '', x, fixed = T))
  SuppRight=as.numeric(SuppRight)
  
  
  # Conviction=lapply(strsplit(as.character(Rules.List),">"),"[",2)
  #  Conviction=lapply(strsplit(as.character(Conviction),";"),"[",6)
  #  Conviction=lapply(strsplit(as.character(Conviction),"="),"[",2)
  #  Conviction=as.numeric(Conviction)
  #  Conviction[is.na(Conviction)]=-1
  #  maxConv=max(Conviction)
  #  Conviction[Conviction==-1]=maxConv+1
  
  #   Conviction<-normalize.vector(Conviction)
  Leverage.sc=Support-SuppLeft*SuppRight
  PowerFactor.sc=Support *Confidence.sc/100
  Kulczynski.sc=0.5*(Support /SuppLeft +Support /SuppRight )
  Lerman.sc=(Support -SuppLeft *SuppRight )/(sqrt(SuppLeft *SuppRight ))
  AddedValue.sc=Confidence.sc/100-SuppRight 
  Cosine.sc=  Support / sqrt(SuppLeft *SuppRight )
  ConterExampleRate.sc=(Support  -1+Confidence.sc/100)/Support 
  Loevinger.sc=(Confidence.sc/100-SuppRight )/(1-SuppRight )
  ImbalanceRatio.sc=abs(SuppLeft -SuppRight )/(SuppLeft +SuppRight -Support )
  Jaccard.sc=Support/(SuppLeft+SuppRight-Support)
  Klosgen.sc=sqrt(Support )*(Confidence.sc/100-SuppRight )
  
  
  
  
  
  
  
  
  Confidence.sc=lapply(Confidence.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Confidence.sc=lapply(Confidence.sc, function(x){((as.double(x)-0.00000000000001)) })
  Confidence.sc=normalize.vector(unlist(Confidence.sc))
  
  
  Lift.sc=lapply(Lift.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Lift.sc=lapply(Lift.sc, function(x)as.double(x))
  NormalizedLift.sc=normalize.vector(unlist(Lift.sc))
  
  
  
  Leverage.sc =lapply(Leverage.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Leverage.sc =lapply(Leverage.sc , function(x)as.double(x))
  # Leverage.sc=normalize.vector(unlist(Leverage.sc))
  
  PowerFactor.sc=lapply(PowerFactor.sc, function(x)gsub("NaN", '0', x, fixed = T))
  PowerFactor.sc=lapply(PowerFactor.sc , function(x)as.double(x))
  #PowerFactor.sc=normalize.vector(unlist(PowerFactor.sc))
  
  Kulczynski.sc=lapply(Kulczynski.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Kulczynski.sc=lapply(Kulczynski.sc , function(x)as.double(x))
  #Kulczynski.sc=normalize.vector(unlist(Kulczynski.sc))
  
  Lerman.sc=lapply(Lerman.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Lerman.sc=lapply(Lerman.sc , function(x)as.double(x))
  #Lerman.sc=normalize.vector(unlist(Lerman.sc))
  
  AddedValue.sc=lapply(AddedValue.sc, function(x)gsub("NaN", '0', x, fixed = T))
  AddedValue.sc=lapply(AddedValue.sc , function(x)as.double(x))
  #AddedValue.sc=normalize.vector(unlist(AddedValue.sc))
  
  Cosine.sc=lapply(Cosine.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Cosine.sc=lapply(Cosine.sc , function(x)as.double(x))
  #Cosine.sc=normalize.vector(unlist(Cosine.sc))
  
  ConterExampleRate.sc=lapply(ConterExampleRate.sc, function(x)gsub("NaN", '0', x, fixed = T))
  ConterExampleRate.sc=lapply(ConterExampleRate.sc , function(x)as.double(x))
  # ConterExampleRate.sc=normalize.vector(unlist(ConterExampleRate.sc))
  
  Loevinger.sc=lapply(Loevinger.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Loevinger.sc=lapply( Loevinger.sc, function(x)as.double(x))
  #Loevinger.sc=normalize.vector(unlist(Loevinger.sc))
  
  ImbalanceRatio.sc=lapply(ImbalanceRatio.sc, function(x)gsub("NaN", '0', x, fixed = T))
  ImbalanceRatio.sc=lapply( ImbalanceRatio.sc, function(x)as.double(x))
  #ImbalanceRatio.sc=normalize.vector(unlist(ImbalanceRatio.sc))
  
  Jaccard.sc=lapply(Jaccard.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Jaccard.sc=lapply(Jaccard.sc , function(x)as.double(x))
  # Jaccard.sc=normalize.vector(unlist(Jaccard.sc))
  
  Klosgen.sc=lapply(Klosgen.sc, function(x)gsub("NaN", '0', x, fixed = T))
  Klosgen.sc=lapply(Klosgen.sc , function(x)as.double(x))
  #Klosgen.sc=normalize.vector(unlist(Klosgen.sc))
  
  
  #Confidence.sc=lapply(Confidence.sc, function(x)gsub("%", '', x, fixed = T))
  #Confidence.sc=lapply(Confidence.sc, function(x)gsub("\\]", '', x, fixed = T))
  Res.Rules=lapply(strsplit(as.character(Res.Rules),"}"),"[",1)
  Res.Rules=lapply(Res.Rules, function(x)gsub("{", '', x, fixed = T))
  Res.Rules=lapply(Res.Rules, function(x)gsub(" ", '', x, fixed = T))
  #df.RareAssocRulesOutPut=data.frame()  
  #df.RareAssocRulesOutPut=do.call(rbind, Map(data.frame, "Cond.Rules"=Cond.Rules, "Res.Rules"=Res.Rules,"Support"=Support,"Confidence.sc"=Confidence.sc,"Lift.sc"=Lift.sc))
  #write.csv(file=paste0(path,"/Rares_AssociationRules_",ListofContexts[idcontext],"_Conf_",MinConf,"_Sup_",MinSup,".csv",sep=""), df.RareAssocRulesOutPut)
  #Combination.sc =   rowMeans(cbind(Confidence.sc, Lift.sc,Leverage.sc,PowerFactor.sc,PowerFactor.sc,Kulczynski.sc,Lerman.sc,AddedValue.sc,Cosine.sc,ConterExampleRate.sc,Loevinger.sc,ImbalanceRatio.sc,Jaccard.sc,Klosgen.sc), na.rm=TRUE)
  
#  MaxCombination.sc =  pmax(Confidence.sc, Lift.sc,Leverage.sc,PowerFactor.sc,PowerFactor.sc,Kulczynski.sc,Lerman.sc,AddedValue.sc,Cosine.sc,ConterExampleRate.sc,Loevinger.sc,ImbalanceRatio.sc,Jaccard.sc,Klosgen.sc)
  
  returned.list <- list("Positives.List" = Positives.List, 
                        "Rules.List" = Rules.List,
                        "objectsOf.Rules"= objectsOf.Rules, 
                        "Cond.Rules" = Cond.Rules,
                        "objectsOf.Rules"=objectsOf.Rules,
                        "Res.Rules" = Res.Rules,
                        "Confidence.sc"=Confidence.sc,
                        "Lift.sc" = NormalizedLift.sc,
                        "OriginalLift.sc" = Lift.sc,
                        "Leverage.sc" = Leverage.sc,
                        "PowerFactor.sc" = PowerFactor.sc,
                        "Kulczynski.sc" = Kulczynski.sc,
                        "Lerman.sc" = Lerman.sc,
                        "AddedValue.sc" = AddedValue.sc,
                        "Cosine.sc" = Cosine.sc,
                        "ConterExampleRate.sc" = ConterExampleRate.sc,
                        "Loevinger.sc" = Loevinger.sc,
                        "ImbalanceRatio.sc" = ImbalanceRatio.sc,
                        "Jaccard.sc" = Jaccard.sc,
                        "Klosgen.sc" = Klosgen.sc#,
                   #     "Combination.sc" = Combination.sc,
                    #    "MaxCombination.sc"=MaxCombination.sc
  )
  return(returned.list)
  
}
