package com.docbase.infrastructure.client.python.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PythonChatRequest {

    @JsonProperty("kb_id")
    private Integer kbId;

    @JsonProperty("conv_id")
    private Integer convId;

    private String question;

    private Boolean stream;
}
