

from typing import Dict
import xml.etree.ElementTree as ET
from xml.etree.ElementTree import Element

ENCODING_TYPE = "utf-8"
"""

比对name、source、comment、translation在两个文件中是否一致
<context>
    <name>QObject</name>
    <message>
        <source>任务名称</source>
        <comment>2</comment>
        <translation>Task name</translation>
    </message>
    <message>
        <source>是否删除该计算目标?</source>
        <translation>Whether to delete this compute target?</translation>
    </message>
    <message>
        <source>第%1行数据库为空，请重新输入</source>
        <translation>The database in line %1  is empty, please re-enter it</translation>
    </message>
<context>
<context>
    <name>CalcEditorCalcMdl</name>
    <message>
        <source>CalcEditorCalcMdl</source>
        <translation>CalcEditorCalcMdl</translation>
    </message>
    <message>
        <source>验证</source>
        <translation>Verify</translation>
    </message>
</context>

为了比对两个TS文件是否一致,则首先需要创建第一个TS文件对应的数据结构,
                    第一个文件中出现的次数，第二个文件中出现的次数
        node1 -------> 5次      5次
        node2 -------> 3次      4次
        ………

        nodeN -------> 0次      6次
        
        

"""

class Node:

    def __init__(self,text : str | None,attributes : dict):
        self._text = text
        if attributes is None:
            self._attributes = dict()
        elif isinstance(attributes,dict):
            self._attributes = attributes   # key和value必须可以执行hash函数
        else:
            raise Exception("attributes不是dict类型也不是None类型")
        
    @property
    def text(self):
        return self._text
    
    @property
    def attributes(self):
        return self._attributes
    
    def __hash__(self):
        hashValue = 7
        hashValue = 31 * hashValue + hash(self.text)
        for key,value in zip(self._attributes.keys(),self._attributes.values()):
            hashValue = 31 * hashValue + hash(key) + hash(value)
        return hashValue


    def __eq__(self, obj):
        if obj is None:
            return False
        if not isinstance(obj,Node):
            return False
        if hash(self) != hash(obj):
            return False
        return self._text == obj.text and self._attributes == obj.attributes



class ContextNode(Node):

    def __init__(self, attributes = None,nameNode = None):
        super().__init__(None, attributes)
        self._nameNode = nameNode
        self._messageNodes = dict() # node1 -------> 5次      5次

    @property
    def messageNodes(self):
        return self._messageNodes
    
    @property
    def nameNode(self):
        return self._nameNode

    @nameNode.setter
    def nameNode(self,nameNode):
        self._nameNode = nameNode
    
    def __hash__(self):
        hashValue = super().__hash__()
        for messageNode in self._messageNodes.keys():
            hashValue = 31 * hashValue + hash(messageNode)
        return hashValue

    def __eq__(self, obj):
        return \
            super().__eq__(obj) \
            and self._nameNode.__eq__(obj.nameNode) \
            and self._messageNodes == obj.messageNodes
    
    def addMessageNode(self,messageNode):
        if messageNode in self._messageNodes.keys():
            self._messageNodes[messageNode] += 1
        else:
            self._messageNodes[messageNode] = 1

    

    class NameNode(Node):

        def __init__(self, text, attributes = None):
            super().__init__(text, attributes)

    class MessageNode(Node):
        
        def __init__(self, text, attributes = None,sourceNode = None,translationNode = None):
            super().__init__(text, attributes)
            self._sourceNode = sourceNode
            self._translationNode = translationNode

        @property
        def sourceNode(self):
            return self._sourceNode
        
        @sourceNode.setter
        def sourceNode(self,sourceNode):
            self._sourceNode = sourceNode
            return

        @property
        def translationNode(self):
            return self._translationNode
        
        @translationNode.setter
        def translationNode(self,translationNode):
            self._translationNode = translationNode
            return
        
        def __hash__(self):
            hashValue = super().__hash__()
            hashValue = 31 * hashValue + hash(self._sourceNode)
            hashValue = 31 * hashValue + hash(self._translationNode)
            return hashValue

        def __eq__(self, obj):
            return super().__eq__(obj) \
            and self._sourceNode.__eq__(obj.sourceNode) \
            and self._translationNode.__eq__(obj.translationNode)

        class SourceNode(Node):

            def __init__(self, text, attributes = None):
                super().__init__(text, attributes)

        class TranslationNode(Node):

            def __init__(self, text, attributes = None):
                super().__init__(text, attributes)


    

class ConsistencyChecker:
    
    CONTEXT_TAG = "context"
    NAME_TAG = "name"
    MESSAGE_TAG = "message"
    SOURCE_TAG = "source"
    TRANSLATION_TAG = "translation"


    def checkConsistency(self,filePath : str):
        """
            检查一致性
        """

        contextNodeMap = dict() #   key代表contextNode,value代表出现这个contextNode的个数
        with open(filePath,mode = "r",encoding = ENCODING_TYPE) as tsFile:
            tree = ET.parse(tsFile)
            rootNode = tree.getroot()
            for subNode in rootNode:
                if subNode.tag == self.__class__.CONTEXT_TAG:
                    self.constructContextNodes(subNode,contextNodeMap)
                else:
                    continue
        
        return contextNodeMap

    def constructContextNodes(self,context : Element,contextNodeMap : dict):
        """
            根据ts文件创建ContextNode节点的集合
        """
        contextNode = ContextNode(attributes = context.attrib)


        for subNode in context:
            tag = subNode.tag
            if tag == self.__class__.NAME_TAG:
                contextNode.nameNode = ContextNode.NameNode(subNode.text,attributes = subNode.attrib)
            elif tag == self.__class__.MESSAGE_TAG:
                messageNode = self.constructMessageNode(subNode)
                contextNode.addMessageNode(messageNode)
            else:
                continue

        # 这个必须放到最后，因为ContextNode的hash值与它内部的成员变量的hash值有关
        if contextNode in contextNodeMap.keys():
            contextNodeMap[contextNode] += 1
        else:
            contextNodeMap[contextNode] = 1         

    def constructMessageNode(self,message : Element):

        messageNode = ContextNode.MessageNode(message.text,attributes = message.attrib)
        for subNode in message:
            tag = subNode.tag
            if tag == self.__class__.SOURCE_TAG:
                messageNode.sourceNode = \
                    ContextNode.MessageNode.SourceNode(subNode.text,attributes = subNode.attrib)
            elif tag == self.__class__.TRANSLATION_TAG:
                messageNode.translationNode = \
                    ContextNode.MessageNode.TranslationNode(subNode.text,attributes = subNode.attrib)
            else:
                # <location></location>
                continue
        
        return messageNode

if __name__ == "__main__":
    checker = ConsistencyChecker()
    map1 = checker.checkConsistency("D:\\work\\translationtool\\test\\calc_editor_calcmdl_en_US.ts")
    map2 = checker.checkConsistency("D:\\work\\translationtool\\test\calc_editor_calcmdl_en_US.ts")
    print(map1 == map2)