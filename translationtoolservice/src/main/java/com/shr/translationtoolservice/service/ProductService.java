package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.ProductEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.entity.vo.ProductTreeVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
public interface ProductService extends IService<ProductEntity> {


    List<VersionEntity> getProductVersion(String productName, String department);

    List<ProductEntity> getProduct(ProductEntity productEntity);

    int getProductTotal(ProductEntity productEntity);

    String addProduct(ProductEntity productEntity, HttpServletRequest request);

    String deleteProduct(List<String> idList);

    String updateProduct(ProductEntity productEntity);


}
