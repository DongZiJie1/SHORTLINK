package com.DONGZJ.shortLink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
/**
 * 分页请求
* */
@Data
public class ShortLinkPageReqDTO extends Page {
    private String gid;
    /**
     * 排序标识
     */
    private String orderTag;
}
