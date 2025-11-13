package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.ProfanityFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class OrderController {

    private final CartService cartService;
    private final ProfanityFilterService profanityFilterService;
    private final OrderService orderService;

    @Autowired
    public OrderController(CartService cartService, ProfanityFilterService profanityFilterService, OrderService orderService) {
        this.cartService = cartService;
        this.profanityFilterService = profanityFilterService;
        this.orderService = orderService;
    }


    @GetMapping("/order")
    public String showOrderPage(Model model, @RequestParam(required = false) String itemId, @RequestParam(required = false) String orderId) {
        model.addAttribute("cartCount", cartService.getCartCount());

        if (itemId != null) {
            if (orderId != null) {
                Order order = orderService.getOrderById(orderId);
                if (order != null) {
                    model.addAttribute("orderId", orderId);
                    order.getItems().stream()
                        .filter(item -> item.id().equals(itemId))
                        .findFirst()
                        .ifPresent(item -> {
                            model.addAttribute("editingItem", item);
                            model.addAttribute("lastMessage", item.message());
                            model.addAttribute("lastColor", item.icingColorName().toLowerCase().replace(" ", ""));
                        });
                }
            } else {
                cartService.getItemById(itemId).ifPresent(item -> {
                    model.addAttribute("editingItem", item);
                    model.addAttribute("lastMessage", item.message());
                    model.addAttribute("lastColor", item.icingColorName().toLowerCase().replace(" ", ""));
                });
            }
        } else {
            if (!model.containsAttribute("lastMessage")) {
                model.addAttribute("lastMessage", "");
            }
            if (!model.containsAttribute("lastColor")) {
                model.addAttribute("lastColor", "");
            }
        }

        return "order";
    }


    @PostMapping("/add-to-cart")
    public String addToCart(@RequestParam String productId,
                            @RequestParam String productName,
                            @RequestParam double price,
                            @RequestParam String cookieMessage,
                            @RequestParam("icingColor") String icingColorValue,
                            @RequestParam(name = "buyNow", required = false, defaultValue = "false") boolean buyNow,
                            @RequestParam(name = "itemId", required = false) String itemId,
                            @RequestParam(name = "orderId", required = false) String orderId,
                            RedirectAttributes redirectAttributes) {

        if (profanityFilterService.hasProfanity(cookieMessage)) {
            redirectAttributes.addFlashAttribute("cartError", "Your message contains inappropriate language and was not added to the cart.");
            redirectAttributes.addFlashAttribute("lastMessage", cookieMessage);
            redirectAttributes.addFlashAttribute("lastColor", icingColorValue);
            return "redirect:/order";
        }

        Map<String, String> colorDetails = getIcingColorDetails(icingColorValue);

        String id = (itemId != null && !itemId.isEmpty()) ? itemId : productId + "_" + cookieMessage.hashCode();

        CartItem item = new CartItem(
                id,
                productName,
                cookieMessage,
                colorDetails.get("name"),
                colorDetails.get("hex"),
                price
        );

        if (orderId != null && !orderId.isEmpty()) {
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                if (itemId != null && !itemId.isEmpty()) { // This is an update to an existing item in the order
                    order.getItems().removeIf(existing -> existing.id().equals(itemId));
                    order.getItems().add(item);
                    orderService.updateOrder(order);
                    redirectAttributes.addFlashAttribute("cartMessage", "Item updated successfully!");
                    return "redirect:/order/edit/" + orderId;
                } else { // This is a brand new item being added to an existing order
                    order.getItems().add(item);
                    orderService.updateOrder(order);
                    redirectAttributes.addFlashAttribute("cartMessage", "New item added to order successfully!");
                    return "redirect:/order/edit/" + orderId;
                }
            }
        }

        // Existing logic for session-based cart (when not editing an order)
        cartService.addItem(item);
        redirectAttributes.addFlashAttribute("cartSuccess", "Your cookie has been saved!");

        if (buyNow) {
            return "redirect:/checkout";
        } else if (itemId != null && !itemId.isEmpty()) {
            return "redirect:/cart";
        }
        else {
            return "redirect:/order";
        }
    }


    @GetMapping("/order/edit/{id}")
    public String editOrder(@PathVariable("id") String orderId, Model model, RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/dashboard";
        }

        List<CartItem> orderItems = order.getItems();

        order.setSubtotal(orderService.calculateSubtotal(order));
        order.setTax(orderService.calculateTax(order));
        order.setTotal(orderService.calculateTotal(order));

        model.addAttribute("order", order);

        model.addAttribute("cartCount", orderItems.size());

        return "edit-order";
    }



    @PostMapping("/order/save")
    public String saveOrder(@RequestParam String orderId, RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/dashboard";
        }

        order.setSubtotal(orderService.calculateSubtotal(order));
        order.setTax(orderService.calculateTax(order));
        order.setTotal(orderService.calculateTotal(order));

        orderService.updateOrder(order);

        redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " has been saved successfully!");
        return "redirect:/dashboard";
    }

    @PostMapping("/order/edit/{id}/remove")
    public String removeOrderItem(@PathVariable("id") String orderId,
                                  @RequestParam String itemId,
                                  RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderById(orderId);

        if (order != null) {
            orderService.removeItemFromOrder(order, itemId);
            redirectAttributes.addFlashAttribute("cartMessage", "Item removed from order.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Order not found");
        }

        return "redirect:/order/edit/" + orderId;
    }


    @PostMapping("/order/cancel/{id}")
    public String cancelOrder(@PathVariable("id") String orderId, RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/dashboard";
        }

        boolean canceled = orderService.cancelOrder(order);

        if (!canceled) {
            redirectAttributes.addFlashAttribute("error", "Only pending orders can be canceled");
        } else {
            redirectAttributes.addFlashAttribute("success", "Order canceled successfully");
        }

        return "redirect:/dashboard";
    }


    private Map<String, String> getIcingColorDetails(String colorValue) {
        Map<String, String> details = new HashMap<>();
        switch (colorValue) {
            case "white":
                details.put("name", "Classic White");
                details.put("hex", "#FFFFFF");
                break;
            case "pink":
                details.put("name", "Pretty Pink");
                details.put("hex", "hotpink");
                break;
            case "red":
                details.put("name", "Vibrant Red");
                details.put("hex", "red");
                break;
            case "blue":
                details.put("name", "Cool Blue");
                details.put("hex", "blue");
                break;
            case "green":
                details.put("name", "Festive Green");
                details.put("hex", "green");
                break;
            case "chocolate":
                details.put("name", "Chocolate");
                details.put("hex", "#8B5A2B");
                break;
            default:
                details.put("name", "Unknown");
                details.put("hex", "#eee");
                break;
        }
        return details;
    }

}
