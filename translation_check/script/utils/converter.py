
import os
from system_utils import SystemExecutor

class CommandLineCombiner:

    _splitor = " "

    @classmethod
    def getSplitor(cls) -> str:
        return cls._splitor
    
    @classmethod
    def setSplitor(cls,newSplitor : str) -> None:
        cls._splitor = newSplitor
        return

    @classmethod
    def combine(cls,*args) -> str: 
        """
            根据传进来的参数组装对应需要执行的命令,传进来的参数之间有空格分隔
        """
        return cls._splitor.join(args)
    

class Converter:
    """
        用来进行QM和TS文件之间的相互转换
    """

    def __init__(self,binPath):
        """
            binPath: QT的bin文件夹的路径

        """
        self._lconvert = "lconvert.exe" # QM转TS
        self._lrelease = "lrelease.exe" # TS转QM
        self._lupdate = "lupdate.exe"   # 将文件夹中的源代码转变为ts文件,这部分已经通过generate_ts_linux.py实现了
        self._binPath = binPath

    def lconvert(self,*args) -> bool:
        """
            将QM文件转变为TS文件
            args: 输入到命令行中的参数
        """
        
        lconvertPath = os.path.join(self._binPath,self._lconvert)

        command = CommandLineCombiner.combine(lconvertPath,*args)
  
        return SystemExecutor.execute(command)
    
    def lrelease(self,*args):
        """
            将TS文件转变为QM文件
            args: 输入到命令行中的参数
        """

        lreleasePath = os.path.join(self._binPath,self._lrelease)

        command = CommandLineCombiner.combine(lreleasePath,*args)

        return SystemExecutor.execute(command)
    

class ExecutorBuilder:
    """
        用来创建执行器对应的对象,对于每种不同用途的对象,采用不同的模式获取，比如单例等
    """
    SINGLE_MODE = 1

    def __init__(self):
        self._mode = self.__class__.SINGLE_MODE
        self._converter : Converter = None

  
    def setMode(self,mode):
        pass

    def acquireConverter(self,binPath,mode = None) -> Converter:
        """
            用来获取执行lconverter.exe可执行文件或lrelease.exe可执行文件
        """
        while True:
            if mode is None:
                if self._mode == self.__class__.SINGLE_MODE:
                    if self._converter is None:
                        self._converter = Converter(binPath = binPath)    
                    return self._converter
                else:
                    print("mode暂时只支持{}".format("单例模式"))
                    return None
            else:
                self.setMode(mode)


if __name__ == "__main__":
    binPath = "D:/work/env/QT/5.12.4/msvc2017_64/bin"
    executorBuilder = ExecutorBuilder()
    converter = executorBuilder.acquireConverter(binPath)
    qmPath = "D:\work\\translationtool\\test\\lang\\en_US\\qm\\calc_editor_calcmdl_en_US.qm"
    # tsPath = "D:\work\\translationtool\\test\lang\zh_CN\\ts\\report_plugin_zh_CN.ts"
    newTsPath = "D:\work\\translationtool\\test\\calc_editor_calcmdl_en_US.ts"
    # converter.lrelease(tsPath,"-qm",qmPath)#有问题
    converter.lconvert("-i",qmPath,"-o",newTsPath,"-of","ts")