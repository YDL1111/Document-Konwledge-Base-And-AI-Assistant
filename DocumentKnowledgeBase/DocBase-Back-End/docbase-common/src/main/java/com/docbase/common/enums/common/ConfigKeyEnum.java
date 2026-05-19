package com.docbase.common.enums.common;

import com.docbase.common.enums.BasicEnum;

/**
 * 系统配置
 * @author valarchie
 * 对应 sys_config表的config_key字段
 */
public enum ConfigKeyEnum implements BasicEnum<String> {

    /**
     * 菜单类型
     */
    SKIN_THEME("sys.index.skinName", "系统皮肤主题"),
    INIT_PASSWORD("sys.user.initPassword", "初始密码"),
    SIDE_BAR_THEME("sys.index.sideTheme", "侧边栏开关"),
    CAPTCHA("sys.account.captchaOnOff", "验证码开关"),
    REGISTER("sys.account.registerUser", "注册开放功能"),
    KNOWLEDGE_DOC_MAX_SIZE("knowledge.document.maxUploadSizeMb", "文档最大上传大小(MB)"),
    KNOWLEDGE_AI_CHAT_ENABLED("knowledge.ai.chatEnabled", "AI问答功能开关"),
    KNOWLEDGE_AUDIT_REQUIRED("knowledge.document.auditRequired", "文档是否需要审核"),
    KNOWLEDGE_DEFAULT_VISIBILITY("knowledge.document.defaultVisibility", "文档默认可见范围");

    private final String value;
    private final String description;

    ConfigKeyEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String description() {
        return description;
    }


}
