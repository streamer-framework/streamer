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
model <- redisGet(paste("model",numTopic,sep=""))
data <- redisLRange(paste("datatest",numTopic,sep=""),0,-1)
l <- redisGet(paste("windowsLength",numTopic,sep=""))
redisClose()

getDataBackFromRedis <- function(data) {
  
  nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]])
  dataSet <- matrix(nrow = length(data),ncol = nbfeatures)
  date <- c()
  
  for (i in 1:length(data)) {
    if (strsplit(data[[i]][1],";")[[1]][1] != "None") {
      date <- c(date,strsplit(data[[i]][1],";")[[1]][1])
      #date <- c(date,as.POSIXct(strsplit(data[[i]][1],";")[[1]][1]))
      #date <- c(date,as.numeric(strsplit(data[[i]][1],";")[[1]][1]))
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

N <- length(xSource) 
if(is.null(l)) {l = 1} else {l <- strtoi(l)}

if (length (xSource)%% l!=0){ 
  xSource<-xSource[1:(N-(N%% l))] 
}

OneclasssvmdetectionTest <- function(datatest, model) {
  
  xtest<-datatest;
  model.ksvm <- model
  svm.predtest<-predict(model.ksvm,xtest)
  indxtrue<-which(svm.predtest==TRUE)
  indxfalse<-which(svm.predtest==FALSE)
  decisiontest<-rep(0,nrow(svm.predtest))
  decisiontest[indxtrue]<-0
  decisiontest[indxfalse]<-1
  return (decisiontest)
  
}

##############Test part##################

if (l != 1) {
  datatest<-t(matrix(xSource,l))#windows of l records values
} else {
  datatest <- xSource
}

meth1 <- OneclasssvmdetectionTest(datatest,model)

############# Results ###################

Alarmecl <- list()
Alarmecl[[1]]<- meth1### alarm result

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
resultStr = ""
for (i in 1:length(Alarmecl[[1]])) {
  resultStr = paste(resultStr,Alarmecl[[1]][i],sep = "")
}
redisSet(paste("classifResults",numTopic,sep = ""),charToRaw(resultStr))
redisClose()

#Print it
cat(Alarmecl[[1]])


