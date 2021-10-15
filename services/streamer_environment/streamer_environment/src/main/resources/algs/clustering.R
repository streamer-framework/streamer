library(rredis)
library(clustertend)
library(optCluster)
library(factoextra)

################### GET PARAMS FROM REDIS ##########################
#Get the arguments (i.e number of the topic)
args = commandArgs(trailingOnly=TRUE)
numTopic = args[1]

if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Load) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Load) Connecting to Redis ",args[2]))
	redisConnect()
}
data <- redisLRange(paste0("dataclus",numTopic),1,-1) 
 
hd<- redisLRange(paste0("dataclus",numTopic),0,0)
#param <- redisLRange(paste0("params",numTopic),0,-1)
redisClose()


################### EXTRACT THE INPUTS ##########################

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

date_header<-strsplit(hd[[1]][1],";")[[1]][1]
data_header<-strsplit(strsplit(hd[[1]][1],";")[[1]][2]," ")[[1]]

names(datainit)<-data_header
names(date)<-date_header


#params <-list()
#for (i in 1:length(param)) {
#	split <- strsplit(param[[i]][1], " ")
#	names = split[[1]][1]
#	paramvalue = split[[1]][2]
#	params[names] <- paramvalue
#}

set.seed(10)

############################### GET THE BEST CLUSTERING MODEL #####################################################
#by using hopking meassure

data <-cbind(datainit)
data<-cbind(V0 = rownames(data), data, row.names = "V0")

df<-scale(data)

hop<-hopkins(df, n=nrow(df)-1)
clmethods <- c("hierarchical", "kmeans","pam")	#to pass by parameter
if(hop<0.5){
    optMouse <- tryCatch(optCluster(df, 2:5, clMethods = clmethods, validation = "internal"))	#to pass by parameter (min and max)
    best<-optMouse@rankAgg$top.list[1]
    separtion<-strsplit(best,"-")[[1]]
    method<-separtion[1]
    if(method=='hierarchical') method<-'hclust'
    nbcluster<-strtoi(separtion[2])
}else{
   # print("Data is not Clusterable")
}


############################### RUN MODEL #####################################################

print(paste('Clustering method: ',method))
clust<-eclust(df,method,k=nbcluster)


############# RESULTS ###################
 
Alarmecl <- list()
Alarmecl[[1]]<- clust$cluster


#resultStr = ""
#for (i in 1:length(Alarmecl[[1]])) {
#	resultStr = paste(resultStr,Alarmecl[[1]][i],sep = ";")
#}

#write.table(resultStr, file = "results.txt")
#write.table((param[[i]][1], "pppp.csv" ,sep=";", row.names =F, col.names=F)

#serialize is VERY important, use charToRaw function for storing strings
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Store) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Store) Connecting to Redis ",args[2]))
	redisConnect()
}
#redisSet(paste("outputs",numTopic,sep = ""),charToRaw(resultStr) )

redisRPush(paste("outputs",numTopic,sep = ""),charToRaw("NaN") )#first element is just headers, no prediction for it
for (i in 1:length(Alarmecl[[1]])) {
	redisRPush(paste("outputs",numTopic,sep = ""),charToRaw(toString(Alarmecl[[1]][i])) )
}


#redisSet(paste("clustered",numTopic,sep=""),clusRest)
redisClose()
