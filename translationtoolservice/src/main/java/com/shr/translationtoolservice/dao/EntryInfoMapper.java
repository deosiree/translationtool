package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.DO.EntryInfoEntityDO;
import com.shr.translationtoolservice.entity.DO.TaskStateEntityDO;
import com.shr.translationtoolservice.entity.vo.EntryInfoEntityQO;
import com.shr.translationtoolservice.entity.vo.EntryInfoReplicateCheckVO;
import com.shr.translationtoolservice.entity.vo.TsVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Entity com.shr.translationtoolservice.entity.EntryInfoEntity
 */
@Mapper
public interface EntryInfoMapper extends BaseMapper<EntryInfoEntity> {

    List<EntryInfoEntity> getEntryByVersion(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    int getEntryByVersionTotal(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    List<EntryInfoEntity> getEntryByAbbr(@Param("abbr") String abbr, @Param("versionID") String versionID, @Param("tableName") String tableName);

    int insertEntry(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity, @Param("tableName") String tableName);

    int updateEntryInfo(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    int batchUpdateEntryInfo(@Param("entryInfos") Collection<EntryInfoEntity> entryInfos);

    int updateEntryInfoForManager(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    EntryInfoEntity selectEntryById(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);

    int deleteByIdList(@Param("idList") List<String> idList, @Param("tableName") String tableName);

    List<EntryInfoEntity> getEntryByTaskID( @Param("id")String id,@Param("tableName") String tableName);

    List<EntryInfoEntity> getWriteEntryByTaskID( @Param("id")String id,@Param("tableName") String tableName);

    List<EntryInfoEntity> getEntryByTaskIDAndEntry( @Param("id")String id,@Param("entryState") String entryState,@Param("entry") String entry);

    List<EntryInfoEntity> getEntryByVersionID(@Param("tableName") String tableName,@Param("versionID") String versionID);

    List<EntryInfoEntity> getExistEntryList(@Param("productTableName")String productTableName, @Param("entryInfoEntity")EntryInfoEntity entryInfoEntity,@Param("productID")String productID);

    List<EntryInfoEntity> getExistEntryListForEquipment(@Param("productTableName")String productTableName, @Param("entryInfoEntity")EntryInfoEntity entryInfoEntity,@Param("productID")String productID);

    List<EntryInfoEntity> getExistEntryListForXML(@Param("productTableName")String productTableName, @Param("entryInfoEntity")EntryInfoEntity entryInfoEntity,@Param("productID")String productID);

    Integer getLastVersionNum(@Param("entryInfoEntity")EntryInfoEntity entryInfoEntity);

    List<EntryInfoEntity> getTransStateEntry(String sql);

    List<EntryInfoEntity> getEntryInfo(@Param("entryInfoEntity")EntryInfoEntity entryInfoEntity);

    int deleteByProductIdList(@Param("idList") List<String> idList);

    int insertEntryList(@Param("entryInfoEntityList")  List<EntryInfoEntity> entryInfoEntityList);

    String getTransByID(@Param("id")String id,@Param("transColum") String transColum);

    List<EntryInfoEntity> getReTransEntry(@Param("taskId")String taskID);

    int updateEntryState(@Param("id")String id,@Param("entryState")int entryState);
    List<EntryInfoEntity> getbppEntrys();


    List<String> getUsedDicByTaskID(@Param("taskID")String taskID);

    List<EntryInfoEntity> getEntryByClassfy(@Param("entryInfoEntity")EntryInfoEntityQO entryInfoEntity,
                                            @Param("productidList")List<String> productidList,
                                            @Param("clearMatchSet") Set<String> clearMatchSet,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    List<EntryInfoEntity> getEntryByClassfyTotal(@Param("entryInfoEntity")EntryInfoEntity entryInfoEntity,
                                                 @Param("productidList")List<String> productidList);

    List<EntryInfoEntity> getEntryByIDs(@Param("idList") Set<String> entryIdList);

    List<EntryInfoEntity> getEntryInfoUsingTranslate(@Param("translateEntity") TranslateEntity translateEntity);
    
    List<EntryInfoEntity> getEntryByTsVo(@Param("tsVo") TsVo tsVo);

    List<TsVo> getEntryByImportTypeUsingTranslate(@Param("entryInfoEntity") EntryInfoEntity entryInfoEntity,@Param("department") String department);
    int getCounts();

    List<EntryInfoEntity> getEntryForTaskNotUseEntry(
        @Param("idList") Set<String> entryIdList,  
        @Param("condition") Map<String,String> isCondition,
        @Param("translateEntity") TranslateEntity translateEntity,
        @Param("entryInfoEntity") EntryInfoEntity entryInfoEntity);


    int forrbiddenEntry(@Param("idList") Set<String> idList);

    List<EntryInfoEntityDO> selectEntryInfosByIDs(@Param("entryIDs") List<String> entryIdList);

    Set<String> selectEntryInfoIDsByConditions(
        @Param("ids") List<String> ids,
        @Param("entryInfoEntity") EntryInfoEntityQO entryInfoEntity,
        @Param("clearMatchSet") Set<String> clearMatchSet,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );

    Set<String> selectEntryInfoIDsByTranslateInfos(@Param("entryInfoEntity")EntryInfoEntityQO entryInfoEntity,@Param("ids") Set<String> entryInfoIDs,@Param("clearMatchSet") Set<String> clearMatchSet);

    Set<String> selectEntryInfoIDsByEntryTransConditions(
        @Param("entryInfoEntity")EntryInfoEntityQO entryInfoEntity,
        @Param("ids") Set<String> entryInfoIDs,
        @Param("productIDs") Set<String> productIDs,
        @Param("clearMatchSet") Set<String> clearMatchSet,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    String countEntryIDsByConditions(
        @Param("entryInfoEntity")EntryInfoEntityQO entryInfoEntity,
        @Param("ids") Set<String> entryInfoIDs,
        @Param("productIDs") Set<String> productIDs,
        @Param("clearMatchSet") Set<String> clearMatchSet,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );

    /**
     * 查询指定的产品中是否存在以下词条
     * 查询指定产品的指定任务中是否存在以下词条
     * 查询库中是否存在以下词条
     * @param entryInfoReplicateCheckVOs
     * @param productID
     * @param taskID
     * @return
     */
    Set<EntryInfoReplicateCheckVO> newGetEntryInfoExists(@Param("checkConditions") Set<EntryInfoReplicateCheckVO> entryInfoReplicateCheckVOs,@Param("productID") String productID,@Param("taskID") String taskID);

    Set<String> selectEntrySourcesByIDs(@Param("productIDs") Set<String> productIDs,@Param("entryIDs") Set<String> entryIDs,@Param("writeType") String writeType);

    Set<String> selectWriteTypesByIDs(
        @Param("column") String columnName,
        @Param("productIDs") Set<String> productIDs,
        @Param("entryIDs") Set<String> entryIDs,
        @Param("writeType") String writeType
    );

    List<TaskStateEntityDO> countEntryTranslateStateForTasks(@Param("taskIDs") Set<String> taskIDs);
}




