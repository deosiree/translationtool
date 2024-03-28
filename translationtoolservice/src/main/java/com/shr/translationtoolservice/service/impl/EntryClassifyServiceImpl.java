package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.UserProductMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryClassify;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.Menu;
import com.shr.translationtoolservice.service.EntryClassifyService;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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

    @Override
    public String deleteEntryClassfy(List<String> idList) {

        int update = entryClassifyMapper.deleteList(idList);

        if (update < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }
}




