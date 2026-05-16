package com.docbase.admin.customize.service.login.dto;

import com.docbase.common.enums.dictionary.DictionaryData;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author valarchie
 */
@Data
public class ConfigDTO {

    private Boolean isCaptchaOn;

    private Boolean isRegisterEnabled;

    private Map<String, List<DictionaryData>> dictionary;

}
