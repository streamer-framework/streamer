import redis
from datetime import datetime
import sys
from pathlib import Path
import pandas as pd
import pickle
#from skmultiflow.data import DataStream
import time
import json

from Record import Record
from sklearn.metrics import accuracy_score

import os

def configuringRedis(host='localhost', port=6379, db=0):
	r = redis.StrictRedis(host, port, db)
	return r


def loggingSizeDataRedis(len_list, path):
	dir = os.path.split(os.path.abspath(path))[0]
	if not os.path.exists(dir):
		os.mkdir(dir)
	f = open(path, "a")
	f.write(str(datetime.now()) + ": the size of the list from redis is: " + str(len_list) + "\n")
	f.close()


def loggingExecutionTime(execution_time, path):
	dir = os.path.split(os.path.abspath(path))[0]
	if not os.path.exists(dir):
		os.mkdir(dir)
	f = open(path, "a")
	f.write(str(datetime.now()) + ": the execution time is: " + str(execution_time) + " seconds\n")
	f.close()


class BaseTest:

	def __init__(self, dataset_key):
		self.dataset_key = dataset_key
		if len(sys.argv) > 1:
			self.dataset_key = sys.argv[1]

	def run(self):

		redis_key_data = 'datatest' + self.dataset_key
		redis_key_target = 'datatest' + self.dataset_key + 'target'
		redis_key_model = self.dataset_key + 'model'
		redis_key_output = 'outputs' + self.dataset_key
		redis_key_output_sep = 'separation' + self.dataset_key

		# Redis connection to connect the python program to Redis
		redis_conn = configuringRedis()
		if len(sys.argv) > 3:
			redis_conn = configuringRedis(host=sys.argv[2], port=int(sys.argv[3]))

        # reading the properties/hyper-parameters of the models if there's any / converting them to a dictionary
        # the key always starts with props__ + id of the application ( dataset_key )
        
        
		redis_key_properties = 'props__' + self.dataset_key
		model_parameters = None
		if redis_conn.exists(redis_key_properties):
			model_parameters = json.loads(redis_conn.lrange(redis_key_properties, 0, -1)[0])
			print("loading models parameters...\n")

		# read the training data from redis
		list_data = [Record(string_data.decode('utf8').strip()) for string_data in
					 redis_conn.lrange(redis_key_data, 0, -1)]

		print("passing {0} for tests\n".format(len(list_data)))

		# log the number of records into a local file for debugging reasons
		log_path = str(Path().absolute()) + str(Path("/logs/" + self.dataset_key +  "_testing-data.txt"))
		loggingSizeDataRedis(len(list_data), path=log_path)
		if not list_data:
			print("Please check if the testing data is stored properly in Redis!")
			return

		# read the labels from redis for the testing data
		list_output = redis_conn.lrange(redis_key_target, 0, -1)
		if not list_data:
			print("Please check if the labels of the testing data are stored properly in Redis!")
			return

		test_data, output = self.prepare_data(list_data, list_output)

		# loading the existing model from redis
		model = None
		try:
			model = pickle.loads(redis_conn.get(redis_key_model))
		except (AttributeError, EOFError, ImportError, IndexError, TypeError):
			pass

		# if model doesn't exist then exit (model is not ready yet or something wrong in training phase)
		if not model:
			print("model not trained yet.")
			return

		# predict the values of the testing data and append them to the list_pred
		list_pred = self.model_evaluate(model, test_data, output, model_parameters)

		# transform the list to a string separated by comma
		#sep = ','
		#predictions = sep.join(str(v) for v in list_pred)
		# save them in redis
		# print("accuracy is: {0}%".format(accuracy_score(output, list_pred) * 100))
		#redis_conn.set(redis_key_output, predictions)
		#redis_conn.set(redis_key_output_sep, sep)
		#print(type(list_pred))
		if type(list_pred) is tuple:
			#print("I am a tuple")
			means, stds = list_pred
			for i in range (len(means)):
				value = ""+str(means[i])+" "+str(means[i] - stds[i])+" "+str(means[i] + stds[i])
				redis_conn.rpush(redis_key_output, str(value))   #push the outputs in redis (as a list)
		else:
			#print("I am not a tuple")
			for v in list_pred: #push the outputs in redis (as a list)
				redis_conn.rpush(redis_key_output, str(v)) 

	@staticmethod
	def prepare_data(data, output):
		return data, output

	@staticmethod
	def model_evaluate(model, data, output_history, model_parameters) -> list:
		pass
