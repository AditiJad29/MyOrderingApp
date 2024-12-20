package com.example.payment.service;

import com.example.payment.payload.PaymentRequest;
import com.example.payment.payload.PaymentResponse;

public interface PaymentService {
    long doPayment(PaymentRequest paymentRequest);

    PaymentResponse getPaymentDetailsByOrderId(long orderId);
}