import json
import pickle
import sys
from datetime import datetime
from pathlib import Path

import redis

from Record import Record

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


class BaseTrain:

	def __init__(self, dataset_key):
		self.dataset_key = dataset_key
		if len(sys.argv) > 1:
			self.dataset_key = sys.argv[1]
		# Redis connection to connect the python program to Redis
		self.redis_conn = configuringRedis()
		if len(sys.argv) > 3:
			self.redis_conn = configuringRedis(host=sys.argv[2], port=int(sys.argv[3]))

	def run(self):

		#if len(sys.argv) > 1:
		#	self.dataset_key = sys.argv[1]

		# setting redis keys for connection
		redis_key_data = 'datatrain' + self.dataset_key
		redis_key_target = 'datatrain' + self.dataset_key + 'target'
		redis_key_model = self.dataset_key + 'model'

		# reading the properties/hyper-parameters of the models if there's any / converting them to a dictionary
		# the key always starts with props__ + id of the application ( dataset_key )

		redis_key_properties = 'props__' + self.dataset_key
		model_parameters = None
		if self.redis_conn.exists(redis_key_properties):
			model_parameters = json.loads(self.redis_conn.lrange(redis_key_properties, 0, -1)[0])
			print("loading models parameters...\n")
		# read the training data from redis
		list_data = [Record(string_data.decode('utf8').strip()) for string_data in
					 self.redis_conn.lrange(redis_key_data, 0, -1)]
		#print(list_data)
		print("pushing {0} rows for training".format(len(list_data)))

		# log the number of records into a local file for debugging reasons

		log_path = str(Path().absolute()) + str(Path("/logs/" + self.dataset_key + "_training-data.txt"))
		loggingSizeDataRedis(len(list_data), path=log_path)

		if not list_data:
			print("Please check if the training data is stored properly in Redis!")
			return

		# read the labels from redis for the training data

		list_output = self.redis_conn.lrange(redis_key_target, 0, -1)
		if not list_data:
			print("Please check if the labels of the training data are stored properly in Redis!")
			return

		data, output = self.prepare_data(list_data, list_output)

		# loading the existing model from redis

		try:
			if self.redis_conn.exists(redis_key_model):
				#print("loading the model")
				model = pickle.loads(self.redis_conn.get(redis_key_model))
			else:
				model = self.model_init(data, output, model_parameters)
		except (AttributeError, EOFError, ImportError, IndexError, TypeError):
			print("can't fetch pickle model. creating new one")
			model = self.model_init(data, output, model_parameters)

		#print(f"model shapelets size are {model.output_shapelet.size}")
		# update the model based on the records received for training
		# saving the model in redis
		model = self.model_fit(model, data, output, model_parameters)
		pickled_model = self.store_model(model)
		self.redis_conn.set(redis_key_model, pickled_model)

		# calling the post processing model method
		self.post_process_model(model)

	@staticmethod
	def model_init(data, output, parameters):
		pass

	@staticmethod
	def model_fit(model, data, output, parameters):
		pass

	@staticmethod
	def prepare_data(data, output):
		return data, output

	def post_process_model(self, model):
		return model

	def store_model(self, model):
		return pickle.dumps(model)
