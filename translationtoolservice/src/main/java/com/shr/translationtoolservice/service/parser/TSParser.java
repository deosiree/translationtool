package com.shr.translationtoolservice.service.parser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
@Slf4j
public class TSParser extends AbstractXMLParser<DictionaryVO>{




    public XMLInfoContainer<DictionaryVO> parse(Document document, String langCode) {

        if(document == null){
            log.error("");
            throw new NullPointerException("document为null");
        }
        XMLInfoContainer<DictionaryVO> xmlInfoContainer = new XMLInfoContainer<>();
        // TODO Auto-generated method stub
        Set<DictionaryVO> entities = new HashSet<>();
        if(StringUtils.isBlank(langCode)){
            log.error("");
            throw new RuntimeException("");
        }

        NodeList contextList = document.getElementsByTagName("context");
        for(int i = 0 ;i < contextList.getLength() ; i ++ ){
            Node contextNode = contextList.item(i);
            NodeList contextChildNodes = contextNode.getChildNodes();
            /* 获取同属一个tag的词条 */
            String tag = "";
            for(int idx = 0 ; idx < contextChildNodes.getLength() ; idx ++ ){
                Node contextChildNode = contextChildNodes.item(idx);
                String nodeName = contextChildNode.getNodeName();
                if(nodeName.equals("name")){
                    /* tag标签 */
                    tag = contextChildNode.getTextContent();  // <name></name>
                    break;  // 只读第一个
                }
            }

            /* 获取词条  */
            for(int idx = 0 ; idx < contextChildNodes.getLength() ; idx ++ ){
                Node contextChildNode = contextChildNodes.item(idx);
                String nodeName = contextChildNode.getNodeName();
                if(nodeName.equals("message")){
                    /* 词条信息 */
                    NodeList messageChildNodes = contextChildNode.getChildNodes();  // <message></message>

                    DictionaryVO dictionaryVO = new DictionaryVO();
                    dictionaryVO.setTag(tag == null ? "" : tag);
                    for(int nextIdx = 0 ; nextIdx < messageChildNodes.getLength() ; nextIdx ++ ){
                        Node messageChildNode = messageChildNodes.item(nextIdx);
                        String messageNodeName = messageChildNode.getNodeName();
                        if(messageNodeName.equals("source")){
                            String source = messageChildNode.getTextContent();
                            if(StringUtils.isBlank(source)){
                                log.warn("");
                                continue;
                            }
                            dictionaryVO.setSource(source);
                        }else if(messageNodeName.equals("translation")){
                            HashMap<String,String> translationMap = new HashMap<>();
                            String translation = messageChildNode.getTextContent();
                            if(!StringUtils.isBlank(translation)){
                                /* 翻译不是空 */
                                translationMap.put(langCode, translation);   
                            }
                            dictionaryVO.setTranslation(translationMap);
                        }else if(messageNodeName.equals("comment")){
                            String comment = messageChildNode.getTextContent();
                            dictionaryVO.setComments(comment == null ? "" : comment);
                        }
                    }   
                    entities.add(dictionaryVO);
                }
            }

        }
        xmlInfoContainer.set(entities);
        return xmlInfoContainer;
    }

    @Override
    public XMLInfoContainer<DictionaryVO> parse(Document document) {
        throw new UnsupportedOperationException("请调用带 langCode 参数的重载方法");
    }

    public XMLInfoContainer<DictionaryVO> parse(InputStream inputStream , String langCode){
        Document document = this.getDocument(inputStream);
        return this.parse(document, langCode);
    }

}
