library(rredis)
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
param <- redisLRange("paramP",0,-1)
dataSrckey<-paste("data",numTopic,sep="")
data <- redisLRange(dataSrckey,1,-1)
hd<- redisLRange(dataSrckey,0,0)

redisClose()

params <-list()
for (i in 1:length(param)) {
  split <- strsplit(param[[i]][1], " ")
  names = split[[1]][1]
  paramvalue = split[[1]][2]
	params[names] <- paramvalue
}


getDataBackFromRedis <- function(data) {
  
  nbfeatures = length(strsplit(strsplit(data[[1]][1],";")[[1]][2]," ")[[1]])
  dataSet <- matrix(nrow = length(data),ncol = nbfeatures)
     
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
    print(featuresvalues[1])
  return(list(dataSet,date))
}

normalize <- function(x, min, max) {

  return ((x - min) / (max - min))
}
#Extract params
minmaxVar<- params["minMaxVariables"]
minmaxVal<- params["minMaxVariablesValues"]
evRecordOrder<-strsplit(params["evObjectFieldsOrder"][[1]],";")[[1]]

write.table(params["evObjectFieldsOrder"],"amini.csv", sep=";", row.names=F)
write.table(params["minMaxVariablesValues"],"bmini.csv", sep=";", row.names=F)
minMaxVariables<- strsplit(minmaxVar[[1]],";")[[1]]
minMaxValues<-strsplit(minmaxVal[[1]],";")[[1]]

initVec<-c(0,0)
minMaxData<-data.frame(initVec)

for (j in minMaxValues){
        t<-c(as.double(strsplit(j,":")[[1]]))
       minMaxData<-cbind(minMaxData,t)
}
minMaxData$initVec<-NULL
names(minMaxData)<-minMaxVariables
res <- getDataBackFromRedis(data)
datainit <-data.frame( res[1][[1]])
date <- data.frame(res[2][[1]])
 
date_header<-strsplit(hd[[1]][1],";")[[1]][1]
data_header<-strsplit(strsplit(hd[[1]][1],";")[[1]][2]," ")[[1]]

names(datainit)<-data_header
names(date)<-date_header

normInitVec<-rep(0, nrow(datainit))
datanorm<-data.frame(normInitVec)
for (i in names(datainit)){
  
    mini<-minMaxData[[i]][1]
    maxi<-minMaxData[[i]][2]
    #write.table(names(datainit),"deb.csv", sep=";")
    d<-datainit[i]
    temp<-normalize(d, mini, maxi)
    
    datanorm<-cbind(datanorm, temp)
}
datanorm$normInitVec<-NULL
names(datanorm)<-names(datainit)[-1]

names(datanorm)<-data_header


datanorm<-cbind(date, datanorm)
datanorm<-datanorm[evRecordOrder]
write.table(datanorm,"normalized.csv", sep=";", row.names=F)
ddd<-as.matrix(datanorm)

t<-colnames(ddd)
lineH<-t[1]
for(i in 2:length(t)){
    lineH<-paste0(lineH,";",t[i])
}

dataFormated<-c(lineH)
for(i in 1:nrow(ddd)){
    nb<-ncol(ddd)
    line<-as.character(ddd[i,1])
    for(j in 2:nb){
        line<-paste0(line,";",ddd[i,j],options(scipen = 999))
    } 
    dataFormated<-c(dataFormated,line)
}

#write.table(dataFormated,"normalizedFF.csv", sep=";", row.names=F)

key<-paste("normalized",numTopic,sep="")
if(length(args) == 3){
	if( args[2]!="" & args[3]!="" ){
		print(paste0("(Store) Connecting to Redis IP: ",args[2]," and Port: ",args[3]))
		redisConnect(host=args[2], port=as.numeric(args[3]))
	}
}else{
	print(paste0("(Store) Connecting to Redis ",args[2]))
	redisConnect()
}
redisDelete(key)
#redisRPush(key,serialize(datanorm,NULL, ascii=T))
if (length(dataFormated) != 0) {
  for (i in 1:length(dataFormated)) {
    redisRPush(key,charToRaw(dataFormated[i]),NULL)
  }
}
redisSet("minMaxTwizz", minMaxData)
redisClose()
