#######################################Libraries###############################
library(caret,quietly = T, warn.conflicts = F)
library(e1071,quietly = T, warn.conflicts = F)
library(kernlab,quietly = T, warn.conflicts = F)
library(rredis,quietly = T, warn.conflicts = F)

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
data <- redisLRange(paste("datatrain",numTopic,sep=""),0,-1)
l <- strtoi(redisGet(paste("windowsLength",numTopic,sep="")))
redisClose()

getDataBackFromRedis <- function(data) {
  
  nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]])
  dataSet <- matrix(nrow = length(data),ncol = nbfeatures)
  date <- c()
  
  for (i in 1:length(data)) {
    if (strsplit(data[[i]][1],";")[[1]][1] != "None") {
      #date <- c(date,as.POSIXct(strsplit(data[[i]][1],";")[[1]][1]))
      date <- c(date,strsplit(data[[i]][1],";")[[1]][1])
    }
    
    featuresvalues = strsplit(strsplit(data[[i]][1],";")[[1]][2]," ")[[1]]
    for (feature in 1:nbfeatures) {
      dataSet[i,feature] <- as.numeric(featuresvalues[feature])
    }
  }
  
  return(list(dataSet,date))
}

res <- getDataBackFromRedis(data)
xSource <- res[1][[1]]
date <- res[2][[1]]
Xtrain <- xSource;# time serie source
N<-length(Xtrain)

if(is.null(l)) {l = 1} else {l <- strtoi(l)}

if (length (Xtrain)%% l!=0){ Xtrain<-Xtrain[1:(N-(length(Xtrain)%% l))]
} else { Xtrain<-Xtrain}

#Learning Step

if (l!=1) {
  datatrain <- t(matrix(Xtrain,l))#windows of l records values
} else {
  datatrain <- Xtrain
}

OneclasssvmdetectionTraining<- function(datatrain, nu, gamma) {
trainPositive<-datatrain;
  
  
  ########cross validation##############################################
  ###
  inTrain<-createDataPartition(1:nrow(trainPositive),p=0.6,list=FALSE)
  trainpredictors<-trainPositive[inTrain,]#
  trainLabels<-rep(TRUE,nrow(inTrain))
  
  ######OCSVM R grid research sigma and nu ########################
  
  tuned <-tune.svm(trainpredictors, trainLabels,type="one-classification",
                   nu =nu,
                   gamma =gamma,
                   scale=TRUE, kernel="radial")
  
  model.ksvm<- ksvm(trainpredictors,trainLabels,type="one-svc",
                    kernel=rbfdot,kpar=list(sigma=tuned$best.parameters$gamma),nu=tuned$best.parameters$nu,prob.model=TRUE)
  
  print(model.ksvm)
  summary(model.ksvm) #print summary
  
  ### confusion matrix #######
  
  
  svm.predtrain<-predict(model.ksvm,trainpredictors)
  
  
  confTrain<-table(Predicted=svm.predtrain,Reference=trainLabels)
  print(confTrain)
  
  return (model.ksvm)
}


model <-OneclasssvmdetectionTraining(datatrain,0.004, 10^(-2:2))


#Store in Redis
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Store) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Store) Connecting to Redis ",args[2]))
	redisConnect()
}
redisSet(paste("model",numTopic,sep=""),model)
redisClose()
