library(grid)
library(ggplot2)
library(gridBase)
library(gridExtra)
library(grid)
library(ggplot2)
library(lattice)
library(latticeExtra)
gt_file= fromJSON("/home/terminator2/Documents/Adapt_Project/Database/Engagement_2/ground_truth/groundtruthadmuuids/fivedirections_bovia_simple.json",simplifyVector = TRUE) 
#cadets_pandex_drakon1.json",simplifyVector = TRUE) 
#                   #cadets_pandex_webshell.json",simplifyVector = TRUE) 
#gt_file=read.csv('/home/terminator2/Documents/Adapt_Project/Database/Engagement_2/ground_truth/CADETS/labeled_ground_truth/drakon1.csv') cadets_pandex_drakon1.json
gt_Objects=as.list(gt_file)
#gt_Objects=unique(as.list(gt_file$gt_object))
#####rank over confidence's score

length(gt_Objects)

gt_Objects=as.character(unlist(gt_Objects))


contextname=currentview

for (i in c(5:11)){
  sup=Sup[i]
  for(j in c(5:11)){
    #if((i==2)&(j==1)) next
    conf=Conf[j]
    cat("\n ===============\n")
    cat("\n Sup: ",sup)
    cat("\n Conf: ",conf)
    input_scoring_file =paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_FeedBack_benign10x10_",
                               weight,"_",contextname,"_Conf_",conf,"_Sup_",sup,".csv",sep="")
    
    Original_input_scoring_file =paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_",contextname,"_Conf_",conf,"_Sup_",sup,".csv",sep="")
    if(!file.exists(input_scoring_file))next
    if (file.size(input_scoring_file) == 0)   next
    
    Objects_With_Scores   <- read.csv(input_scoring_file) 
    ObjectViolator=as.list(Objects_With_Scores$Objects)
    ObjectViolator=as.character(unlist(ObjectViolator))
    length(ObjectViolator) 
    if(length(ObjectViolator) ==0)next
    Objects_With_Scores =arrange(Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))

     
    Original_Objects_With_Scores   <- read.csv(Original_input_scoring_file) 
    Original_Objects_With_Scores =arrange(Original_Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))
    Original_ObjectViolator=as.list(Original_Objects_With_Scores$Objects)
    Original_ObjectViolator=as.character(unlist(Original_ObjectViolator))
    
    
    TP=   intersect(ObjectViolator,gt_Objects) 
    Original_TP=   intersect(Original_ObjectViolator,gt_Objects) 
    cat("\n TP :",length(TP))
    if(length(TP)>0){
      
      indx= which(ObjectViolator %in%  as.character(unlist(TP)))
      Original_indx= which(Original_ObjectViolator %in%  as.character(unlist(Original_TP)))
      cat("\n")
      cat(paste0(indx,sep = ','))
      cat("\n")
      cat(paste0(Original_indx,sep = ','))
      jpeg(filename=paste0("./contexts/",database,"/data/",contextname,
                           "/_FEEDBACK_WithConfRanking_benign10x10_fivedirections_bovia_simple_weight_",weight,"_",contextname,"_Conf_",conf,"_Sup_",sup,".jpeg",sep=""))
    #  par(mar=c(5, 4, 4, 6) + 0.1)
      title=paste0("Violator objects tractory analysis"," Sup:",sup,"|Conf:",conf,sep="")
      plot(1:length(TP),indx, type='l',xlab="Attack objects (True positives)" ,
           ylab="Position in the ranked list (Conf)" ,col="red",main=title,ylim=range(  min(indx,Original_indx),max(indx,Original_indx)))
      #axis(2, ylim=range(indx),col="red",las=1)  ## las=1 makes horizontal labels
     # box()
      
      ## Allow a second plot on the same graph
    #  par(new=TRUE)
      
     # points(1:length(TP),indx,  col="dark red",pch="+")
      lines(1:length(TP),Original_indx, type='l',lty="dashed",
            xlab="Attack objects (True positives)" , ylab="Position in the ranked list (Conf)" ,col="blue", ylim=range(  min(indx,Original_indx),max(indx,Original_indx)))
     # axis(4, ylim=range(Original_indx), col="blue",col.axis="blue",las=1)
      legend("bottomright", legend=c("Position after feedback", "Position before feedback"),
             col=c("red", "blue"),  lty=1:2,cex=0.8)
      
      #axis(1,pretty(1:length(TP)))
      #mtext("Attack objects (True positives)",side=1,col="black",line=2.5)  
      
      dev.off() 
      
    }
  }}
