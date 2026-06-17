package com.shr.translationtoolservice.service.exporter;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.processor.groupby.DefaultEntryGroupbyStrategy;
import com.shr.translationtoolservice.service.processor.groupby.GeneralReplicatedVOType;
import com.shr.translationtoolservice.util.EntryUtils;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class TSExporter implements XMLExporter<EntryInfoEntity>{

    protected EntryUtils entryUtils;

    protected TLanguage language;

    protected Map<String,String> translateGetMethodMap = ConstantInterface.entryInfoEntityGetterTranslateMap();

    public TSExporter(EntryUtils entryUtils, TLanguage language) {
        this.entryUtils = entryUtils;
        this.language = language;
    }

    protected void convert(Document document, OutputStream outputStream){
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(domSource, streamResult);
            
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            log.error("");
        } catch(Exception e){
            log.error("");
        }
        return;

    }

    protected Element buildTSElement(Document document){
        Element tsElement = document.createElement("TS");
        tsElement.setAttribute("version", "2.1");
        tsElement.setAttribute("language",this.language.getCode());
        return tsElement;
    }

    protected Element buildContextElement(Document document,String tag,Method getTransMethod,Collection<EntryInfoEntity> entryInfos){
        if(entryInfos.isEmpty()){
            return null;
        }

        Element contextElement = document.createElement("context");

        Element nameElement = document.createElement("name");
        nameElement.setTextContent(StringUtils.isBlank(tag) ? "" : tag);

        contextElement.appendChild(nameElement);

        
        for(EntryInfoEntity entryInfo : entryInfos){
            String entry = entryInfo.getEntry();
            if(StringUtils.isBlank(entry)){
                log.warn("");
                continue;
            }
            String translation = null;
            try {
                translation = getTransMethod.invoke(entryInfo) == null ? "" : String.valueOf(getTransMethod.invoke(entryInfo));
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            Element messageElement = document.createElement("message");
            Element sourceElement = document.createElement("source");
            sourceElement.setTextContent(entry);
            Element translationElement = document.createElement("translation");
            translationElement.setTextContent(StringUtils.isBlank(translation) ? "" : translation);
            messageElement.appendChild(sourceElement);
            messageElement.appendChild(translationElement);
            contextElement.appendChild(messageElement);
        }

        return contextElement;

    }

    @Override
    public int export(Collection<EntryInfoEntity> collection, OutputStream outputStream) {
        /*
            要测GBK编码导出的文件能不能正常翻译
            测试特殊字符
            空值
        */ 
        DefaultEntryGroupbyStrategy strategy = new DefaultEntryGroupbyStrategy();
        strategy.addTargetAttribute("tag");
        /* 按tag对词条进行分组 */
        Map<GeneralReplicatedVOType, List<EntryInfoEntity>> entryInfoGroups = entryUtils.makeGroupMapForEntryInfoEntities(collection, strategy);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(documentBuilder == null){
            return -1;  // 构造失败
        }

        Document document = documentBuilder.newDocument();
        document.setXmlVersion("1.0");
        document.setXmlStandalone(true);
        
        Element tsElement = this.buildTSElement(document);
        document.appendChild(tsElement);

        String languageDesc = this.language.getName();
        String getTransMethodName = this.translateGetMethodMap.get(languageDesc);

        if(getTransMethodName == null){
            return -1;
        }
        Method getTransMethod = null;
        try {
            getTransMethod = EntryInfoEntity.class.getMethod(getTransMethodName);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

        for(Map.Entry<GeneralReplicatedVOType,List<EntryInfoEntity>> entryInfoGroup : entryInfoGroups.entrySet()){
            List<EntryInfoEntity> entryInfos = entryInfoGroup.getValue();
            String tag = entryInfos.get(0).getTag();
            Element contextElement = this.buildContextElement(document, tag, getTransMethod, entryInfos);
            tsElement.appendChild(contextElement);
        }

        this.convert(document, outputStream);
        return 0;
    }


}
