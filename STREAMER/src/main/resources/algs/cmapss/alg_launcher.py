import redis
import lightning as L
import torch
import pickle
import numpy as np
import argparse

from data_manager import serialize_model, deserialize_model
from datamodule import CMAPSSRULDataModule
from models import RUL_LSTM


def define_par(redis_key, state, redisIP, redisPort, data_mode):
    
    par = {}

    ### Parameters for the data

    data_par = {}
    data_par['dir_path'] = "../data/fl_cmapss/FD001_3_clients_txt"
    data_par['mode'] = data_mode
    data_par['train_val_split_value'] = 0.7
    data_par['input_seq_length'] = 31
    data_par['alpha_exp_smoothing'] = None
    data_par['batch_size'] = 64
    data_par['num_workers'] = 4
    data_par['pin_memory'] = True

    path_data_par = data_par['dir_path'] + "/par.pkl"
    with open(path_data_par, 'rb') as f:
        data_par_pkl = pickle.load(f)
    data_par.update(data_par_pkl)

    par['data'] = data_par

    ### Parameters for the model

    model_par = {}
    model_par['input_size'] = len(data_par['input_columns'])
    model_par['n_hidden'] = 8
    model_par['n_layers'] = 1
    model_par['lr'] = 0.001
    par['model'] = model_par


    ### Parameters for Redis

    redis_par = {}
    redis_par['key'] = redis_key
    redis_par['ip'] = redisIP
    redis_par['port'] = redisPort
    redis_par['key_model'] = redis_par['key'] + 'model'
    redis_par['key_metrics'] = redis_par['key'] + 'metrics'
    redis_par['key_data'] = 'data' + redis_par['key']
    redis_par['key_target'] = redis_par['key_data'] + 'target'
    redis_par['key_output'] = 'outputs' + redis_par['key']
    par['redis'] = redis_par

    ### Others parameters

    par['state'] = state
    #par['seed'] = 1
    par['n_epochs'] = 5

    return par

def launch_streamer_alg(redis_key, state, redisIP, redisPort, data_mode):

    par = define_par(redis_key, state, redisIP, redisPort, data_mode)

    redis_conn = configuringRedis(par['redis'])

    if 'seed' in par:
        L.seed_everything(par['seed'], workers=True)

    if state == 'init':
        init_process(redis_conn, par['model'], par['redis']['key_model'])
    else:

        model = load_model_from_redis(redis_conn, par['model'], par['redis']['key_model'])

        print("Instantiating datamodule <CMAPSSRULDataModule>")
        par_data = par['data']
        datamodule = CMAPSSRULDataModule(
            subset_name=par_data['subset_name'],
            input_columns=par_data['input_columns'],
            input_seq_length=par_data['input_seq_length'],
            train_val_split_value=par_data['train_val_split_value'],
            batch_size=par_data['batch_size'],
            num_workers=par_data['num_workers'],
            pin_memory=par_data['pin_memory'],
            mode=par_data['mode'],
            dir_path=par_data['dir_path'],
            r_early=par_data['r_early'],
            alpha_exp_smoothing=par_data['alpha_exp_smoothing'],
            redis_conn=redis_conn,
            state=par['state'],
            par_redis=par['redis'],
            min_df=par_data['min_df'],
            max_df=par_data['max_df']
        )

        if state == 'train':
            fit_process(model, datamodule, redis_conn, par['redis']['key_model'], par['n_epochs'])
        elif state == 'evaluate':
            evaluate_process(model, datamodule, redis_conn, par['redis']['key_metrics'])
        elif state == 'inference':
            inference_process(model, datamodule, redis_conn, par['redis']['key_output'])

    return 1

def configuringRedis(par_redis):
    host = par_redis['ip'] if 'ip' in par_redis else 'localhost'
    port = par_redis['port'] if 'port' in par_redis else 6379
    db = par_redis['db'] if 'db' in par_redis else 0
    r = redis.StrictRedis(host, port, db)
    return r

def init_process(redis_conn, par_model, redis_key_model):
    print("Initializing the model")

    print("Instantiating model <RUL_LSTM>")
    model = RUL_LSTM(par_model)

    serialized_model = serialize_model(model)
    redis_conn.set(redis_key_model, serialized_model)

def load_model_from_redis(redis_conn, par_model, redis_key_model):
    print("Loading the model from redis")
    serialized_model = redis_conn.get(redis_key_model)

    print("Instantiating model <RUL_LSTM>")
    model = RUL_LSTM(par_model)

    model = deserialize_model(model, serialized_model)
    return model

def fit_process(model, datamodule, redis_conn, redis_key_model, n_epochs):
    print("Fitting the model")
    
    print("Instantiating trainer <lightning.pytorch.trainer.Trainer>")
    callbacks = []
    trainer = L.pytorch.trainer.Trainer(
        max_epochs=n_epochs,
        logger=False,
        enable_checkpointing=False,
        enable_progress_bar=False,
    )

    print("Starting training!")
    trainer.fit(model=model, datamodule=datamodule)

    print("Fitting step lasts {} epochs".format(trainer.current_epoch))

    serialized_model = serialize_model(model)
    redis_conn.set(redis_key_model, serialized_model)

def evaluate_process(model, datamodule, redis_conn, redis_key_metrics):
    print("Evaluating the model")

    print("Instantiating trainer <lightning.pytorch.trainer.Trainer>")
    trainer = L.pytorch.trainer.Trainer(
        max_epochs=0,
        logger = False,
        enable_checkpointing=False,
        enable_progress_bar=False,
    )

    print("Starting testing!")
    metrics = trainer.test(model, datamodule)

    test_loss = metrics[0]["test/loss"]
    redis_conn.rpush(redis_key_metrics, str(test_loss))

def inference_process(model, datamodule, redis_conn, redis_key_output):
    print("Performing the inference of the data with the model")

    datamodule.setup()

    list_idx_seq = datamodule.list_idx_seq
    df = datamodule.df
    
    if datamodule.data_test is not None:

        dataloader = datamodule.test_dataloader()

        print("Instantiating trainer <lightning.pytorch.trainer.Trainer>")
        trainer = L.pytorch.trainer.Trainer(
            max_epochs=0,
            logger = False,
            enable_checkpointing=False,
            enable_progress_bar=False,
        )

        preds = trainer.predict(model, dataloader)
        preds = torch.cat(preds, 0)
        preds = preds.squeeze(-1)
        preds = preds.numpy()

        outputs=[]
        j=0
        for i in range(len(df.index)):
            if i in list_idx_seq:
                outputs.append(preds[j])
                j+=1
            else:
                outputs.append(np.nan)
    
    else:
        outputs = np.empty(len(df.index))
        outputs.fill(np.nan)

    df['output'] = outputs
    df = df.sort_index()
    for output in df['output']:
        redis_conn.rpush(redis_key_output, str(output))

    """
    # Metrics debugging
    df['error'] = df['output'] - df['RUL']
    df['squared_error'] = df['error'].pow(2)
    rmse = np.sqrt(df['squared_error'].mean())
    print('rmse: {}'.format(rmse))
    df['scored_error'] = df['error'].apply(lambda x: np.where(x >= 0, np.exp(x/10.0)-1, np.exp(-x/13.0)-1))
    score = df['scored_error'].sum()
    print('score: {}'.format(score))
    """

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("redis_key")
    parser.add_argument("state")
    parser.add_argument("redisIP")
    parser.add_argument("redisPort")
    parser.add_argument("data_mode")
    args = parser.parse_args()
    launch_streamer_alg(args.redis_key, args.state, args.redisIP, args.redisPort, args.data_mode)
