library(rredis)
library(xgboost)
library(caret)

#Get the arguments (i.e number of the topic)
args = commandArgs(trailingOnly=TRUE)
numTopic = args[1]

#Load datatrain from Redis
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Load) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Load) Connecting to Redis ",args[2]))
	redisConnect()
}
param <- redisLRange("param",0,-1)
tuning <- redisLRange("tuning",0,-1)
data <- redisLRange(paste("datatrain",numTopic,sep=""),0,-1)
redisClose()


#Extract values
nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]]) - 1
datatrain <- matrix(nrow = length(data),ncol = nbfeatures)
labels <- c()
date <- c()
labelsmap <- list()
countLabel = 0

for (i in 1:length(data)) {
  
  #datatrain[i,1] <- as.POSIXct(paste(strsplit(data[[i]][1]," ")[[1]][1],strsplit(data[[i]][1]," ")[[1]][2],sep = " "))
  if (strsplit(data[[i]][1],";")[[1]][1] != "None") {
    #date <- c(date,as.POSIXct(strsplit(data[[i]][1],";")[[1]][1]))
    date <- c(date,as.numeric(strsplit(data[[i]][1],";")[[1]][1]))
  }
  
  featuresvalues = strsplit(strsplit(data[[i]][1],";")[[1]][2]," ")[[1]]
  
  for (feature in 1:nbfeatures) {
    datatrain[i,feature] <- as.numeric(featuresvalues[feature])
  }
  
  #Map the label (making start the labels from 0)
  originalLabel = featuresvalues[length(featuresvalues)]
  if (is.null(labelsmap[originalLabel][[1]])) {
    labelsmap[originalLabel][[1]] = countLabel
    countLabel = countLabel +1
  }
  labels <- c(labels,labelsmap[originalLabel][[1]])
}


#Extract params
params <-list()
for (i in 1:length(param)) {
  split <- strsplit(param[[i]][1], " ")
  names = split[[1]][1]
  paramvalue = split[[1]][2]
  if (names == "num_class") {
    params[names] <- as.numeric(paramvalue)
  } else {
    params[names] <- paramvalue
  }
}
if (!params$objective == "multi:softmax") {
  params$num_class = NULL
}

#Extract tuning
tuningBool <- list()
for (i in 1:length(tuning)) {
  split <- strsplit(tuning[[i]][1], " ")
  namesTuning = split[[1]][1]
  tuningBool[namesTuning] = eval(parse(text = toupper(split[[1]][2])))
}


#Data to proper xgboost form
dtrain <- xgb.DMatrix(data = cbind(date,datatrain), label = labels)


################### DEFAULT PARAM ##########################################
eta <- 0.3
bestSubSample = 1
bestColSample = 1
bestMinChildWeight = 1
bestMaxDepth= 6
bestGamma = 0
ntrees = 10
 
#################### NROUND TUNING ########################################
if (tuningBool$nround) {
  print("Tuning nround parameter")
  xgboostCV <- xgb.cv(data =  dtrain, 
                      nrounds = 100, 
                      nfold = 5, 
                      showsd = TRUE,
                      verbose = FALSE,
                      params = params, 
                      "max_depth" = 5, 
                      "eta" = eta,                               
                      "subsample" = 0.8, "colsample_bytree" = 0.8,
                      early_stopping_rounds = 20)
  
  ntrees <- xgboostCV$best_ntreelimit
  #print(ntrees)
}


#################### MAX_DEPTH and MIN_CHILD_WEIGHT TUNING ########################################
if (tuningBool$maxdepth_minchild) {
  print("Tuning min_child_weight and max_depth parameters")
  searchGrid <- expand.grid(min_child_weight = c(1, 3, 5),
                            max_depth = c(3,5,7,9))
  
  minChildMaxWeight <- apply(searchGrid, 1, function(grid) {
    
    minchildweight <- grid[["min_child_weight"]]
    maxdepth <- grid[["max_depth"]]
    
    xgboostCV <- xgb.cv(data =  dtrain, 
                        nrounds = ntrees, 
                        nfold = 5, 
                        showsd = TRUE, 
                        verbose = FALSE,
                        params = params, 
                        "subsample" = 0.8,
                        "colsample_bytree" = 0.8,
                        "eta" = eta,
                        "max_depth" = maxdepth,
                        "min_child_weight" = minchildweight)
    
    error <- as.numeric(xgboostCV$evaluation_log[ntrees,4])
    #print(error)
    return(c(error, minchildweight,maxdepth))
    
  })
  
  minErrorIdx <- which.min(minChildMaxWeight[1,])
  bestMinChildWeight <- minChildMaxWeight[2,minErrorIdx]
  bestMaxDepth <- minChildMaxWeight[3,minErrorIdx]
}


################################# GAMMA TUNING ###########################################
if (tuningBool$gamma) {
  print("Tuning gamma parameter")
  searchGrid <- expand.grid(gamma = c(0,0.1,0.2,0.3,0.4))
  gammaTuning <- apply(searchGrid, 1, function(grid) {
  
  gamma <- grid[["gamma"]]
  
  xgboostCV <- xgb.cv(data =  dtrain, 
                      nrounds = ntrees, 
                      nfold = 5, 
                      showsd = TRUE, 
                      verbose = FALSE,
                      params = params, 
                      "subsample" = 0.8,
                      "colsample_bytree" = 0.8,
                      "eta" = eta,
                      "max_depth" = bestMaxDepth,
                      "min_child_weight" = bestMinChildWeight,
                      "gamma" = gamma)
  
  error <- as.numeric(xgboostCV$evaluation_log[ntrees,4])
  #print(error)
  return(c(error, gamma))

  })
  bestGamma = gammaTuning[2,which.min(gammaTuning[1,])]
}


########################### SUBSAMPLE and COLSAMPLE TUNING ##########################################
if (tuningBool$colsubsample) {
  print("Tuning subsample and colsample parameters")
  searchGrid <- expand.grid(subsample = c(0.5,0.75,1), 
                            colsample_bytree = c(0.6, 0.8, 1))
  
  subcolsample <- apply(searchGrid, 1, function(grid) {
    
    subsample <- grid[["subsample"]]
    colsample <- grid[["colsample_bytree"]]
    
    xgboostCV <- xgb.cv(data =  dtrain, 
                        nrounds = ntrees, 
                        nfold = 5, 
                        showsd = TRUE, 
                        verbose = FALSE,
                        params = params, 
                        "subsample" = subsample,
                        "colsample_bytree" = colsample,
                        "eta" = eta,
                        "gamma" = bestGamma,
                        "max_depth" = bestMaxDepth,
                        "min_child_weight" = bestMinChildWeight)
    
    error <- as.numeric(xgboostCV$evaluation_log[ntrees,4])
    #print(error)
    return(c(error, subsample,colsample))
    
  })
  minErrorIndex <- which.min(subcolsample[1,])
  bestSubSample <- subcolsample[2,minErrorIndex]
  bestColSample <- subcolsample[3,minErrorIndex]
}



#Save best parameters
params["subsample"] = bestSubSample
params["colsample_bytree"] = bestColSample
params["min_child_weight"] = bestMinChildWeight
params["max_depth"] = bestMaxDepth
params["gamma"] = bestGamma
params["eta"] = eta



#Train the XGB model
xgModel = xgboost(data = dtrain, param=params, nround = ntrees, verbose = FALSE)



#Store model in Redis
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Store) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Store) Connecting to Redis ",args[2]))
	redisConnect()
}
redisSet(paste("labelsmap",numTopic,sep=""), labelsmap)
redisSet(paste("model",numTopic,sep=""),xgModel)
redisClose()

