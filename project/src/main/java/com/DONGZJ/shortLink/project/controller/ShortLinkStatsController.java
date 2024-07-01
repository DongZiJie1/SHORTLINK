package com.DONGZJ.shortLink.project.controller;

import com.DONGZJ.shortLink.project.comment.convention.Result;
import com.DONGZJ.shortLink.project.comment.convention.Results;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkStatsReqDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkStatsRespDTO;
import com.DONGZJ.shortLink.project.service.shortLinkStatsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 短链接监控控制层
* */
@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {
    private final shortLinkStatsService service;

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return Results.success(service.oneShortLinkStats(requestParam));
    }
    /**
     * 访问分组短链接指定时间内的监控数据
    * */
    @GetMapping("/api/short-link/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        return Results.success(service.groupShortLinkStats(requestParam));
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return Results.success(service.shortLinkStatsAccessRecord(requestParam));
    }
    /**
     * 访问分组短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        return Results.success(service.groupShortLinkStatsAccessRecord(requestParam));
    }

}
