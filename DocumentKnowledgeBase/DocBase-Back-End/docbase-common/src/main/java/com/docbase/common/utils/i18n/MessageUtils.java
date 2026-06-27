package com.docbase.common.utils.i18n;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 获取i18n资源文件
 *
 * @author valarchie
 */
@Component
public class MessageUtils implements ApplicationContextAware {

    private static MessageSource messageSource;

    private MessageUtils() {
    }

    /**
     * 根据消息键和参数 获取消息 委托给spring messageSource
     *
     * @param code 消息键
     * @param args 参数
     * @return 获取国际化翻译值
     */
    public static String message(String code, Object... args) {
        if (messageSource == null) {
            return code;
        }
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        messageSource = applicationContext.getBean(MessageSource.class);
    }
}
