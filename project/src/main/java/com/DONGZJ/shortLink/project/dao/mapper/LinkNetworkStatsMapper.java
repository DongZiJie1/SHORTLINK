
package com.DONGZJ.shortLink.project.dao.mapper;

import com.DONGZJ.shortLink.project.dao.entity.LinkNetworkStatsDO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.DONGZJ.shortLink.project.dto.req.ShortLinkStatsReqDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 访问网络监控持久层
 */
public interface LinkNetworkStatsMapper extends BaseMapper<LinkNetworkStatsDO> {

    /**
     * 记录访问设备监控数据
     */
    @Insert("INSERT INTO t_link_network_stats (full_short_url, gid, date, cnt, network, del_flag) " +
            "VALUES( #{linkNetworkStats.fullShortUrl}, #{linkNetworkStats.gid}, #{linkNetworkStats.date}, #{linkNetworkStats.cnt}, #{linkNetworkStats.network}, 0) " +
            "ON DUPLICATE KEY UPDATE cnt = cnt +  #{linkNetworkStats.cnt};")
    void shortLinkNetworkState(@Param("linkNetworkStats") LinkNetworkStatsDO linkNetworkStatsDO);

    /**
     * 根据短链接获取指定日期内访问网络监控数据
     */
    @Select("SELECT " +
            "    network, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_network_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, network;")
    List<LinkNetworkStatsDO> listNetworkStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 根据分组获取指定日期内访问网络监控数据
     */
    @Select("SELECT " +
            "    network, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_network_stats " +
            "WHERE " +
            "    gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, network;")
    List<LinkNetworkStatsDO> listNetworkStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);
}
