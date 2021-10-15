
class Record:

    def __init__(self, record_string):
        self.timestamp, self.values = record_string.split(";", 1)
