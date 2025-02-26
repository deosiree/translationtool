
from abc import ABC,abstractmethod
import os,tree_sitter,sys
from typing import List,Any
import tree_sitter_cpp as tsCpp
from tree_sitter import Language,Parser
sys.path.append("D:\\work\\translationtool\\test")
from script.utils.dic_parser import DicGeneral,DicParser

ENCODEING_TYPE = "utf-8"

class FunctionExtractor(ABC):

    def __init__(self,targetName,language):
        self._TARGET_FUN_NAME = targetName
        self._parser = Parser(language)


    def extractByRootNode(self,rootNode : tree_sitter.Node,resultList : List):
        """
            从rootNode开始扫描, 在节点中找到函数表达式, 并从函数表达式中提取目标函数函数, 提取的结果放到resultList中
            resultList根据需求可以变更

            可以用于扫描源文件的部分代码或全部代码
        """
        if rootNode is None:
            return
        for node in rootNode.children:
            if node.type.find("expression") != -1:
                result = self.extractFunction(node)
                if result is not None and len(result) > 0:
                    resultList.append(result)
               
            else:
                self.extractByRootNode(node,resultList)
        return   
    
    @abstractmethod
    def extractFunction(self,callExpressionNode : tree_sitter.Node) -> Any:
        """
            在一个表达式中寻找i18n_tr

            最终获得的结果是(funName,arg1,arg2,……)
        """
        pass
    
    
class i18nTrExtractor(DicGeneral,FunctionExtractor):

    def __init__(self,language):
        super().__init__("i18n_tr".encode(ENCODEING_TYPE),language)
        # self._parser = Parser(language)
    
    def extractByFile(self,filePath : str):
        """
            给定文件路径,提取对应文件的i18n_tr的相关参数
        """
        if not filePath.endswith(".cpp"):
            raise Exception("文件: {} 不是一个cpp文件".format(filePath))
        with open(filePath,mode = "r",encoding = ENCODEING_TYPE) as file:
            sourceCode = " ".join(file.readlines()).strip()
        return self.extractByCode(sourceCode)

    def extractByCode(self,sourceCode : str):
        """
            输入源代码字符串, 解析出来dic的字典, 例如
            [
                {
                    "comments": "",
                    "source": "初始化词条",
                    "tag": "",
                    "translation": {
                        "en_US": "init entry"
                    }
                }
            ]
        """
        tree = self._parser.parse(bytes(sourceCode,encoding = ENCODEING_TYPE))
        rootNode = tree.root_node
       
        resultList = []
        self.extractByRootNode(rootNode,resultList)
        
        return resultList
        
    def extractByRootNode(self,rootNode : tree_sitter.Node,resultList : List):
        """
            扫描源代码中每一个节点, 在节点中找到函数表达式, 并从函数表达式中提取i18n_tr函数
        """
        if rootNode is None:
            return
        for node in rootNode.children:
            if node.type.find("expression") != -1:
                result = self.extractFunction(node)
                if len(result) > 0:
                    resultList.append(result)
               
            else:
                self.extractByRootNode(node,resultList)
        return   

    def extractFunction(self,callExpressionNode : tree_sitter.Node):
        """
            在一个表达式中寻找i18n_tr

            最终获得的结果是(funName,arg1,arg2,……)
        """
        if callExpressionNode is None:
            return None
        if len(callExpressionNode.children) != 2:
            print("{} 对应的节点的childern列表的元素个数不等于2".format(callExpressionNode.text.decode(ENCODEING_TYPE)))

        """tree-sitter包将一个表达式分为了函数部分和参数列表部分"""
        expressionNode = self.extractFunctionName(callExpressionNode.children[0])
        if expressionNode is None:
            raise Exception("{} 中提取不到函数表达式".format(callExpressionNode.text.decode(ENCODEING_TYPE)))
        argumentNode = callExpressionNode.children[-1]

        if expressionNode.text != self._TARGET_FUN_NAME:
            """
                当前的表达式的函数名不是i18n_tr,但存在嵌套调用的情况

                callfun(i18n_tr("词条1"),i18n_tr("词条2","tag1","comment1"),……))
            """
            wholeResult = [] # 所有i18n_tr的参数的列表
            for node in argumentNode.children:
                nodeType = node.type
                if nodeType.find("expression") != -1:
                    """参数列表的某个参数是函数调用后返回的值"""
                    result = self.extractFunction(node)
                    
                    if result is None:
                        # 函数里面也没有调用i18n_tr函数
                        continue
                    else:
                        wholeResult.append(result)  # 代表在嵌套调用的内部存在调用i18n_tr函数
                elif nodeType == "(" or nodeType == ")" or nodeType == ",":
                    continue
                else:
                    continue
            return wholeResult
        else:
            """
                当前函数表达式函数名是i18n_tr
                i18n_tr("132")
                i18n_tr("132","conte")
                i18n_tr("c1","os1","la1")
            """
         
            result = []
            for node in argumentNode.children:
                # i18n_tr的内部不存在嵌套调用i18n_tr的情况,i18n_tr(i18n_tr())
                nodeType = node.type
                if nodeType == "(" or nodeType == ")" or nodeType == ",":
                    continue
                elif nodeType != "string_literal":
                    if node.text.decode(ENCODEING_TYPE) == "\\":
                        """
                            string a1 = i18n::i18n_tr("control all mechaine", \
                            "control","c1")
                        """
                        # 换行符的情况
                        continue
                    raise Exception("当前i18n_tr函数的参数存在非字符串类型,当前节点文本为: {}".format(argumentNode.text.decode(ENCODEING_TYPE)))
                else:
                    """
                        纯string类型对象,存放i18n_tr的每个参数, 例如
                        ["source","comment","tag"]
                    """
                    result.append(self.processString(node.text[1:-1].decode(ENCODEING_TYPE)))
            return result
        
    def processString(self,text : str):
        """
            cpp源文件读取的字符串如果出现'\'，ast树生成时会自动补充一个 "\ 
                例如：
                    "\t" ---> "\\t"
                    "\n" ---> "\\n"
                
            所以本方法的目的在于，消除ast树多补充的'\', 以及长字符串换行存在的问题（"\ \n"）

            如果\n前面有\\则删掉一个
        """

     
        subTextList = [subText.strip() for subText in text.split("\n")] # 消除换行带来的问题
        processText = "".join(subTextList)

        return processText
    
    def extractFunctionName(self,functionNameNode : tree_sitter.Node) -> tree_sitter.Node:
        """
            提取表达式调用的函数名
                例如iasp::i18n->i18n_tr提取出i18n_tr的对象
            这个方法的实现仅针对提取funname(arg1,arg2,……)这种情况
            对于 return (a+b)这种情况提取结果是存在问题的
    
        """
        if functionNameNode is None:
            return None
        nextNode = functionNameNode

        while True:
            if nextNode.children.__len__() == 0:
                return nextNode
            nextNode = nextNode.children[-1]

class TrExtractor(FunctionExtractor):

    def __init__(self,  language):
        super().__init__("tr".encode(ENCODEING_TYPE), language)

    def extractByRootNode(self,rootNode : tree_sitter.Node,resultList : List):
        """
            从rootNode开始扫描, 在节点中找到函数表达式, 并从函数表达式中提取目标函数函数, 提取的结果放到resultList中
            resultList根据需求可以变更

            可以用于扫描源文件的部分代码或全部代码
        """
        if rootNode is None:
            return
        for node in rootNode.children:
            if node.type.find("expression") != -1:
                result = self.extractFunction(node)
                if result is not None:
                    resultList.append(result)
            else:
                self.extractByRootNode(node,resultList)
        return   
        
    def extractFunction(self, callExpressionNode):
        """
            选择一个代表表达式的节点, 搜索这个节点下是否存在tr函数
        """
        if callExpressionNode is None:
            return None
        if len(callExpressionNode.children) != 2:
            print("{} 对应的节点的childern列表的元素个数不等于2".format(callExpressionNode.text.decode(ENCODEING_TYPE)))

        """tree-sitter包将一个表达式分为了函数部分和参数列表部分"""
        expressionNode = self.extractFunctionName(callExpressionNode.children[0])
        if expressionNode is None:
            raise Exception("{} 中提取不到函数表达式".format(callExpressionNode.text.decode(ENCODEING_TYPE)))
        
        argumentNode = callExpressionNode.children[-1]

        if expressionNode.text != self._TARGET_FUN_NAME:    

            for node in argumentNode.children:
                nodeType = node.type
                if nodeType.find("expression") != -1:
                    """参数列表的某个参数是函数调用后返回的值"""
                    result = self.extractFunction(node)
                    
                    if result is None:
                        # 函数里面也没有调用tr函数
                        continue
                    else:
                        return result  # 代表在嵌套调用的内部存在调用tr函数
                elif nodeType == "(" or nodeType == ")" or nodeType == ",":
                    continue
                else:
                    continue
            return None
        else:
            return expressionNode

    def extractFunctionName(self,functionNameNode : tree_sitter.Node) -> tree_sitter.Node:
        """
            提取表达式调用的函数名
                例如iasp::i18n->i18n_tr提取出i18n_tr的对象
            这个方法的实现仅针对提取funname(arg1,arg2,……)这种情况
            对于 return (a+b)这种情况提取结果是存在问题的
    
        """
        if functionNameNode is None:
            return None
        nextNode = functionNameNode

        while True:
            if nextNode.children.__len__() == 0:
                return nextNode
            nextNode = nextNode.children[-1]