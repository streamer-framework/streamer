library(xgboost)
library(rredis)

#Get the arguments (i.e number of the topic)
args = commandArgs(trailingOnly=TRUE)
numTopic = args[1]

#Load model and data from Redis
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Load) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Load) Connecting to Redis ",args[2]))
	redisConnect()
}
labelsmap <- redisGet(paste("labelsmap",numTopic,sep=""))
xgModel <- redisGet(paste("model",numTopic,sep=""))
data <- redisLRange(paste("datatest",numTopic,sep=""),0,-1)
redisClose()

#Extract values
nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]]) - 1
datatest <- matrix(nrow = length(data),ncol = nbfeatures)
labels <- c()
date <- c()

for (i in 1:length(data)) {
  
  #datatrain[i,1] <- as.POSIXct(paste(strsplit(data[[i]][1]," ")[[1]][1],strsplit(data[[i]][1]," ")[[1]][2],sep = " "))
  if (strsplit(data[[i]][1],";")[[1]][1] != "None") {
    date <- c(date,as.numeric(strsplit(data[[i]][1],";")[[1]][1]))
  }
  featuresvalues = strsplit(strsplit(data[[i]][1],";")[[1]][2]," ")[[1]]
  
  for (feature in 1:nbfeatures) {
    datatest[i,feature] <- as.numeric(featuresvalues[feature])
  }
  
  originalLabel = featuresvalues[length(featuresvalues)]
  labels <- c(labels,labelsmap[originalLabel][[1]])
  
}

#Test step
prediction = predict(xgModel, cbind(date,datatest))
if (xgModel$params$objective == "binary:logistic") {
  prediction <- as.numeric(prediction > 0.5)
}

print(paste("Accuracy (%): ",length(which(labels == prediction))/length(prediction) *100))
