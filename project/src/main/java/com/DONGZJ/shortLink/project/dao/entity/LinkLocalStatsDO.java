package com.DONGZJ.shortLink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@TableName("t_link_locale_stats")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkLocalStatsDO {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 日期
     */
    private Date date;

    /**
     * 访问量
     */
    private Integer cnt;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标识
     */
    private int delFlag;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市编码
     */
    private String adcode;

    /**
     * 市名称
     */
    private String city;

    private String country;

}
