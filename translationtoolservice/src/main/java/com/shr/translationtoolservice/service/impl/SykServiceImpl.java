package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;
import com.shr.translationtoolservice.service.SykService;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
public class SykServiceImpl implements SykService {
    private static final Logger log = LoggerFactory.getLogger(SykServiceImpl.class);
    @Autowired
    private TranslateMapper translateMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private TaskInfoMapper taskInfoMapper;
    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private ProductRelationMapper productRelationMapper;
    @Autowired
    private EntryInfoMapper entryInfoMapper;
    @Autowired
    private EntryClassifyMapper entryClassifyMapper;


    @Override
    public List<TranslateEntity> getSykEntry(TranslateEntity translate, Integer pageIndex, Integer pageSize) {

        return translateMapper.getSykTrans(translate, (pageIndex - 1) * pageSize, pageSize);
    }

    @Override
    public String updateSykEntry(List<TranslateEntity> translates) {
        int a = translateMapper.updateTranslation(translates);
        if (a == translates.size()) {
            return "success";
        }
        return "";
    }

    @Override
    public List<SykEntryVO> getSykEntryRelation(List<TranslateEntity> translates) {
        if (CollectionUtils.isEmpty(translates)) {
            return Collections.emptyList();
        }
        List<SykEntryVO> sykEntryVOS = new ArrayList<>();
        for (TranslateEntity translate : translates) {
            String id = translate.getId();
            QueryWrapper<EntryInfoEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_delete", 0);
            switch (translate.getType()){
                case ConstantInterface.ENGLISH:
                    queryWrapper.eq("en_trans_id", id);
                    break;
                case ConstantInterface.FRENCH:
                    queryWrapper.eq("fr_trans_id", id);
                    break;
                case ConstantInterface.SPANISH:
                    queryWrapper.eq("es_trans_id", id);
                    break;
                case ConstantInterface.RUSSIAN:
                    queryWrapper.eq("ru_trans_id", id);
                    break;
            }
            List<EntryInfoEntity> entities = entryInfoMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(entities)) {
                continue;
            }
            for (EntryInfoEntity entity : entities) {
                QueryWrapper<ProductRelationEntity> queryWrapper1 = new QueryWrapper<>();
                queryWrapper1.eq("entry_id", entity.getId());
                List<ProductRelationEntity> productRelationEntitys = productRelationMapper.selectList(queryWrapper1);


                if (!CollectionUtils.isEmpty(productRelationEntitys)) {
                    ProductRelationEntity productRelationEntity ;
                    if (productRelationEntitys.size() > 1) {
                        log.error("词条关联多个产品 词条id:{}", entity.getId());
                        continue;
                    }else  {
                        productRelationEntity = productRelationEntitys.get(0);
                    }

                    SykEntryVO sykEntryVO = new SykEntryVO();
                    ProductEntity productEntity = productMapper.selectById(productRelationEntity.getProductId());
                    sykEntryVO.setProductName(productEntity.getName());
                    if (StringUtils.isNotBlank(productRelationEntity.getVersionId())) {
                        sykEntryVO.setVersionName(versionMapper.selectById(productRelationEntity.getVersionId()).getName());
                    }

                    sykEntryVO.setTaskName(taskInfoMapper.selectById(productRelationEntity.getTaskId()).getName());
                    sykEntryVO.setId(entity.getId());
                    sykEntryVO.setEntry(translate.getEntry());
                    sykEntryVO.setTranslate(translate.getTranslate());
                    EntryClassify entryClassfyById = entryClassifyMapper.getEntryClassfyById(productEntity.getId());
                    EntryClassify entryClassfyByParentId = entryClassifyMapper.getEntryClassfyById(entryClassfyById.getParentId());
                    sykEntryVO.setClassify(entryClassfyByParentId.getTitle());

                    sykEntryVO.setAbbr(entity.getAbbr());
                    sykEntryVO.setEntrySource(entity.getEntrySource());
                    sykEntryVO.setDiName(entity.getDiFileName());
                    sykEntryVO.setUserName(entity.getUpdate());
                    sykEntryVO.setCreateTime(entity.getUpdateTime());
                    sykEntryVO.setTag(entity.getTag());
                    sykEntryVO.setEntryState(entity.getEntryState());
                    sykEntryVO.setTranslateState(translate.getTranslateState());
                    sykEntryVOS.add(sykEntryVO);
                }
            }

        }
        return sykEntryVOS;
    }

    @Override
    public List<TranslateEntity> getSykNotUsed(TranslateEntity translateTemplate,String token) {
        // String token = request.getHeader("token");
        // String department = JWTTokenUtils.getDepartment(token);
        String userName = JWTTokenUtils.getUserName(token);
        String department = translateTemplate.getVisualRange();
        if(department == null || department.equals("")){
            // 没有提供查询的部门则查本部门的
            translateTemplate.setVisualRange(JWTTokenUtils.getDepartment(token));
        }
        List<TranslateEntity> translateEntityList = getSykEntry(translateTemplate, -1, -1);
        // List<TranslateEntity> translateEntityList = translateMapper.selectList(new QueryWrapper<TranslateEntity>().eq("delete_state", 0).eq("visual_range", department));
        List<EntryInfoEntity> entities = entryInfoMapper.selectList(new QueryWrapper<EntryInfoEntity>().eq("is_delete", 0));
        processTranslateEntity(translateEntityList,entities);

        return translateEntityList.stream().filter(translateEntity -> translateEntity.getDeleteState() == 1).collect(Collectors.toList());
    }

    private void processTranslateEntity(List<TranslateEntity> translateEntityList,List<EntryInfoEntity> entities) {
        int numberOfThreads = 32; // Number of threads to use
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        int chunkSize = translateEntityList.size() / numberOfThreads;
        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numberOfThreads - 1) ? translateEntityList.size() : (i + 1) * chunkSize;
            List<TranslateEntity> sublist = translateEntityList.subList(start, end);

            new Thread(() -> {
                try {
                    for (TranslateEntity translateEntity : sublist) {
                        //开始全删
                        translateEntity.setDeleteState(1);

                        if (CollectionUtils.isEmpty(entities)) {
                            continue;
                        }

                        for (EntryInfoEntity entity : entities) {
                            //如果有一个词条使用了翻译 则不删除
                            if (translateEntity.getId().equals(entity.getEnTransId())
                                    || translateEntity.getId().equals(entity.getFraTransId())
                                    || translateEntity.getId().equals(entity.getSpaTransId())
                                    || translateEntity.getId().equals(entity.getRuTransId())) {
                                translateEntity.setDeleteState(0);
                                break;
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(); // Wait for all threads to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


    }
}
