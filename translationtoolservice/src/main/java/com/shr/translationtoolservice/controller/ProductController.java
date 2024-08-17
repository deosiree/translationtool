package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.entity.ProductEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.UserProductEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.entity.vo.UserDetailsVo;
import com.shr.translationtoolservice.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
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
public class ProductController extends BaseController {


    @Autowired
    private ProductService productService;

    @PostMapping("/getProductVersion")
    @ApiOperation("获取产品版本")
    @CrossOrigin
    @Token
    public HttpResponse<ResponseListModel<VersionEntity>> getProductVersion(String productName, String department) {
        ResponseListModel<VersionEntity> responseListModel = new ResponseListModel<VersionEntity>();
        List<VersionEntity> versionEntities = productService.getProductVersion(productName, department);
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
//        int total = productService.getProductTotal(productEntity);
        result.setTotalNum(0);
        return checkResult(result);

    }

    @PostMapping("/addProduct")
    @ApiOperation("产品新增")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<ProductEntity> addProduct(@RequestBody ProductEntity productEntity, HttpServletRequest request) {
        List<ProductEntity> product1 = productService.getProduct(productEntity);
        if (!product1.isEmpty()) {
            return error(productEntity, "产品名称已存在！");
        }
        productService.addProduct(productEntity, request);
        return checkResult(productEntity);

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

    @PostMapping("/bindtPermissonByUserProduct")
    @ApiOperation("产品用户绑定")
    @CrossOrigin
    @Transactional
    //返回id 入参用户
    public HttpResponse<String> bindUserProduct(@RequestBody List<UserDetailsVo> userDetailsVos, String productID) {

        String result = productService.bindUserProduct(userDetailsVos, productID);

        return checkResult(result);

    }


    @PostMapping("/getPermissonByUserProduct")
    @ApiOperation("用户产品权限查询")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<ResponseListModel<UserDetailsVo>> getPermissonByUserProduct(String userName, String productId) {
        ResponseListModel<UserDetailsVo> result = new ResponseListModel<>();
        List<UserDetailsVo> userDetailsVos = productService.getPermissonByUserProduct(userName, productId);
        result.setList(userDetailsVos);
        result.setTotalNum(userDetailsVos.size());
        return checkResult(result);

    }

    @PostMapping("/getUserProduct")
    @ApiOperation("查询产品用户权限")
    @CrossOrigin
    @Transactional
    public HttpResponse<UserProductEntity> getUserProduct(String productId, HttpServletRequest request) {

        if (StringUtils.isBlank(productId)) {
            return null;
        }
        UserProductEntity result = productService.getUserProduct(productId, request);

        return checkResult(result);

    }

}
