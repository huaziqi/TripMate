package com.LHZ.TripMate.dto.route;

import lombok.Data;

import java.util.List;

/**
 * 个性化路线推荐请求：来自小程序端的游客问卷画像。
 * 行为信号（收藏 / 浏览历史 / 数字人对话）由后端根据登录用户自动补充。
 */
@Data
public class PersonalizeRequestDTO {

    /** 兴趣标签 key 列表，如 history / nature / family，见 InterestOptionDTO */
    private List<String> interests;

    /** 计划游玩时长：half(≤3小时) / most(4-5小时) / full(6小时以上) */
    private String duration;

    /** 同行人群：solo(独自) / partner(情侣朋友) / kids(带小孩) / elder(带老人) */
    private String companions;

    /** 体力水平：low / medium / high（决定是否推荐登216级台阶等路线） */
    private String stamina;

    /** 游客自由描述，如“我喜欢拍照，对佛教历史也感兴趣”，后端做关键词画像 */
    private String freeText;
}
