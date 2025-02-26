
import os
from typing import List
import xml.etree.ElementTree as ET
from xml.etree.ElementTree import Element

class UIChecker:

    STRING_ATTRIBUTE_NOTR = """<string>标签是否存在notr = "file"属性"""

    @classmethod
    def checkStringAttrOnUi(cls,uiFilePath):
        """
            检查UI文件的<string>标签是否存在notr = "file"属性
        """

        tree= ET.parse(uiFilePath)
        return cls._checkStringAttrOnUiInternal(tree.getroot())

    @classmethod
    def _checkStringAttrOnUiInternal(cls,root : Element):
        """
            多叉树检测xml文件
        """
        if root is None:
            return False
        for element in root:
            # if element.tag == "string":
            #     notrValue = element.get("notr")
            #     if notrValue is not None and notrValue.lower() == "true":
            #         return True
            if cls._checkSingleElementNotrValue(element):
                return True
            # 防止递归调用出现StackOverFlow
            for subElement in element:
                if cls._checkSingleElementNotrValue(subElement):
                    return True
                if cls._checkStringAttrOnUiInternal(subElement):
                    return True
        return False
    
    @classmethod
    def _checkSingleElementNotrValue(cls,element : Element):
        if element.tag == "string":
            notrValue = element.get("notr")
            if notrValue is not None and notrValue.lower() == "true":
                return True
        return False


    @classmethod
    def check(cls,
              checkType : str,
              uiFilePath : str,
              logMessage : str | tuple[str, ...],
              logPath : str = None
    ):
        """给定UI文件的路径,选择检查项，并确定检查出对应结果后应当展示的信息"""
        try:
            """选择检查项"""
            if checkType == cls.STRING_ATTRIBUTE_NOTR:
                result = cls.checkStringAttrOnUi(uiFilePath)
            else:
                print("当前支持的检查类型不包含{}".format(checkType))

            """打印结果"""
            if result:
                if logPath is not None:
                    with open(logPath,encoding = "utf-8") as checkFile:
                        checkFile.newlines(logMessage)
                else:
                    print(logMessage)
        except Exception as e:
            print(f"❌ 解析 {uiFilePath} 时出错: {e}")
        finally:
            print("检查器 {} 执行检查任务类型: {} 完成".format(cls.__name__,checkType))



if __name__ == "__main__":

    uiFilePath = ""
    logMessage = "ui文件中string包含notr=true属性"
    UIChecker.check(UIChecker.STRING_ATTRIBUTE_NOTR,uiFilePath,logMessage)
    


        