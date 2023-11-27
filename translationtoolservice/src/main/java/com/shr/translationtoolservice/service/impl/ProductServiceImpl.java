package com.shr.translationtoolservice.service.impl;

import cn.afterturn.easypoi.cache.manager.IFileLoader;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.UserMapper;
import com.shr.translationtoolservice.dao.UserProductMapper;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.ProductTreeVO;
import com.shr.translationtoolservice.entity.vo.UserDetailsVo;
import com.shr.translationtoolservice.service.ProductService;
import com.shr.translationtoolservice.dao.ProductMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.LDAPUtils;
import com.shr.translationtoolservice.util.TreeUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 *
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity>
        implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private TreeUtils treeUtils;
    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private UserProductMapper userProductMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LDAPUtils ldapUtils;

    @Override
    public List<VersionEntity> getProductVersion(String productName, String department) {
        List<VersionEntity> versionEntities = versionMapper.getVersionByProductName(productName, department);

        return versionEntities;
    }

    @Override
    public List<ProductEntity> getProduct(ProductEntity productEntity) {
        List<ProductEntity> productEntities = productMapper.getProductList(productEntity);
        return productEntities;
    }

    @Override
    public int getProductTotal(ProductEntity productEntity) {
        return productMapper.getProductListTotal(productEntity);
    }

    @Override
    public String addProduct(ProductEntity productEntity, HttpServletRequest request) {
        if (StringUtils.isBlank(productEntity.getId())) {
            productEntity.setId(commonUtils.getUUID());
        }
        String token = request.getHeader("token");

        if (StringUtils.isBlank(productEntity.getCreator())) {
            String userName = JWTTokenUtils.getUserName(token);
            productEntity.setCreator(userName);
        }
        if (StringUtils.isBlank(productEntity.getDepartment())) {
            String department = JWTTokenUtils.getDepartment(token);
            productEntity.setDepartment(department);
        }
        //创建时间
        if (Objects.isNull(productEntity.getCreateTime())) {
            Date date = new Date(System.currentTimeMillis());
            productEntity.setCreateTime(date);
        }
        productEntity.setIsDelete(0);
        int insert = productMapper.insert(productEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String deleteProduct(List<String> idList) {

        int update = productMapper.deleteList(idList);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        return ConstantInterface.OK_STR;

    }

    @Override
    public String updateProduct(ProductEntity productEntity) {
        int update = productMapper.updateById(productEntity);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        return ConstantInterface.OK_STR;
    }

    @Override
    public String bindUserProduct(List<UserDetailsVo> userDetailsVos, String productID) {
        int insert = 0;
        for (UserDetailsVo userDetailsVo : userDetailsVos) {
            //查询表里是否有数据，如果有进行修改
            UserProductEntity userProductEntity;
            userProductEntity = userProductMapper.getPermissionByNameAndDepartment(userDetailsVo.getName(), userDetailsVo.getDepartment());
            if (Objects.isNull(userProductEntity)) {
                // 没有进行新增
                userProductEntity = new UserProductEntity();
                User user = userMapper.getPermissionByNameAndDepartment(userDetailsVo.getName(), userDetailsVo.getDepartment());
                userProductEntity.setId(commonUtils.getUUID());
                userProductEntity.setUserId(user.getId());
                userProductEntity.setProductId(productID);
                if (userDetailsVo.getReadState()) {
                    userProductEntity.setRead(1);
                } else {
                    userProductEntity.setRead(0);
                }
                if (userDetailsVo.getWriteState()) {
                    userProductEntity.setWrite(1);
                } else {
                    userProductEntity.setWrite(0);
                }
                //已有进行更新
            } else {
                if (userDetailsVo.getReadState()) {
                    userProductEntity.setRead(1);
                } else {
                    userProductEntity.setRead(0);
                }
                if (userDetailsVo.getWriteState()) {
                    userProductEntity.setWrite(1);
                } else {
                    userProductEntity.setWrite(0);
                }
            }
            insert += userProductMapper.insert(userProductEntity);

        }
        if (insert < userDetailsVos.size()) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<UserDetailsVo> getPermissonByUserProduct(String userName) {
        List<UserDetailsVo> userDetailsAllVos = new ArrayList<>();
        // key -> department , value -> ldapUsers
        Map<String, List<LDAPUser>> allUser = ldapUtils.getAllUser();
        //全量构建
        Set<String> ldapUserSet = allUser.keySet();
        Iterator<String> iterator = ldapUserSet.iterator();


        while (iterator.hasNext()) {
            UserDetailsVo userDetailsDepartVo = new UserDetailsVo();
            String department = iterator.next();

            List<LDAPUser> ldapUsers = allUser.get(department);
            List<UserDetailsVo> userDetailsVos = constructLdapList(ldapUsers, department, userName);
            if (CollectionUtils.isEmpty(userDetailsVos)) {
                continue;
            }
            userDetailsDepartVo.setName(department);
            userDetailsDepartVo.setType("department");
            userDetailsDepartVo.setChildren(userDetailsVos);
            userDetailsAllVos.add(userDetailsDepartVo);
            //userDetailsVo.setChildren(allUser.get(department));
        }

        return userDetailsAllVos;
    }

    private List<UserDetailsVo> constructLdapList(List<LDAPUser> ldapUsers, String department, String filter) {
        List<UserDetailsVo> userDetailsVos = new ArrayList<>();
        for (LDAPUser ldapUser : ldapUsers) {
            UserDetailsVo userDetailsVo = new UserDetailsVo();

            String name = ldapUser.getName();
            if (!StringUtils.isBlank(filter) && !name.equals(filter)) {
                continue;
            }
            UserProductEntity user = userProductMapper.getPermissionByNameAndDepartment(name, department);
            userDetailsVo.setType(ConstantInterface.USER);
            userDetailsVo.setName(name);

            if (!Objects.isNull(user)) {
                userDetailsVo.setDepartment(user.getDepartment());
                //角色信息添加
                if (user.getRead() == 1) {
                    userDetailsVo.setReadState(true);
                } else {
                    userDetailsVo.setReadState(false);
                }
                if (user.getWrite() == 1) {
                    userDetailsVo.setWriteState(true);
                } else {
                    userDetailsVo.setWriteState(false);
                }

            } else {
                userDetailsVo.setTranslator(false);
                userDetailsVo.setDeveloper(false);
                userDetailsVo.setTranslateReviewer(false);
                userDetailsVo.setEntryReviewer(false);
                userDetailsVo.setAdmin(false);
                userDetailsVo.setReadState(false);
                userDetailsVo.setWriteState(false);
                userDetailsVo.setDepartment(department);
            }
            userDetailsVos.add(userDetailsVo);
        }
        return userDetailsVos;
    }

}




