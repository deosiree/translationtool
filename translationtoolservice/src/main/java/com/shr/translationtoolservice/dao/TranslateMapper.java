package com.shr.translationtoolservice.dao;

import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.TranslateEntitiyReplicateCheckVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @Entity com.shr.translationtoolservice.entity.TranslateEntity
 */
@Mapper
public interface TranslateMapper extends BaseMapper<TranslateEntity> {

    List<TranslateEntity> getTrans(@Param("translateEntity") TranslateEntity translateEntity,@Param("offset") int offset,@Param("limit")  Integer pageSize);


    int getPublicEntryTotal(@Param("translateEntity")TranslateEntity translateEntity);

    int deleteById(@Param("translateEntity") TranslateEntity translateEntity);

    int deleteByIds(@Param("idList") List<String> idList);

    TranslateEntity selectPublicByEntry(TranslateEntity translateEntity);

    List<TranslateEntity> getPublicEntry(@Param("translateEntity") TranslateEntity translateEntity,
                                         @Param("offset") int offset,@Param("limit")  Integer pageSize);

    int updateTranslation(@Param("translateEntityList") List<TranslateEntity> translateEntityList);

    int updateEntity(@Param("translateEntity") TranslateEntity translateEntity);

    List<TranslateEntity> selectRepByEntryTemp(@Param("entryTempEntity") EntryTempEntity entryTempEntity);

    List<TranslateEntity> getSuggestTrans(@Param("name") String name,@Param("translateType")  String type,@Param("visualRange")  String visualRangeh);
    List<TranslateEntity> getVersionSuggestTrans(@Param("entry") String entry,@Param("translateType") String translateType,@Param("visualRange")  String visualRange);

    List<TranslateEntity> getRepTrans(@Param("entry")String entry,@Param("translateType") String translateType,@Param("translate") String translate,@Param("visualRange")  String visualRange);


    List<TranslateEntity> getSykTrans(@Param("translateEntity") TranslateEntity translateEntity,@Param("matchList") Set<String> matchList,@Param("offset") int offset,@Param("limit")  Integer pageSize);

    List<TranslateEntity> checkSykSameEntry(@Param("translateEntity") TranslateEntity translateEntity,@Param("offset") int offset,@Param("limit")  Integer pageSize);

    int insertTranslate(@Param("translateEntity") TranslateEntity translateEntity);

    int batchInsertTranslate(@Param("translateEntities") Collection<TranslateEntity> translateEntities);
    List<TranslateEntity> getTransForCheckSykEntry(@Param("translateEntity") TranslateEntity translateEntity,@Param("offset") int offset,@Param("limit")  Integer pageSize);
    List<TranslateEntity> getTransForSykNotUsed(@Param("translateEntity") TranslateEntity translateEntity,@Param("offset") int offset,@Param("limit")  Integer pageSize);

    List<TranslateEntitiyReplicateCheckVO> newGetRepTrans(@Param("conditionVOs") Set<TranslateEntitiyReplicateCheckVO> translateEntitiyReplicateCheckVOs);

}




