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
pScores <- rep(0, nrow(X))

fitGamma <- function(ifScores){
  tryCatch({
    G <- fitdistr(ifScores, densfun = 'gamma', lower=c(1e-6,1e-6))
    cat('Gamma fit successful!\n')
    return (G)
  }, error = function(e){}, warning = function(w){})
  cat('Gamma fit failed!\n')
  return (NULL)
}

fitGev <- function(ifScores){
  tryCatch({
    G <- gevFit(ifScores, type = 'pwm')
    cat('GEV fit successful!\n')
    return (G)
  }, error = function(e){}, warning = function(w){})
  cat('GEV fit failed!\n')
  return (NULL)
}

fitWeibull <- function(ifScores){
  tryCatch({
    G <- fitdistr(ifScores, densfun = 'weibull', lower=c(1e-6, 1e-6))
    cat('Weibull fit successful!\n')
    return (G)
  }, error = function(e){}, warning = function(w){})
  cat('Weibull fit failed\n')
  return (NULL)
}

if(var(ifScores) < 1e-9){
  cat('Variance of scores is equal or near to 0, can not estimate distribution parameters')
}else if(args$distribution == 'gamma'){
  G <- fitGamma(ifScores)
  if(is.null(G)){
    pScores <- rep(0, nrow(X))
  }else{
    shape <- G$estimate["shape"]
    rate <- G$estimate["rate"]
    pScores <- ptrunc(q = ifScores, spec = 'gamma', a = 0, b = 1, shape = shape, rate = rate)
  }
}else if(args$distribution == 'gev'){
  G <- fitGev(ifScores)
  if(is.null(G)){
    pScores <- rep(0, nrow(X))
  }else{
    loc <- attributes(G)$fit$par.ests['mu']
    scale <- attributes(G)$fit$par.ests['beta']
    shape <- attributes(G)$fit$par.ests['xi']
    pScores <- ptrunc(q = ifScores, spec = 'gev', a = 0, b = 1, loc = loc, scale = scale, shape = shape)
  }
}else if(args$distribution == 'weibull'){
  G <- fitWeibull(ifScores)
  if(is.null(G)){
    pScores <- rep(0, nrow(X))
  }else{
    shape <- G$estimate["shape"]
    scale <- G$estimate["scale"]
    pScores <- ptrunc(q = ifScores, spec = 'weibull', a = 0, b = 1, shape = shape, scale = scale)
  }
}else{
  test_gamma <- list(statistic=Inf)
  est_gamma <- fitGamma(ifScores)
  if(is.null(est_gamma) == F){
    shape1 <- est_gamma$estimate["shape"]
    rate1 <- est_gamma$estimate["rate"]
    tryCatch({
      test_gamma <- ad.test(ifScores, distr.fun = pgamma, shape = shape1, rate = rate1)
    }, error = function(e){}, warning = function(w){})
  }

  test_gev <- list(statistic=Inf)
  est_gev <- fitGev(ifScores)
  if(is.null(est_gev) == F){
    loc2 <- attributes(est_gev)$fit$par.ests['mu']
    scale2 <- attributes(est_gev)$fit$par.ests['beta']
    shape2 <- attributes(est_gev)$fit$par.ests['xi']
    tryCatch({
      test_gev <- ad.test(ifScores, distr.fun = pgev, loc = loc2, scale = scale2, shape = shape2)
    }, error = function(e){}, warning = function(w){})
  }

  test_weibull <- list(statistic=Inf)
  est_weibull <- fitWeibull(ifScores)
  if(is.null(est_weibull) == F){
    shape3 <- est_weibull$estimate["shape"]
    scale3 <- est_weibull$estimate["scale"]
    tryCatch({
      test_weibull <- ad.test(ifScores, distr.fun = pweibull, shape = shape3, scale = scale3)
    }, error = function(e){}, warning = function(w){})
  }

  test_res <- c(test_gamma$statistic, test_gev$statistic, test_weibull$statistic)
  if(sum(test_res == Inf) < 3){
    d <- which.min(test_res)
    if(d == 3){
      cat('weibull is choosen.\n')
      pScores <- ptrunc(q = ifScores, spec = 'weibull', a = 0, b = 1, shape = shape3, scale = scale3)
    }else if(d == 2){
      cat('gev is choosen.\n')
      pScores <- ptrunc(q = ifScores, spec = 'gev', a = 0, b = 1, loc = loc2, scale = scale2, shape = shape2)
    }else{
      cat('gamma is choosen.\n')
      pScores <- ptrunc(q = ifScores, spec = 'gamma', a = 0, b = 1, shape = shape1, rate = rate1)
    }
  }
}

write.csv(cbind(X, percentile_score = pScores), args$outFile, row.names = F, quote = F)
