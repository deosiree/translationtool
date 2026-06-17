package com.shr.translationtoolservice.service.parser;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.shr.translationtoolservice.service.parser.AbbrAppXMLParser.AbbrAppInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AbbrAppXMLParser extends AbstractXMLParser<AbbrAppInfo> {
    

    @Override
    public XMLInfoContainer<AbbrAppInfo> parse(Document document) {
        // TODO Auto-generated method stub
        if(document == null){
            throw new NullPointerException("document为null");
        }
        XMLInfoContainer<AbbrAppInfo> xmlInfoContainer = new XMLInfoContainer<>();
        NodeList nodeList = document.getElementsByTagName("abbrinfo");
        for(int i = 0 ; i < nodeList.getLength() ; i ++ ){
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            Node entry = attributes.getNamedItem("abbr");
            Node entrySource = attributes.getNamedItem("source_types");
            Node chineseInterpretation = attributes.getNamedItem("comments");
            AbbrAppInfo abbrAppInfo = new AbbrAppInfo();
            abbrAppInfo.setAbbr(entry.getTextContent());
            abbrAppInfo.setSourceTypes(entrySource.getTextContent());
            abbrAppInfo.setComments(chineseInterpretation.getTextContent());
            xmlInfoContainer.addIFNotExists(abbrAppInfo);
      
        }
        return xmlInfoContainer;
    }

    public static class AbbrAppInfo{

        public String abbr;

        public String sourceTypes;

        public String comments;

        public String getAbbr() {
            return abbr;
        }

        public void setAbbr(String abbr) {
            this.abbr = abbr;
        }

        public String getSourceTypes() {
            return sourceTypes;
        }

        public void setSourceTypes(String sourceTypes) {
            this.sourceTypes = sourceTypes;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((abbr == null) ? 0 : abbr.hashCode());
            result = prime * result + ((sourceTypes == null) ? 0 : sourceTypes.hashCode());
            result = prime * result + ((comments == null) ? 0 : comments.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AbbrAppInfo other = (AbbrAppInfo) obj;
            if (abbr == null) {
                if (other.abbr != null)
                    return false;
            } else if (!abbr.equals(other.abbr))
                return false;
            if (sourceTypes == null) {
                if (other.sourceTypes != null)
                    return false;
            } else if (!sourceTypes.equals(other.sourceTypes))
                return false;
            if (comments == null) {
                if (other.comments != null)
                    return false;
            } else if (!comments.equals(other.comments))
                return false;
            return true;
        }
    }


}
