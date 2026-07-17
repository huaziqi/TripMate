package com.LHZ.TripMate.dto.admin;

import com.LHZ.TripMate.entity.KnowledgeDoc;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增 / 编辑知识文档的请求体 */
@Data
public class SaveKnowledgeDocDTO {

    private String spotKey;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "分类不能为空")
    private KnowledgeDoc.Category category;

    @NotBlank(message = "内容不能为空")
    private String content;

    private boolean enabled = true;
}
