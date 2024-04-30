package com.shr.translationtoolservice.util;

import com.shr.translationtoolservice.entity.Menu;
import com.shr.translationtoolservice.entity.ProductEntity;
import com.shr.translationtoolservice.entity.vo.ProductTreeVO;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @ClassName TreeUtils
 * @USER: Cola
 * @Date 2023/11/16 0016 16:36
 **/
@Component
public class TreeUtils {


    /**
     * list转tree
     *
     * @param menuList
     * @return
     */
    public List<Menu> listTree(List<Menu> menuList) {
        //新集合
        List<Menu> returnList = new ArrayList<>();

        List<String> tempList = new ArrayList<>();
        for (Menu menu : menuList) {
            tempList.add(menu.getId());
        }
        for (Menu menu : menuList) {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(menu.getParentId())) {
                recursionFn(menuList, menu);
                returnList.add(menu);
            }
        }
        //没有查询到节点则以当前节点
        if (returnList.isEmpty()) {
            returnList = menuList;
        }
        //排序
        Collections.sort(returnList, Comparator.comparingInt(Menu::getRank));

        return returnList;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<Menu> list, Menu t) {
        // 得到子节点列表
        List<Menu> childList = getChildList(list, t);
        // 子节点排序
        Collections.sort(childList, Comparator.comparingInt(Menu::getRank));
        t.setChildren(childList);
        for (Menu tChild : childList) {
            // 判断是否有子节点
            if (StringUtils.isNotBlank(tChild.getParentId()) && tChild.getParentId().equals(t.getId())) {
                for (Menu n : childList) {
                    recursionFn(list, n);
                }
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<Menu> getChildList(List<Menu> list, Menu t) {
        List<Menu> tList = new ArrayList<>();
        for (Menu n : list) {
            if (StringUtils.isNotBlank(n.getParentId()) && n.getParentId().equals(t.getId())) {
                tList.add(n);
            }
        }
        return tList;
    }



}
