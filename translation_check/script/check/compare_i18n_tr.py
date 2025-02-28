from abc import ABC
import os,tree_sitter,sys
from typing import List
import tree_sitter_cpp as tsCpp
from tree_sitter import Language,Parser
sys.path.append("D:\\work\\translationtool\\test")
from script.utils.dic_parser import DicGeneral,DicParser
from script.utils.extractor import i18nTrExtractor

ENCODEING_TYPE = "utf-8"

class i18nTrCompartor:
    """
        i18nTr比较器, 利用cpp语言的解析器生成AST语法树, 随后根据对ast语法树进行解析比对生成的dic文件是否一致
    """
    def __init__(self):
        
        self._extractor = i18nTrExtractor(Language(tsCpp.language()))
        self._parser = DicParser()

    def compareFiles(self,sourceFilePath,dicFilePath):

        dicObject1 = self._extractor.__class__.constructList(self._extractor.extractByFile(sourceFilePath))
        dicObject2 = self._parser.parseFile(dicFilePath)

        return self.compareDicObject(dicObject1,dicObject2)

    def compareCodeWithFile(self,sourceCode,dicFilePath):

        dicObject1 = self._extractor.__class__.constructList(self._extractor.extractByCode(sourceCode))
        dicObject2 = self._parser.parseFile(dicFilePath)

        return self.compareDicObject(dicObject1,dicObject2)
        
    def compareDicObject(self,dicObject1,dicObject2):

        nodeDict1 = self._parser.parse(dicObject1)
        nodeDict2 = self._parser.parse(dicObject2)
        """
            比对两个dicNode字典是否是一样的

        """
        nodeSet1,nodeSet2 = set(nodeDict1.keys()),set(nodeDict2.keys())
        exceptionSet = nodeSet1.symmetric_difference(nodeSet2)
        leftMissingNodes,rightMissingNodes = {},{}
        if len(exceptionSet) != 0:
            """有的词条不存在于另一个词条集合里面"""
            for exceptionElement in exceptionSet:
                if exceptionSet in nodeSet1:
                    rightMissingNodes[exceptionElement] = nodeDict1[exceptionElement]
                elif exceptionSet in nodeSet2:
                    leftMissingNodes[exceptionElement] = nodeDict2[exceptionElement]
                else:
                    raise Exception("编程错误，存在节点既不属于第一个dic对象，也有不属于第二个dic对象的")   
           
        else:
            for node,leftCount in zip(nodeDict1.keys(),nodeDict1.values()):
                rightCount = nodeDict2[node]
                if rightCount < leftCount:
                    rightMissingNodes[node] = leftCount - rightCount
                elif rightCount > leftCount:
                    leftMissingNodes[node] = rightCount - leftCount
                else:
                    continue

        return leftMissingNodes,rightMissingNodes     

if __name__ == "__main__":
    extractor = i18nTrExtractor(Language(tsCpp.language()))

    cppFilePath = "D:\\src\\atc\\hello.cpp"
    resultList = extractor.extractByFile(cppFilePath)
   
    """
        a1\n  --> a1
        a1\\12--->a1\\12
        a1\t--->a1\\t
    """
    # print(resultList)
    print(extractor.__class__.constructList(resultList))
    # extractor.extractByCode("""string a1 = iasp::i18n::i18n_tr("12","control","c1")""") 
    # extractor.extractByCode("""string a1 = iasp::i18n::i18n_tr("12","control")""") 
    # extractor.extractByCode("""string a1 = iasp::i18n::i18n_tr("12")""") 
    # extractor.extractByCode("""string a1 = callfun(i18n_tr("词条1","fuck","1"),i18n_tr("词条2"));
    #                         """) 
    # extractor.extractByCode("""string a1 = i18n::i18n_tr("12","control")""") 
    # extractor.extractByCode("""string a1 = i18n::i18n_tr("12")""")  
    # extractor.extractByCode("""string a1 = i18n_tr("12","control","c1")""") 
    # extractor.extractByCode("""string a1 = i18n_tr("12","control")""") 
    # extractor.extractByCode("""string a1 = i18n_tr("12")""")  
    # extractor.extractByCode("""string a1 = i18n::i18n_tr("callfun("12"))")""")
    # extractor.extractByCode("""string a1 = iasp::i18n::i18n_tr("词条")""")
    # resultList = extractor.extractByCode("""string a1 = callfun(i18n_tr("calroe23 /r
    #                         caonc","23"),i18n_tr
    #                         ("c1","comment1", "tag1"),i18n_tr
    #                         ("xxxxx
    #                             xxxx"
    #                         )
    #                         )""")
    
    

"""
    提取代码中使用QObject::tr("")或tr("")
    call_expression function,identifier,argument_list,string_literal
    iasp::i18n::i18n_tr


    string a1 = i18n_tr("词条")
    {
        declaration: string a1 = i18n_tr("词条")
            type_identifier: string
            init_declarator: a1 = i18n_tr("词条")
                identifier: a1
                =: 
                call_expression: i18n_tr("词条")
                    identifier: i18n_tr
                    argument_list: ("词条")
                        (:
                        string_literal: "词条"
                            ": "
                            string_content: 词条
                            ": "
                        ):
            ;: ;
    }

    
    string a1 = i18n_tr("词条","","12"){
    
        declaration: string a1 = i18n_tr("词条","","12")
            type_identifier: string
            init_identifier: a1 = i18n_tr("词条","","12")
                identifier: a1
                =: =
                call_expression: i18n_tr("词条","","12")
                    identifier: i18n_tr
                    argument_list: ("词条","","12")
                        (: (
                        string_literal
                            ": "
                            string_content: 词条
                            ": "
                        ,: ,
                        string_literal
                            ": "
                            ": "
                        ,: ,
                        string_literal
                            ": "
                            string_content: 12
                            ": "
                        ): )
            ;: ;

    }

    callfun(i18n_tr(i18n_tr("12")))
    {
        express_statement: callfun(i18n_tr(i18n_tr("12")))
            call_expression: callfun(i18n_tr(i18n_tr("12")))
                identifier: callfun
                argument_list: (i18n_tr(i18n_tr("12")))
                    (: (
                    call_expression: i18n_tr(i18n_tr("12"))
                        identifier: i18n_tr
                        argument_list: (i18n_tr("12"))
                            (: (
                            call_expression: i18n_tr("12")
                                identifier: i18n_tr
                                argument_list: ("12")
                                    (: (
                                    string_literal: "12"
                                        ": "
                                        string_content: 12
                                        ": "
                                    ): )
                            ): )
                    ): )
            ;
    }

    string a1 = i18n::i18n_tr("词条")
    {
        declaration string a1 = i18n::i18n_tr("词条")
            type_identifier: string
            init_declarator: a1 = i18n::i18n_tr("词条")
                identifier: a1
                =: =
                call_expression: i18n::i18n_tr("词条")
                    qualified_identifier: i18n::i18n_tr
                        namespace_identifier: i18n
                        :: : ::
                        identifier: i18n_tr
                    argument_list:("词条")
                        (: (
                        string_literal: "词条"
                            ": "
                            string_content: 词条
                            ": "
                        ): )
            ;
    }

    string a1 = iasp::i18n::i18n_tr("词条"){
    
        declaration: string a1 = iasp::i18n::i18n_tr("词条")
            type_identifier: string
            init_declartor: a1 = iasp::i18n::i18n_tr("词条")
                identifier: a1
                =: =
                call_expression: iasp::i18n::i18n_tr("词条")
                    qualified_identifer: iasp::i18n::i18n_tr
                        namespace_identifier: iasp
                        :: : ::
                        qualified_identifier: i18n::i18n_tr
                            namespace_identifier: i18n
                            :: : ::
                            identifier: i18n_tr
                    argument_list: ("词条")
                        (: (
                        string_literal: "词条"
                            ": "
                            string_content: 词条
                            ": "
                        ): )
            ;
    
    }
"""