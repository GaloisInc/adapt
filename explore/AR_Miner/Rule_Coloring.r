Rule_Coloring=function(RulesFile,List_Attributes,ViolatorObjectList,AVGScoresOfObjectsConfidence){
  Rules_File=file(RulesFile,"r")
    fileColor=file("./sample/coloring/rulz.col","w")
  #cmd="./post02_ruleColoring.sh sample/coloring/rulz.txt sample/coloring/rulz.col >/tmp/rulz.html"
  #paste0("./contexts/AssociationRulesOnly_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".txt",sep="")
   # Rules_File=capture.output(SoftAssRulesresult,file=paste0("./contexts/AssociationRulesOnly_Conf_",currentview,"_",MinConf,"_Sup_",MinSup,".txt",sep=""))
  nb=length(List_Attributes)
  color = grDevices::colors()[grep('gr(a|e)y', grDevices::colors(), invert = T)]
  col=sample(color, nb) 
  writeLines("body { background-color: #EAF1FD; }",fileColor)
  for (i  in 1:nb) {
    
    writeLines(paste0( col[i]," { ",List_Attributes[i]," }"),fileColor)
  }
  close(fileColor) 
  
  cmd= paste0(getwd(),"/coron-0.8/post02_ruleColoring.sh ",RulesFile," ./sample/coloring/rulz.col  > ./tmp/rulz.html")
  try(system(cmd, intern = TRUE,  wait = TRUE))  
  cat("\n RULES HTML FILE GENERATED, PLEASE OPEN  ./tmp/rulz.html \n")
  
  
  set.seed(1234)
  wordcloud(words = as.list(paste("Obj",seq(1:length(ViolatorObjectList)),sep=".")), 
            freq = as.vector(round(as.numeric(AVGScoresOfObjectsConfidence))), min.freq = 1,
            max.words=200, random.order=FALSE, rot.per=0.35, 
            colors=brewer.pal(8, "Dark2"))
  
  
  close(Rules_File) 
}
