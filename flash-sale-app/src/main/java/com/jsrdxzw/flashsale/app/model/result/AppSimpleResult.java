package com.jsrdxzw.flashsale.app.model.result;

import com.jsrdxzw.flashsale.app.exception.AppErrorCode;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.TRY_LATER;

/**
 * @author xuzhiwei
 * @date 2021/12/1 6:55 下午
 */
@Data
@Accessors(chain = true)
public class AppSimpleResult <T> {
    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> AppSimpleResult<T> ok(T data) {
        AppSimpleResult<T> appSimpleResult = new AppSimpleResult<>();
        appSimpleResult.setSuccess(true);
        appSimpleResult.setData(data);
        return appSimpleResult;
    }

    /**
     * 数据更新中，稍后再试
     * 客户端对此类错误不做页面刷新等其他操作
     */
    public static <T> AppSimpleResult<T> tryLater() {
        AppSimpleResult<T> appSimpleResult = new AppSimpleResult<>();
        appSimpleResult.setSuccess(false);
        appSimpleResult.setCode(TRY_LATER.getErrCode());
        appSimpleResult.setMessage(TRY_LATER.getErrDesc());
        return appSimpleResult;
    }

    public static <T> AppSimpleResult<T> failed(String errCode, String errDesc) {
        AppSimpleResult<T> appSimpleResult = new AppSimpleResult<>();
        appSimpleResult.setSuccess(false);
        appSimpleResult.setCode(errCode);
        appSimpleResult.setMessage(errDesc);
        return appSimpleResult;
    }

    public static <T> AppSimpleResult<T> failed(String errCode, String errDesc, T data) {
        AppSimpleResult<T> appSimpleResult = new AppSimpleResult<>();
        appSimpleResult.setSuccess(false);
        appSimpleResult.setData(data);
        appSimpleResult.setCode(errCode);
        appSimpleResult.setMessage(errDesc);
        return appSimpleResult;
    }

    public static <T> AppSimpleResult<T> failed(AppErrorCode appErrorCode) {
        AppSimpleResult<T> appSimpleResult = new AppSimpleResult<>();
        appSimpleResult.setSuccess(false);
        appSimpleResult.setCode(appErrorCode.getErrCode());
        appSimpleResult.setMessage(appErrorCode.getErrDesc());
        return appSimpleResult;
    }

    @SuppressWarnings("rawtypes")
    public static AppSimpleResult ok() {
        return ok(true);
    }
}
