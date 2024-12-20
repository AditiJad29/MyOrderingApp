package com.example.order.exception;

import lombok.Data;

@Data
public class OrderServiceCustomException  extends RuntimeException{

    private String errorCode;

    public OrderServiceCustomException (String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}