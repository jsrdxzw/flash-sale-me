package com.jsrdxzw.flashsale.app.mq;


import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;

public interface OrderTaskPostService {
    boolean post(PlaceOrderTask placeOrderTask);
}
