from typing import Any
from lightning import LightningModule
import torch.nn as nn
from torch.optim import Adam
from torchmetrics import MetricCollection

from metrics import RMSE, Score

class RUL_LSTM(LightningModule):

    def __init__(self, par):
        super().__init__()

        self.learning_rate = par['lr']
        self.model = SimpleLSTM(par['input_size'], par['n_hidden'], par['n_layers'])
        self.name = "LSTM"
        par['model_name'] = self.name
        par['criterion'] = 'nn.MSELoss'
        par['optimizer'] = 'Adam'

        self.criterion = nn.MSELoss()

        metrics = MetricCollection([RMSE()])
        self.train_metrics = metrics.clone(prefix='train/')
        self.val_metrics = metrics.clone(prefix='val/')

        metrics_test = MetricCollection([RMSE(), Score()])
        self.test_metrics = metrics_test.clone(prefix='test/')
        self.test_global_rmse = RMSE()

        self.save_hyperparameters(par)

    def forward(self, x):
        output = self.model(x)
        return output

    def on_train_start(self):
        # by default lightning executes validation step sanity checks before training starts,
        # so it's worth to make sure validation metrics don't store results from these checks
        pass

    def model_step(self, batch: Any):
        x, y = batch['sequence'], batch['label']
        y_hat = self.forward(x)
        loss = self.criterion(y_hat, y)
        return loss, y_hat, y

    def training_step(self, batch, batch_idx):
        loss, preds, targets = self.model_step(batch)
        self.log('train/loss', loss)
        output_metrics = self.train_metrics(preds, targets)
        self.log_dict(output_metrics)
        return loss

    def on_train_epoch_end(self):
        pass

    def validation_step(self, batch, batch_idx):
        loss, preds, targets = self.model_step(batch)
        self.log('val/loss', loss)
        output_metrics = self.val_metrics(preds, targets)
        self.log_dict(output_metrics)

    def on_validation_epoch_end(self):
        pass

    def test_step(self, batch, batch_idx):
        loss, preds, targets = self.model_step(batch)
        self.log('test/loss', loss)
        output_metrics = self.test_metrics(preds, targets)
        self.log_dict(output_metrics)
        self.test_global_rmse.update(preds, targets)
        return loss

    def on_test_epoch_end(self):
        global_rmse = self.test_global_rmse.compute()
        self.log('test/GlobalRMSE', global_rmse)
        self.test_global_rmse.reset()
    
    def predict_step(self, batch, batch_idx):
        x = batch['sequence']
        y_hat = self.forward(x)
        return y_hat

    def configure_optimizers(self):
        optimizer = Adam(self.parameters(), lr=self.learning_rate)
        return optimizer

class SimpleLSTM(nn.Module):

    def __init__(self, input_size, n_hidden, n_layers):
        super().__init__()
        self.lstm = nn.LSTM(
            input_size=input_size,
            hidden_size=n_hidden,
            batch_first=True,
            num_layers=n_layers,
        )
        self.regressor = nn.Linear(n_hidden, 1)

    def forward(self, x):
        self.lstm.flatten_parameters()
        _, (hidden, _) = self.lstm(x)
        out = hidden[-1]
        y_hat = self.regressor(out)
        return y_hat

