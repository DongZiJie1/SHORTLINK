package com.DONGZJ.shortLink.project.service;

import com.DONGZJ.shortLink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkStatsReqDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkStatsRespDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface shortLinkStatsService {
     IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam);



    /**
     * 获取单个短链接监控数据
     *
     * @param requestParam 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     *
     * @param requestParam 获取短链接监控访问记录数据入参
     * @return 访问记录监控数据
     */
    IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam);

    ShortLinkStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam);
}
