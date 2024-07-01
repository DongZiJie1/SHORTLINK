package com.DONGZJ.shortLink.project.comment.convention;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
@Accessors(chain = true)
@Data
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 8789798798787886786L;
    public static final String SUCCESS_CODE = "0";
    private String code;
    private String message;
    private T data;
    private String requestId;
    public boolean isSuccess(){
        return SUCCESS_CODE.equals(code);
    }
}

