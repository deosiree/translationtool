import os,sys
import logging,json
from typing import Dict, List
from .. import *
sys.path.append(SCRIPT_ROOT_PATH)
from script.check.check_grammer import GrammerChecker,TrailingReturnWithNamespaceChecker
from tree_sitter import Language,Parser,Node
from django.http import HttpRequest
import tree_sitter_cpp as tsCpp



class TrailingReturnWithNamespaceHandler:

    def __init__(self):
        """设定日志等级"""
        self.logger = logging.getLogger(__name__)
        self.logger.setLevel(logging.DEBUG)
        TEST_PY_PATH = os.path.dirname(__file__)
        streamHandler = logging.StreamHandler()
        fileHandler = logging.FileHandler(os.path.join(TEST_PY_PATH,"log.log"),encoding = "utf-8")
        formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
        # 打印到终端以及log中
        streamHandler.setLevel(logging.WARN)
        streamHandler.setFormatter(formatter)
        fileHandler.setLevel(logging.WARN)
        fileHandler.setFormatter(formatter)
        self.logger.addHandler(streamHandler)
        self.logger.addHandler(fileHandler)

        self._checker = TrailingReturnWithNamespaceChecker(Language(tsCpp.language()))

    def searchOnFile(self,filePath : str) -> Dict[str,str | List[str]] | None:
        """
            输入文件的绝对路径,检测该文件下是否存在函数定义存在后置返回值，函数体存在tr函数的情况
        """
        # self.logger.info("正在检查文件: %s",filePath)

        results : List[Node]= self._checker.checkByFile(filePath)
        outputTexts = ["line: {},auto {}".format(result.start_point.row,result.text.decode(ENCODING_TYPE)) for result in results]
        # if result:
        #     self.logger.warning("文件: %s 存在后置返回值有名称空间",filePath)
    
        # self.logger.info("文件: %s 检查完毕",filePath)
        return {
            "name": filePath,
            "methods": outputTexts
        } if len(outputTexts) > 0 else None

    def searchOnDir(self,dirPath : str,multipleFileOutputs : List[Dict]):
        """
            dirPath是一个文件夹的绝对路径，搜索文件夹以及其子文件夹下所有cpp文件
        """
        dirPath = os.path.abspath(dirPath)
        # self.logger.info("搜索的路径为: %s",dirPath)
        fileList=  os.listdir(dirPath)

        for file in fileList:
            filePath = os.path.join(dirPath,file)
            if os.path.isfile(filePath) and file.endswith(".cpp"):
                singleFileOutputs : Dict[str,str | List[str]] | None = self.searchOnFile(filePath) 
                if singleFileOutputs is not None:
                    multipleFileOutputs.append(singleFileOutputs)
            elif os.path.isdir(filePath):
                self.searchOnDir(filePath,multipleFileOutputs)
            else:
                continue # 文件后缀不是cpp

        # self.logger.info("路径: %s 搜索完成",dirPath)
        return


def checkNamespace(request : HttpRequest):
    """

    """
    requestBodyDict = json.loads(request.body)
    paramsDict : dict = requestBodyDict["params"]
    # 获取文件路径
    rootPath = paramsDict["checkURL"] if "checkURL" in paramsDict.keys() else None
    return checkNamespaceWithPath(rootPath)

def checkNamespaceWithPath(rootPath):
    searchPath = os.path.abspath(rootPath) if rootPath is not None else DEFAULT_SEARCH_PATH
    if not os.path.exists(searchPath):
        raise Exception("checkURL Error")
    checker = TrailingReturnWithNamespaceHandler()
    multipleFileOutputs : List[Dict] = []
    if os.path.isdir(searchPath):
        checker.searchOnDir(searchPath,multipleFileOutputs)
    elif os.path.isfile(searchPath) and searchPath.endswith(".cpp"):
        multipleFileOutputs.append(checker.searchOnFile(searchPath))
        # print(checker.searchOnFile(searchPath))
    else:
        pass    # 路径是一个文件，但这个文件不是cpp类型
    return {
        "totalNum": len(multipleFileOutputs),
        "list": multipleFileOutputs
    }