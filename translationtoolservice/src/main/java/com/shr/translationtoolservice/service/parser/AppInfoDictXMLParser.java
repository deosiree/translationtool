package com.shr.translationtoolservice.service.parser;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.shr.translationtoolservice.service.parser.AppInfoDictXMLParser.AppDictInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 装置开发部，appDict.xml的xml文件
 */
@Slf4j
@Component
public class AppInfoDictXMLParser extends AbstractXMLParser<AppDictInfo>{

    @Override
    public XMLInfoContainer<AppDictInfo> parse(Document document) {

        if(document == null){
            throw new NullPointerException("document为null");
        }
        XMLInfoContainer<AppDictInfo> xmlInfoContainer = new XMLInfoContainer<>();
        NodeList nodeList = document.getElementsByTagName("ITEM");
        for(int i = 0 ; i < nodeList.getLength() ; i ++ ){
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            Node entry = attributes.getNamedItem("abbr");   // abbr写到entry字段上
            Node cnDesc = attributes.getNamedItem("cn_desc");
            String chinese = cnDesc.getTextContent();
            Node enDesc = attributes.getNamedItem("en_desc");
            String english = enDesc.getTextContent();
            Node esDesc = attributes.getNamedItem("es_desc");
            String spanish = esDesc.getTextContent();
            Node ruDesc = attributes.getNamedItem("ru_desc");
            String russian = ruDesc.getTextContent();
            AppDictInfo appDictInfo = new AppDictInfo();
            appDictInfo.setAbbr(entry.getTextContent());
            appDictInfo.setCnDesc(chinese);
            appDictInfo.setEnDesc(english);
            appDictInfo.setEsDesc(spanish);
            appDictInfo.setRuDesc(russian);
            xmlInfoContainer.addIFNotExists(appDictInfo);
        }
        return xmlInfoContainer;
    }


    public static class AppDictInfo{


        public String abbr;

        public String cnDesc;

        public String enDesc;

        public String esDesc;
        
        public String ruDesc;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((abbr == null) ? 0 : abbr.hashCode());
            result = prime * result + ((cnDesc == null) ? 0 : cnDesc.hashCode());
            result = prime * result + ((enDesc == null) ? 0 : enDesc.hashCode());
            result = prime * result + ((esDesc == null) ? 0 : esDesc.hashCode());
            result = prime * result + ((ruDesc == null) ? 0 : ruDesc.hashCode());
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
            AppDictInfo other = (AppDictInfo) obj;
            if (abbr == null) {
                if (other.abbr != null)
                    return false;
            } else if (!abbr.equals(other.abbr))
                return false;
            if (cnDesc == null) {
                if (other.cnDesc != null)
                    return false;
            } else if (!cnDesc.equals(other.cnDesc))
                return false;
            if (enDesc == null) {
                if (other.enDesc != null)
                    return false;
            } else if (!enDesc.equals(other.enDesc))
                return false;
            if (esDesc == null) {
                if (other.esDesc != null)
                    return false;
            } else if (!esDesc.equals(other.esDesc))
                return false;
            if (ruDesc == null) {
                if (other.ruDesc != null)
                    return false;
            } else if (!ruDesc.equals(other.ruDesc))
                return false;
            return true;
        }

        public String getAbbr() {
            return abbr;
        }

        public void setAbbr(String abbr) {
            this.abbr = abbr;
        }

        public String getCnDesc() {
            return cnDesc;
        }

        public void setCnDesc(String cnDesc) {
            this.cnDesc = cnDesc;
        }

        public String getEnDesc() {
            return enDesc;
        }

        public void setEnDesc(String enDesc) {
            this.enDesc = enDesc;
        }

        public String getEsDesc() {
            return esDesc;
        }

        public void setEsDesc(String esDesc) {
            this.esDesc = esDesc;
        }

        public String getRuDesc() {
            return ruDesc;
        }

        public void setRuDesc(String ruDesc) {
            this.ruDesc = ruDesc;
        }

    
    }
    
}
