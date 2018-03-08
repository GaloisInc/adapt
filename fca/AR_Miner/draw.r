library(grid)
library(ggplot2)
library(gridBase)
library(gridExtra)
library(grid)
library(ggplot2)
library(lattice)

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

for (i in 1:11){
  sup=Sup[i]
  for(j in 1:11){
    #if((i==2)&(j==1)) next
    conf=Conf[j]
    cat("\n ===============\n")
    cat("\n Sup: ",sup)
    cat("\n Conf: ",conf)
 input_scoring_file =paste0("./contexts/",database,"/data/",contextname,"/Objects_With_Scores_",contextname,"_Conf_",conf,"_Sup_",sup,".csv",sep="")
 if (file.size(input_scoring_file) == 0) {
  next
 }
Objects_With_Scores   <- read.csv(file=input_scoring_file) 
Objects_With_Scores =arrange(Objects_With_Scores ,desc(AVGScoresOfObjectsConfidence))#AVGScoresOfObjectsLift))#AVGScoresOfObjectsConfidence))
ObjectViolator=as.list(Objects_With_Scores$Objects)
ObjectViolator=as.character(unlist(ObjectViolator))
indx50percent=round(length(ObjectViolator)/2)
score50percent=Objects_With_Scores$AVGScoresOfObjectsConfidence[indx50percent]#AVGScoresOfObjectsLift[indx50percent]
  #AVGScoresOfObjectsLift[indx50percent]   #AVGScoresOfObjectsConfidence[indx50percent]
#topscore=Objects_With_Scores$AVGScoresOfObjectsConfidence[1]
length(ObjectViolator) 
score50percent <- formatC(score50percent, format="f", digits=2)
Allscores=Objects_With_Scores$AVGScoresOfObjectsConfidence#AVGScoresOfObjectsLift
  #AVGScoresOfObjectsConfidence
Allscores<- formatC(Allscores, format="f", digits=2)
#topscore
#score50percent=paste0("score ",as.character(score50percent),sep="")


TP=   intersect(ObjectViolator,gt_Objects) 
cat("\n TP :",length(TP))
if(length(TP)>0){
  #cat("\n True positives:\n")
  #cat(as.character(unlist(TP)))
  #index=match(ObjectViolator,   as.character(unlist(TP)) )
  # ObjectViolator[which(!is.na(index))]
  #
 indx= which(ObjectViolator %in%  as.character(unlist(TP)))
 topscore=Objects_With_Scores$AVGScoresOfObjectsConfidence[indx[1]]
 topscore <- formatC(topscore, format="f", digits=2)
 #par(mfrow=c(2,1))
 #curve=plot(Allscores, type='l',xlabel="Position of the violator objects" , ylabel="score" )
 jpeg(filename=paste0("./contexts/",database,"/data/",contextname,
                      "/_SpectralImage_WithConfidenceRanking_ffivedirections_bovia_simple_",contextname,"_Conf_",conf,"_Sup_",sup,".jpeg",sep=""))
 
codbare=ggplot() +
    #for(k in 1:length(indx)){
     #
    #}
#  +
    geom_vline(xintercept=indx, color="red")+
    geom_vline(xintercept=indx50percent, color="blue")+
    #geom_text(aes(xintercept=indx50percent, label=score50percent), colour="red", angle=90, vjust = -1, text=element_text(size=11))
    #annotate("text", x = indx50percent, y = 5, angle = 90, label =topscore,   parse = TRUE)
#  ggsave(filename="geomline.png", width=5.5, height=2*3, dpi=300)
    geom_vline(xintercept=length(ObjectViolator), color="white") +
  # annotate("text", x = indx50percent, y = 5, angle = 90, label =score50percent,   parse = TRUE)+
  xlab("Position of the violator object") + ylab(" . ")
  
  

  #Create figure window and layout
  plot.new()
  grid.newpage()
  pushViewport(viewport(layout = grid.layout(2,1)))
  
  pushViewport(viewport(layout.pos.row  =1))
  par(fig = gridFIG(), new = TRUE)
  curve=plot(Allscores, type='l',xlab="Position of the violator object" , ylab="score" )  
  popViewport()
  
  pushViewport(viewport(layout.pos.row = 2))
  print(codbare, newpage = FALSE)
  popViewport()
  
  dev.off() 
  
  #+
    #annotate("text", x = indx[1], y = 5, angle = 90, label =topscore,   parse = TRUE)
  #  geom_vline(xintercept=2, color="red") +
   # geom_vline(xintercept=10, color="red") 
 
}
}}
   