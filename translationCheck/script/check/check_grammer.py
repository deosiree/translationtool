
import os,sys
from typing import List
import tree_sitter
from tree_sitter import Language,Parser
import tree_sitter_cpp as tsCpp

sys.path.append(os.path.dirname(os.path.dirname(__file__)))
from utils.extractor import TrExtractor
"""
常规的类型
(translation_unit 
    (
        function_definition type: (primitive_type) 
        declarator: (
            function_declarator declarator: (identifier) 
            parameters: (
                parameter_list 
                    (parameter_declaration type: (primitive_type) declarator: (identifier)) 
                    (parameter_declaration type: (primitive_type) declarator: (identifier))
            )
        ) 
        body: (compound_statement)
    )
)
后置返回值类型
(translation_unit 
    (
        function_definition type: (placeholder_type_specifier (auto)) 
        declarator: 
            (
                function_declarator declarator: (identifier) 
                parameters: (
                    parameter_list 
                        (parameter_declaration type: (primitive_type) declarator: (identifier)) 
                        (parameter_declaration type: (primitive_type) declarator: (identifier))
                ) 
                (
                    trailing_return_type (type_descriptor type: (qualified_identifier scope: (namespace_identifier) name: (type_identifier)))
                )
            ) 
        body: (compound_statement)
    )
)
"""

ENCODEING_TYPE = "utf-8"

class GrammerChecker:
    """
        用来进行代码语法的检查，防止词条翻译工具使用出错
        前提: 代码本身能通过CPP编译器的检查
    """


    def __init__(self,language : Language):
        # Language(tsCpp.language())
        self._parser = Parser(language)

    
    def checkByFile(self,filePath):
        if filePath is None:
            raise FileNotFoundError("filePath为None")
        return

    def checkByCode(self,sourceCode):
        if sourceCode is None:
            raise Exception("传入的代码字符串是空的")
        return
    

class TrailingReturnWithNamespaceChecker(GrammerChecker):

    def __init__(self, language):
        super().__init__(language)
        self._trExtractor = TrExtractor(language)

    def checkByFile(self,filePath):
        """
            通过源代码文件检查是否存在表达式有后置返回值，函数体内部有调用tr函数
        """
        super().checkByFile(filePath)
        
        with open(filePath,mode = "r",encoding = ENCODEING_TYPE) as file:
            code =  "".join(file.readlines()).strip()
        result = self.checkByCode(code)
        return result

    def checkByCode(self,sourceCode : str):
        """
            通过源代码的字符串查找是否存在表达式有后置返回值，函数体内部有调用tr函数
        """
        super().checkByCode(sourceCode)
        # 代码解析成AST树
        tsTree : tree_sitter.Tree = self._parser.parse(bytes(sourceCode,encoding = ENCODEING_TYPE),encoding = ENCODEING_TYPE)
        result = []
        self._checkTrailingReturnWithNamespace(tsTree.root_node,result)
        return result
        
    def _checkTrailingReturnWithNamespace(
            self,treeNode: tree_sitter.Node,
            nodeList : List[tree_sitter.Node]
        ):
        """
            1、AST的节点的类型如果是trailing_return_type
            2、该节点内部存在类型为namespace_identifier（命名空间）的节点
            3、并且对应函数体里面存在tr函数
            则将对应节点存到nodeList中
            nodeList: 返回存在问题的AST树节点，可以用来记录问题代码的行号
        """
        if treeNode is None:
            return
        for node in treeNode.children:
            if node.type == "trailing_return_type":
                # -> 类型
                targetNode = node.children[-1]
                if self._isNameSpaceExist(targetNode):
                    # 存在后置返回值并且有命名空间
                    """
                        function_definition,这个节点包含函数体和函数参数，函数名
                        auto Func1
                            (ArgType1& a, ArgType2& b) 
                            -> isap::bool
                        { 

                        string s = tr("23");
                            return (a + b);
                        }
                    """
                    searchRootNode = treeNode.parent # 检查这个函数表达式对应的函数体是否调用了tr函数
                    resultList = []
                    self._trExtractor.extractByRootNode(searchRootNode,resultList)
                    if len(resultList) > 0:
                        # 后置返回值的表达式内部有存在使用tr
                        nodeList.append(searchRootNode)
                        return
                    else:
                        continue
                else:
                    # 存在后置返回值但没有命名空间，跳过
                    continue
            else:
                self._checkTrailingReturnWithNamespace(node,nodeList)
                        
        return 
    

    
    def _isNameSpaceExist(self,trailingReturnNode : tree_sitter.Node):
        """
            节点的文本格式：
            -> [返回类型]
            
        """
        if trailingReturnNode is None:
            return False
        for nextNode in trailingReturnNode.children:
            if nextNode.type == "namespace_identifier":
                return True
            if self._isNameSpaceExist(nextNode):
                return True
        return False
                

if __name__ == "__main__":

    checker = TrailingReturnWithNamespaceChecker(Language(tsCpp.language()))
    result = checker.checkByFile("D:\work\\translationtool\\test\\codeTest\\namespace\\test_case.cpp")

    print(result)


    