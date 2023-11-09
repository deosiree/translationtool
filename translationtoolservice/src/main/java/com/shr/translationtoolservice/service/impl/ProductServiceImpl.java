package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ProductEntity;
import com.shr.translationtoolservice.service.ProductService;
import com.shr.translationtoolservice.dao.ProductMapper;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity>
    implements ProductService{

}




