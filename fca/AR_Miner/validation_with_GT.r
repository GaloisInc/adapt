validation_with_gt=function(csv_file,input_scoring_file,gt_file)
{
  
  dbkeyspace=c("Cadet_Pandex",
               "Cadet-Bovia",
               "Cadets_benign",
               "5dir-benign",
               "5dir-bovia",
               "5dir-pandex",
               "cleqrcope-benign",
               "clearscope-bovia",
               "clearscop-pandex",
               "trace-bovia" ,
               "trace-pandex",
               "Trace_Benign"
  )
  
  Listviews=c("ProcessEvent",
  "ProcessEventExec",
    "ProcessNetflow",
    "ProcessByIPPort",
    "FileEventType",
    "FileEventExec",
    "FileEvent"
  )
  
  database=as.character(dbkeyspace[6])
  contextname=as.character(Listviews[2])
  ################################################
  library(plyr)
  library('RJSONIO')
  Conf=c(50,60,70,80,90,95,97)
  Sup=c(50,60,70,80,90,95,97)
  
  csv_file=paste0("./contexts/",database,"/Context_",contextname,".csv",sep="")
  cat('\n Init System LOAD FROM  CSV... \n ')
  cat('\n Parameters are: \n ')
  cat('\n CSV \n ',csv_file)
  source("load_csv.r")
  returns_args=load_csv(csv_file)
  List_Objects=returns_args$List_Objects
  List_Attributes=returns_args$List_Attributes
  AttributesofObject=returns_args$AttributesofObject 
  ObjectOfAttributes=returns_args$ObjectOfAttributes
 gt_file= fromJSON("/home/terminator2/Documents/Adapt_Project/Database/Engagement_2/ground_truth/groundtruthadmuuids/cadets_bovia_webshell.json",simplifyVector = TRUE) 
                   #cadets_pandex_drakon1.json",simplifyVector = TRUE) 
#                   #cadets_pandex_webshell.json",simplifyVector = TRUE) 
    #gt_file=read.csv('/home/terminator2/Documents/Adapt_Project/Database/Engagement_2/ground_truth/CADETS/labeled_ground_truth/drakon1.csv') cadets_pandex_drakon1.json
   gt_Objects=as.list(gt_file)
     #gt_Objects=unique(as.list(gt_file$gt_object))
  #####rank over confidence's score

  length(gt_Objects)
  
  List_Objects=as.character(unlist(List_Objects))
  gt_Objects=as.character(unlist(gt_Objects))
  
  
  df.TrueAttackObjectsRules <- data.frame(matrix(ncol = 5, nrow = 0))  
  colnames (df.TrueAttackObjectsRules)=c("Sup",
                                         "Conf",
                                         "Attack_Objects",
                                         "Attack_Rules",
                                         "TopScoreConfidence")
  SupList=list()
  ConfList=list()
  AttackList=list()
  AttackRules=list()
  
  
  MatrixTP<- matrix(data=NA,byrow=TRUE,ncol = 11 , nrow=11)
  MatrixFP<- matrix(data=NA,byrow=TRUE,ncol = 11 , nrow=11)
  MatrixTN<- matrix(data=NA,byrow=TRUE,ncol = 11, nrow=11)
  MatrixFN <- matrix(data=NA,byrow=TRUE,ncol = 11, nrow=117)
  MatrixPrec<- matrix(data=NA,byrow=TRUE,ncol = 11, nrow=11)
  MatrixRec<- matrix(data=NA,byrow=TRUE,ncol = 11, nrow=11)
  MatrixFscore<- matrix(data=NA,byrow=TRUE,ncol = 11, nrow=11)
  
  for (i in 1:length(Sup)){
    sup=Sup[i]
    for(j in 1:length(Conf)){
      #if((i==2)&(j==1)) next
      conf=Conf[j]
      cat("\n ===============\n")
      cat("\n Sup: ",sup)
      cat("\n Conf: ",conf)
      input_scoring_file =paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_",contextname,"_Conf_",conf,"_Sup_",sup,".csv",sep="")
      Objects_With_Scores   <- read.csv(file=input_scoring_file) 
      Objects_With_Scores =arrange(Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))
      ObjectViolator=as.list(Objects_With_Scores$Objects)
      ObjectViolator=as.character(unlist(ObjectViolator))
      
      length(ObjectViolator) 
 
      
      
  TP=   intersect(ObjectViolator,gt_Objects) 
  cat("\n TP :",length(TP))
  if(length(TP)>0){
    #cat("\n True positives:\n")
    #cat(as.character(unlist(TP)))
    index=match(ObjectViolator,   as.character(unlist(TP)) )
   # ObjectViolator[which(!is.na(index))]
  # w
    #
   # 
    #ggplot() +
     # geom_vline(xintercept=2, color="red") +
      #geom_vline(xintercept=10, color="red") +
      #geom_vline(xintercept=length(ObjectViolator), color="white") 
    
    
    #cat("\n Having these rules: \n" )
    print(unlist(Objects_With_Scores$ViolatedRulesForEachObjectConfidence[which(!is.na(index))]))
    SupList=c(SupList,sup)
    ConfList=c(ConfList,conf)
    AttackList=c(AttackList,paste(TP,collapse=" | "))
    tmp=Objects_With_Scores$ViolatedRulesForEachObjectConfidence[which(!is.na(index))]
    AttackRules=c(AttackRules,paste(tmp,collapse=" | "))
                                         }
  FP=setdiff(ObjectViolator,TP)
  cat("\n FP :",length(FP))
  TN=setdiff(List_Objects,ObjectViolator)
  cat("\n TN :",length(TN))
  FN=setdiff(gt_Objects,TP)
  cat("\n FN :",length(FN))
  Prec=length(TP)/(length(TP)+length(FP))
  cat("\n Prec: ",Prec)
  Rec=length(TP)/(length(TP)+length(FN))
  cat("\n Rec: ",Rec)
  F1Score=2*Prec*Rec/(Prec+Rec)
  cat("\n F1-Score: ",F1Score)
  
  MatrixTP[i,j]=length(TP)
  MatrixFP[i,j]=length(FP)
  MatrixTN[i,j]=length(TN)
  MatrixFN[i,j]=length(FN)
  MatrixPrec[i,j]=Prec
  MatrixRec[i,j]=Rec
  MatrixFscore[i,j]=F1Score
  
  
    }}
  #===
  
 
  df.TrueAttackObjectsRules=do.call(rbind, Map(data.frame, 
                                               "Sup"=as.list(SupList),
                                               "Conf"=as.list(ConfList),
                                               "Attack_Objects"=as.list(AttackList),
                                               "Attack_Rules"=as.list(AttackRules) ))
  
  
  write.csv(file=paste0("./contexts/",database,"/data/",currentview,"/TrueAttackObjectsRules_cadets_bovia_webshell_",currentview,
                       "_.csv",sep=""), df.TrueAttackObjectsRules)
  #####################plot metrics matrices
  require("gplots")
  venn(list(first.vector = TP, second.vector = FN))
  pal <- colorRampPalette(c(rgb(0.96,0.96,1), rgb(0.1,0.1,0.9)), space = "rgb")
  
  xval <- formatC(MatrixTP, format="f", digits=5)
  x_hm <-try(heatmap.2(MatrixTP, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=  "MatrixTP" , 
                       xlab=" Sup ", 
                       ylab= " Conf ", col=pal, tracecol="#303030", 
                       trace="none", cellnote=MatrixTP, notecol="black", notecex=0.8, 
                       keysize = 1.5, margins=c(5, 5)))    
  
  
  
  
  ################################################################## stats
  ##calculate min max avg
  ##calculate nb detected objects
  

 
  
  MatrixNbViolator<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  MatrixavgConf<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  MatrixavgLift<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  MatrixmaxConf<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  MatrixmaxLift<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  MatrixminConf<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  MatrixminLift<- matrix(data=NA,byrow=TRUE,ncol = 7, nrow=7)
  SharedViolatedObjects=list()
  
  pal <- colorRampPalette(c(rgb(0.96,0.96,1), rgb(0.1,0.1,0.9)), space = "rgb")
  Init_Objects_With_Scores_File =read.csv(file=paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_",contextname,"_Conf_50_Sup_50.csv",sep=""))
  SharedViolatedObjects=as.list(Init_Objects_With_Scores_File$Objects)
   for (i in 1:length(Sup)){
    sup=Sup[i]
    for(j in 1:length(Conf)){
      #if((i==2)&(j==1)) next
      conf=Conf[j]
      cat("\n ===============\n")
      cat("\n Sup: ",sup)
      cat("\n Conf: ",conf)
      
      input_scoring_file =paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_",contextname,"_Conf_",conf,"_Sup_",sup,".csv",sep="")
      Objects_With_Scores   <- read.csv(file=input_scoring_file)
      Objects_With_Scores =arrange(Objects_With_Scores,desc(AVGScoresOfObjectsConfidence))
      ObjectViolator=as.list(Objects_With_Scores$Objects)
      nbViolator=length(ObjectViolator)
      
      minConf=min(Objects_With_Scores$AVGScoresOfObjectsConfidence)
      maxConf=max(Objects_With_Scores$AVGScoresOfObjectsConfidence)
      avgConf=mean(Objects_With_Scores$AVGScoresOfObjectsConfidence)
      
      minLift=min(Objects_With_Scores$AVGScoresOfObjectsLift)
      maxLift=max(Objects_With_Scores$AVGScoresOfObjectsLift)
      avgLift=mean(Objects_With_Scores$AVGScoresOfObjectsLift)
      
      MatrixNbViolator[i,j]<- nbViolator
      MatrixavgConf[i,j]<-avgConf
      MatrixavgLift[i,j]<- avgLift
      MatrixmaxConf[i,j]<- maxConf
      MatrixmaxLift[i,j]<- maxLift
      MatrixminConf[i,j]<- minConf
      MatrixminLift[i,j]<- minLift
      
      cat("\n Top-5 Violator Objects: \n" )
      print(unlist(ObjectViolator[1:5]))
      cat("\n \n")
      cat("\n Having these rules: \n" )
      print(unlist(Objects_With_Scores$TopViolatedRulesForEachObjectConfidence[1:5]))
      cat("\n \n")
       SharedViolatedObjects=intersect(as.character(unlist(SharedViolatedObjects)),   as.character(unlist(ObjectViolator)) )
       SharedViolatedObjects=unique(SharedViolatedObjects)
         #Reduce(intersect, list(SharedViolatedObjects,ObjectViolator))
    }
   }
  
  save(SharedViolatedObjects,  file=paste0("./contexts/",database,"/data/",contextname,"/SharedViolatedObjects_",contextname,"_.RData",sep=""))
  write.csv(SharedViolatedObjects,file=paste0("./contexts/",database,"/data/",contextname,"/SharedViolatedObjects_",contextname,"_.csv",sep=""))
  ########################################################################
   ###plot of the avg,min,max matrices
  library(gplots)
  #MatrixNbViolator[i,j]<- nbViolator
  #MatrixavgConf[i,j]<-avgConf
  #MatrixavgLift[i,j]<- avgLift
  #MatrixmaxConf[i,j]<- maxConf
  #MatrixmaxLift[i,j]<- maxLift
  #MatrixminConf[i,j]<- minConf
  #MatrixminLift[i,j]<- minLift
  xval <- formatC(MatrixNbViolator, format="f", digits=2)
  jpeg(file=paste0("./img/im_assocrules_",currentview,"_conf_",Conf,"_sup_",Sup,"_.jpg",sep=""))
  x_hm <-try(heatmap.2(MatrixNbViolator, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=  "MatrixNbViolator" , 
                       xlab=" Conf ", 
                       ylab= " Sup ", col=pal, tracecol="#303030", 
                       trace="none", cellnote=MatrixNbViolator, notecol="black", notecex=0.8, 
                       keysize = 1.5, margins=c(5, 5)))    
  dev.off() 
  ###avg lift
  yval <- formatC(y, format="f", digits=2)
  jpeg(file=paste0("./img/im_assocrules_",currentview,"_conf_",Conf,"_sup_",Sup,"_.jpg",sep=""))
  y_hm <-try(heatmap.2(y, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=  "AVG scores (Lift)" , xlab=Sup, 
                       ylab= Conf, col=pal, tracecol="#303030", trace="none", cellnote=yval, notecol="black", notecex=0.8, 
                       keysize = 1.5, margins=c(5, 5)))    
  dev.off() 
  
  
  
  ####################trajectory analysis and shared objects disparity
  library(plyr)
  length(SharedViolatedObjects)
  require(data.table)
 # Top10SharedObject=SharedViolatedObjects[1:100]
  Top10SharedObject=SharedViolatedObjects
    df <- data.frame(IDofTopSharedObject=c(1:length(SharedViolatedObjects)) )       #100))
  
  
  
  
  for (i in 1:length(Sup)){
    sup=Sup[i]
    for(j in 1:length(Conf)){
      if((i==2)&(j==1)) next
      conf=Conf[j]
      cat("\n ===============\n")
      cat("\n Sup: ",sup)
      cat("\n Conf: ",conf)
      
      input_scoring_file =paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_",contextname,"_",conf,"_Sup_",sup,".csv",sep="")
      Objects_With_Scores   <- read.csv(file=input_scoring_file)
      Objects_With_Scores =arrange(Objects_With_Scores,desc(AVGScoresOfObjectsConfidence))
      ObjectViolator=as.list(Objects_With_Scores$Objects)
       
      index=match(Top10SharedObject,   as.character(unlist(ObjectViolator)) )
      name=paste0("RankedList_",sup,"X",conf,sep="")
      df[name]  <-index
      #Reduce(intersect, list(SharedViolatedObjects,ObjectViolator))
    }
  }
  
  #dff=df[,c(1,2,3,4,5, 6,7,8,9,10)]
  save(df,file=paste0("./contexts/",database,"/data/",contextname,"/df.SharedViolatedObjects_",contextname,"_",conf,"_Sup_",sup,".RData",sep=""))  
  df=get(load("/home/terminator2/Documents/Adapt_Project/Repository/AR_Rule_Mining_Coron/adapt/fca/AR_Miner/contexts/Cadet_Pandex/data/ProcessEvent/df.Shared_Objects_With_Scores_ProcessEvent_97_Sup_97.RData"))
  dff=df
  library(reshape2)
  library(ggplot2)
  dff <- melt(dff) 
  dff$rowid <- 1:length(Top10SharedObject)
  ggplot(dff, aes(variable, value, group=factor(rowid))) + geom_line(aes(color=factor(rowid),linetype=factor(rowid))  )+      # Thicker line
    geom_point(aes(shape=factor(rowid)),   # Shape depends on cond
               size = 4) 
  
  
  geom_line(aes(linetype=cond), # Line type depends on cond
            size = 1.5)
  
  
  #######################################################################
  ### tests of the ranking plot
  names(dff)
  plot(dff$IDofTopSharedObject ,dff)
    require(ggplot2)
    require(reshape2)
  melted=melt(dff,id.vars='IDofTopSharedObject')
  ggplot(melted,aes(x=factor(IDofTopSharedObject),y=value,color=factor(variable),group=factor(variable)))+
    geom_line()+xlab('IDofTopSharedObject')+guides(color=guide_legend("Series"))+
    labs(title="Insert Title Here")
  
  xyplot(factor(dff$IDofTopSharedObject)~dff$RankedList_50X70,
       data=dff,type='l',auto.key=T)
  
  dff <- melt(dff ,  id.vars = dff$IDofTopSharedObject, variable.name = 'series')
    # plot on same grid, each series colored differently -- 
    # good if the series have same scale
    ggplot(dff , aes(time,value)) + geom_line(aes(colour = series))
  
    # or plot on different plots
    ggplot(dff, aes(time,value)) + geom_line() + facet_grid(series ~ .)
  
    dff <- data.frame(time = 1:10,
                     a = cumsum(rnorm(10)),
                     b = cumsum(rnorm(10)),
                     c = cumsum(rnorm(10)))
    dff <- melt(dff ,  id.vars = 'time', variable.name = 'series')
  
    # plot on same grid, each series colored differently -- 
    # good if the series have same scale
    ggplot(dff, aes(time,value)) + geom_line(aes(colour = series))
    
    # or plot on different plots
    ggplot(df, aes(time,value)) + geom_line() + facet_grid(series ~ .)
    
    
  ####################################################################
  ###avg conf

  #############################################################
  library(fields)
  image.plot(matrix(rnorm(25),5,5))
  
  library(corrplot)
  corrplot(matrix(rnorm(25),5,5), method = "number") 
  
  
  
  
  library(reshape2)
  m=matrix(rnorm(25),5,5)
  dat <- melt( (m))
  
  library(ggplot2)
  p <- ggplot(data =  dat, aes(x = Var2, y = Var1)) +
    geom_tile(aes(fill = value), colour = "white") +
    geom_text(aes(label = sprintf("%1.2f",value)), vjust = 1) +
    scale_fill_gradient(low = "white", high = "steelblue")
  plot(p)
  
  
  x=matrix(rnorm(25),7,7)
  xval <- formatC(x, format="f", digits=2)
  pal <- colorRampPalette(c(rgb(0.96,0.96,1), rgb(0.1,0.1,0.9)), space = "rgb")
  y_val <- formatC(y, format="f", digits=2)  
  x_hm <-try(heatmap.2(x, Rowv=FALSE, Colv=FALSE, dendrogram="none", main=  "Supp X Conf Heatmap FCRules  " , xlab="Confidence (x10)", 
                       ylab="Support (x10)", col=pal, tracecol="#303030", trace="none", cellnote=xval, notecol="black", notecex=0.8, 
                       keysize = 1.5, margins=c(5, 5)))                     
  ########################
  library(gplots)
  library("reshape2")
  library("ggplot2")
   
  
  library(pheatmap)
  pheatmap(m, cluster_row = FALSE, cluster_col = FALSE, color=gray.colors(2,start=1,end=0))
  ########################
  
  library(tidyverse)
  
  # Fake data
  set.seed(2)
  bc = data.frame(x=cumsum(0 + runif(10, 0.05, 0.1)),
                  w=seq(1:10),
                  num = sample(0:9, 10, replace=TRUE))
  bc$xpos = seq(min(bc$x)+0.15,max(bc$x)-0.15,length=nrow(bc))
  
  ggplot(bc) +
    geom_rect(aes(xmin=x - 0.5*w, xmax=x + 0.5*w, ymin=0, ymax=1), 
              show.legend=FALSE, fill="black") +
   # geom_label(aes(label=num, x=xpos, y=0.02), 
    #           label.size=0, label.padding=unit(0.2, "cm"), label.r=unit(0, "cm")) +
    coord_fixed(0.5) +
    theme_void()
  
  ##############################
  
  barcode = function(x, w, num) {
    
    bc = data.frame(x, w, num)
    bc$xpos = seq(min(bc$x)+0.15, max(bc$x)-0.15, length=nrow(bc))
    
    ggplot(bc) +
      geom_rect(aes(xmin=x - 0.5*w, xmax=x+0.5*w, ymin=0, ymax=1), 
                show.legend=FALSE, fill="black") +
      geom_label(aes(label=num, x=xpos, y=0.02), 
                 label.size=0, label.padding=unit(0.2, "cm"), label.r=unit(0, "cm")) +
      coord_fixed(0.5) +
      theme_void()
  }
  
  barcode(x=cumsum(0 + runif(10, 0.05, 0.1)),
          w=runif(10,0.01,0.05),
          num=sample(0:9, 10, replace=TRUE))
  
  ######################
  library(limma)
  stat <- rnorm(100)
  sel <- 1:10
  sel2 <- 11:20
  stat[sel] <- stat[sel]+1
  stat[sel2] <- stat[sel2]-1
  
  # One directional
  barcodeplot(stat, index = sel)
  
  # Two directional
  barcodeplot(stat, index = sel, index2 = sel2)
  
  ############
  
  
  library (ROCR);
  #...
  
  #y <- ... # logical array of positive / negative cases
  #predictions <- ... # array of predictions
  
  #pred <- prediction(predictions, y);
  
  # Recall-Precision curve             
 # RP.perf <- performance(pred, "prec", "rec");
  
  #plot (RP.perf);
  
  # ROC curve
  #ROC.perf <- performance(pred, "tpr", "fpr");
  #plot (ROC.perf);
  
  # ROC area under the curve
  #auc.tmp <- performance(pred,"auc");
  #auc <- as.numeric(auc.tmp@y.values)
  
  
}

