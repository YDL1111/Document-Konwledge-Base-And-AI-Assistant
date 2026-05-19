package com.docbase.domain.knowledge.document.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("knowledge_document_version")
@ApiModel(value = "KnowledgeDocumentVersionEntity", description = "知识库文档版本表")
public class KnowledgeDocumentVersionEntity extends Model<KnowledgeDocumentVersionEntity> {

    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    @TableField("document_id")
    private Long documentId;

    @TableField("version_no")
    private String versionNo;

    @TableField("file_name")
    private String fileName;

    @TableField("file_ext")
    private String fileExt;

    @TableField("file_size")
    private Long fileSize;

    @TableField("storage_type")
    private String storageType;

    @TableField("storage_path")
    private String storagePath;

    @TableField("storage_url")
    private String storageUrl;

    @TableField("content_hash")
    private String contentHash;

    @TableField("version_remark")
    private String versionRemark;

    @TableField("parse_status")
    private Integer parseStatus;

    @TableField("is_current")
    private Boolean isCurrent;

    @TableField(value = "creator_id", fill = FieldFill.INSERT)
    private Long creatorId;

    @TableField("create_time")
    private Date createTime;

    @TableField("deleted")
    @TableLogic
    private Boolean deleted;

    @Override
    public Serializable pkVal() {
        return this.versionId;
    }
}
