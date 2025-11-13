package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/orders")
public class ShippingController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/{orderId}/ship")
    @ResponseBody
    public String markAsShipped(@PathVariable String orderId,
                                @RequestParam String carrier,
                                @RequestParam String trackingNumber) {

        Order order = orderService.getOrderById(orderId);
        if (order == null) return "Order not found";

        order.setCarrier(carrier);
        order.setTrackingNumber(trackingNumber);
        order.setStatus(Status.Shipped);

        orderService.updateOrder(order);

        return "Order " + orderId + " marked as shipped with " + carrier + " tracking " + trackingNumber;
    }
}
