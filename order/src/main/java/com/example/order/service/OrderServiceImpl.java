package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.exception.OrderServiceCustomException;
import com.example.order.gateway.PaymentService;
import com.example.order.gateway.ProductService;
import com.example.order.payload.OrderRequest;
import com.example.order.payload.OrderResponse;
import com.example.order.payload.PaymentRequest;
import com.example.order.payload.PaymentResponse;
import com.example.order.repository.OrderRepository;
import com.example.products.entity.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final RestTemplate restTemplate;

    private final ProductService productService;

    private final PaymentService paymentService;

    @Override
    public long placeOrder(OrderRequest orderRequest) {

        log.info("OrderServiceImpl | placeOrder is called");

        //Order Entity -> Save the data with Status Order Created
        //Product Service - Block Products (Reduce the Quantity)
        //Payment Service -> Payments -> Success-> COMPLETE, Else
        //CANCELLED

        log.info("OrderServiceImpl | placeOrder | Placing Order Request orderRequest : " + orderRequest.toString());

        log.info("OrderServiceImpl | placeOrder | Calling productService through FeignClient");
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("OrderServiceImpl | placeOrder | Creating Order with Status CREATED");
        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();

        order = orderRepository.save(order);

        log.info("OrderServiceImpl | placeOrder | Calling Payment Service to complete the payment");

        PaymentRequest paymentRequest
                = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .referenceNumber(""+orderRequest.getProductId())
                .build();

        String orderStatus = null;

        try {
            paymentService.doPayment(paymentRequest);
            log.info("OrderServiceImpl | placeOrder | Payment done Successfully. Changing the Order status to PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.error("OrderServiceImpl | placeOrder | Error occurred in payment. Changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }

        order.setOrderStatus(orderStatus);
        orderRepository.save(order);
        log.info("OrderServiceImpl | placeOrder | Order Placed successfully with Order Id: {}", order.getId());
        return order.getId();
    }

    @Override
    @CircuitBreaker(name="ORDER-SERVICE", fallbackMethod = "getNoPaymentResponse")
    public OrderResponse getOrderDetails(long orderId) {

        log.info("OrderServiceImpl | getOrderDetails | Get order details for Order Id : {}", orderId);

        Order order
                = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceCustomException("Order not found for the order Id:" + orderId,"NOT_FOUND"));

        log.info("OrderServiceImpl | getOrderDetails | Invoking Product service to fetch the product for id: {}", order.getProductId());
        ResponseEntity<Product> productResponse = productService.getProductById(order.getProductId());
        //ProductResponse productResponse = restTemplate.getForObject("http://localhost:8081/product/" + order.getProductId(), ProductResponse.class);

        log.info("OrderServiceImpl | getOrderDetails | Getting payment information form the payment Service");
        ResponseEntity<PaymentResponse> paymentResponse = paymentService.getPaymentByOrderId(order.getId());
             //   = restTemplate.getForObject("http://localhost:8082/payment/order/" + order.getId(),      PaymentResponse.class      );

        OrderResponse.ProductDetails productDetails
                = OrderResponse.ProductDetails
                .builder()
                .productName(productResponse.getBody().getProductName())
                .productId(productResponse.getBody().getProductId())
                .build();

        OrderResponse.PaymentDetails paymentDetails
                = OrderResponse.PaymentDetails
                .builder()
                .paymentId(paymentResponse.getBody().getPaymentId())
                .paymentStatus(paymentResponse.getBody().getStatus())
                .paymentDate(paymentResponse.getBody().getPaymentDate())
                .paymentMode(paymentResponse.getBody().getPaymentMode())
                .build();

        OrderResponse orderResponse
                = OrderResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .orderDate(order.getOrderDate())
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .build();

        log.info("OrderServiceImpl | getOrderDetails | orderResponse : " + orderResponse.toString());
        return orderResponse;
    }

    public OrderResponse getNoPaymentResponse(long orderId, Throwable ex){
        return new OrderResponse();
    }
}