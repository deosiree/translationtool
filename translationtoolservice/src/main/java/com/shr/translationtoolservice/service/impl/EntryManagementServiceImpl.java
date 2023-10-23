package com.shr.translationtoolservice.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

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
    private VersionTableMapper versionTableMapper;

    @Autowired
    private TLanguageMapper tLanguageMapper;
    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private ExcelUtils excelUtils;


    @Override
    public ResponseListModel searchEntry(EntryCommonEntity entryEntity, String entryState, Integer pageIndex, Integer pageSize) {
        //校验前端日期格式

        if (Objects.nonNull(entryEntity.getCreateTime()) && entryEntity.getCreateTime().toString().length() < 10) {
            String time = entryEntity.getCreateTime().toString() + ConstantInterface.TIME_ZERO;
            entryEntity.setCreateTime(new Date(time));
        }
        if (Objects.nonNull(entryEntity.getCreateEndRTime()) && entryEntity.getCreateEndRTime().toString().length() < 10) {
            String time = entryEntity.getCreateEndRTime().toString() + ConstantInterface.TIME_ZERO;
            entryEntity.setCreateEndRTime(new Date(time));
        }
        ResponseListModel<EntryCommonEntity> responseListModel = new ResponseListModel<>();

        //如果包含标签，则需要查出该标签及其子集下所有的词条的内容
        if (StringUtils.isNotBlank(entryEntity.getClassifyId())) {
            List<String> ids = new ArrayList<>();
            ids.add(entryEntity.getClassifyId());
            List<EntryClassify> classfyList = getAllChildClassfy(ids);
            classfyList.add(entryClassifyMapper.selectClassfyById(entryEntity.getClassifyId()));
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

    public ResponseListModel<EntryCommonEntity> getAllEntry(EntryCommonEntity entryEntity, Integer pageIndex, Integer pageSize, String entryState, List<EntryClassify> classfyList) {

        ResponseListModel<EntryCommonEntity> result = new ResponseListModel<>();
        int total = 0;

        List<EntryCommonEntity> entry = new ArrayList();


        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;

            entryEntity.setTableName(entryEntity.getTableName());
            List<EntryCommonEntity> entryEntity1 = entryCommonEntityMapper.selectListByEntries(entryEntity, classfyList, pageSize, offset, entryState);

            total = entryCommonEntityMapper.selectListByEntriesTotal(entryEntity, entryState, classfyList);
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
        queryWrapper.orderByDesc("operate_time");
        return entryOperateMapper.selectList(queryWrapper);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public List<EntryCommonEntity> selectNoMergeEntry(String chinese) {
        List<EntryCommonEntity> entryEntities = new ArrayList<>();
        //合并和未合并sql 增加过滤
        entryEntities = entryCommonEntityMapper.selectNoMerge(chinese).stream().distinct().collect(Collectors.toList());

        return entryEntities;
    }

    @Override
    public List<EntryCommonEntity> selectMergeEntry(String chinese) {
        List<EntryCommonEntity> entryEntities = new ArrayList<>();
        //合并和未合并sql 增加过滤
        entryEntities = entryCommonEntityMapper.selectMerge(chinese).stream().distinct().collect(Collectors.toList());

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
                Index index1 = indexMapper.getIndexByEntry(entryEntity.getChinese());
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
                index.setEntry(entryEntity.getChinese());
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
    public List<EntryClassify> getEntryClassfy(String department) {
        //查询对应部门下的分类
        List<EntryClassify> entryClassifies = new ArrayList<>();
        entryClassifies = entryClassifyMapper.getEntryClassfyIdsByDepartment(department);
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
    public String addEntryClassfy(EntryClassify entryClassify, HttpServletRequest request) {
        entryClassify.setKey(commonUtils.getUUID());
        String token = request.getHeader("token");
        String department = JWTTokenUtils.getDepartment(token);
        String userName = JWTTokenUtils.getUserName(token);
        entryClassify.setCreator(userName);
        entryClassify.setDepartment(department);
        Date date = new Date(System.currentTimeMillis());
        entryClassify.setCreateTime(date);
        int insert = entryClassifyMapper.insertClassfy(entryClassify);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String updateEntryClassfy(EntryClassify entryClassify) {
        int update = entryClassifyMapper.updateClassfyById(entryClassify);
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
    public ResponseListModel<EntryLabel> queryLabel(EntryLabel entryLabel, int pageIndex, int pageSize) {
        ResponseListModel<EntryLabel> result = new ResponseListModel<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            QueryWrapper<EntryLabel> queryWrapper = new QueryWrapper<>();

            List<EntryLabel> entryLabels = entryLabelMapper.getLabels(entryLabel, pageSize, offset);
            result.setList(entryLabels);
            int total = entryLabelMapper.getLabelsTotal(entryLabel, pageSize, offset);
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
    public String mergerSplit(List<String> idList) {

        String id = idList.get(0);

        EntryCommonEntity entryEntity = entryCommonEntityMapper.selectEntryById(id);

        String repeatEntryId = entryEntity.getRepeatEntryId();


        int update = entryMapper.mergerSplit(idList);

        if (update < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        int delete = indexMapper.deleteByRepeatId(repeatEntryId);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryCommonEntity> importExcle(MultipartFile multipartFile) {
        String name = multipartFile.getOriginalFilename();

        //读取excle转换的实体
        List<ImportExcleEntry> importExcleEntries = new ArrayList<>();
        try {

            importExcleEntries = ExcelUtils.readExcelToEntity(ImportExcleEntry.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryCommonEntity> entryEntitys = new ArrayList<>();

        for (ImportExcleEntry importExcleEntry : importExcleEntries) {
            EntryCommonEntity entryEntity = new EntryCommonEntity();
            BeanUtils.copyProperties(importExcleEntry, entryEntity);
            if (entryEntity.getEntryState() == null) {
                entryEntity.setEntryState(2);
            }

            entryEntitys.add(entryEntity);
        }


        return entryEntitys;
    }

    @Override
    public String createVersionTable(List<EntryCommonEntity> entryEntities, String version, String remark, String department, HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(department)) {
            department = JWTTokenUtils.getDepartment(token);
        }
        String userName = JWTTokenUtils.getUserName(token);

        //1.先查关系表是否有对应的表名 如果没有在关系表中写入当前月的表名关系
        List<VersionTable> versionTables = versionTableMapper.getVersionInfoByVersion(version);
        // 如果不存在则关系表中插入关系为对应当前月的版本表
        if (CollectionUtils.isEmpty(versionTables)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMM ");
            Date date = new Date();
            String da = format.format(date);
            String tableName = " t_version_" + da;
            VersionTable versionTable = new VersionTable();
            versionTable.setId(commonUtils.getUUID());
            versionTable.setVersion(version);
            versionTable.setVersionTableName(tableName);
            versionTable.setRemark(remark);
            versionTable.setDepartment(department);
            versionTable.setCreator(userName);
            versionTable.setCreateTime(new Date(System.currentTimeMillis()));

            versionTableMapper.addVersionTable(versionTable);
            //将待打版本的词条插入
            //判断是否存在表
            int exist = versionTableMapper.existTable(tableName);
            if (exist == 0) {
                //创建表
                versionTableMapper.createVersionTable(tableName);
            }
            //批量插入

            for (EntryCommonEntity entryEntity1 : entryEntities) {
                entryEntity1.setKey(commonUtils.getUUID());
                versionTableMapper.insertVersionTable(tableName, entryEntity1, version);
            }


            //存在插入对应表内
        } else {

            //版本校验
            List<VersionEntity> allVersionTable = versionTableMapper.getReVersionTableByName(entryEntities, versionTables.get(0).getVersionTableName(), version);
            if (!CollectionUtils.isEmpty(allVersionTable)) {
                throw new ExceptionUtils(ConstantInterface.REPETITION_STR);
                //return ConstantInterface.REPETITION_STR;
            }


            for (EntryCommonEntity entryEntity1 : entryEntities) {
                entryEntity1.setKey(commonUtils.getUUID());
                versionTableMapper.insertVersionTable(versionTables.get(0).getVersionTableName(), entryEntity1, version);
            }

        }
        return ConstantInterface.OK_STR;

    }

    @Override
    public List<VersionTable> getVersionTable(String tableName, String version, Integer pageIndex, Integer pageSize,String department) {
        List<VersionTable> versionTables = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            versionTables = versionTableMapper.getVersionTable(tableName, version, pageSize, offset,department);
        }

        return versionTables;
    }

    @Override
    public String bachAddEntry(List<EntryCommonEntity> entryEntities) {
        QueryWrapper<EntryCommonEntity> entryCommonEntityQueryWrapper = new QueryWrapper<>();

        List<EntryCommonEntity> entryCommonEntities = entryCommonEntityMapper.getRepAbbrAndVersionEntry(entryEntities);
        List<EntryCommonEntity> entryCommonEntitiesResult = new ArrayList<>();
        List<EntryCommonEntity> entryCommonEntitiesRes = new ArrayList<>();

        for (EntryCommonEntity entryEntity : entryEntities) {
            //重复词条校验,没有重复则放入到list 里
            boolean re = false;
            for (EntryCommonEntity entryCommonEntity : entryCommonEntities) {
                if (entryEntity.getAbbr().equals(entryCommonEntity.getAbbr()) &&
                        entryEntity.getVersion().equals(entryCommonEntity.getVersion()) &&
                        entryCommonEntity.getEntryState() > 0) {
                    entryCommonEntitiesRes.add(entryEntity);
                    re = true;
                    break;
                }
            }
            //没有重复写入
            if (!re) {
                if (StringUtils.isBlank(entryEntity.getId())) {
                    entryEntity.setId(commonUtils.getUUID());
                }
                entryCommonEntitiesResult.add(entryEntity);

            }
        }
        int insert = 0;
        if (CollectionUtils.isEmpty(entryCommonEntitiesResult)) {
            String msg = " 批量待插入词条总数为 ：" + entryEntities.size() + ", 其中重复词条数 ：" + entryCommonEntitiesRes.size() + " , 成功插入词条数： " + insert;
            return msg;
        }
        //写入
        for (EntryCommonEntity entryCommonEntity : entryCommonEntitiesResult) {
            insert += entryCommonEntityMapper.insert(entryCommonEntity);
        }

        String msg = " 批量待插入词条总数为 ：" + entryEntities.size() + ", 其中重复词条数 ：" + entryCommonEntitiesRes.size() + " , 成功插入词条数： " + insert;
        return msg;
    }

    @Override
    public EntryResponse getEntryToVersion(String version, List<String> classfies, String tag, String creator) {
        EntryResponse entryResponse = new EntryResponse();
        List<EntryCommonEntity> entryEntities = new ArrayList<>();
        List<EntryClassify> entryClassifies = new ArrayList<>();
        List<VersionTable> versionTables = new ArrayList<>();
        List<VersionEntity> versionEntities = new ArrayList<>();

        if (!CollectionUtils.isEmpty(classfies)) {
            entryClassifies = entryClassifyMapper.getEntryClassfyByNames(classfies);
        }
        //增量
        if (StringUtils.isNotBlank(version)) {
            versionTables = versionTableMapper.getVersionInfoByVersion(version);
            versionEntities = versionTableMapper.getAllVersionTable(versionTables.get(0).getVersionTableName(), versionTables.get(0).getVersion());
            List<String> idList = new ArrayList<>();
            if (CollectionUtils.isEmpty(versionEntities)) {
                entryResponse.setVersionEntries(new ArrayList<>());
            } else {
                for (VersionEntity versionEntity : versionEntities) {
                    idList.add(versionEntity.getId());
                }
                //版本库词条
                // TODO 不知有何作用 报错 故先注释
//                entryEntities = entryCommonEntityMapper.selectBatchIds(idList);
                entryResponse.setVersionEntries(versionEntities);
            }
            List<EntryCommonEntity> entryToVersion = entryCommonEntityMapper.getEntryToVersion(classfies, tag, creator, versionEntities);

            if (CollectionUtils.isEmpty(entryToVersion)) {
                entryResponse.setFuzzyEntries(new ArrayList<>());
            } else {
                //条件查询词条
                entryResponse.setFuzzyEntries(entryToVersion);
            }
        } else {
            entryEntities = entryCommonEntityMapper.getEntryToVersion(classfies, tag, creator, versionEntities);
            entryResponse.setVersionEntries(new ArrayList<>());
            if (CollectionUtils.isEmpty(entryEntities)) {
                entryResponse.setFuzzyEntries(new ArrayList<>());
            } else {
                //条件查询词条
                entryResponse.setFuzzyEntries(entryEntities);
            }
        }
        return entryResponse;
    }

    @Override
    public List<EntryCommonEntity> getTranslatedEntry(Integer pageIndex, Integer pageSize) {
        List<EntryCommonEntity> entryCommonEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryCommonEntities = entryCommonEntityMapper.getTranslatedEntry(pageSize, offset);

        }

        return entryCommonEntities;
    }

    @Override
    public HttpResponse<EntryCommonEntity> upgradeEntry(EntryCommonEntity entryEntity, HttpServletRequest request, String insertType) {
        //旧词条新版本字段置为0
        String oldID = entryEntity.getId();
        EntryCommonEntity entryCommonEntity = new EntryCommonEntity();
        entryCommonEntity.setId(oldID);
        entryCommonEntity.setIsLatestVersion(0);
        entryCommonEntityMapper.updateEntryById(entryCommonEntity);

        //新词条构建
        entryEntity.setId(commonUtils.getUUID());
        String version = entryEntity.getVersion();
        //版本+1
        double i = Double.parseDouble(version.replace("V", "")) + 1;
        String newVersion = "V" + Double.toString(i);
        entryEntity.setVersion(newVersion);
        //设置为待审核状态
        entryEntity.setEntryState(2);

        //增加创建者
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        entryEntity.setCreator(userName);
        Date date = new Date(System.currentTimeMillis());
        entryEntity.setCreateTime(date);
        entryEntity.setUpdate(userName);
        entryEntity.setUpdateTime(date);

        return insertEntry(entryEntity, request, insertType);
    }

    @Override
    public List<String> getKindEntryVersion(String typeID) {

        List<String> versionIDs = entryCommonEntityMapper.getKindEntryVersion(typeID);

        return versionIDs;
    }


    @Override
    public HttpResponse<EntryCommonEntity> insertEntry(EntryCommonEntity entryEntity, HttpServletRequest request, String insertType) {
        HttpResponse<EntryCommonEntity> response = new HttpResponse<>();
        List<EntryCommonEntity> entryEntities = entryCommonEntityMapper.selectByAbbr(entryEntity.getAbbr(), entryEntity.getVersion());
        List<EntryCommonEntity> entryEntities1 = entryCommonEntityMapper.selectByName(entryEntity);
        if (insertType.equals(ConstantInterface.OPERATION_TYPE_INSERT)) {
            entryEntity.setTypeId(commonUtils.getUUID());
        }
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

        //设置词条状态为新建
        if (Objects.isNull(entryEntity.getEntryState())) {
            entryEntity.setEntryState(1);
        }

        entryEntity.setIsLatestVersion(1);
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


        int insert = entryCommonEntityMapper.insertEntry(entryEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            response.setMessage(ErrorCodeList.INSERT_ERROR);
            response.setCode(HttpResponse.Type.ERROR.getVal());
            response.setType(HttpResponse.Type.ERROR);
            return response;
        }
        EntryOperate entryOperate = new EntryOperate();
        //添加操作类型
        entryOperate.setType(insertType);
        entryOperate.setOperateContent(insertType);
        int insert1 = constructOperate(entryOperate, entryEntity.getId(), request);


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

    /**
     * 设置词条翻译状态和长度
     * @param entryEntity
     */
    private void constructEntry(EntryCommonEntity entryEntity) {
        if (StringUtils.isNotBlank(entryEntity.getChinese())) {
            entryEntity.setChineseLength(entryEntity.getChinese().length());
            entryEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
        }else {
            entryEntity.setChineseLength(ConstantInterface.ZERO);
            entryEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getEnglish())) {
            entryEntity.setEnglishLength(entryEntity.getEnglish().length());
            entryEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setEnglishLength(ConstantInterface.ZERO);
            entryEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
        }

        /*if (StringUtils.isNotBlank(entryEntity.getEnglishDisable())) {
            entryEntity.setEnglishDisableLength(entryEntity.getEnglishDisable().length());
        }*/
        if (StringUtils.isNotBlank(entryEntity.getRussian())) {
            entryEntity.setRussianLength(entryEntity.getRussian().length());
            entryEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setRussianLength(ConstantInterface.ZERO);
            entryEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getSpanish())) {
            entryEntity.setSpanishLength(entryEntity.getSpanish().length());
            entryEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setSpanishLength(ConstantInterface.ZERO);
            entryEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getFrench())) {
            entryEntity.setFrenchLength(entryEntity.getFrench().length());
            entryEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setFrenchLength(ConstantInterface.ZERO);
            entryEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
        }
       /* //是否最新版本
        if (StringUtils.isNotBlank(entryEntity.getVersion())) {
            EntryVersion newVersion = entryVersionMapper.getNewVersion();
            if (entryEntity.getVersion().equals(newVersion.getName())) {
                entryEntity.setIsLatestVersion(1);
            } else {
                entryEntity.setIsLatestVersion(0);
            }
        }*/
//        Index index = indexMapper.getIndexByEntry(entryEntity.getEntry());
//        if (!Objects.isNull(index)) {
//            entryEntity.setRepeatEntryId(index.getRepeatEntryId());
//        }

        //entryEntity.setEntryState(2);


    }


    @Override
    public ResultObject updateEntry(EntryCommonEntity entryEntity, HttpServletRequest request, String notes) {

        EntryCommonEntity beforEntry = entryCommonEntityMapper.selectEntryById(entryEntity.getId());

        //abbr 重复判断
        if (StringUtils.isNotBlank(entryEntity.getAbbr())) {
            String version = "";
            if (StringUtils.isNotBlank(entryEntity.getVersion())) {
                version = entryEntity.getVersion();
            } else {
                version = beforEntry.getVersion();
            }

            //判断是否存在重复同版本Abbr
            List<EntryCommonEntity> entryEntities = entryCommonEntityMapper.selectByAbbr(entryEntity.getAbbr(), version);
            entryEntities.removeIf(entryEntity1 -> entryEntity1.getId().equals(entryEntity.getId()));
            if (!CollectionUtils.isEmpty(entryEntities)) {


                return new ResultObject(ErrorCodeList.ABBR_HAS_EXIST);
            }
        }
        constructEntry(entryEntity);


        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        entryEntity.setUpdate(userName);
        entryEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        int update = entryCommonEntityMapper.updateEntryById(entryEntity);
        EntryCommonEntity resultEntryEntity = entryCommonEntityMapper.selectEntryById(entryEntity.getId());
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {

            return new ResultObject(ErrorCodeList.UPDATE_ERROR);

        }
        if (Objects.isNull(entryEntity.getEntryState())) {
            entryEntity.setEntryState(beforEntry.getEntryState());
        }
        //更新操作记录表
        EntryOperate entryOperate = new EntryOperate();
        entryOperate.setType(ConstantInterface.OPERATION_TYPE_UPDATE);
        entryOperate.setNotes(notes);
        List<ComparisonResult> results = new ArrayList<>();
        OperateContentEntity operateContentEntity = new OperateContentEntity();
        EntryCommonEntity afterEntry = entryCommonEntityMapper.selectEntryById(entryEntity.getId());
        try {

            results = CompareUtils.compareFields(beforEntry, afterEntry, EntryCommonEntity.class);
            if (results.size() == 0) {
                log.error(" t_entry_operate no change ! ");
//                return new ResultObject(ErrorCodeList.INSERT_ERROR);
                return new ResultObject(resultEntryEntity, ConstantInterface.OK_STR);
            }
            operateContentEntity.setResults(results);
            operateContentEntity.setEntryID(entryEntity.getId());
            String res = " ";
            //操作记录写入
            for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
                String name = comparisonResult.getKey();
                //不写入操作内容的字段
                if ("update".equals(name) || "updateTime".equals(name) || "chineseLength".equals(name)
                        || "englishLength".equals(name) || "englishTranslateState".equals(name) || "englishDisable".equals(name) || "englishDisableLength".equals(name)
                        || "russianLength".equals(name) || "russianhTranslateState".equals(name) || "russianDisable".equals(name) || "russianDisableLength".equals(name)
                        || "spanishLength".equals(name) || "spanishhTranslateState".equals(name) || "spanishDisable".equals(name) || "spanishDisableLength".equals(name)
                        || "frenchLength".equals(name) || "frenchhTranslateState".equals(name) || "frenchDisable".equals(name) || "frenchDisableLength".equals(name)

                ) {
                    continue;
                }
                HashMap<String, String> entryName = ConstantInterface.constructEntryName();
                String str = "";
                String r1 = comparisonResult.getPrevious();
                String r2 = comparisonResult.getLater();
                if (StringUtils.isBlank(r1)) {

                    if ("classifyId".equals(comparisonResult.getKey())) {
                        str = entryName.get(comparisonResult.getKey()) + "新增值为 ( " + entryClassifyMapper.selectClassfyById(r2).getTitle() + " )  ";
                    } else {
                        str = entryName.get(name) + " 新增值为： " + r2;
                    }
                } else {

                    if ("classifyId".equals(comparisonResult.getKey())) {
                        str = entryName.get(comparisonResult.getKey()) + " 值由 ( " + entryClassifyMapper.selectClassfyById(r1).getTitle() + " ) 改为 ( " + entryClassifyMapper.selectById(r2).getTitle() + " )  ";
                    } else {
                        str = entryName.get(name) + " 值由 ( " + r1 + " ) 改为 ( " + r2 + " )  ";
                    }
                }

                res += str + " ; ";
            }
            entryOperate.setOperateContent(res);

            int insert = constructOperate(entryOperate, entryEntity.getId(), request);
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
    public TranslateEntities translate(String name, String type) {


        TranslateEntities translateEntities = new TranslateEntities();
        List<TranslateEntity> translateEntityList = new ArrayList<>();
        List<TLanguage> tLanguages = tLanguageMapper.selectList(new QueryWrapper<>());
        TranslateEntity baiduEntities = baiduTranslate(name, type, tLanguages);
        translateEntityList.add(baiduEntities);


        //有道翻译
        TranslateEntity youdao_Entities = youdaoTranslate(name, type, tLanguages);
        translateEntityList.add(youdao_Entities);


        translateEntities.setTranslateEntities(translateEntityList);
        return translateEntities;
    }

    private TranslateEntity youdaoTranslate(String name, String type, List<TLanguage> tLanguages) {
        // YoudaoTrans.readJsonFromUrl(name,ConstantInterface.ENGLISH);]
        TranslateEntity entryEntity = new TranslateEntity();
        //QueryWrapper queryWrapper = new QueryWrapper();
        //List<TLanguage> tLanguages = tLanguageMapper.selectList(new QueryWrapper<>());

        entryEntity.setSource("有道翻译");
        entryEntity.setEntry(name);
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();


        for (TLanguage tLanguage : tLanguages) {
            if (tLanguage.getCode().equals(type)) {
                continue;
            }
            languageEntities.add(YoudaoTrans.youdaoTranslate(name, ConstantInterface.AUTO, tLanguage));
        }


        entryEntity.setLanguageEntities(languageEntities);

        return entryEntity;

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
    private TranslateEntity baiduTranslate(String entry, String type, List<TLanguage> tLanguages) {
        TranslateEntity entryEntity = new TranslateEntity();
        entryEntity.setSource("百度翻译");
        entryEntity.setEntry(entry);
        //ArrayList<TranslateEntity> list = new ArrayList<>();
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();

        try {
            for (TLanguage tLanguage : tLanguages) {
                if (tLanguage.getCode().equals(type)) {
                    continue;
                }
                languageEntities.add(translate.getTranslateResult(entry, ConstantInterface.AUTO, tLanguage));
                Thread.sleep(1000);
            }
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
    public String bathAudit(List<EntryGroupEntity> entryGroupEntities, int state, HttpServletRequest request, String note) {


        for (EntryGroupEntity entryGroupEntity : entryGroupEntities) {

            List<String> entryIDs = entryGroupEntity.getIds();
            for (String entryID : entryIDs) {


                //批量审核
                EntryCommonEntity entryEntity = auditByID(entryID, state);
                if (Objects.isNull(entryEntity)) {
                    return ErrorCodeList.UPDATE_ERROR;
                }
                EntryOperate entryOperate = new EntryOperate();

//                if (entryEntity.getEntryState() == 3) {
//                    entryOperate.setOperateContent("审核通过");
//                }
                entryOperate.setNotes(note);
                entryOperate.setType(ConstantInterface.OPERATION_TYPE_AUDIT);
                entryOperate.setOperateContent("词条审核");
                int insert = constructOperate(entryOperate, entryID, request);
                if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                    log.error(" t_entry_operate update insert error ! ");
                    return ErrorCodeList.INSERT_ERROR;
                }

            }

        }


        return ConstantInterface.OK_STR;
    }

    private int constructOperate(EntryOperate entryOperate, String entryId, HttpServletRequest request) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        Date date = new Date();
        entryOperate.setOperateTime(date);
        entryOperate.setOperator(userName);
        entryOperate.setId(commonUtils.getUUID());
        entryOperate.setEntryId(entryId);

        int insert = entryOperateMapper.insert(entryOperate);
        return insert;
    }


    private EntryCommonEntity auditByID(String entryID, int state) {

        EntryCommonEntity entryEntity = entryCommonEntityMapper.selectEntryById(entryID);


        int update = entryCommonEntityMapper.auditById(entryID, state);

        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return entryEntity;
        }
        return entryEntity;
        //工程表


    }


}
