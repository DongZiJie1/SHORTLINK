package com.DONGZJ.shortLink.project.service;

import com.DONGZJ.shortLink.project.dao.entity.ShortLinkDO;
import com.DONGZJ.shortLink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkCreateReqDO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkPageReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkUpdateReqDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkCreateRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkGroupQueryCountRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkPageRespDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.List;

public interface ShortLinkService extends IService<ShortLinkDO> {
    ShortLinkCreateRespDTO createLink(ShortLinkCreateReqDO requestParam);

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    List<ShortLinkGroupQueryCountRespDTO> listGroupShortLinkCount(List<String> requestParam);

    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) throws IOException;

    void updateShortLink(ShortLinkUpdateReqDTO requestParam);
    /**
     * 短链接统计
     *
     * @param fullShortUrl         完整短链接
     * @param gid                  分组标识
     * @param shortLinkStatsRecord 短链接统计实体参数
     */
    void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO shortLinkStatsRecord);
}
