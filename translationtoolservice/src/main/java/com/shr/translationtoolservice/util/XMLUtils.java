// package com.shr.translationtoolservice.util;

// import java.io.IOException;
// import java.io.InputStream;
// import java.util.*;
// import java.util.stream.Collectors;

// import com.shr.translationtoolservice.entity.ConstantInterface;
// import com.shr.translationtoolservice.entity.EntryInfoEntity;
// import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;

// import io.micrometer.core.instrument.util.StringUtils;
// import lombok.extern.slf4j.Slf4j;

// import javax.xml.parsers.*;

// import org.w3c.dom.Document;
// import org.w3c.dom.NamedNodeMap;
// import org.w3c.dom.Node;
// import org.w3c.dom.NodeList;
// import org.xml.sax.SAXException;

// @Slf4j
// public class XMLUtils {

//     protected static class DocumentManager{

//         Document document = null;

//         public Document getDocument() {
//             return document;
//         }

//         public DocumentManager(Document document){
//             this.document = document;
//         }
//     }

//     public static abstract class AbstractXMLHandler<T>{

//         public abstract List<T> handle(DocumentManager documentManager);

//         public abstract List<T> handleInternal(DocumentManager documentManager);

//         public abstract void afterHandle(List<T> entities);

//     }
    
//     public static abstract class XMLHandler extends AbstractXMLHandler<EntryInfoEntity>{

//         public List<EntryInfoEntity> handle(DocumentManager documentManager){
//             List<EntryInfoEntity> entities = handleInternal(documentManager);
//             afterHandle(entities);
//             return entities;
//         }

//         public abstract List<EntryInfoEntity> handleInternal(DocumentManager documentManager);

//         public void afterHandle(List<EntryInfoEntity> entities){
//             for(EntryInfoEntity entity : entities){
//                 entity.setImportType(ConstantInterface.XML);
//             }
//         }
//     }

//     // public static class DefaultXMLHandler extends XMLHandler{

//     //     @Override
//     //     public List<EntryInfoEntity> handleInternal(DocumentManager documentManager) {
//     //         // TODO Auto-generated method stub
//     //         throw new UnsupportedOperationException("当前不支持解析XML文件");
//     //     }

//     // }

//     public static class XMLHandlerForEquipment extends XMLHandler {

//         @Override
//         public List<EntryInfoEntity> handleInternal(DocumentManager documentManager) {
//             // TODO Auto-generated method stub
//             List<EntryInfoEntity> entities = new ArrayList<>();
//             Document document = documentManager.getDocument();
//             if(document == null){
//                 throw new NullPointerException("document为null");
//             }
//             NodeList nodeList = document.getElementsByTagName("abbrinfo");
//             for(int i = 0 ; i < nodeList.getLength() ; i ++ ){
//                 Node node = nodeList.item(i);
//                 NamedNodeMap attributes = node.getAttributes();
//                 Node entry = attributes.getNamedItem("abbr");
//                 Node entrySource = attributes.getNamedItem("source_types");
//                 Node chineseInterpretation = attributes.getNamedItem("comments");
//                 EntryInfoEntity entity = new EntryInfoEntity();
//                 entity.setEntry(entry.getTextContent());
//                 entity.setEntrySource(entrySource.getTextContent());
//                 entity.setChineseInterpretation(chineseInterpretation.getTextContent());
//                 entities.add(entity);
//             }
//             return entities;
//         }
    
        
//     }

//     public static class XMLHandlerForEquipment2 extends XMLHandler{

//         @Override
//         public List<EntryInfoEntity> handleInternal(DocumentManager documentManager) {
//             // TODO Auto-generated method stub
//             List<EntryInfoEntity> entities = new ArrayList<>();
//             Document document = documentManager.getDocument();
//             if(document == null){
//                 throw new NullPointerException("document为null");
//             }
//             NodeList nodeList = document.getElementsByTagName("ITEM");
//             for(int i = 0 ; i < nodeList.getLength() ; i ++ ){
//                 Node node = nodeList.item(i);
//                 NamedNodeMap attributes = node.getAttributes();
//                 Node entry = attributes.getNamedItem("abbr");   // abbr写到entry字段上
//                 Node cnDesc = attributes.getNamedItem("cn_desc");
//                 String chinese = cnDesc.getTextContent();
//                 Node enDesc = attributes.getNamedItem("en_desc");
//                 String english = enDesc.getTextContent();
//                 Node esDesc = attributes.getNamedItem("es_desc");
//                 String spanish = esDesc.getTextContent();
//                 Node ruDesc = attributes.getNamedItem("ru_desc");
//                 String russian = ruDesc.getTextContent();

//                 EntryInfoEntity entity = new EntryInfoEntity();
//                 entity.setEntry(entry.getTextContent());
//                 /* 写中文翻译 */
//                 entity.setChinese(chinese);
//                 entity.setZhCharLength(chinese.length());
//                 entity.setChineseTranslateState(StringUtils.isEmpty(chinese) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

//                 /* 写英文翻译 */
//                 entity.setEnglish(english);
//                 entity.setEnCharLength(english.length());
//                 entity.setEnglishTranslateState(StringUtils.isEmpty(english) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

//                 /* 写西班牙语 */
//                 entity.setSpanish(spanish);
//                 entity.setSpaCharLength(spanish.length());
//                 entity.setSpanishTranslateState(StringUtils.isEmpty(spanish) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);

//                 /* 写俄文翻译 */
//                 entity.setRussian(russian);
//                 entity.setRuCharLength(russian.length());
//                 entity.setRussianTranslateState(StringUtils.isEmpty(russian) ? ConstantInterface.UNTRANSLATED : ConstantInterface.TRANSLATED);


//                 entity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
//                 entities.add(entity);
//             }
//             return entities;
//         }


//     }

//     public static List<EntryInfoEntity> parseXML(InputStream ins){
//         return parseXML(ins, null);
//     }

//     public static List<EntryInfoEntity> parseXML(InputStream ins,Class<? extends XMLHandler> xmlHandlerClazz){
//         if(ins == null || xmlHandlerClazz == null){
//             return new ArrayList<>();
//         }
//         /* 获取xml文本内容处理器 */
//         XMLHandler xmlHandler = null;
//         try {
//             xmlHandler = xmlHandlerClazz.newInstance();
//         } catch (Exception e){
//             throw new RuntimeException(e);
//         }
//         /* 获取xml解析的内容 */
//         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//         DocumentBuilder builder = null;
//         try {
//             builder = factory.newDocumentBuilder();
//         } catch (ParserConfigurationException e) {
//             // TODO Auto-generated catch block
//             throw new RuntimeException(e);
//         }
//         try {
//             if(builder == null){
//                 log.error("builder为null");
//                 return null;
//             }
//             DocumentManager documentManager = new DocumentManager(builder.parse(ins));
//             return xmlHandler.handle(documentManager);
//         } catch (SAXException e) {
//             // TODO Auto-generated catch block
//             log.error("XML文件解析异常");
//             throw new RuntimeException(e);
//         } catch (IOException e) {
//             // TODO Auto-generated catch block
//             throw new RuntimeException(e);
//         } 

//     }

// }
