package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.EntryCommonEntityService;
import com.shr.translationtoolservice.service.EntryProductEntityService;
import com.shr.translationtoolservice.service.EntryProjectEntityService;
import com.shr.translationtoolservice.service.EntryManagementService;
import com.shr.translationtoolservice.util.*;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @ClassName TermManagementServiceImpl
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/19 0019 16:58
 **/
@Service
@Slf4j
public class EntryManagementServiceImpl implements EntryManagementService {
    @Autowired
    private Translate translate;
    @Autowired
    private EntryProductEntityMapper entryProductEntityMapper;
    @Autowired
    private EntryProjectEntityMapper entryProjectEntityMapper;
    @Autowired
    private EntryCommonEntityMapper entryCommonEntityMapper;
    @Autowired
    private EntryProductEntityService entryProductEntityService;
    @Autowired
    private EntryCommonEntityService entryCommonEntityService;
    @Autowired
    private EntryMapper entryMapper;
    @Autowired
    private EntryOperateMapper entryOperateMapper;
    @Autowired
    private EntryVersionMapper entryVersionMapper;
    @Autowired
    private IndexMapper indexMapper;
    @Autowired
    private EntryClassifyMapper entryClassifyMapper;
    @Autowired
    private EntryLabelMapper entryLabelMapper;
    @Autowired
    private ThesaurusMapper thesaurusMapper;
    @Autowired
    private HTTPUtils httpUtils;
    @Autowired
    private EntryPropertyMapper entryPropertyMapper;

    @Autowired
    private CommonUtils commonUtils;


    @Override
    public ResponseListModel searchEntry(EntryEntity entryEntity, String entryState, Integer pageIndex, Integer pageSize) {
        //校验前端日期格式

        if (Objects.nonNull(entryEntity.getCreateTime()) && entryEntity.getCreateTime().toString().length() < 10) {
            String time = entryEntity.getCreateTime().toString() + ConstantInterface.TIME_ZERO;
            entryEntity.setCreateTime(new Date(time));
        }
        if (Objects.nonNull(entryEntity.getCreateEndRTime()) && entryEntity.getCreateEndRTime().toString().length() < 10) {
            String time = entryEntity.getCreateEndRTime().toString() + ConstantInterface.TIME_ZERO;
            entryEntity.setCreateEndRTime(new Date(time));
        }
        ResponseListModel<EntryEntity> responseListModel = new ResponseListModel<>();

        //如果包含标签，则需要查出该标签及其子集下所有的词条的内容
        if (StringUtils.isNotBlank(entryEntity.getClassifyId())) {
            List<String> ids = new ArrayList<>();
            ids.add(entryEntity.getClassifyId());
            List<EntryClassify> classfyList = getAllChildClassfy(ids);
            classfyList.add(entryClassifyMapper.selectById(entryEntity.getClassifyId()));
            responseListModel = getAllEntry(entryEntity, pageIndex, pageSize, entryState, classfyList);

        } else {
            responseListModel = getAllEntry(entryEntity, pageIndex, pageSize, entryState, Lists.newArrayList());
        }


        return responseListModel;
    }

    /**
     * 递归查询所有子节点
     */
    public List<EntryClassify> getAllChildClassfy(List<String> ids) {

        log.info(ids.toString());
        List<EntryClassify> entryClassfyByIds = entryClassifyMapper.getEntryClassfyByParentId(ids);
        if (!CollectionUtils.isEmpty(entryClassfyByIds)) {
            List<String> childIDs = entryClassfyByIds.stream().map(entryClassify -> entryClassify.getKey()).collect(Collectors.toList());
            entryClassfyByIds.addAll(getAllChildClassfy(childIDs));
            return entryClassfyByIds;
        } else {
            return Lists.newArrayList();
        }
    }


    //先查project表，不够再查 product ，最后再查comm

    public ResponseListModel<EntryEntity> getAllEntry(EntryEntity entryEntity, Integer pageIndex, Integer pageSize, String entryState, List<EntryClassify> classfyList) {

        ResponseListModel<EntryEntity> result = new ResponseListModel<>();
        int total = 0;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'");

        List<EntryEntity> entry = new ArrayList();

        List<String> tableNames1 = new ArrayList<>();

        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;

            entryEntity.setTableName(entryEntity.getTableName());
            List<EntryEntity> entryEntity1 = entryMapper.selectListByEntries(entryEntity, classfyList, pageSize, offset, entryState);

            total = entryMapper.selectListByEntriesTotal(entryEntity, entryState, classfyList);
            entry.addAll(entryEntity1);


        }
        result.setTotalNum(total);
        result.setList(entry);
        return result;
    }

    private String constructSql(EntryEntity entryEntity) {
        String sql = " select   id,abbr,entry,\n" +
                "        entry_length,chinese_interpretation,english_interpretation,\n" +
                "        entry_source,entry_state,creator,\n" +
                "        create_time,`update`,update_time,\n" +
                "        version,is_latest_version,entry_label,\n" +
                "        part_of_speech,classify_id,repeat_entry_id,\n" +
                "        english,english_length,english_translate_state,\n" +
                "        english_disable,english_disable_length,russian,\n" +
                "        russian_length,russian_translate_state,spanish,\n" +
                "        spanish_length,spanish_translate_state,french,\n" +
                "        french_length,french_translate_state " +
                entryEntity.getTableName() +
                " as tableName from " +
                entryEntity.getTableName();
        return sql;
    }

    @Override
    public List<EntryOperate> queryOperate(EntryOperate entryOperate) {

        QueryWrapper<EntryOperate> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(entryOperate.getId())) {
            queryWrapper.eq("id", entryOperate.getId());
        }
        if (StringUtils.isNotBlank(entryOperate.getEntryId())) {
            queryWrapper.eq("entry_id", entryOperate.getEntryId());
        }
        if (StringUtils.isNotBlank(entryOperate.getOperator())) {
            queryWrapper.eq("operator", entryOperate.getOperator());
        }
        if (Objects.nonNull(entryOperate.getStartOperateTime()) && Objects.nonNull(entryOperate.getEndOperateTime())) {
            queryWrapper.le("operate_time", entryOperate.getEndOperateTime());
            queryWrapper.ge("operate_time", entryOperate.getStartOperateTime());
        }
        queryWrapper.select("id", "operator", "DATE_FORMAT(operate_time, '%Y-%m-%d %H:%i:%s') as operate_time", "operate_content", "entry_id", "notes", "type");
        return entryOperateMapper.selectList(queryWrapper);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public List<EntryEntity> selectNoMergeEntry(String entry) {
        List<EntryEntity> entryEntities = new ArrayList<>();
        //合并和未合并sql 增加过滤
        entryEntities = entryMapper.selectNoMerge(entry).stream().distinct().collect(Collectors.toList());

        return entryEntities;
    }

    @Override
    public List<EntryEntity> selectMergeEntry(String entry) {
        List<EntryEntity> entryEntities = new ArrayList<>();
        //合并和未合并sql 增加过滤
        entryEntities = entryMapper.selectMerge(entry).stream().distinct().collect(Collectors.toList());

        return entryEntities;
    }

    @Override
    public String entryMerge(List<EntryEntity> entryEntities) {
        Index index = new Index();
        index.setId(commonUtils.getUUID());
        index.setRepeatEntryId(commonUtils.getUUID());
        int i = 0;
        if (entryEntities.size() > 0) {

            for (EntryEntity entryEntity : entryEntities) {
                i += 1;
                Index index1 = indexMapper.getIndexByEntry(entryEntity.getEntry());
                //如果存在 返回异常
                if (!Objects.isNull(index1)) {
                    return ErrorCodeList.OBJECT_HAS_EXIST;
                }


                //更新词条
                entryEntity.setRepeatEntryId(index.getRepeatEntryId());
                int update = entryMapper.updateById(entryEntity);
                if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                    return ErrorCodeList.UPDATE_ERROR;
                }
                //写入index
                index.setEntry(entryEntity.getEntry());
             /*   switch (i) {
                    case 1:
                        index.setTable1(entryEntity.getTableName());

                    case 2:
                        index.setTable2(entryEntity.getTableName());
                        break;
                    case 3:
                        index.setTable3(entryEntity.getTableName());
                        break;
                }*/

            }
        }
        int insert = indexMapper.insert(index);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    //TODO
    public List<EntryClassify> getEntryClassfy() {
        List<EntryClassify> entryClassifies = new ArrayList<>();
        entryClassifies = entryClassifyMapper.getEntryClassfyByIds();
// 返回的树形数据
        List<EntryClassify> tree = new ArrayList<EntryClassify>();
        // 第一次遍历
        for (EntryClassify classify : entryClassifies) {
            // 找到根节点，这里我的根节点的pid为0
            if (classify.getParentId().equals("0")) {
                tree.add(classify);
            }
            // 定义list用于存储子节点
            List<EntryClassify> children = new ArrayList<EntryClassify>();
            // 再次遍历list，找到子节点
            for (EntryClassify node : entryClassifies) {
                // 子节点的pid等于父节点的id
                if (node.getParentId().equals(classify.getKey())) {
                    children.add(node);
                }
            }
            // 给父节点设置子节点
            classify.setChildren(children);
        }
        return tree;
    }

    @Override
    public List<Thesaurus> getThesaurus() {
        QueryWrapper queryWrapper = new QueryWrapper();
        List<Thesaurus> thesauruses = thesaurusMapper.selectList(queryWrapper);
        return thesauruses;
    }

    @Override
    public String addEntryClassfy(EntryClassify entryClassify) {
        entryClassify.setKey(commonUtils.getUUID());
        int insert = entryClassifyMapper.insert(entryClassify);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String updateEntryClassfy(EntryClassify entryClassify) {
        int update = entryClassifyMapper.updateById(entryClassify);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String deleteEntryClassfy(List<String> idList) {
        int delete = entryClassifyMapper.deleteByIds(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public ResponseListModel<EntryLabel> queryLabel(EntryLabel entryLabel ,int pageIndex, int pageSize) {
        ResponseListModel<EntryLabel> result = new ResponseListModel<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            QueryWrapper<EntryLabel> queryWrapper = new QueryWrapper<>();

            List<EntryLabel> entryLabels = entryLabelMapper.getLabels(entryLabel,pageSize, offset);
            result.setList(entryLabels);
            int total = entryLabelMapper.getLabelsTotal(entryLabel,pageSize, offset);
            result.setTotalNum(total);
        }
        return result;
    }

    @Override
    public String deleteLabel(List<String> idList) {
        int delete = entryLabelMapper.deleteBatchIds(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.DELETE_ERROR;
        }
        //int delete = entryLabelMapper.deleteLabel(idList);
        return ConstantInterface.OK_STR;
    }

    @Override
    public String addLabel(EntryLabel entryLabel) {
        String uuid = commonUtils.getUUID();
        entryLabel.setId(uuid);
        List<EntryLabel> entryLabels = entryLabelMapper.selectList(new QueryWrapper<>());
        for (EntryLabel entryLabel1 : entryLabels) {
            if (entryLabel1.getLabelName().equals(entryLabel.getLabelName())) {
                return ErrorCodeList.OBJECT_HAS_EXIST;
            }
        }
        int insert = entryLabelMapper.insert(entryLabel);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return uuid;
    }

    @Override
    public String updateLabel(EntryLabel entryLabel) {
        int update = entryLabelMapper.updateById(entryLabel);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryProperty> queryEntryProperty(EntryProperty entryProperty) {
        QueryWrapper<EntryProperty> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(entryProperty.getPropertyName())) {
            queryWrapper.eq("property_name", entryProperty.getPropertyName());
        } else if (StringUtils.isNotBlank(entryProperty.getId())) {
            queryWrapper.eq("id", entryProperty.getId());
        }


        List<EntryProperty> entryProperties = entryPropertyMapper.selectList(new QueryWrapper<>());

        return entryProperties;
    }


    @Override
    public HttpResponse<EntryEntity> insertEntry(EntryEntity entryEntity, HttpServletRequest request) {
        HttpResponse<EntryEntity> response = new HttpResponse<>();
        entryEntity.setTableName(ConstantInterface.COMMON_TABLE_Name);
        List<EntryEntity> entryEntities = entryMapper.selectByAbbr(entryEntity.getAbbr(), entryEntity.getVersion());
        List<EntryEntity> entryEntities1 = entryMapper.selectByName(entryEntity);
        //校验ABBR 重复
        if (!CollectionUtils.isEmpty(entryEntities)) {
            response.setMessage(ErrorCodeList.ABBR_HAS_EXIST);
            response.setCode(HttpResponse.Type.ERROR.getVal());
            response.setType(HttpResponse.Type.ERROR);
            return response;
            //校验NAME 重复
        } else if (!CollectionUtils.isEmpty(entryEntities1)) {
            response.setMessage(ErrorCodeList.NAME_EXIST);
            response.setCode(HttpResponse.Type.ERROR.getVal());
            response.setType(HttpResponse.Type.ERROR);
            return response;
        }
        String uuid = commonUtils.getUUID();
        entryEntity.setId(uuid);
        //构建字符长度
        constructEntry(entryEntity);
        if (Objects.isNull(entryEntity.getEntryState())) {
            entryEntity.setEntryState(1);
        }

        //创建人
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        if (StringUtils.isBlank(entryEntity.getCreator())) {
            entryEntity.setCreator(userName);
        }
        if (Objects.isNull(entryEntity.getCreateTime())) {
            Date date = new Date(System.currentTimeMillis());
            entryEntity.setCreateTime(date);
        }


     /*   String lable = entryEntity.getEntryLabel();
        if (StringUtils.isNotBlank(lable)) {
            //插入标签
            QueryWrapper<EntryLabel> queryWrapper = new QueryWrapper();
            queryWrapper.eq("label_name", lable);
            EntryLabel entryLabel = entryLabelMapper.selectOne(queryWrapper);
            if (!Objects.isNull(entryLabel)) {
                response.setMessage("the table  lable  reuse !");
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }
            EntryLabel entryLabel1 = new EntryLabel();
            entryLabel1.setId(commonUtils.getUUID());
            entryLabel1.setLabelName(lable);
            entryLabelMapper.insert(entryLabel1);
        }*/


        int insert = entryMapper.insert(entryEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            response.setMessage(ErrorCodeList.INSERT_ERROR);
            response.setCode(HttpResponse.Type.ERROR.getVal());
            response.setType(HttpResponse.Type.ERROR);
            return response;
        }
        EntryOperate entryOperate = new EntryOperate();

        entryOperate.setOperateContent("新增词条");
        int insert1 = constructOperate(entryOperate, entryEntity.getTableName(), entryEntity.getId(), request);


        if (insert1 != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            response.setMessage(ErrorCodeList.INSERT_ERROR);
            response.setCode(HttpResponse.Type.ERROR.getVal());
            response.setType(HttpResponse.Type.ERROR);
            return response;
        }
        response.setMessage(ErrorCodeList.SUCCESS);
        response.setCode(HttpResponse.Type.OK.getVal());
        response.setType(HttpResponse.Type.OK);
        response.setData(entryEntity);
        return response;
    }

    private void constructEntry(EntryEntity entryEntity) {
        if (StringUtils.isNotBlank(entryEntity.getEntry())) {
            entryEntity.setEntryLength(entryEntity.getEntry().length());
        }
        if (StringUtils.isNotBlank(entryEntity.getEnglish())) {
            entryEntity.setEnglishLength(entryEntity.getEnglish().length());
            entryEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getEnglishDisable())) {
            entryEntity.setEnglishDisableLength(entryEntity.getEnglishDisable().length());
        }
        if (StringUtils.isNotBlank(entryEntity.getRussian())) {
            entryEntity.setRussianLength(entryEntity.getRussian().length());
            entryEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getSpanish())) {
            entryEntity.setSpanishLength(entryEntity.getSpanish().length());
            entryEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getFrench())) {
            entryEntity.setFrenchLength(entryEntity.getFrench().length());
            entryEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
        }
        //是否最新版本
        if (StringUtils.isNotBlank(entryEntity.getVersion())) {
            EntryVersion newVersion = entryVersionMapper.getNewVersion();
            if (entryEntity.getVersion().equals(newVersion.getName())) {
                entryEntity.setIsLatestVersion(1);
            } else {
                entryEntity.setIsLatestVersion(0);
            }
        }
//        Index index = indexMapper.getIndexByEntry(entryEntity.getEntry());
//        if (!Objects.isNull(index)) {
//            entryEntity.setRepeatEntryId(index.getRepeatEntryId());
//        }

        //entryEntity.setEntryState(2);


    }


    @Override
    public ResultObject updateEntry(EntryEntity entryEntity, HttpServletRequest request) {

        entryEntity.setTableName(ConstantInterface.COMMON_TABLE_Name);
        EntryEntity beforEntry = entryMapper.selectById(entryEntity.getId());

        //abbr 重复判断
        if (StringUtils.isNotBlank(entryEntity.getAbbr())) {
            String version = "";
            if (StringUtils.isNotBlank(entryEntity.getVersion())) {
                version = entryEntity.getVersion();
            } else {
                version = beforEntry.getVersion();
            }
            List<EntryEntity> entryEntities = entryMapper.selectByAbbr(entryEntity.getAbbr(), version);
            entryEntities.removeIf(entryEntity1 -> entryEntity1.getId().equals(entryEntity.getId()));
            if (!CollectionUtils.isEmpty(entryEntities)) {


                return new ResultObject(ErrorCodeList.ABBR_HAS_EXIST);
            }
        }
        constructEntry(entryEntity);

       /* //label  判断重复
        String lable = entryEntity.getEntryLabel();

        if (StringUtils.isNotBlank(lable)) {
            //插入标签
            QueryWrapper<EntryLabel> queryWrapper = new QueryWrapper();
            queryWrapper.eq("label_name", lable);
            EntryLabel entryLabel = entryLabelMapper.selectOne(queryWrapper);
            if (!Objects.isNull(entryLabel)) {
                return "the table  lable  reuse !";
            }
            EntryLabel entryLabel1 = new EntryLabel();
            entryLabel1.setId(commonUtils.getUUID());
            entryLabel1.setLabelName(lable);
            entryLabelMapper.insert(entryLabel1);
        }
*/

        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        entryEntity.setUpdate(userName);
        entryEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        int update = entryMapper.updateById(entryEntity);
        EntryEntity resultEntryEntity = entryMapper.selectById(entryEntity.getId());
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {

            return new ResultObject(ErrorCodeList.UPDATE_ERROR);

        }
        if (Objects.isNull(entryEntity.getEntryState())) {
            entryEntity.setEntryState(beforEntry.getEntryState());
        }
        //更新操作记录表
        EntryOperate entryOperate = new EntryOperate();
        List<ComparisonResult> results = new ArrayList<>();
        OperateContentEntity operateContentEntity = new OperateContentEntity();
        try {
            EntryEntity afterEntry = entryMapper.selectById(entryEntity.getId());
            results = CompareUtils.compareFields(beforEntry, afterEntry, EntryEntity.class);
            if (results.size() == 0) {
                log.error(" t_entry_operate no change ! ");
                return new ResultObject(ErrorCodeList.INSERT_ERROR);
            }
            operateContentEntity.setResults(results);
            operateContentEntity.setEntryID(entryEntity.getId());
            String res = " ";
            //操作记录写入
            for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
                String name = comparisonResult.getKey();
                //不写入操作内容的字段
                if ("update".equals(name) || "updateTime".equals(name) || "entryLength".equals(name)
                        || "englishLength".equals(name) || "englishTranslateState".equals(name) || "englishDisable".equals(name) || "englishDisableLength".equals(name)
                        || "russianLength".equals(name) || "russianhTranslateState".equals(name) || "russianDisable".equals(name) || "russianDisableLength".equals(name)
                        || "spanishLength".equals(name) || "spanishhTranslateState".equals(name) || "spanishDisable".equals(name) || "spanishDisableLength".equals(name)
                        || "frenchLength".equals(name) || "frenchhTranslateState".equals(name) || "frenchDisable".equals(name) || "frenchDisableLength".equals(name)

                ) {
                    continue;
                }
                HashMap<String, String> entryName = constructEntryName();
                String str = "";
                String r1 = comparisonResult.getPrevious();
                String r2 = comparisonResult.getLater();
                if (StringUtils.isBlank(r1)) {

                    if ("classifyId".equals(comparisonResult.getKey())) {
                        str = entryName.get(comparisonResult.getKey()) + "新增值为 ( " + entryClassifyMapper.selectById(r2).getTitle() + " )  ";
                    } else {
                        str = entryName.get(name) + " 新增值为： " + r2;
                    }
                } else {

                    if ("classifyId".equals(comparisonResult.getKey())) {
                        str = entryName.get(comparisonResult.getKey()) + " 值由 ( " + entryClassifyMapper.selectById(r1).getTitle() + " ) 改为 ( " + entryClassifyMapper.selectById(r2).getTitle() + " )  ";
                    } else {
                        str = entryName.get(name) + " 值由 ( " + r1 + " ) 改为 ( " + r2 + " )  ";
                    }
                }

                res += str + " ; ";
            }
            entryOperate.setOperateContent(res);
            int insert = constructOperate(entryOperate, entryEntity.getTableName(), entryEntity.getId(), request);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                log.error(" t_entry_operate update insert error ! ");
                return new ResultObject(ErrorCodeList.INSERT_ERROR);
            }
        } catch (Exception e) {
            log.error(" ComparisonResult 类型对比异常 ！ ");
            log.error(e.getMessage());
        }
        return new ResultObject(resultEntryEntity, ConstantInterface.OK_STR);
    }


    private static HashMap<String, String> constructEntryName() {
        HashMap<String, String> entryName = new HashMap<>();
        entryName.put("entry", "词条");
        entryName.put("abbr", "abbr");
        entryName.put("chineseInterpretation", "中文释义");
        entryName.put("englishInterpretation", "英文释义");
        entryName.put("entrySource", "词条来源");
        entryName.put("entryState", "词条状态");
        entryName.put("creator", "创建人");
        entryName.put("createTime", "创建时间");
        entryName.put("update", "修改人");
        entryName.put("updateTime", "修改时间");
        entryName.put("version", "版本");
        entryName.put("isLatestVersion", "是否最新版本");
        entryName.put("entryLabel", "词条标签");
        entryName.put("partOfSpeech", "词性备注");
        entryName.put("classifyId", "词条所属分类");
        entryName.put("repeatEntryId", "重复词条id");
        entryName.put("english", "英文翻译");
        entryName.put("russian", "俄文翻译");
        entryName.put("spanish", "西文翻译");
        entryName.put("french", "法文翻译");

        return entryName;
    }


    @Override
    public String deleteEntry(List<String> idList) {
        int delete = entryMapper.deleteEntries(idList);
        //更改词条状态为0
        if (delete != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }


        return ConstantInterface.OK_STR;

    }

    @Override
    public TranslateEntities translate(String name) {


        TranslateEntities translateEntities = new TranslateEntities();
        TranslateEntity baiduEntities = baiduTranslate(name);
        List<TranslateEntity> translateEntityList = new ArrayList<>();
        translateEntityList.add(baiduEntities);
        translateEntities.setTranslateEntities(translateEntityList);
        //entryMapper.updateById(entryEntity);


        // ArrayList<TranslateEntity>  moudleEntities =  moudleTranslate(name);

        return translateEntities;
    }

    private ArrayList<TranslateEntity> moudleTranslate(String entry) {
        String url = "http://127.0.0.1:8080/module/zh_en";
        Map<String, String> params = new HashMap<>();
        params.put("entry", entry);
        try {
            String str = httpUtils.get(url, params);
            System.out.println(" **** " + str + " **** ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //百度翻译
    private TranslateEntity baiduTranslate(String entry) {
        TranslateEntity entryEntity = new TranslateEntity();
        entryEntity.setSource("百度翻译");
        entryEntity.setEntry(entry);
        //ArrayList<TranslateEntity> list = new ArrayList<>();
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();
        String russia = "";
        String spanish = "";
        String french = "";
        String english = "";
        try {

            LanguageEntity EN_LanguageEntity = translate.getTranslateResult(entry, ConstantInterface.AUTO, ConstantInterface.ENGLISH);

            languageEntities.add(EN_LanguageEntity);

            Thread.sleep(1000);

            LanguageEntity RU_LanguageEntity = translate.getTranslateResult(entry, ConstantInterface.AUTO, ConstantInterface.RUSSIAN);
            languageEntities.add(RU_LanguageEntity);
            Thread.sleep(1000);
            LanguageEntity SPA_LanguageEntity = translate.getTranslateResult(entry, ConstantInterface.AUTO, ConstantInterface.SPANISH);
            languageEntities.add(SPA_LanguageEntity);
            Thread.sleep(1000);
            LanguageEntity FRE_LanguageEntity = translate.getTranslateResult(entry, ConstantInterface.AUTO, ConstantInterface.FRENCH);
            languageEntities.add(FRE_LanguageEntity);
            entryEntity.setLanguageEntities(languageEntities);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return entryEntity;
    }

    //判断list 是否满足 pageindex 的页码要求
    private boolean checkPage(List list, Integer pageIndex, Integer pageSize) {
        if (list.size() < pageSize) {
            return false;
        }
        int pageNowIndex = getPageIndex(list.size(), pageSize);
        //先查project 查出页码和参数页码相同 即返回
        if (pageNowIndex >= pageIndex) {
            return true;
        }
        return false;
    }

    private int getPageIndex(int size, Integer pageSize) {
        return size / pageSize + size % pageSize == 0 ? 0 : 1;
    }


    @Override
    //TODO
    public String bathAudit(List<EntryGroupEntity> entryGroupEntities, int state, HttpServletRequest request, String note) {


        for (EntryGroupEntity entryGroupEntity : entryGroupEntities) {

            List<String> entryIDs = entryGroupEntity.getIds();
            String tableName = entryGroupEntity.getTableName();
            for (String entryID : entryIDs) {


                //批量审核
                EntryEntity entryEntity = auditByID(tableName, entryID, state);
                if (Objects.isNull(entryEntity)) {
                    return ErrorCodeList.UPDATE_ERROR;
                }
                EntryOperate entryOperate = new EntryOperate();

                //更新操作记录表
                EntryEntity entryEntity1 = entryMapper.selectById(entryID);

                List<ComparisonResult> results = new ArrayList<>();
                OperateContentEntity operateContentEntity = new OperateContentEntity();
                try {
                    results = CompareUtils.compareFields(entryEntity, entryEntity1, EntryEntity.class);
                    if (results.size() == 0) {
                        log.error(" t_entry_operate compare result is null ! ");
                        return ErrorCodeList.INSERT_ERROR;
                    }
                    operateContentEntity.setResults(results);
                    operateContentEntity.setEntryID(entryID);
                    String res = " ";
                    for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
                        res += comparisonResult.getStr() + " ; ";
                    }
                    entryOperate.setOperateContent(res);
                    entryOperate.setNotes(note);
                    int insert = constructOperate(entryOperate, entryGroupEntity.getTableName(), entryID, request);
                    if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                        log.error(" t_entry_operate update insert error ! ");
                        return ErrorCodeList.INSERT_ERROR;
                    }
                } catch (Exception e) {
                    log.error(" ComparisonResult 类型对比异常 ！ ");
                    log.error(e.getMessage());
                }


            }

        }


        return ConstantInterface.OK_STR;
    }

    private int constructOperate(EntryOperate entryOperate, String tableName, String entryId, HttpServletRequest request) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        Date date = new Date();
        entryOperate.setOperateTime(date);
        entryOperate.setOperator(userName);
        entryOperate.setId(commonUtils.getUUID());
        entryOperate.setEntryId(entryId);
        entryOperate.setType(tableName);

        int insert = entryOperateMapper.insert(entryOperate);
        return insert;
    }


    private EntryEntity auditByID(String tableName, String entryID, int state) {

        EntryEntity entryEntity = entryMapper.selectById(entryID);

        int update = entryMapper.auditById(tableName, entryID, state);

        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return entryEntity;
        }
        return entryEntity;
        //工程表


    }
}
