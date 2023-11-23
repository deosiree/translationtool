package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.entity.ProductEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @ClassName ProductController
 * @USER: Cola
 * @Date 2023/11/9 0009 16:24
 **/


@RestController
@RequestMapping("/product")
@Api(tags = "产品管理")
@Slf4j
public class ProductController extends BaseController{


    @Autowired
    private ProductService productService;

    @PostMapping("/getProductVersion")
    @ApiOperation("获取产品版本")
    @CrossOrigin
    @Token
    public HttpResponse<ResponseListModel< VersionEntity>> getProductVersion(String  productName, String department) {
        ResponseListModel< VersionEntity> responseListModel = new ResponseListModel< VersionEntity>();
        List<VersionEntity> versionEntities = productService.getProductVersion(productName,department);
        responseListModel.setList(versionEntities);
        responseListModel.setTotalNum(versionEntities.size());
        return checkResult(responseListModel);

    }



    @PostMapping("/getProduct")
    @ApiOperation("产品查询")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<ResponseListModel<ProductEntity>> getProduct(@RequestBody ProductEntity productEntity) {
        ResponseListModel<ProductEntity> result = new ResponseListModel<>();

        List<ProductEntity> productEntities = productService.getProduct(productEntity);
        result.setList(productEntities);
        int total = productService.getProductTotal(productEntity);
        result.setTotalNum(total);
        return checkResult(result);

    }

    @PostMapping("/addProduct")
    @ApiOperation("产品新增")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<String> addProduct(@RequestBody ProductEntity productEntity, HttpServletRequest request) {

        String id = productService.addProduct(productEntity,request);

        return checkResult(id);

    }

    @PostMapping("/deleteProduct")
    @ApiOperation("产品删除")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<String> deleteProduct(@RequestBody List<String> idList) {

        String result = productService.deleteProduct(idList);

        return checkResult(result);

    }

    @PostMapping("/updateProduct")
    @ApiOperation("产品编辑")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<String> updateProduct(@RequestBody ProductEntity productEntity) {

        String result = productService.updateProduct(productEntity);

        return checkResult(result);

    }



}
