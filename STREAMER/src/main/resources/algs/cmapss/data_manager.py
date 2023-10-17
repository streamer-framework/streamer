import torch
from collections import OrderedDict
import json
import pandas as pd
import random
from torch.utils.data import Dataset

### Main functions

def basic_data_preprocessing(df, par, other_params, columns_name):

    df = df.sort_values(by=['unit_nr', 'time_cycles'])

    ### Clip the RUL -> work normally before cycle = r_early
    if('r_early' in par) and (par['r_early'] is not None) and ('RUL' in df.columns):
        df["RUL"] = df["RUL"].clip(upper=par['r_early'])
    
    df[par['input_columns']] = min_max_normalization(df[par['input_columns']], other_params['min_df'], other_params['max_df'])

    if ('alpha_exp_smoothing' in par) and (par['alpha_exp_smoothing'] is not None):
        df = exponential_smoothing_by_groups(df, par['input_columns'], columns_name['group'], alpha=par['alpha_exp_smoothing'])

    return df

def min_max_normalization(df, train_min, train_max, feature_range=[-1, 1]):
    df_std = (df - train_min) / (train_max - train_min)
    min, max = feature_range
    df_scaled = df_std * (max - min) + min
    return df_scaled

def exponential_smoothing_by_groups(df, input_columns, groups, alpha=0.2):
    
    df_smooth = df.copy()
    df_smooth[input_columns] = df_smooth.groupby(groups, group_keys=False)[input_columns].apply(
                            lambda group: group.ewm(alpha=alpha).mean())
    
    return df_smooth

### FOR REDIS

def serialize_model(model):
    # Save Pytorch model to JSON
    params = [val.tolist() for _, val in model.state_dict().items()]
    return json.dumps(params)
    
def deserialize_model(model, json_model):
    # Load JSON model into Pytorch model
    params = json.loads(json_model)
    params_dict = zip(model.state_dict().keys(), params)
    state_dict = OrderedDict({key: torch.tensor(value) for key, value in params_dict})
    model.load_state_dict(state_dict)
    return model
    
### 

def load_data(mode, dir_path, state, par_redis, redis_conn):
    if mode == 'redis':
        with_target = (state != 'inference')
        return load_data_from_redis(redis_conn, par_redis, with_target)
    elif mode == 'pickle':
        if state == 'train':
            return load_df_client_from_pickle(dir_path, par_redis['key'])
        elif state == 'evaluate':
            return load_test_df_from_pickle(dir_path)
        else:
            return 0
    else:
        return 0
    

def load_data_from_redis(redis_conn, par_redis, with_target=True):
    print("Loading the data from redis")
    list_data = redis_conn.lrange(par_redis['key_data'], 0, -1)
    list_data = [string_data.decode('utf8').strip().split(";", 1)[1].split(" ") for string_data in list_data]

    index_names = ['unit_nr', 'time_cycles']
    setting_names = ['setting_1', 'setting_2', 'setting_3']
    sensor_names = ['s_0{}'.format(i) if i<10 else 's_{}'.format(i) for i in range(1, 22)]
    col_names = index_names + setting_names + sensor_names

    df = pd.DataFrame(list_data, columns=col_names)
    df[index_names] = df[index_names].astype(int)
    df[setting_names] = df[setting_names].astype(float)
    df[sensor_names] = df[sensor_names].astype(float)
    if(with_target):
        list_target = redis_conn.lrange(par_redis['key_target'], 0, -1)
        list_target = [string_target.decode('utf8').strip() for string_target in list_target]
        df_rul = pd.DataFrame(list_target, columns=['RUL'])
        df_rul['RUL'] = df_rul['RUL'].astype(int)
        df = pd.concat([df, df_rul], axis=1)
    
    return df

def load_df_client_from_pickle(data_dir_path, redis_key):
    df_path = data_dir_path + "/df_{}.pkl".format(redis_key)
    df = pd.read_pickle(df_path)
    return df

def load_test_df_from_pickle(data_dir_path):
    test_df_path = data_dir_path + "/df_test.pkl"
    test_df = pd.read_pickle(test_df_path)
    return test_df

### Auxiliaries functions

def shuffle_and_split_df_by_groups(df, split_value):
    groups = [df_inter for _, df_inter in df.groupby('unit_nr')]
    random.shuffle(groups)
    train_df = pd.concat(groups[:round(len(groups)*split_value)]).reset_index(drop=True)
    val_df = pd.concat(groups[round(len(groups)*split_value):]).reset_index(drop=True)
    return train_df, val_df

def create_seq_by_groups(df, input_columns, sequence_length):
    sequences = []
    for _, df_filtered in df.groupby('unit_nr', group_keys=False):
        data_size = len(df_filtered)
        for i in range(data_size+1-sequence_length):
            x = df_filtered.iloc[i:i+sequence_length][input_columns]
            y = df_filtered.iloc[i+sequence_length-1][["RUL"]]
            sequences.append([x.to_numpy(), y.to_numpy()])
    return sequences

def create_only_last_seq_by_groups(df, input_columns, sequence_length):
    sequences = []
    for _, df_filtered in df.groupby('unit_nr', group_keys=False):
        data_size = len(df_filtered)
        x = df_filtered.iloc[data_size-sequence_length:data_size][input_columns]
        y = df_filtered["RUL"].min()
        sequences.append([x.to_numpy(), [y]])
    return sequences

def create_seq_by_groups_without_label(df, input_columns, sequence_length):
    sequences = []
    list_idx_seq = []
    total_size = 0
    for _, df_filtered in df.groupby(['unit_nr'], group_keys=False):
        data_size = len(df_filtered)
        for i in range(data_size+1-sequence_length):
            x = df_filtered.iloc[i:i+sequence_length][input_columns]
            sequences.append(x.to_numpy())
            list_idx_seq.append(total_size+sequence_length+i-1)
        total_size += data_size
    return sequences, list_idx_seq

### FOR PYTORCH MODELS

class CMAPSSDataset(Dataset):

    def __init__(self, sequences):
        self.sequences = sequences
    
    def __len__(self):
        return len(self.sequences)

    def __getitem__(self, idx):
        sequence, label = self.sequences[idx]
        return dict(
            sequence = torch.Tensor(sequence),
            label = torch.Tensor(label)
        )

class CMAPSSDatasetWithoutLabel(Dataset):

    def __init__(self, sequences):
        self.sequences = sequences
    
    def __len__(self):
        return len(self.sequences)

    def __getitem__(self, idx):
        return dict(
            sequence = torch.Tensor(self.sequences[idx])
        )
