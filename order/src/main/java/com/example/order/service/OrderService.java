package com.example.order.service;

import com.example.order.payload.OrderRequest;
import com.example.order.payload.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}