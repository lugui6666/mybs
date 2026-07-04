package com.whpu.mybs.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页查询结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends R<List<T>> {

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Long page;

    /** 每页大小 */
    private Long size;

    public static <T> PageResult<T> of(List<T> records, Long total, Long page, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setCode(200);
        result.setMessage("success");
        result.setTimestamp(System.currentTimeMillis());
        result.setData(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

}
