package com.DONGZJ.shortLink.project.controller;

import com.DONGZJ.shortLink.project.comment.convention.Result;
import com.DONGZJ.shortLink.project.comment.convention.Results;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkCreateReqDO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkPageReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkUpdateReqDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkCreateRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkGroupQueryCountRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkPageRespDTO;
import com.DONGZJ.shortLink.project.mq.test.MessageQueue;
import com.DONGZJ.shortLink.project.service.ShortLinkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService service;
    private final MessageQueue messageQueue;
    @GetMapping("/api/short-link/project/v1/link/dmm")
    public void test(){
        messageQueue.sendMessage();
    }
    /**
     * 创建短链接
     */
    @PostMapping("api/short-link/project/v1/link")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDO requestParam){
        return Results.success(service.createLink(requestParam));
    }
    /**
     * 分页查询
    * */
    @GetMapping("api/short-link/project/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        return Results.success(service.pageShortLink(requestParam));
    }
    /**
     * 查询短链接分组内数量
    * */
    @GetMapping("api/short-link/project/v1/count")
    public Result<List<ShortLinkGroupQueryCountRespDTO>> listGroupShortLinkCount(@RequestParam("requestParam") List<String> requestParam){
        return Results.success(service.listGroupShortLinkCount(requestParam));
    }
    /**
     * 短链接跳转
    * */
    @GetMapping("/{short-uri}")
    public void restoreUrl(@PathVariable("short-uri") String shortUri, ServletRequest request, ServletResponse response) throws IOException {
        service.restoreUrl(shortUri, request, response);
    }
    @PostMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        service.updateShortLink(requestParam);
        return Results.success();
    }
}
