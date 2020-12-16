library(rredis)
library(neuralnet)

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
neuraModel <- redisGet(paste0("model",numTopic))

regTuning <- redisLRange(paste0("regTuning",numTopic),0,-1)

data <- redisLRange(paste0("dataTest",numTopic),1,-1) 
header<- redisLRange(paste0("dataTest",numTopic),0,0)

target <- redisLRange(paste0("target",numTopic),0,-1) 

redisClose()

#write.table(data, "PAAAA.txt", append=F, row.names=F)
#write.table(header, "namES.txt", append=F, row.names=F)


################### EXTRACT THE INPUTS ##########################

#Function to retreive data
getDataBackFromRedis <- function(data) {
  
	nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]])
	dataSet <- matrix(nrow = length(data),ncol = nbfeatures)
	date_header<-c()
	date <- c()
	  
	for (i in 1:length(data)) {
		if (strsplit(data[[i]][1],";")[[1]][1] != "None") {
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

#write.table(datainit, "INPUTS.txt", append=F, row.names=F)


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

############################### RUN MODEL #####################################################

temp_test <- subset(datainit, select = predicatorVariables)

nn.results <- compute(neuraModel, temp_test)

#write.table(predicatorVariables, "pred.txt", append=F, row.names=F)
#write.table(temp_test, "temp.txt", append=F, row.names=F)
#write.table(nn.results$net.result, "RESULTS.txt", append=F, row.names=F)

############################### CALCULATE STATISTICS (not used) #####################################################
#results <- data.frame(actual = data[dependantVariable], prediction = nn.results$net.result)
#results <- nn.results$net.result
#predicted=results$prediction * abs(diff(range(minMax[dependantVariable]))) + min(minMax[dependantVariable])
#actual=data[dependantVariable] * abs(diff(range(minMax[dependantVariable]))) + min(minMax[dependantVariable])

#deviation=((actual-predicted)/actual)

#accuracy=1-abs(mean(deviation[,dependantVariable]))

#write.table(paste0(accuracy*100,"--",numTopic), "acc.txt", append=T, row.names=F)
#write.table(predicted, "pred.csv", sep=";", row.names =F)
#print(paste("Accuracy (%) :", accuracy*100 ))


############# RESULTS ###################

Alarmecl <- list()
Alarmecl[[1]]<- nn.results$net.result

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
  resultStr = paste(resultStr,Alarmecl[[1]][i],sep = ";")
}

#write.table(resultStr, "RESULTS.txt", append=F, row.names=F, col.names=F)


#serialize is VERY important, use charToRaw function for storing strings
redisSet(paste("classifResults",numTopic,sep = ""),charToRaw(resultStr) )

redisSet(paste("separation",numTopic,sep = ""),charToRaw(";") )#separation for the numbers passed as string

#for (i in 1:5) {
#    redisRPush('prueba',charToRaw('hola'),NULL)
#}

redisClose()

#Print it
#cat(Alarmecl[[1]])

#predictionKey = paste("classifResults",numTopic,sep = "")
#redisConnect()
#redisDelete(predictionKey)
#redisSet(predictionKey, nn.results$net.result)
#redisClose()
