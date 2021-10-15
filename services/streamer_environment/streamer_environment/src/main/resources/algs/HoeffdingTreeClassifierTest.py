# import the online algorithm Hoeffding Tree Classifier
# the data source for streaming data (not necessary but it is a good practice)
from skmultiflow.data import DataStream #here datastream and not filestream because of using redis
# import numpy and pandas libraries for reading from csv and data preparation
import pandas as pd
# import the redis library to read/write from/to redis
import redis
# save and retreive complex objects to/from redis
import pickle
# to add the time for logging the size of the data read from Redis
from datetime import datetime
# to calculate the execution time of this phase
import time
# to read the arguments
import sys
from pathlib import Path


def configuringRedis(host='localhost' , port=6379, db=0):
    r = redis.StrictRedis(host , port, db)
    return r

def loggingSizeDataRedis(len_list, path):
    currentDT = datetime.now()
    f = open(path, "a")
    f.write(str(currentDT) + ": the size of the list from redis is: " + str(len_list)  + "\n")
    f.close()
    
def loggingExecutionTime(exectime, path):
    currentDT = datetime.now()
    f = open(path, "a")
    f.write(str(currentDT) + ": the execution time is: " + str(exectime)  + " seconds\n")
    f.close()
    
    
def main():
    redis_key = 'kdd-cup-99'
    if(len(sys.argv)>1):
        redis_key = sys.argv[1]

    redis_key_data = 'datatest' + redis_key
    redis_key_target = 'datatest' + redis_key + 'target'
    redis_key_model = redis_key + 'model'
    redis_key_output = 'outputs' + redis_key
    
    # Redis connection to connect the python program to Redis    
    redisConn = configuringRedis()
    if(len(sys.argv)>3):
        redisConn = configuringRedis(host=sys.argv[2], port=int(sys.argv[3]))
    
    #read the testing data from redis
    list_data = redisConn.lrange(redis_key_data, 0, -1)
    # log the number of records into a local file for debugging reasons
    log_path = str(Path().absolute()) + str(Path("/logs/testingdata_hoeffdingTreeClassifier.txt"))
    loggingSizeDataRedis(len(list_data), path=log_path)
    if not list_data:
        print("Please check if the testing data is stored properly in Redis!")
        return
        
    # read the labels from redis for the testing data
    list_output = redisConn.lrange(redis_key_target, 0, -1)
    if not list_output:
        print("Please check if the labels of the testing data are stored properly in Redis!")
        return
        
    # preparign the testing data
    df = pd.DataFrame([record.split(bytes(';', encoding = 'utf-8')) for record in list_data])
    del df[0]
    df[1]= df[1].str.decode(encoding = 'UTF-8')
    X_df = df[1].str.split(' ', expand=True)

    # preparing the labels for the testing dataset
    Y_df = pd.DataFrame([record.decode("UTF-8")  for record in list_output])
    Y_df.columns=['label']

    # parse all the types of the columns into float
    #X_df= X_df.astype('float')
    #Y_df= Y_df.astype('float')
    X_df = X_df.apply(pd.to_numeric, errors='ignore')
    Y_df= Y_df.apply(pd.to_numeric, errors='ignore')

    # store the testing data of the model as data stream
    stream = DataStream(data=X_df,y=Y_df.to_numpy())
    
    # loading the existing model from redis
    model = None
    try:
        model = pickle.loads(redisConn.get(redis_key_model))
    except Exception as ex:
        pass
    
    # if model doesn't exist then exit (model is not ready yet or something wrong in training phase
    if not model:
        return
    
    # predict the values of the testing data and append them to the list_outputs
    list_outputs = []
    while stream.has_more_samples():
        X, y = stream.next_sample()
        y_pred = model.predict(X)
        list_outputs.append(y_pred[0])
    
    # # transform the list to a string separated by comma
    # sep = ','
    # preds = sep.join(str(v) for v in list_outputs)
    # # save them in redis
    # redisConn.set(redis_key_output, preds)
    # redisConn.set(redis_key_output_sep, sep)
    
    for v in list_outputs:#push the outputs in redis (as a list)
        redisConn.rpush(redis_key_output, str(v)) 
    
    
start_time = time.time()  
main()
log_path_exec = str(Path().absolute()) + str(Path("/logs/exec_testing_hoeffdingTreeClassifier.txt"))
total_exec_time = time.time() - start_time
loggingExecutionTime(total_exec_time, path=log_path_exec)