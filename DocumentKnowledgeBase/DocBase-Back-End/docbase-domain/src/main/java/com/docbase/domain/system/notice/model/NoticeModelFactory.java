package com.docbase.domain.system.notice.model;

import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.domain.system.notice.db.SysNoticeEntity;
import com.docbase.domain.system.notice.db.SysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 公告模型工厂
 * @author valarchie
 */
@Component
@RequiredArgsConstructor
public class NoticeModelFactory {

    private final SysNoticeService noticeService;

    public NoticeModel loadById(Long noticeId) {
        SysNoticeEntity byId = noticeService.getById(noticeId);

        if (byId == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, noticeId, "通知公告");
        }

        return new NoticeModel(byId);
    }

    public NoticeModel create() {
        return new NoticeModel();
    }


}
