package com.docbase.domain.common.dto;

import com.docbase.domain.system.user.dto.UserDTO;
import java.util.Set;
import lombok.Data;

/**
 * @author valarchie
 */
@Data
public class CurrentLoginUserDTO {

    private UserDTO userInfo;
    private String roleKey;
    private Set<String> permissions;


}
