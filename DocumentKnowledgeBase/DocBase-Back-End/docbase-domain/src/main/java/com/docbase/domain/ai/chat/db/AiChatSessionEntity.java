package com.docbase.domain.ai.chat.db;

import com.docbase.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_chat_session")
public class AiChatSessionEntity extends BaseEntity<AiChatSessionEntity> {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    @TableField("session_title")
    private String sessionTitle;

    @TableField("user_id")
    private Long userId;

    @TableField("dept_id")
    private Long deptId;

    @TableField("last_message_time")
    private Date lastMessageTime;

    @TableField("python_conv_id")
    private Integer pythonConvId;

    @TableField("`status`")
    private Integer status;

    @Override
    public Serializable pkVal() {
        return this.sessionId;
    }
}
