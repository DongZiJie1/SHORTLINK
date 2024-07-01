package com.DONGZJ.shortLink.project.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
/**
 * 短链接创建对象
* */
@Data
public class ShortLinkCreateReqDO {


    /**
     * 域名
     */
    private String domain;



    /**
     * 原始链接
     */
    private String originUrl;


    /**
     * 分组标识
     */
    private String gid;


    /**
     * 创建类型
     */
    private int createdType;

    /**
     * 有效期类型
     */
    private int validDateType;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date validDate;

    /**
     * 描述
     */
    private String describe;
}
