package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.ProductEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.entity.vo.ProductTreeVO;
import com.shr.translationtoolservice.service.ProductService;
import com.shr.translationtoolservice.dao.ProductMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.TreeUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity>
    implements ProductService{

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private TreeUtils treeUtils;
    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private CommonUtils commonUtils;



    @Override
    public List<VersionEntity> getProductVersion(String productName, String department) {
        List<VersionEntity> versionEntities  = versionMapper.getVersionByProductName(productName,department);

        return versionEntities;
    }

    @Override
    public List<ProductEntity> getProduct(ProductEntity productEntity) {
        List<ProductEntity>  productEntities = productMapper.getProductList(productEntity);
        return productEntities;
    }

    @Override
    public int getProductTotal(ProductEntity productEntity) {
        return productMapper.getProductListTotal(productEntity);
    }

    @Override
    public String addProduct(ProductEntity productEntity, HttpServletRequest request) {
        if (StringUtils.isBlank(productEntity.getId())){
            productEntity.setId(commonUtils.getUUID());
        }
        String token = request.getHeader("token");

        if (StringUtils.isBlank(productEntity.getCreator())){
            String userName = JWTTokenUtils.getUserName(token);
            productEntity.setCreator(userName);
        }
        if (StringUtils.isBlank(productEntity.getDepartment())){
            String department = JWTTokenUtils.getDepartment(token);
            productEntity.setDepartment(department);
        }
        //创建时间
        if (Objects.isNull(productEntity.getCreateTime())){
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

       int update =  productMapper.deleteList(idList);
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




}




