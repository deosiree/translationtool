package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.TI8nAddress;
import com.shr.translationtoolservice.service.TI8nAddressService;
import com.shr.translationtoolservice.dao.TI8nAddressMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author zhp
* @description 针对表【t_i8n_address】的数据库操作Service实现
* @createDate 2024-09-13 09:13:14
*/
@Service
public class TI8nAddressServiceImpl extends ServiceImpl<TI8nAddressMapper, TI8nAddress>
implements TI8nAddressService{

    @Autowired
    private TI8nAddressMapper ti8nAddressMapper;
    @Autowired
    private CommonUtils commonUtils;
    @Override
    public List<TI8nAddress> getI18nAdress() {
        return ti8nAddressMapper.selectList(new QueryWrapper<TI8nAddress>());
    }

    @Override
    public int addI18nAdress(String ip) {
        TI8nAddress ti8nAddress = new TI8nAddress();
        ip= "http://"+ip + ":18099/";
        ti8nAddress.setIp(ip);
        ti8nAddress.setId(commonUtils.getUUID());
        ti8nAddress.setState("1");
        return ti8nAddressMapper.insert(ti8nAddress);
    }

    @Override
    public int changeI18nAdress(String id, String ip) {
        TI8nAddress ti8nAddress = new TI8nAddress();
        ti8nAddress.setIp(ip);
        ti8nAddress.setId(id);
        return ti8nAddressMapper.updateById(ti8nAddress);
    }

    @Override
    public int deleteI18nAdress(String id) {
        return ti8nAddressMapper.deleteById(id);
    }
}
