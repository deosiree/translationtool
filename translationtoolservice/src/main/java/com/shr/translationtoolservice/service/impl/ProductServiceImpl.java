package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.ProductEntity;
import com.shr.translationtoolservice.service.ProductService;
import com.shr.translationtoolservice.dao.ProductMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity>
    implements ProductService{

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CommonUtils commonUtils;

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
        String id = commonUtils.getUUID();
        productEntity.setId(id);
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        productEntity.setCreator(userName);
        productEntity.setDepartment(department);
        productEntity.setIsDelete(0);

        int insert =  productMapper.insert(productEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }

        return id;
    }

    @Override
    public String deleteProduct(List<String> idList) {
        int delete = productMapper.deleteByIds(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
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




