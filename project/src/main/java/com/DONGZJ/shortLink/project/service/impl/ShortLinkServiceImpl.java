package com.DONGZJ.shortLink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.DONGZJ.shortLink.project.comment.convention.exception.ClientException;
import com.DONGZJ.shortLink.project.comment.convention.exception.ServiceException;
import com.DONGZJ.shortLink.project.dao.entity.*;
import com.DONGZJ.shortLink.project.dao.mapper.*;
import com.DONGZJ.shortLink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkCreateReqDO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkPageReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkUpdateReqDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkCreateRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkGroupQueryCountRespDTO;
import com.DONGZJ.shortLink.project.dto.resp.ShortLinkPageRespDTO;
import com.DONGZJ.shortLink.project.mq.GeneralMessageDemoProduce;
import com.DONGZJ.shortLink.project.mq.GeneralMessageEvent;
import com.DONGZJ.shortLink.project.mq.producer.RocketmqProducer;
import com.DONGZJ.shortLink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.DONGZJ.shortLink.project.service.LinkStatsTodayService;
import com.DONGZJ.shortLink.project.service.ShortLinkService;
import com.DONGZJ.shortLink.project.util.HashUtil;
import com.DONGZJ.shortLink.project.util.LinkUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.producer.SendResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.DONGZJ.shortLink.project.comment.enums.VailDateTypeEnum;
import org.springframework.transaction.annotation.Transactional;

import static com.DONGZJ.shortLink.project.comment.Constant.RedisKeyConstant.*;
import static com.DONGZJ.shortLink.project.comment.Constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private final GeneralMessageDemoProduce generalMessageDemoProduce;
    private final RocketmqProducer rocketmqProducer;
    private final RBloomFilter<String> shortLinkRegisterCachePenetrationBloomFilter;
     private final ShortLinkGotoMapper shortLinkGotoMapper;
     private final StringRedisTemplate redisTemplate;
     private final RedissonClient redissonClient;
     private final LinkAccessStatsMapper linkAccessStatsMapper;
     private final LinkLocaleStatsMapper linkLocaleStatsMapper;
     private final LinkOsStatsMapper linkOsStatsMapper;
     private final LinkBrowserStatsMapper linkBrowserStatsMapper;
     private final LinkDeviceStatsMapper linkDeviceStatsMapper;
     private final LinkNetworkStatsMapper linkNetworkStatsMapper;
     private final LinkAccessLogsMapper linkAccessLogsMapper;
     private final LinkStatsTodayMapper linkStatsTodayMapper;
     private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
     private final LinkStatsTodayService linkStatsTodayService;

    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;
    /**
     * 创建短链接
    * */
    @Override
    public ShortLinkCreateRespDTO createLink(ShortLinkCreateReqDO requestParam) {
        //生成短链接
       String shortLinkSuffix = generator(requestParam);
       //完整链接
       String fullShortUrl = createShortLinkDefaultDomain + ":8002/" + shortLinkSuffix;
        //生成短链接数据库实体
       ShortLinkDO shortLinkDO = BeanUtil.toBean(requestParam, ShortLinkDO.class);
       shortLinkDO.setDomain("");
       shortLinkDO.setShortUri(shortLinkSuffix);
       shortLinkDO.setEnableStatus(0);
       shortLinkDO.setFullShortUri(fullShortUrl);
       shortLinkDO.setFavicon(getFavicon(requestParam.getOriginUrl()));
       shortLinkDO.setTodayPv(0);
       shortLinkDO.setTodayUv(0);
       shortLinkDO.setTodayUip(0);
       shortLinkDO.setOrignUrl(requestParam.getOriginUrl());
       ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .gid(requestParam.getGid())
                .fullShortUri(fullShortUrl)
                .build();
       try{
           shortLinkGotoMapper.insert(shortLinkGotoDO);
           baseMapper.insert(shortLinkDO);
       }catch (DuplicateKeyException ex){
           //出现了短链接重复 说明 布隆过滤器里没有数据 但是 mysql里已经有数据了
           //出现这种一般是很极端的情况，比如  数据库持久化成功了 但是走到下面的布隆过滤器的redis持久化失败了
           //导致 布隆过滤器中没有 但是 数据库中有记录。
           // 首先判断是否存在布隆过滤器，如果不存在直接新增
           if (!shortLinkRegisterCachePenetrationBloomFilter.contains(fullShortUrl)) {
               shortLinkRegisterCachePenetrationBloomFilter.add(fullShortUrl);
           }
           throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
//           //防止出现意外 比如缓存和数据库不一致
//           LambdaQueryWrapper<ShortLinkDO> eq = Wrappers.lambdaQuery(ShortLinkDO.class)
//                   .eq(ShortLinkDO::getFullShortUri, fullShortUrl);
//           ShortLinkDO hasShortLinkDO = baseMapper.selectOne(eq);
//           //如果确实发生了异常,就这么处理,没有发生异常执行后续逻辑
//           if(hasShortLinkDO != null){
//               log.warn("短链接: "+fullShortUrl+"重复入库");
//               throw new ServiceException("生成短链接重复");
//           }
       }
       //生成短链接的时候就应该去存到缓存中,做缓存预热
       redisTemplate.opsForValue().set(String.format(
               GOTO_SHORT_LINK_KEY, fullShortUrl),
               shortLinkDO.getOrignUrl(),
               LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
               TimeUnit.MILLISECONDS
       );
       shortLinkRegisterCachePenetrationBloomFilter.add(fullShortUrl);
       return ShortLinkCreateRespDTO.builder()
               .fullShortUrl(shortLinkDO.getFullShortUri())
               .orignUrl(requestParam.getOriginUrl())
               .gid(requestParam.getGid())
               .build();
    }
    /**
     * 短链接分页查询
    * */
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setFullShortUrl("nur.ink:8002/"+each.getShortUri());
            result.setDomain("http://"+"nur.ink");
            LambdaQueryWrapper<LinkStatsTodayDO> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(LinkStatsTodayDO::getDate,LocalDate.now(ZoneId.of("Asia/Shanghai")))
                    .eq(LinkStatsTodayDO::getGid,each.getGid())
                    .eq(LinkStatsTodayDO::getFullShortUrl,each.getFullShortUri());
            LinkStatsTodayDO linkStatsTodayDO = linkStatsTodayMapper.selectOne(queryWrapper);
            if(linkStatsTodayDO != null){
                result.setTodayPv(linkStatsTodayDO.getTodayPv());
                result.setTodayUip(linkStatsTodayDO.getTodayUip());
                result.setTodayUv(linkStatsTodayDO.getTodayUv());
            }
            return result;
        });
    }
    /**
     * 分组查询短链接数量
    * */
    @Override
    public List<ShortLinkGroupQueryCountRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> wrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid,count(*) as count")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        //这里只有用map才能复制对象到返回list之中.
        List<Map<String,Object>> objects = baseMapper.selectMaps(wrapper);
        return BeanUtil.copyToList(objects,ShortLinkGroupQueryCountRespDTO.class);
    }
    /**
     * 短链接跳转
    * */
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) throws IOException {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName + "/" + shortUri;
        String originalLink = redisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl));
        //字符串格式化函数 这里相当于两个拼接在一起
        //判断redis里是否有短链接跳转的key
        if(StrUtil.isNotBlank(originalLink)){
            //更新这条短链接的状态统计
            //0.1
//            shortLinkStats(fullShortUrl,null,request,response);
            //redis作为消息队列
//            sendMessage();
            send1("DINGMM");
            sendMessage();
//            shortLinkStats(fullShortUrl, null, buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        //缓存中没有判断布隆过滤器中是否含有 fullShortUrl
        //如果没有 说明肯定这个短链接从来没有被生成过,那么数据库中也肯定没有
        //如果判定有,由于误判有两种情况,不管真有还是假有 ,都要继续判断.后续判断可以解决布隆过滤器的误判缺点
        boolean contains = shortLinkRegisterCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //在缓存中判断是否有跳转的空值,如果有空值,那么代表这个短链接是不存在的,因为只有不存在的短链接,才会被扔到redis中(缓存空值的校验)
        String gotoIsNullShortLink = redisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //redis中没有 去数据库里找有没有该短链接,但不能让所有请求都去数据库找,所以用redisson的锁,抢占到锁的去开始lock
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try{
            //为了防止后来进锁的线程再次进来该锁执行以下逻辑,这里进行双重判定锁
            originalLink = redisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl));
            if(StrUtil.isBlank(originalLink)){
                //查询路由表中是否有完整短链接 因为分片键是完整短链接
                LambdaQueryWrapper<ShortLinkGotoDO> gotoDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUri, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(gotoDOLambdaQueryWrapper);
                //shortLinkGotoDO == null 代表没有这个东西 直接返回
                if(shortLinkGotoDO == null){
                    //缓存空值 做一个风控
                    //这里为空,代表短链接其实根本就不存在 我们呼应上文中 "gotoIsNullShortLink" 来做一个缓存空值
                    redisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30,TimeUnit.MINUTES);
                    return;
                }
                //shortLinkGotoDO 不为空 查询短链接表 带着gid(短链接表的分片键)
                LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                        .eq(ShortLinkDO::getFullShortUri, fullShortUrl)
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
                //shortLinkDO == null 这种情况一般不会出现 代表路由表有记录 但是 短链接表没有
                //可能情况1.同一个短链接再两个表的gid不同
                //2.保存了路由表但是没有保存短链接表
                if(shortLinkDO == null  ||( shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) ){
                    redisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }
                redisTemplate.opsForValue().set(
                        String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                        shortLinkDO.getOrignUrl(),
                        LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS
                );
//                shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
                shortLinkStats(fullShortUrl, shortLinkDO.getGid(), buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOrignUrl());

            }else {
//                shortLinkStats(fullShortUrl,null,request,response);
                shortLinkStats(fullShortUrl, null, buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                ((HttpServletResponse) response).sendRedirect(originalLink);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    /**
     * 生成短链接后缀,要避免生成短链接冲突
    * */
    private String generator(ShortLinkCreateReqDO requestParam){
        String shortUrl;
        int customGeneratorCount = 0;
        String origin = requestParam.getOriginUrl();
        while(true){
            if(customGeneratorCount > 10){
                throw  new ServiceException("短链接频繁生成,请稍后再试");
            }
            shortUrl = HashUtil.hashToBase62(origin);
            //避免查询数据库,这是一个布隆过滤器的很好应用的地方
            if(!shortLinkRegisterCachePenetrationBloomFilter.contains("nur.ink"+"/"+ shortUrl)){
                break;
            }
            //如果走到这里说明布隆过滤器重复了，则需要重新生成一个url
            origin += java.util.UUID.randomUUID().toString();
            customGeneratorCount++;
        }
        return shortUrl;
    }
    @SneakyThrows
    private String getFavicon(String url) {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == responseCode) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }
    /**
     * 短链接统计
    * */
    private void shortLinkStats(String fullShortUrl, String gid, ServletRequest request, ServletResponse response){
        AtomicBoolean uvFirstFlag = new AtomicBoolean();//原子类 线程安全
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();//获取cookie数组
        try{
            AtomicReference<String> uv = new AtomicReference<>();//原子更新引用对象
            Runnable addResponseCookieTask = () -> {
                uv.set(UUID.fastUUID().toString());//uv赋值
                Cookie uvCookie = new Cookie("uv", uv.get());//生成cookie
                uvCookie.setMaxAge(60 * 60 * 24 * 30);//时间
//                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));//取出短链接后缀
                uvCookie.setPath("/");
                ((HttpServletResponse) response).addCookie(uvCookie);//给response加上cookie
                uvFirstFlag.set(Boolean.TRUE);
                redisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
            };
            if (ArrayUtil.isNotEmpty(cookies)) {
                //找到第一个名字叫uv的cookie
                //获取它的value
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals(each.getName(), "uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            //如果uv存在,不存在执行  addResponseCookieTask
                            //能走到这里说明是 名字为uv的cookie不为空
                            //而这里的uv是上文中的一个变量 这里的each  其实是 Cookie::getValue
                            uv.set(each);//log的用户标识
                            Long uvAdded = redisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            System.out.println(uvAdded);//0代表添加的是重复数据
                            uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                        }, addResponseCookieTask);
            } else {
                addResponseCookieTask.run();
            }
            //获取ip 来处里uip
            String remoteAddr = LinkUtil.getActualIp((HttpServletRequest) request);//获取ip
            Long uipAdded = redisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);//放redis的set结构判断
            Boolean uipFirstFlag = (uipAdded != null && uipAdded>0L);//不是0 说明 不重复 uip++
            //获取gid
            if(StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDO> wrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUri, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(wrapper);
                gid = shortLinkGotoDO.getGid();
            }
            //获取ip地理位置
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocaleAmapKey);
            localeParamMap.put("ip", remoteAddr);
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);//向高德地图发请求
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);//返回对象
            String infoCode = localeResultObj.getString("infocode");
            String actualProvince;
            String actualCity;
            // infoCode 不为空 且 infoCode = 10000
            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                String province = localeResultObj.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                LinkLocalStatsDO linkLocaleStatsDO = LinkLocalStatsDO.builder()
                        .province(actualProvince = unknownFlag ? "未知" : province)
                        .city(actualCity = unknownFlag ? "未知" : localeResultObj.getString("city"))
                        .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .country("中国")
                        .gid(gid)
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
                //获取系统
                String os = LinkUtil.getOs((HttpServletRequest) request);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .os(os)
                        .cnt(1)
                        .date(new Date())
                        .build();
                linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
                //获取浏览器
                String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .browser(browser)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);

                //获取设备
                String device = LinkUtil.getDevice(((HttpServletRequest) request));
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .device(device)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
                //获取网络
                String network = LinkUtil.getNetwork(((HttpServletRequest) request));
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .network(network)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
                //增加访问记录  为了做高频ip记录
                //这里可以做访客判断是老访客还是新访客 通过user这个字段.
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .user(uv.get())
                        .ip(remoteAddr)
                        .browser(browser)
                        .os(os)
                        .network(network)
                        .device(device)
                        .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);


                //uv pv uip
                int hour = DateUtil.hour(new Date(), true);
                Week week = DateUtil.dayOfWeekEnum(new Date());
                int weekValue = week.getIso8601Value();
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .hour(hour)
                        .weekday(weekValue)
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .pv(1)
                        .uv(uvFirstFlag.get() ? 1 : 0)
                        .uip(uipFirstFlag ? 1 : 0)
                        .date(new Date())
                        .build();
                linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
                baseMapper.incrementStats(gid, fullShortUrl, 1, uvFirstFlag.get() ? 1 : 0, uipFirstFlag ? 1 : 0);
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                        .todayPv(1)
                        .todayUv(uvFirstFlag.get() ? 1 : 0)
                        .todayUip(uipFirstFlag ? 1 : 0)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
            }
        }catch (Throwable ex){
            log.error("短链接统计异常",ex);
        }
    }
    /**
     * 修改短链接
    * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        //1 判断是否有该短链接
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUri, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        //2. 如果为null  不存在
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        //3. 如果两个gid相同 说明不用改变分组
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUri, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .favicon(hasShortLinkDO.getFavicon())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .orignUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
        // 两个gid 不同  需要将原先的短链接 删除
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            if (!rLock.tryLock()) {
                throw new ServiceException("短链接正在被访问，请稍后再试...");
            }
            try {
                //确认更新条件
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUri, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
//                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
//                        .delTime(System.currentTimeMillis())
                        .build();
                //将删除标志设为1
                delShortLinkDO.setDelFlag(1);
                //更新数据库
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                //设置新的要插入的短链接实体
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .domain(createShortLinkDefaultDomain)
                        .orignUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(hasShortLinkDO.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(hasShortLinkDO.getShortUri())
                        .enableStatus(hasShortLinkDO.getEnableStatus())
                        .totalPv(hasShortLinkDO.getTotalPv())
                        .totalUv(hasShortLinkDO.getTotalUv())
                        .totalUip(hasShortLinkDO.getTotalUip())
                        .fullShortUri(hasShortLinkDO.getFullShortUri())
                        .favicon(getFavicon(requestParam.getOriginUrl()))
//                        .delTime(0L)
                        .build();
                baseMapper.insert(shortLinkDO);
                //today 统计有每一天的统计 所以是List收集结果
                LambdaQueryWrapper<LinkStatsTodayDO> statsTodayQueryWrapper = Wrappers.lambdaQuery(LinkStatsTodayDO.class)
                        .eq(LinkStatsTodayDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkStatsTodayDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkStatsTodayDO::getDelFlag, 0);
                List<LinkStatsTodayDO> linkStatsTodayDOList = linkStatsTodayMapper.selectList(statsTodayQueryWrapper);
                //如果不为空 则利用流获取List中的每天记录的id，然后将today表的所有记录都删除掉
                if (CollUtil.isNotEmpty(linkStatsTodayDOList)) {
                    linkStatsTodayMapper.deleteBatchIds(linkStatsTodayDOList.stream()
                            .map(LinkStatsTodayDO::getId)
                            .toList()
                    );
                    //将list中的每条记录的gid改成要修改的gid 然后批量插入这些数据
                    linkStatsTodayDOList.forEach(each -> each.setGid(requestParam.getGid()));
                    linkStatsTodayService.saveBatch(linkStatsTodayDOList);
                }
                //对于路由表同样处理，首先删除之前的路由表记录，然后将查到对象改gid，然后再新增
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUri, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
                //不用删除 直接修改gid就好
                //网络来源
                LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessStatsDO::getDelFlag, 0);
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
                //地址
                LambdaUpdateWrapper<LinkLocalStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocalStatsDO.class)
                        .eq(LinkLocalStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkLocalStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkLocalStatsDO::getDelFlag, 0);
                LinkLocalStatsDO linkLocaleStatsDO = LinkLocalStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkLocaleStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
                //访问系统
                LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkOsStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkOsStatsDO::getDelFlag, 0);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);
                //访问浏览器
                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
                //设备
                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
                //网络
                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
                //访问记录
                LambdaUpdateWrapper<LinkAccessLogsDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
                        .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessLogsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessLogsDO::getDelFlag, 0);
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                rLock.unlock();
            }
        }
        if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())) {
            //如果原始记录和要修改的记录的日期类型或有效期不同 执行以下逻辑
            //删除缓存中的短链接
            redisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            //如果原始记录的有效日期不为null,且原始记录已过期
            if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(new Date())) {
                if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(new Date())) {
                    redisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }
    }

    @Override
    public void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }
    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        Runnable addResponseCookieTask = () -> {
            uv.set(UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
            uvFirstFlag.set(Boolean.TRUE);
            redisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, uv.get());
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long uvAdded = redisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, each);
                        uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                    }, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getActualIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        String network = LinkUtil.getNetwork(((HttpServletRequest) request));
        Long uipAdded = redisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .build();
    }
    //rocketmq 失败的方法
    private void sendMessage(){
        String keys = UUID.randomUUID().toString();
        GeneralMessageEvent generalMessageEvent = GeneralMessageEvent.builder()
                .body("DDDDDDD")
                .keys(keys)
                .build();
        SendResult sendResult = generalMessageDemoProduce.sendMessage(
                "DONG",
                "dong",
                keys,
                generalMessageEvent
        );
    }
    //test
    private void send1(String msg){
        rocketmqProducer.sendMessage(msg);
    }
}
