import os
import xml.etree.ElementTree as ET
from xml.etree.ElementTree import Element


class XmlParser:
    """

    """

    def __init__(self):
        """"""
        pass


    def parse(self,filePath):

        tree = ET.parse(filePath)
        self._parseInternal(tree.getroot())


    def _parseInternal(self,rootNode : Element):
        """
            
        """
        if rootNode is None:
            return
        
        
        
