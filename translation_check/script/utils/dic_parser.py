
import json,os
from typing import Dict, List
import xml.etree.ElementTree as ET

from xml.etree.ElementTree import ElementTree,Element


class DicGeneral:

    COMMENTS_KEY = "comments"
    SOURCE_KEY = "source"
    TAG_KEY = "tag"
    TRANSLATION_KEY = "translation"

    @classmethod
    def constructList(cls,multipleArgumentList : List[List[str]]):
        dicList = []
        for argumentList in multipleArgumentList:
            # 解析每一条语句中的i18n_tr的相关参数   callfun(i18n_tr(p1,p2,p3),i18n_tr(p1,p2))
            dicList.append(cls.constructSingleList(argumentList))
        return dicList

    @classmethod
    def constructSingleList(cls,argumentList : List[str]):
        #         "comments": "",
        # "source": "新增工具 ",
        # "tag": "",
        # "translation": {
        #     "en_US": ""
        # }
        length = len(argumentList)
        return {
            cls.COMMENTS_KEY : "" if length <= 2 else argumentList[2],
            cls.SOURCE_KEY : "" if length <= 0 else argumentList[0],
            cls.TAG_KEY : "" if length <= 1 else argumentList[1],
            cls.TRANSLATION_KEY : {
                "en_US" : ""
            }
        }
    
class DicParser(DicGeneral):
    
    def __init__(self):
        pass

    @classmethod
    def parseFile(cls,dicFilePath : str):
        if not os.path.exists(dicFilePath):
            raise Exception("路径{} 不存在".format(dicFilePath))
        if not dicFilePath.endswith(".dic"):
            raise Exception("解析的文件不属于dic类型")
        with open(dicFilePath,mode = "r",encoding = "UTF-8") as dicFile:
            text = "".join(dicFile.readlines()).strip()
            parsedObject = json.loads(text.replace("\\","\\\\"),strict= False)

        return cls.parse(parsedObject)

    @classmethod
    def parse(cls,parsedObject : List[Dict[str,str]]):
        """
            解析利用json解析产生的object对象,实际上是一个list
           |-- node1 --|--node2--|--obj3--|
                |           |
                |           |
             出现次数     出现次数

        """
        nodeCollections : Dict[DicNode,int] = dict()
        for nextNode in parsedObject:
            nextDicNode = DicNode(
                nextNode[cls.SOURCE_KEY],
                nextNode[cls.TRANSLATION_KEY],
                nextNode[cls.COMMENTS_KEY],
                nextNode[cls.TAG_KEY]
            )
            curNum = nodeCollections.get(nextDicNode,-1)
            if curNum == -1:
                nodeCollections[nextDicNode] = 1
            else:
                nodeCollections[nextDicNode] = curNum + 1
        return nodeCollections
        
class DicNode(DicGeneral):

    def __init__(self,source : str,translation : dict[str,str],comment : str = None,tag : str = None):
        if source is None:
            raise Exception("source不能是None")
        self._source = source
        self._comment = comment
        self._tag = tag
        self._translation = translation

    @property
    def source(self):
        return self._source
    
    @property
    def comment(self):
        return self._comment
    
    @property
    def tag(self):
        return self._tag
    
    @property
    def translation(self):
        return self._translation
    
    def hashTranslation(self):
        """
            计算translation项的hash值
        """
        hashValue = 7
        for value in self.translation.values():
            hashValue = 31 * hashValue + hash(value)
        return hashValue
    
    def __str__(self):
        return f"""{self.__class__.SOURCE_KEY}: {self.source},
        {self.__class__.TAG_KEY}: {self.tag}, 
        {self.__class__.COMMENTS_KEY}: {self.comment}, 
        {self.__class__.TRANSLATION_KEY}: {self.translation}"""
    
    def __hash__(self):
        """
            根据对象的source,comment,tag以及translation的hash值获取节点最终的hash值
        """

        hashValue = 7
        hashValue = 31 * hashValue + hash(self.source)
        hashValue = 31 * hashValue + hash(0 if self.comment is None else self.comment)
        hashValue = 31 * hashValue + hash(0 if self.tag is None else self.tag)
        hashValue = 31 * hashValue + self.hashTranslation()

        return hashValue

    def __eq__(self, value):
        """
            两个节点是否相同
            计算两个节点的hash值
                如果不同,则认为两个节点不同
                否则
                    如果source,comment,tag的字符串存在一个不相同，则认为是不同的
                    否则
                        如果translation项中的key,value对存在不同，则认为不同
                        否则
                            相同
        """
        if value is None:
            raise Exception("value是None")
        if not isinstance(value,DicNode):
            return False
        if hash(value) != hash(self):
            return False
        if not \
            (self._source == value.source and \
            self._comment == value.comment and \
            self._tag == value.tag) :
            return False
        for selfKey,selfValue in zip(self._translation.keys(),self._translation.values()):
            if value.translation[selfKey] != selfValue:
                return False
        return True 
            

if __name__  == "__main__":
    dicPath = "D:\\work\\translationtool\\test\\lang\\dic\\jk\\gui_sec_privmgr.dic"
    # with open(dicPath,mode = "r",encoding = "utf-8") as file:
    #     text = "".join(file.readlines()).strip()
    #     obj = json.loads(text.replace("\\","\\\\"),strict= False)
    # print(obj)
    parser = DicParser()
    nodeDict = parser.parseFile(dicPath)
    
    # nodeDict = parser.parse(obj)
    for key,value in zip(nodeDict.keys(),nodeDict.values()):
        print(key,value)

  
    # d1 = dict()
    # node1 = DicNode("翻译",translation = {"en_US":""})
    # node2 = DicNode("翻译",translation = {"en_US":""})
    # d1[node1] = 1
    # d1[node2] = 2
    # print(d1[node1])
    # print(d1[node2])


