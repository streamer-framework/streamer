library(rredis)
library(neuralnet)

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
param <- redisLRange(paste0("param",numTopic),0,-1)
regTuning <- redisLRange(paste0("regTuning",numTopic),0,-1)

data <- redisLRange(paste0("datatrain",numTopic),1,-1) 
header<- redisLRange(paste0("datatrain",numTopic),0,0)

redisClose()

################### EXTRACT THE INPUTS ##########################

#Function to retreive data
getDataBackFromRedis <- function(data) {
  
	nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]])
	dataSet <- matrix(nrow = length(data),ncol = nbfeatures)
	date_header<-c()
	date <- c()
	  
	for (i in 1:length(data)) {
		if (strsplit(data[[i]][1],";")[[1]][1] != "None") {
			#date <- c(date,as.POSIXct(strsplit(data[[i]][1],";")[[1]][1]))
			#date <- c(date,as.numeric(strsplit(data[[i]][1],";")[[1]][1]))
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

datainit <- data.frame(res[1][[1]])
date <- data.frame(res[2][[1]])

date_header<-strsplit(header[[1]][1],";")[[1]][1]
data_header<-strsplit(strsplit(header[[1]][1],";")[[1]][2]," ")[[1]]

names(datainit)<-data_header
names(date)<-date_header


#Extract Hidden Layers
params<- list()
for (i in 1:length(param)) {
	split <- strsplit(param[[i]][1], " ")
	names = split[[1]][1]
	paramvalue = split[[1]][2]
	params[names] <- paramvalue
}

hiddenLayers<-c()

layerSplit<- strsplit(params$hiddenLayers, ",")    
   
for(i in 1:length(layerSplit[[1]])){
	hiddenLayers<-c(hiddenLayers,strtoi(layerSplit[[1]][i]))
}

#write.table(params, "hid.txt", append=F, row.names=F, col.names=F)


#Extract regTuning
regTunings <- list()
predicatorVariables<-c()
dependantVariable<-c()
for (i in 1:length(regTuning)) {
    split <- strsplit(regTuning[[i]][1], " ")
  	names = split[[1]][1]
  	regTuningvalue = split[[1]][2]
  	if (names == "predicatorVariables") {
	    regTunings[names] <- regTuningvalue
   		predicatorsSplit<- strsplit(regTuningvalue, ";")
   
      	for(i in 1:length(predicatorsSplit[[1]])){
          predicatorVariables<-c(predicatorVariables,predicatorsSplit[[1]][i])
      	}
  
	} else {
    	regTunings[names] <- regTuningvalue
    	dependantSplit<-strsplit(regTuningvalue,";")
        dependantVariable<-dependantSplit[[1]][1]     
  } 
}

set.seed( strtoi(params$seed) )

#write.table(datainit, "sal.txt", append=F, row.names=F)

################### FORMULA FOR NEURALNET FUNCTION##########################
f <- as.formula(paste(regTunings$dependantVariable,"~", paste(predicatorVariables, collapse = " + ")))


################### TRAINING THE MODEL##########################
treshold <- 0.01
#nn<-neuralnet(f, data=datainit, hidden = strtoi(params$hiddenLayers), linear.output =as.logical(params$regressionProblem), threshold = treshold )
nn<-neuralnet(f, data=datainit, hidden = hiddenLayers, linear.output =as.logical(params$regressionProblem), threshold = treshold )


################### Store model in Redis ##########################
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Store) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Store) Connecting to Redis ",args[2]))
	redisConnect()
}
redisSet(paste("model",numTopic,sep=""),nn)
redisClose()
