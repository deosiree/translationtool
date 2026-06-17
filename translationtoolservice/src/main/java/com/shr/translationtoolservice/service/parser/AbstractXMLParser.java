package com.shr.translationtoolservice.service.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractXMLParser<E> implements Parser {

    public abstract XMLInfoContainer<E> parse(Document document);

    public XMLInfoContainer<E> parse(InputStream inputStream){
        Document document = this.getDocument(inputStream);
        return this.parse(document);
    }

    protected final Document getDocument(InputStream inputStream){
        if(inputStream == null){
            return null;
        }
        /* 获取xml解析的内容 */
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        try {
            if(builder == null){
                log.error("builder为null");
                return null;
            }
            return builder.parse(inputStream);
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            log.error("XML文件解析异常");
            throw new RuntimeException(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        } 
    }

    public static class XMLInfoContainer<E>{

        Collection<E> collection;

        public XMLInfoContainer(){
            this.collection = new HashSet<>();
        }

        public XMLInfoContainer(Class<? extends Collection<E>> clazz){
            try {
                collection = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("");
            }
        }

        public boolean addIFNotExists(E xmlInfo){
            if(collection.contains(xmlInfo)){
                return false;
            }
            return this.collection.add(xmlInfo);
        }

        public void set(Collection<E> xmlInfos){
            this.collection = xmlInfos;
            return;
        }

        public Collection<E> getCollection() {
            return collection;
        }


    };

}
