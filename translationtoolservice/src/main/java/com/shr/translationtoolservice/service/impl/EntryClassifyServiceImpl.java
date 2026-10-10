package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.UserProductMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryClassify;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.ProductRelationEntity;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.service.EntryClassifyService;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.ProductMapper;
import com.shr.translationtoolservice.dao.ProductRelationMapper;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;

import lombok.extern.slf4j.Slf4j;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@Slf4j
public class EntryClassifyServiceImpl extends ServiceImpl<EntryClassifyMapper, EntryClassify>
    implements EntryClassifyService{

    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    @Autowired
    private UserProductMapper userProductMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private ProductRelationMapper productRelationMapper;

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CommonUtils commonUtils;

    @Override
    public List<EntryClassify> getEntryClassfy(String department,String className,HttpServletRequest request) {
        //查询对应部门下的分类
        List<EntryClassify> entryClassifies = new ArrayList<>();
        if (StringUtils.isNotBlank(className)){
            // 通过名称条件查询
            // 查询所有的父类
            List<EntryClassify> parentClassify = entryClassifyMapper.getParentClassify(department,className);
            // 查询所有的子类
            List<EntryClassify> childClassify = entryClassifyMapper.getChildClassify(department,className);
            for (EntryClassify classify : parentClassify) {
                if(!entryClassifies.contains(classify)){
                    entryClassifies.add(classify);
                }
            }
            for (EntryClassify classify : childClassify) {
                if(!entryClassifies.contains(classify)){
                    entryClassifies.add(classify);
                }
            }
        }else{
            entryClassifies = entryClassifyMapper.getEntryClassfyIdsByDepartment(department);
        }

        if (StringUtils.isNotBlank(department)){
            // 非管理员
            // 查询该用户可见的产品
            String token = request.getHeader("token");
            String userName = JWTTokenUtils.getUserName(token);
            String userDep = JWTTokenUtils.getDepartment(token);
            List<String> readProductIds = userProductMapper.getUserReadProductId(userName,userDep);
            List<EntryClassify> newList = new ArrayList<>();
            for (EntryClassify entryClassify : entryClassifies) {
                if (ConstantInterface.PRODUCT_TABLE.equals(entryClassify.getType()) ){
                    if (readProductIds.contains(entryClassify.getKey())){
                        newList.add(entryClassify);
                    }
                }else {
                    newList.add(entryClassify);
                }
            }
            entryClassifies = newList;
        }

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
//            Collections.sort(children, Comparator.comparingInt(EntryClassify::getIndex));
            // 给父节点设置子节点
            classify.setChildren(children);
        }
        Collections.sort(tree, Comparator.comparingInt(EntryClassify::getIndex));
        return tree;
    }
    @Override
    public String addEntryClassfy(EntryClassify entryClassify, HttpServletRequest request) {
        if (StringUtils.isBlank(entryClassify.getKey())){
            entryClassify.setKey(commonUtils.getUUID());
        }
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

    /**
     *   
        1、先遍历所有的分类,找到分类下所有的产品，然后判断所有的产品关联的任务
        2、判断有没有关联的任务是开启状态
             如果有
                 提示用户当前有开启的任务，不能删除
             否则
                 找到任务下的词条，把词条的is_delete设定为1，任务的状态也设定为1
                 然后删除entryClassify，删到词条
        
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW,isolation = Isolation.REPEATABLE_READ,rollbackFor = Exception.class)
    @Override
    public String deleteEntryClassfy(List<String> idList) {
        // int update = entryClassifyMapper.deleteList(idList);
        // if (update < ConstantInterface.DB_SUCCESS_RESULT) {
        //     return ErrorCodeList.UPDATE_ERROR;
        // }
        // return ConstantInterface.OK_STR;

        for(String id : idList){
            try {
                if(id == null){
                    throw new RuntimeException("提供的id信息为null");
                }
                boolean result = deleteEntryClassfyInternal(id);
                if(!result){
                    return ErrorCodeList.UPDATE_ERROR;

                }
            } catch (Exception e) {
                // TODO: handle exception
                throw e;
            }
        }
        return ConstantInterface.OK_STR;
    }


    /**
     * 根据classfyId确定该类别下所有的产品，删除所有的产品下所有的信息，该方法主要功能如下:
     * 1、调用{@link #getProductClassfyList}获取所有的产品
     * 2、开启事务，并调用{@link #deleteEntryClassfyOnProduct}删除对应产品的信息
     * 3、如果成功，则提交事务，在删除过程中，如果其中一个产品的信息删除失败，则回滚之前已经删除的其他的产品的相关信息
     * @param classfyId
     * @return {@code true}代表删除成功,{@code false}代表删除失败，其中失败的原因为该词条分类下的产品存在未完成的任务
     */
    @Transactional(propagation = Propagation.REQUIRED,isolation = Isolation.REPEATABLE_READ,rollbackFor = Exception.class)
    public boolean deleteEntryClassfyInternal(String classfyId){
        
        EntryClassify entryClassfyById = entryClassifyMapper.getEntryClassfyById(classfyId);
        String classfyType = entryClassfyById.getType();
        List<String> entryClassfyIds = new ArrayList<>();
        // 添加需要删除的产品
        if(classfyType.equals("classify")){
            // 分类底下会有很多的product
            getProductClassfyList(classfyId, entryClassfyIds);
        }else if(!classfyType.equals("module")){
            entryClassfyIds.add(classfyId);
        }else if(!classfyType.equals("product")){
            throw new RuntimeException("当前提供的classfyId对应的词条分类不是product也不是classify,当前为: " + classfyType);
        
        }else{
            entryClassfyIds.add(classfyId); // classfyId对应的type是product
        }

        try {
            // 删除
            for(String entryClassfyId : entryClassfyIds){
                boolean isSuccess = deleteSingleEntryClassfy(entryClassfyId);
                if(!isSuccess){
                    // 其中一个产品的任务存在未完成的,回滚之前所有的结果,手动回滚的逻辑要改一下 
                    throw new RuntimeException("删除失败,该词条分类下的产品存在未完成的任务");
                }
            }
            return true;
        }catch (Exception e) {
            // TODO: handle exceptio
            // 回滚
            throw e;
        }
        
    }


    /**
     * 删除一个product下的所有信息
     * 注意事项：该方法没有开启事务进行执行，即删除每一个表的记录都被视为是单独的一次事务过程，如果需要保证一致性，在调用该
     * 方法的方法中应当开启事务
     * @param entryClassfyId
     * @return {@code true}代表删除成功,{@code false}代表删除过程中发现该类别下的产品的任务存在未完成的，不能进行删除 
     */
    @Transactional(propagation = Propagation.REQUIRED,isolation = Isolation.REPEATABLE_READ)
    public boolean deleteSingleEntryClassfy(String entryClassfyId){

        List<String> entryClassfyList = Arrays.asList(entryClassfyId);
        // 一个产品可能有多个任务
        try {
            TaskInfoEntity taskInfoEntityTemplate = new TaskInfoEntity();
            taskInfoEntityTemplate.setProductId(entryClassfyId);
            List<TaskInfoEntity> taskInfo = taskInfoMapper.getTaskInfo(taskInfoEntityTemplate, -1, -1);
            List<TaskInfoEntity> completedTaskInfo = taskInfo.stream().filter(new Predicate<TaskInfoEntity>() {
    
                @Override
                public boolean test(TaskInfoEntity t) {
                    // TODO Auto-generated method stub
                    String state = t.getState();
                    return Integer.parseInt(state) > 5;
    
                }
                
            }).collect(Collectors.toList());
            if(taskInfo.size() != completedTaskInfo.size()){
                //存在未完成的任务，不能删除
                return false;
            }
            /**
             * 删除的顺序如下:删除词条、任务、产品、分类、关联表
             */
            // 删除词条
            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setProductId(entryClassfyId);
            List<ProductRelationEntity> productionRelation = productRelationMapper.getProductionRelation(productRelationEntity);
            // 获取词条id
            List<String> entryIds = productionRelation.stream().map(ProductRelationEntity::getEntryId).collect(Collectors.toList());
            // 词条对应的is_delete 设定为1
            if(!entryIds.isEmpty())
                entryInfoMapper.deleteByIdList(entryIds, ConstantInterface.ENTRY_INFO_TABLE_NAME);

            // 执行成功，删除t_task_info表中的内容
            if(!completedTaskInfo.isEmpty()){
                taskInfoMapper.deleteByIds(completedTaskInfo.stream().map(TaskInfoEntity::getId).collect(Collectors.toList()));
            }
            
            // 执行成功，删除t_product的内容,update可能为0，当entryClassfyId的类别不是product
            int update = productMapper.deleteList(entryClassfyList);
            // 执行成功，删除t_entry_classfy表中的内容
            entryClassifyMapper.deleteList(entryClassfyList);
            // 执行成功，删除t_product_relation表中的内容
            productRelationMapper.deleteByProductIdList(entryClassfyList);
            
            return true;
        } catch (Exception e) {
            // TODO: handle exception
            throw e;
        }

    }

    private List<String> getProductClassfyList(String classfyID, List<String> productidList) {

        ArrayList<String> classfyList = new ArrayList<>();
        classfyList.add(classfyID);
        List<EntryClassify> entryClassfyByParentId = entryClassifyMapper.getEntryClassfyByParentId(classfyList);
        for (EntryClassify entryClassify : entryClassfyByParentId) {
            getProductClassfyList(entryClassify.getKey(), productidList);
        }
        productidList.add(classfyID);
        
        return productidList;
    }

}




