
import os

class SystemExecutor:

    @classmethod
    def execute(cls,command):
        return os.system(command)
    
