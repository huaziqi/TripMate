package com.LHZ.TripMate.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 新增 / 编辑景点知识条目的请求体，字段与结构化数据集表格列一一对应 */
@Data
public class SaveKnowledgeSpotEntryDTO {

    @NotBlank(message = "所属景区 spotKey 不能为空")
    private String spotKey;

    @NotBlank(message = "景点ID不能为空")
    private String spotCode;

    private String zoneName;

    @NotBlank(message = "景点名称不能为空")
    private String name;

    private String location;
    private String scaleInfo;
    private String coreFunction;
    private String culture;
    private String description;
    private String tourTips;
    private String ticketInfo;
    private String remark;

    private boolean enabled = true;
}
