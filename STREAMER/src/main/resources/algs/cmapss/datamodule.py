from typing import Any, Dict, Optional, List

from lightning import LightningDataModule
from torch.utils.data import DataLoader, Dataset

from data_manager import load_data
from data_manager import basic_data_preprocessing
from data_manager import shuffle_and_split_df_by_groups
from data_manager import create_seq_by_groups
from data_manager import create_only_last_seq_by_groups
from data_manager import create_seq_by_groups_without_label
from data_manager import CMAPSSDataset, CMAPSSDatasetWithoutLabel


class CMAPSSRULDataModule(LightningDataModule):
    """Example of LightningDataModule for C-MAPSS dataset.
    """

    def __init__(
        self,
        subset_name: str,
        input_columns: List[str],
        input_seq_length: int,
        train_val_split_value: int,
        batch_size: int,
        num_workers: int,
        pin_memory: bool,
        mode: str,
        dir_path: str,
        r_early: int = None,
        alpha_exp_smoothing: float = None,
        **kwargs
    ):
        super().__init__()

        # this line allows to access init params with 'self.hparams' attribute
        # also ensures init params will be stored in ckpt
        self.save_hyperparameters(logger=False, ignore=kwargs.keys())

        self.state = kwargs['state']
        self.par_redis = kwargs['par_redis']
        self.redis_conn = kwargs['redis_conn']
        self.other_params = {'min_df': kwargs['min_df'], 'max_df': kwargs['max_df']}

        self.data_train: Optional[Dataset] = None
        self.data_val: Optional[Dataset] = None
        self.data_test: Optional[Dataset] = None

    def setup(self, stage: Optional[str] = None):
        """Load data. Set variables: `self.data_train`, `self.data_val`, `self.data_test`.

        This method is called by lightning with both `trainer.fit()` and `trainer.test()`, so be
        careful not to execute things like random split twice!
        """
        # load and split datasets only if not loaded already
        if not self.data_train and not self.data_val and not self.data_test:

            df = load_data(
                mode=self.hparams.mode,
                dir_path=self.hparams.dir_path,
                state=self.state,
                par_redis=self.par_redis,
                redis_conn=self.redis_conn,
            )

            input_columns = self.hparams.input_columns
            df = basic_data_preprocessing(df, self.hparams, self.other_params, input_columns)

            if self.state == 'train':
            
                train_df, val_df = shuffle_and_split_df_by_groups(df, self.hparams.train_val_split_value)

                train_seq = create_seq_by_groups(train_df, input_columns, self.hparams.input_seq_length)
                val_seq = create_seq_by_groups(val_df, input_columns, self.hparams.input_seq_length)

                self.data_train = CMAPSSDataset(train_seq)
                self.data_val = CMAPSSDataset(val_seq)
            
            if self.state == 'evaluate':
                test_seq = create_only_last_seq_by_groups(df, input_columns, self.hparams.input_seq_length)
                self.data_test = CMAPSSDataset(test_seq)
            
            if self.state == 'inference':
                self.df = df
                seq, self.list_idx_seq = create_seq_by_groups_without_label(df, input_columns, self.hparams.input_seq_length)
                if seq != []:
                    self.data_test = CMAPSSDatasetWithoutLabel(seq)

    def train_dataloader(self):
        return DataLoader(
            dataset=self.data_train,
            batch_size=self.hparams.batch_size,
            num_workers=self.hparams.num_workers,
            pin_memory=self.hparams.pin_memory,
            shuffle=True,
        )

    def val_dataloader(self):
        return DataLoader(
            dataset=self.data_val,
            batch_size=self.hparams.batch_size,
            num_workers=self.hparams.num_workers,
            pin_memory=self.hparams.pin_memory,
            shuffle=False,
        )

    def test_dataloader(self):
        return DataLoader(
            dataset=self.data_test,
            batch_size=self.hparams.batch_size,
            num_workers=self.hparams.num_workers,
            pin_memory=self.hparams.pin_memory,
            shuffle=False,
        )

    def teardown(self, stage: Optional[str] = None):
        """Clean up after fit or test."""
        pass

    def state_dict(self):
        """Extra things to save to checkpoint."""
        return {}

    def load_state_dict(self, state_dict: Dict[str, Any]):
        """Things to do when loading checkpoint."""
        pass
