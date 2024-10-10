package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.TI8nAddress;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author zhp
* @description 针对表【t_i8n_address】的数据库操作Service
* @createDate 2024-09-13 09:13:14
*/
@Service
public interface TI8nAddressService extends IService<TI8nAddress> {

    List<TI8nAddress> getI18nAdress();

    int addI18nAdress(String ip);

    int changeI18nAdress(String id, String ip);

    int deleteI18nAdress(String id);
}
