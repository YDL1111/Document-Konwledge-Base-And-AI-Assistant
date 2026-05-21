package com.docbase.domain.ai.chat.db;

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
@TableName("ai_chat_message")
public class AiChatMessageEntity implements Serializable {

    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("message_role")
    private Integer messageRole;

    @TableField("message_content")
    private String messageContent;

    @TableField("sources_json")
    private String sourcesJson;

    @TableField("python_conv_id")
    private Integer pythonConvId;

    @TableField("kb_id")
    private Integer kbId;

    @TableField("model_name")
    private String modelName;

    @TableField("error_flag")
    private Integer errorFlag;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private Date createTime;

    @TableField("creator")
    private String creator;

    @TableField("del_flag")
    private Integer delFlag;
}
