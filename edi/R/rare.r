source("rules.processing.r")
source("csv.to.rcf.r")
library("plyr")

rare.rules.ad <- function(Input.file, ## input csv context file
                          OutputScoresFile,
                          MinSup # Support
                          ) {

  cat("\n Ad detection without GT")

  options(max.print =  10000000)

  cat('\n ############### Rare Rule Mining Anomaly Detection ######################## \n')


  ################ Load objects, attributes and binary matrix from CSV file.

  cat('\n reading the current context file \n ',Input.file)

  ###We suppose that the RCF file exists in the same directory of the input csv file.
  ## Later we are going to add a check to test the existance of the RCF file.
  ## If it is missing we will use the csv_to_rcf function to create it.
  ContextFileRCF=gsub("csv","rcf",Input.file)
  if(!file.exists(ContextFileRCF)) {
    cat("The Context RCF file does not exist, creating it\n")
    csv.to.rcf(Input.file,ContextFileRCF)
  }

  cat("\n starting the extraction of the association rules \n")
  #RareAssRulescmd = paste0(getwd(),
  #    "/coron-0.8/core02_assrulex.sh  ",ContextFileRCF, " ",
  #    MinSup,"% 100% -names -alg:BtB -rule:rare  -full  -examples",sep="")
  RareAssRulescmd = paste0("java -Xmx16384m -cp jar/assrulex-pg-bin.jar:jar/coron-pg-bin.jar fr.loria.coronsys.assrulex.Main ",ContextFileRCF," ",MinSup,"% 100% -names -alg:BtB -rule:rare -full -examples")
  cat(RareAssRulescmd)
  RareAssRulesresult=try(system(RareAssRulescmd, intern = TRUE,  wait = TRUE)) 
  #####################
  rules.processing.results = rules.processing(RareAssRulesresult)
  cat('\n Rules extraction done! \n')

  ###########=========================================================================================
  ListScore <- list()
  RuleMaxScore <- list()

  ##reading the results
  PositivesList = rules.processing.results$Positives.List #
  RulesList = rules.processing.results$Rules.List
  objectsOfRules = rules.processing.results$objectsOf.Rules
  CondRules = rules.processing.results$Cond.Rules
  ResRules = rules.processing.results$Res.Rules
  Lift = rules.processing.results$OriginalLift.sc
  n = length(PositivesList)
  if (n > 0) {
    for (id in 1:n) {
      #cat("\n Scoring Object_ID", id, " : ", PositivesList[id],"\n")
      CurrentObject = PositivesList[id]
      maxscore = 0
      ## Check the toxicity of the current object % each rule.
      for (idRule in 1:length(RulesList)) {
          if(CurrentObject %in% objectsOfRules[[idRule]]) {

          #scoreLift = scoreLift-lengthRule*log2(1-as.double(Lift[[idRule]] ))
                                        #        scoreLift = max(scoreLift, as.double(Lift[[idRule]]))
          maxscore = max(maxscore,Lift[[idRule]])
          maxrule = str_c(CondRules[[idRule]]," -> ", ResRules[[idRule]])
        } 
      }
      ListScore = c(ListScore,maxscore)
      RuleMaxScore = c(RuleMaxScore, maxrule)
    }
  }


  ListScore = lapply(ListScore, function(x)as.double(x))
                                        #export  results only
  df.ObjectsWithScores = data.frame(matrix(ncol = 3, nrow = 0))
  colnames(df.ObjectsWithScores) = c("UUID", "Score","Rule" )
  
  if(length(ListScore) > 0) {
    df.ObjectsWithScores = do.call(rbind, Map(data.frame,
                                              "UUID"=as.list(PositivesList),
                                              "Score"= ListScore,
                                              "Rule"=RuleMaxScore ))
    df.ObjectsWithScores = arrange(df.ObjectsWithScores ,desc(df.ObjectsWithScores$Score) )
  }
  ##Export the results
  write.csv(df.ObjectsWithScores,file=OutputScoresFile, row.names = FALSE)
  cat("\n *** Results saved for this context \n ")

}
