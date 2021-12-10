package com.jsrdxzw.flashsale.app.service.placeorder.queued;


import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;
import com.jsrdxzw.flashsale.app.model.enums.OrderTaskStatus;
import com.jsrdxzw.flashsale.app.model.result.OrderTaskSubmitResult;

public interface PlaceOrderTaskService {

    OrderTaskSubmitResult submit(PlaceOrderTask placeOrderTask);

    void updateTaskHandleResult(String placeOrderTaskId, boolean result);

    OrderTaskStatus getTaskStatus(String placeOrderTaskId);
}
