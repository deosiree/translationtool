package com.shr.translationtoolservice.service.impl;

import cn.afterturn.easypoi.cache.manager.IFileLoader;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.entity.vo.UpgradeVO;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.CompareUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.events.Event;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 *
 */
@Service
public class EntryInfoServiceImpl extends ServiceImpl<EntryInfoMapper, EntryInfoEntity>
        implements EntryInfoService {

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private EntryOperateMapper entryOperateMapper;

    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Override
    public List<EntryVO> getEntryByVersion(EntryInfoEntity entryInfoEntity1, Integer offset, Integer pagesize) {
        List<EntryInfoEntity> entryByVersion = entryInfoMapper.getEntryByVersion(entryInfoEntity1, offset, pagesize);

        List<EntryVO> entryVOS = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryByVersion) {
            EntryVO entryVO = new EntryVO();
            List<TranslateEntity> translateEntityList = new ArrayList<>();
            getTransEntity(entryInfoEntity.getEnTransId(), translateEntityList);
            getTransEntity(entryInfoEntity.getRuTransId(), translateEntityList);
            getTransEntity(entryInfoEntity.getSpaTransId(), translateEntityList);
            getTransEntity(entryInfoEntity.getFraTransId(), translateEntityList);
            entryVO.setTranslateEntity(translateEntityList);
            entryVO.setEntryInfoEntity(entryInfoEntity);
            entryVOS.add(entryVO);
        }
        return entryVOS;
    }

    private void getTransEntity(String transId, List<TranslateEntity> translateEntityList) {
        if (StringUtils.isNotBlank(transId)) {
            TranslateEntity translateEntity = translateMapper.selectById(transId);
            translateEntityList.add(translateEntity);
        }
    }

    @Override
    public int getEntryByVersionTotal(EntryInfoEntity entryInfoEntity1) {
        return entryInfoMapper.getEntryByVersionTotal(entryInfoEntity1);
    }

    @Override
    public HttpResponse<String> addEntryByVersion(List<EntryVO> entryVOS, HttpServletRequest request) {
        HttpResponse<String> response = new HttpResponse<>();
        for (EntryVO entryVO : entryVOS) {
            EntryInfoEntity entryInfoEntity = entryVO.getEntryInfoEntity();
            //一次新增只有一种翻译，取第一个元素
            TranslateEntity translateEntities = entryVO.getTranslateEntity().get(0);
            //存在翻译
            if (!Objects.isNull(translateEntities)){
                //查询是否有存在已有的翻译 需传入版本
                if(StringUtils.isBlank(translateEntities.getVersionID())){
                    response.setMessage("TranslateEntity version is null ！ ");
                    response.setCode(HttpResponse.Type.ERROR.getVal());
                    response.setType(HttpResponse.Type.ERROR);
                    return response;
                }
                List<TranslateEntity> translateEntity = translateMapper.getTrans(translateEntities, 0, 20);

                //不存在翻译则创建
                if (CollectionUtils.isEmpty(translateEntity)) {
                    translateEntities.setDeleteState(0);
                    translateEntities.setPublicState(0);
                    //添加transid
                    addTransID(translateEntities, entryInfoEntity);
                    int transInsert = translateMapper.insert(translateEntities);

                    if (transInsert != ConstantInterface.DB_SUCCESS_RESULT) {
                        log.error("  insert error ! ");
                        response.setMessage(ErrorCodeList.INSERT_ERROR);
                        response.setCode(HttpResponse.Type.ERROR.getVal());
                        response.setType(HttpResponse.Type.ERROR);
                        return response;
                    }
                } else {
                    //添加transid
                    addTransID(translateEntity.get(0), entryInfoEntity);
                }
            }
            //校验ABBR
            if (checkAbbrRepe(entryInfoEntity, entryVO.getTableName())) {
                response.setMessage(ErrorCodeList.ABBR_HAS_EXIST);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }
            if (StringUtils.isBlank(entryInfoEntity.getId())) {
                entryInfoEntity.setId(commonUtils.getUUID());
            }
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setEntryState(ConstantInterface.CREATE_STATE);
            int insert = entryInfoMapper.insertEntry(entryInfoEntity, entryVO.getTableName());


            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                log.error("  insert error ! ");
                response.setMessage(ErrorCodeList.INSERT_ERROR);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }


            EntryOperate entryOperate = new EntryOperate();
            //添加操作类型
            entryOperate.setType(ConstantInterface.OPERATION_TYPE_INSERT);
            entryOperate.setOperateContent(ConstantInterface.OPERATION_TYPE_INSERT);
            int insert1 = constructOperate(entryOperate, entryInfoEntity.getId(), request);
            if (insert1 != ConstantInterface.DB_SUCCESS_RESULT) {
                log.error(" t_entry_operate update insert error ! ");
                response.setMessage(ErrorCodeList.INSERT_ERROR);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }
        }

        response.setMessage(ErrorCodeList.SUCCESS);
        response.setCode(HttpResponse.Type.OK.getVal());
        response.setType(HttpResponse.Type.OK);
        return response;
    }

    @Override
    public String addEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request, String tableName) {
        if (StringUtils.isBlank(entryInfoEntity.getId())) {
            entryInfoEntity.setId(commonUtils.getUUID());
        }
        int insert = entryInfoMapper.insertEntry(entryInfoEntity, tableName);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        EntryOperate entryOperate = new EntryOperate();
        //添加操作类型
        entryOperate.setType(ConstantInterface.OPERATION_TYPE_INSERT);
        entryOperate.setOperateContent(ConstantInterface.OPERATION_TYPE_INSERT);
        int insert1 = constructOperate(entryOperate, entryInfoEntity.getId(), request);
        if (insert1 != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String updateEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request, String notes) {
        EntryInfoEntity beforEntry = entryInfoMapper.selectEntryById(entryInfoEntity);
        beforEntry.setTableName(entryInfoEntity.getTableName());
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        Date date = new Date();
        entryInfoEntity.setUpdate(userName);
        entryInfoEntity.setUpdateTime(date);
        int update = entryInfoMapper.updateEntryInfo(entryInfoEntity);
        if (update < ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        //操作记录写入
        EntryOperate entryOperate = null;
        try {
            entryOperate = insertUpdateOperate(beforEntry, notes);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (Objects.isNull(entryOperate)) {
            return " 词条信息无变化 ！ ";
        }
        int insert = constructOperate(entryOperate, beforEntry.getId(), request);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }

        return ConstantInterface.OK_STR;
    }

    //更新操作记录表
    private EntryOperate insertUpdateOperate(EntryInfoEntity beforEntry, String notes) throws Exception {
        EntryOperate entryOperate = new EntryOperate();
        entryOperate.setType(ConstantInterface.OPERATION_TYPE_UPDATE);
        entryOperate.setNotes(notes);
        List<ComparisonResult> results = new ArrayList<>();
        OperateContentEntity operateContentEntity = new OperateContentEntity();
        EntryInfoEntity afterEntry = entryInfoMapper.selectEntryById(beforEntry);


        results = CompareUtils.compareFields(beforEntry, afterEntry, EntryInfoEntity.class);
        if (results.size() == 0) {
            log.error(" t_entry_operate no change ! ");
//                return new ResultObject(ErrorCodeList.INSERT_ERROR);
            return null;
        }
        operateContentEntity.setResults(results);
        operateContentEntity.setEntryID(beforEntry.getId());
        String res = "";
        //操作记录写入
        for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
            String name = comparisonResult.getKey();
            //不写入操作内容的字段
            if ("update".equals(name) || "updateTime".equals(name) || "entryLength".equals(name) || "tableName".equals(name)) {
                continue;
            }
            HashMap<String, String> entryName = ConstantInterface.constructEntryName();
            String str = "";
            String r1 = comparisonResult.getPrevious();
            String r2 = comparisonResult.getLater();
            if (StringUtils.isBlank(r1)) {

                if ("classifyId".equals(name)) {
                    str = entryName.get(comparisonResult.getKey()) + "新增值为 ( " + entryClassifyMapper.selectClassfyById(r2).getTitle() + " )  ";
                    //翻译字段处理
                } else if (ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.EN_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.RU_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.SPA_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.FRA_TRANS)) {
                    String transStr = translateMapper.selectById(r2).getTranslate();
                    str = entryName.get(name) + " 新增值为： " + transStr;

                } else {
                    str = entryName.get(name) + " 新增值为： " + r2;
                }
            } else {

                if ("classifyId".equals(name)) {
                    str = entryName.get(name) + " 值由 ( " + entryClassifyMapper.selectClassfyById(r1).getTitle() + " ) 改为 ( " + entryClassifyMapper.selectById(r2).getTitle() + " )  ";
                } else if (ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.EN_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.RU_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.SPA_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.FRA_TRANS)) {
                    String r1TransStr = translateMapper.selectById(r1).getTranslate();
                    String r2TransStr = translateMapper.selectById(r2).getTranslate();
                    str = entryName.get(name) + " 值由 ( " + r1TransStr + " ) 改为 ( " + r2TransStr + " )  ";

                }else {
                    str = entryName.get(name) + " 值由 ( " + r1 + " ) 改为 ( " + r2 + " )  ";
                }
            }

            res += str + " ; ";
        }
        if (StringUtils.isBlank(res)){
            log.error(" t_entry_operate no change ! ");
//                return new ResultObject(ErrorCodeList.INSERT_ERROR);
            return null;
        }

        entryOperate.setOperateContent(res);


        return entryOperate;
    }

    @Override
    public String deleteEntryInfo(List<String> idList, String tableName) {
        int delete = entryInfoMapper.deleteByIdList(idList, tableName);
        if (delete != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<TranslateEntity> getPublicEntry(TranslateEntity translateEntity, int offset, Integer pageSize) {
        List<TranslateEntity> entryPublicEntities = translateMapper.getPublicEntry(translateEntity, offset, pageSize);
        return entryPublicEntities;
    }

    @Override
    public int getPublicEntryTotal(TranslateEntity translateEntity) {
        return translateMapper.getPublicEntryTotal(translateEntity);
    }

    @Override
    public String updatePublicEntry(TranslateEntity translateEntity) {
        int update = translateMapper.updateById(translateEntity);
        if (update < ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String addPublicEntry(List<TranslateEntity> translateEntities) {
        int insert = 0;
        for (TranslateEntity translateEntity : translateEntities) {
            //判断同版本下是否存在此翻译
            TranslateEntity  translateEntity1 = translateMapper.selectPublicByEntry(translateEntity);
            if (Objects.nonNull(translateEntity1)){
                continue;
            }
            if (StringUtils.isBlank(translateEntity.getId())) {
                translateEntity.setId(commonUtils.getUUID());
            }
            translateEntity.setDeleteState(0);
            insert += translateMapper.insert(translateEntity);
        }
        if (insert < translateEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String deletePublicEntry(List<String> idlist) {
        int delete = translateMapper.deleteByIds(idlist);
        if (delete != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    //TODO  废弃
    public String upgrade(UpgradeVO upgradeVO, HttpServletRequest request) {
        //创建任务 流程到翻译审核员
         TaskInfoEntity taskInfoEntity = upgradeVO.getTaskInfoEntities();
         List<EntryVO> entryVOList = upgradeVO.getEntryVOList();
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        Date date = new Date();
        taskInfoEntity.setDepartment(userName);
        taskInfoEntity.setImportTime(date);
        taskInfoEntity.setEntryAuditor(userName);
        taskInfoEntity.setEntryAutiorStartTime(date);
        taskInfoEntity.setTranslator(userName);
        taskInfoEntity.setTranslateStartTime(date);
        taskInfoEntity.setState("4");
        taskInfoEntity.setDepartment(department);
        int insert = taskInfoMapper.insert(taskInfoEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_TASK_ERROR;
        }

        // 更新词条信息

        for (EntryVO entryVO : entryVOList){
            String tableName = entryVO.getTableName();
            EntryInfoEntity entryInfoEntity = entryVO.getEntryInfoEntity();
            TranslateEntity translateEntity = entryVO.getTranslateEntity().get(0);
            //查找同版本下相同词条是否有同翻译
        }

        // 记录操作记录
        return null;
    }

    @Override
    public String updateEntryInfoList(List<EntryInfoEntity> entryInfoEntities, HttpServletRequest request, String notes) {
        String result ="";
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities){
            if (StringUtils.isBlank(entryInfoEntity.getTableName())) {
                return ErrorCodeList.TBALE_IS_NULL;
            }
            result =  updateEntryInfo(entryInfoEntity,request,notes);

        }

        return result;
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

    private void addTransID(TranslateEntity translateEntities, EntryInfoEntity entryInfoEntity) {
        switch (translateEntities.getType()) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setEnTransId(transID);
                } else {
                    entryInfoEntity.setEnTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setFraTransId(transID);
                } else {
                    entryInfoEntity.setFraTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setRuTransId(transID);
                } else {
                    entryInfoEntity.setRuTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setSpaTransId(transID);
                } else {
                    entryInfoEntity.setSpaTransId(translateEntities.getId());
                }
                break;

        }
    }

    //检查abbr 是否重复 重复返回true
    private boolean checkAbbrRepe(EntryInfoEntity entryInfoEntity, String tableName) {
        List<EntryInfoEntity> abbrEntryInfo = new ArrayList<>();
        if (StringUtils.isNotBlank(entryInfoEntity.getAbbr())) {
            abbrEntryInfo = entryInfoMapper.getEntryByAbbr(entryInfoEntity.getAbbr(), entryInfoEntity.getVersionID(), tableName);
            //校验ABBR 重复
            if (!CollectionUtils.isEmpty(abbrEntryInfo)) {
                return true;
            }

        }
        return false;
    }


}




