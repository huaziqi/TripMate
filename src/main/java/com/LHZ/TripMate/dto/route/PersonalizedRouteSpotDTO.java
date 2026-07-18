package com.LHZ.TripMate.dto.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 个性化路线中的单个景点：在基础信息之上附带针对该游客的讲解重点 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRouteSpotDTO {

    private String displayName;
    private Long spotId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private boolean matched;

    /** 按游客兴趣挑选的讲解重点文本 */
    private String focusText;

    /** 讲解重点对应的兴趣维度展示名，如 历史文化；无个性化时为 null */
    private String focusLabel;
}
