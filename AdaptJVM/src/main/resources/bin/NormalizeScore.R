#!Rscript

dir = '/scratch/ADAPT/Latest/ad/R/lib'
.libPaths(c(dir,.libPaths()))

suppressMessages(library('MASS'))
suppressMessages(library('fExtremes'))
suppressMessages(library('truncdist'))
suppressMessages(library('ADGofTest'))
suppressMessages(library("optparse"))

option_list <- list(
  make_option(c("-i", "--inFile"), action = "store", default = "",
              help="Input file to read anomaly scores"),
  make_option(c("-d", "--distribution"), action="store", default='auto',
              help="Specify a distrubution from 'gamma', 'gev' or 'weibull', using 'auto' will select the best among these using Anderson Darling test"),
  make_option(c("-o", "--outFile"), action="store", default="",
              help="Output file to write percentile scores")
)

args <- parse_args(OptionParser(option_list = option_list))

if(args$inFile == ""){
  stop("Input file must be provided.")
}
if(args$outFile == ""){
  stop("Output file must be provided.")
}

X <- read.csv(args$inFile)
ifScores <- X$anomaly_score

if(args$distribution == 'gamma'){
  G <- fitdistr(ifScores, densfun = 'gamma', lower=c(0,0))
  shape <- G$estimate["shape"]
  rate <- G$estimate["rate"]
  pScores <- ptrunc(q = ifScores, spec = 'gamma', a = 0, b = 1, shape = shape, rate = rate)
}else if(args$distribution == 'gev'){
  G <- gevFit(ifScores, type = 'pwm')
  loc <- attributes(G)$fit$par.ests['mu']
  scale <- attributes(G)$fit$par.ests['beta']
  shape <- attributes(G)$fit$par.ests['xi']
  pScores <- ptrunc(q = ifScores, spec = 'gev', a = 0, b = 1, loc = loc, scale = scale, shape = shape)
}else if(args$distribution == 'weibull'){
    G <- fitdistr(ifScores, densfun = 'weibull', lower=c(0,0))
    shape <- G$estimate["shape"]
    scale <- G$estimate["scale"]
    pScores <- ptrunc(q = ifScores, spec = 'weibull', a = 0, b = 1, shape = shape, scale = scale)
}else{
  est_gamma <- fitdistr(ifScores, densfun = 'gamma', lower=c(0,0))
  shape1 <- est_gamma$estimate["shape"]
  rate1 <- est_gamma$estimate["rate"]
  test_gamma <- ad.test(ifScores, distr.fun = pgamma, shape = shape1, rate = rate1)
  
  est_gev <- gevFit(ifScores, type = 'pwm')
  loc2 <- attributes(est_gev)$fit$par.ests['mu']
  scale2 <- attributes(est_gev)$fit$par.ests['beta']
  shape2 <- attributes(est_gev)$fit$par.ests['xi']
  test_gev <- ad.test(ifScores, distr.fun = pgev, loc = loc2, scale = scale2, shape = shape2)
  
  est_weibull <- fitdistr(ifScores, densfun = 'weibull', lower=c(0,0))
  shape3 <- est_weibull$estimate["shape"]
  scale3 <- est_weibull$estimate["scale"]
  test_weibull <- ad.test(ifScores, distr.fun = pweibull, shape = shape3, scale = scale3)

  d <- which.min(c(test_gamma$statistic, test_gev$statistic, test_weibull$statistic))
  if(d == 1){
    print('gamma is choosen.')
    pScores <- ptrunc(q = ifScores, spec = 'gamma', a = 0, b = 1, shape = shape1, rate = rate1)
  }else if(d == 2){
    print('gev is choosen.')
    pScores <- ptrunc(q = ifScores, spec = 'gev', a = 0, b = 1, loc = loc2, scale = scale2, shape = shape2)
  }else{
    print('weibull is choosen.')
    pScores <- ptrunc(q = ifScores, spec = 'weibull', a = 0, b = 1, shape = shape3, scale = scale3)
  }
}

write.csv(cbind(X, percentile_score = pScores), args$outFile, row.names = F, quote = F)
