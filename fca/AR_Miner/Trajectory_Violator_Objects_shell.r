library(grid)
library(ggplot2)
library(gridBase)
library(gridExtra)
library(grid)
library(ggplot2)
library(lattice)
library(latticeExtra)
library('RCurl')
library('RJSONIO')
library('plyr')
library(jsonlite)

args <- commandArgs()
print(args)
Original_input_scoring_file=as.character(args[6])
input_scoring_file=as.character(args[7])
gt_file= fromJSON(as.character(args[8]),simplifyVector = TRUE) 
contextname=as.character(args[9])
sup=as.numeric(args[10])
conf=as.numeric(args[11])
cat("\n ===============\n")
cat("\n Sup: ",sup)
cat("\n Conf: ",conf)

cat("\n reading GT file \n", as.character(args[8]))  

<<<<<<< HEAD
if(!file.exists( as.character(args[8])))stop(" ground truth file does not exist")
if (file.size( as.character(args[8])) == 0)   stop("ground truth file is null")

=======

if(!file.exists( as.character(args[8])))stop(" ground truth file does not exist")
if (file.size( as.character(args[8])) == 0)   stop("ground truth file is null")


>>>>>>> fcastable
gt_Objects=as.list(gt_file)
gt_Objects=as.character(unlist(gt_Objects))
if(length(gt_Objects)==0)stop("\n  length(GT Objects)==0 \n")

#cat("reading \n",input_scoring_file)

<<<<<<< HEAD
    if(!file.exists(input_scoring_file))stop(" input_scoring_file does not exist")
    if (file.size(input_scoring_file) == 0)   stop("input_scoring_file is null")
    cat("\n reading feedback_input_scoring_file\n",input_scoring_file)
    
    Objects_With_Scores   <- read.csv(input_scoring_file) 
    Objects_With_Scores =arrange(Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))
    
    ObjectViolator=as.list(Objects_With_Scores$Objects)
    ObjectViolator=as.character(unlist(ObjectViolator))
   # length(ObjectViolator) 
    if(length(ObjectViolator) ==0) stop("\n The set of violator Objects is empty \n")
    cat("\n reading Original_input_scoring_file \n",Original_input_scoring_file)
    
    if(!file.exists(Original_input_scoring_file))stop(" Original_input_scoring_file does not exist")
    if (file.size(Original_input_scoring_file) == 0)   stop("Original_input_scoring_file is null")
    
    Original_Objects_With_Scores   <- read.csv(Original_input_scoring_file) 
    Original_Objects_With_Scores =arrange(Original_Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))
    Original_ObjectViolator=as.list(Original_Objects_With_Scores$Objects)
    Original_ObjectViolator=as.character(unlist(Original_ObjectViolator))
    if(length(Original_ObjectViolator) ==0) stop("\n  The set of original violator Objects is empty \n")
    
    
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
        #  par(mar=c(5, 4, 4, 6) + 0.1)
      filename=paste0("./contexts/",contextname,
                      "_FEEDBACK_Trajectory_Conf_",conf,"_Sup_",sup,".jpeg",sep="")
      jpeg(filename)
      
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
      cat("\n image saved in \n",filename)
      
    }
=======
if(!file.exists(input_scoring_file))stop(" input_scoring_file does not exist")
if (file.size(input_scoring_file) == 0)   stop("input_scoring_file is null")
cat("\n reading feedback_input_scoring_file\n",input_scoring_file)

Objects_With_Scores   <- read.csv(input_scoring_file) 
Objects_With_Scores =arrange(Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))

ObjectViolator=as.list(Objects_With_Scores$Objects)
ObjectViolator=as.character(unlist(ObjectViolator))
# length(ObjectViolator) 
if(length(ObjectViolator) ==0) stop("\n The set of violator Objects is empty \n")
cat("\n reading Original_input_scoring_file \n",Original_input_scoring_file)

if(!file.exists(Original_input_scoring_file))stop(" Original_input_scoring_file does not exist")
if (file.size(Original_input_scoring_file) == 0)   stop("Original_input_scoring_file is null")

Original_Objects_With_Scores   <- read.csv(Original_input_scoring_file) 
Original_Objects_With_Scores =arrange(Original_Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))
Original_ObjectViolator=as.list(Original_Objects_With_Scores$Objects)
Original_ObjectViolator=as.character(unlist(Original_ObjectViolator))
if(length(Original_ObjectViolator) ==0) stop("\n  The set of original violator Objects is empty \n")


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
  #  par(mar=c(5, 4, 4, 6) + 0.1)
  filename=paste0("./contexts/",contextname,
                  "_FEEDBACK_Trajectory_Conf_",conf,"_Sup_",sup,".jpeg",sep="")
  jpeg(filename)
  
  title=paste0("Violator objects tractory analysis"," Sup:",sup,"|Conf:",conf,sep="")
  plot(1:length(TP),indx, type='l',xlab="Attack objects (True positives)" ,
       ylab="Position in the ranked list (Conf)" ,col="red",main=title,ylim=range(  min(indx,Original_indx),max(indx,Original_indx)))
  #axis(2, ylim=range(indx),col="red",las=1)  ## las=1 makes horizontal labels
  # box()
>>>>>>> fcastable
  
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
  cat("\n image saved in \n",filename)
  
}

