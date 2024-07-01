package com.DONGZJ.shortLink.project.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ShortLinkCreateRespDTO {
    private String  gid;
    /**
     * 原始链接
     */
    private String orignUrl;

    /**
     * 短链接
    * */
    private String fullShortUrl;


}
