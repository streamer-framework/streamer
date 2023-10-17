import torch
from torchmetrics import Metric, MeanSquaredError

class Score(Metric):

    is_differentiable: bool = True
    higher_is_better: bool = False
    full_state_update: bool = False

    def __init__(self, a1=10.0, a2=13.0):
        super().__init__()
        self.a1, self.a2 = a1, a2
        self.add_state("score", default=torch.tensor(0.0), dist_reduce_fx="sum")
        self.add_state("total", default=torch.tensor(0), dist_reduce_fx="sum")

    def update(self, preds, target):
        diff = torch.squeeze(preds-target)
        diff = torch.where(diff >= 0, torch.exp(diff/self.a1)-1, torch.exp(-diff/self.a2)-1)
        self.score += torch.sum(diff)
        self.total += target.numel()

    def compute(self):
        return self.score / self.total
    
class RMSE(MeanSquaredError):

    def __init__(self):
        super().__init__(squared=False)
