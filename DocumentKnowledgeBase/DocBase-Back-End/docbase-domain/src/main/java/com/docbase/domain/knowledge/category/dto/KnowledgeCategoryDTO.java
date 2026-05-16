package com.docbase.domain.knowledge.category.dto;

import com.docbase.domain.knowledge.category.db.KnowledgeCategoryEntity;
import java.util.Date;
import lombok.Data;

@Data
public class KnowledgeCategoryDTO {

    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private Long deptId;
    private Integer sortNum;
    private Integer status;
    private String remark;
    private Date createTime;

    public KnowledgeCategoryDTO(KnowledgeCategoryEntity entity) {
        if (entity != null) {
            this.categoryId = entity.getCategoryId();
            this.parentId = entity.getParentId();
            this.categoryName = entity.getCategoryName();
            this.deptId = entity.getDeptId();
            this.sortNum = entity.getSortNum();
            this.status = entity.getStatus();
            this.remark = entity.getRemark();
            this.createTime = entity.getCreateTime();
        }
    }
}
