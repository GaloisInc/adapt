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
Original.input.scoring.file <-as.character(args[6])
input.scoring.file <-as.character(args[7])
gt.file <- fromJSON(as.character(args[8]),simplifyVector = TRUE) 
contextname <-as.character(args[9])
sup <-as.numeric(args[10])
conf <-as.numeric(args[11])
cat("\n ===============\n")
cat("\n Sup: ",sup)
cat("\n Conf: ",conf)

cat("\n reading GT file \n", as.character(args[8]))  


if(!file.exists( as.character(args[8])))stop(" ground truth file does not exist")
if (file.size( as.character(args[8])) == 0)   stop("ground truth file is null")


gt.Objects <-as.list(gt.file)
gt.Objects <-as.character(unlist(gt.Objects))
if(length(gt.Objects)==0)stop("\n  length(GT Objects)==0 \n")

#cat("reading \n",input.scoring.file)

if(!file.exists(input.scoring.file))stop(" input.scoring.file does not exist")
if (file.size(input.scoring.file) == 0)   stop("input.scoring.file is null")
cat("\n reading feedback.input.scoring.file\n",input.scoring.file)

Objects.With.Scores   <- read.csv(input.scoring.file) 
Objects.With.Scores  <-arrange(Objects.With.Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))

ObjectViolator <-as.list(Objects.With.Scores$Objects)
ObjectViolator <-as.character(unlist(ObjectViolator))
# length(ObjectViolator) 
if(length(ObjectViolator) ==0) stop("\n The set of violator Objects is empty \n")
cat("\n reading Original.input.scoring.file \n",Original.input.scoring.file)

if(!file.exists(Original.input.scoring.file))stop(" Original.input.scoring.file does not exist")
if (file.size(Original.input.scoring.file) == 0)   stop("Original.input.scoring.file is null")

Original.Objects.With.Scores   <- read.csv(Original.input.scoring.file) 
Original.Objects.With.Scores  <-arrange(Original.Objects.With.Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))
Original.ObjectViolator <-as.list(Original.Objects.With.Scores$Objects)
Original.ObjectViolator <-as.character(unlist(Original.ObjectViolator))
if(length(Original.ObjectViolator) ==0) stop("\n  The set of original violator Objects is empty \n")


TP <-   intersect(ObjectViolator,gt.Objects) 
Original.TP <-   intersect(Original.ObjectViolator,gt.Objects) 
cat("\n TP :",length(TP))
if(length(TP)>0){
  
  indx <- which(ObjectViolator %in%  as.character(unlist(TP)))
  Original.indx <- which(Original.ObjectViolator %in%  as.character(unlist(Original.TP)))
  cat("\n")
  cat(paste0(indx,sep = ','))
  cat("\n")
  cat(paste0(Original.indx,sep = ','))
  #  par(mar=c(5, 4, 4, 6) + 0.1)
  filename=paste0("./contexts/",contextname,
                  ".FEEDBACK.Trajectory.Conf_",conf,"_Sup_",sup,".jpeg",sep="")
  jpeg(filename)
  
  title <-paste0("Violator objects tractory analysis"," Sup:",sup,"|Conf:",conf,sep <-"")
  plot(1:length(TP),indx, type='l',xlab="Attack objects (True positives)" ,
       ylab="Position in the ranked list (Conf)" ,col="red",main=title,ylim=range(  min(indx,Original.indx),max(indx,Original.indx)))
  #axis(2, ylim=range(indx),col="red",las=1)  ## las=1 makes horizontal labels
  # box()
  
  ## Allow a second plot on the same graph
  #  par(new=TRUE)
  
  # points(1:length(TP),indx,  col="dark red",pch="+")
  lines(1:length(TP),Original.indx, type='l',lty="dashed",
        xlab="Attack objects (True positives)" , ylab="Position in the ranked list (Conf)" ,col="blue", ylim=range(  min(indx,Original.indx),max(indx,Original.indx)))
  # axis(4, ylim=range(Original.indx), col="blue",col.axis="blue",las=1)
  legend("bottomright", legend=c("Position after feedback", "Position before feedback"),
         col=c("red", "blue"),  lty=1:2,cex=0.8)
  
  #axis(1,pretty(1:length(TP)))
  #mtext("Attack objects (True positives)",side=1,col="black",line=2.5)  
  dev.off() 
  cat("\n image saved in \n",filename)
  
}