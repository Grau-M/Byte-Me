package ca.sheridan.byteme.bootstrap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ca.sheridan.byteme.beans.BillingAddress;
import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Promotion;
import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.ShippingAddress;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.models.CartItem;
import ca.sheridan.byteme.repositories.OrderRepository;
import ca.sheridan.byteme.repositories.UserRepository;
import ca.sheridan.byteme.services.PromotionService;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
@Profile("!prod") // optional: only run outside prod
public class BootStrapData implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PromotionService promotionService;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) throws Exception {
        // --- existing demo users (unchanged) ---
        String adminEmail = "admin@cookie.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .name("Admin User")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
        }

        String staffEmail = "staff@cookie.com";
        if (userRepository.findByEmail(staffEmail).isEmpty()) {
            User staff = User.builder()
                    .name("Staff User")
                    .email(staffEmail)
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.STAFF)
                    .build();
            userRepository.save(staff);
        }

        String customerEmail = "customer@cookie.com";
        User customer;
        if (userRepository.findByEmail(customerEmail).isEmpty()) {
            customer = User.builder()
                    .name("Customer User")
                    .email(customerEmail)
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(customer);
        } else {
            customer = userRepository.findByEmail(customerEmail).get();
        }

        String anotherEmail = "another@cookie.com";
        User anotherCustomer;
        if (userRepository.findByEmail(anotherEmail).isEmpty()) {
            anotherCustomer = User.builder()
                    .name("Another Customer")
                    .email(anotherEmail)
                    .password(passwordEncoder.encode("Password123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(anotherCustomer);
        } else {
            anotherCustomer = userRepository.findByEmail(anotherEmail).get();
        }

        // --- seed a couple of promotions (only if none exist) ---
        if (promotionService.count() == 0) {
            LocalDate today = LocalDate.now();
            promotionService.saveAll(List.of(
                Promotion.builder()
                    .title("20% OFF Giant Cookie")
                    .blurb("Use code COOKIE20 at checkout.")
                    .badge("LIMITED")
                    .promoCode("COOKIE20")
                    .startsAt(today.minusDays(1))
                    .endsAt(today.plusDays(7))
                    .imageUrl("/images/hero-cookie.jpg")
                    .active(true)
                    .build(),
                Promotion.builder()
                    .title("Free Hand-Piped Message")
                    .blurb("Personalize your cookie—on us!")
                    .badge("NEW")
                    .startsAt(today.minusDays(2))
                    .endsAt(today.plusDays(3))
                    .active(true)
                    .build()
            ));
        }

        // --- SEED ORDERS FOR DEMO GRAPH ---
        // We clear existing orders to ensure the graph looks exactly how we want it
        if (orderRepository.count() > 0) {
            orderRepository.deleteAll();
            System.out.println("Cleared existing orders to ensure clean demo graph.");
        }

        List<Order> demoOrders = new ArrayList<>();
        int orderCounter = 1000;

        // --- NOV 29 (Day 1): Baseline - 2 Orders ---
        demoOrders.add(createDemoOrder(++orderCounter, customer, 29, 11, 1, Status.Delivered));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 29, 14, 2, Status.Delivered));

        // --- NOV 30 (Day 2): Small Rise - 3 Orders ---
        demoOrders.add(createDemoOrder(++orderCounter, customer, 30, 9, 1, Status.Delivered));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 30, 10, 1, Status.Delivered));
        demoOrders.add(createDemoOrder(++orderCounter, customer, 30, 16, 1, Status.Delivered));

        // --- DEC 01 (Day 3): The Dip (Monday lull) - 1 Order ---
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 1, 12, 1, Status.Delivered));

        // --- DEC 02 (Day 4): Recovery - 3 Orders ---
        demoOrders.add(createDemoOrder(++orderCounter, customer, 2, 10, 1, Status.Shipped));
        demoOrders.add(createDemoOrder(++orderCounter, customer, 2, 13, 1, Status.Shipped));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 2, 15, 1, Status.Shipped));

        // --- DEC 03 (Day 5): Growth - 4 Orders ---
        demoOrders.add(createDemoOrder(++orderCounter, customer, 3, 9, 1, Status.Shipped));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 3, 11, 2, Status.Shipped)); // High value
        demoOrders.add(createDemoOrder(++orderCounter, customer, 3, 14, 1, Status.Shipped));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 3, 16, 1, Status.Baking));

        // --- DEC 04 (Day 6 - Yesterday): Moderate - 3 Orders (~$157 Revenue) ---
        // We keep this moderate so Today looks like a huge spike (150%+)
        demoOrders.add(createDemoOrder(++orderCounter, customer, 4, 10, 1, Status.Baking));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 4, 12, 1, Status.Baking));
        demoOrders.add(createDemoOrder(++orderCounter, customer, 4, 15, 1, Status.Pending));

        // --- DEC 05 (Day 7 - Today): THE SPIKE - 8 Orders (High Revenue) ---
        // Mix of single and double orders to drive revenue up massively
        demoOrders.add(createDemoOrder(++orderCounter, customer, 5, 8, 1, Status.Baking));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 5, 9, 2, Status.Baking)); // Big order
        demoOrders.add(createDemoOrder(++orderCounter, customer, 5, 10, 1, Status.Baking));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 5, 11, 1, Status.Pending));
        demoOrders.add(createDemoOrder(++orderCounter, customer, 5, 12, 2, Status.Confirmed)); // Big order
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 5, 13, 1, Status.Shipped));
        demoOrders.add(createDemoOrder(++orderCounter, customer, 5, 14, 1, Status.Ready_for_Shipment));
        demoOrders.add(createDemoOrder(++orderCounter, anotherCustomer, 5, 15, 1, Status.Delivered));

        orderRepository.saveAll(demoOrders);
        System.out.println("Seeded " + demoOrders.size() + " orders for the Demo Graph (Nov 29 - Dec 05).");
    }

    /**
     * Helper to create a realistic order with strict pricing rules.
     * Price: $34.99 per cookie.
     * Tax: 13% of subtotal.
     * Shipping: $12.99 flat.
     */
    private Order createDemoOrder(int idSuffix, User user, int dayOfMonth, int hour, int quantity, Status status) {
        double pricePerCookie = 34.99;
        double shippingRate = 12.99;
        double taxRate = 0.13;

        // 1. Calculate Subtotal
        double subtotal = pricePerCookie * quantity;

        // 2. Calculate Tax
        double tax = subtotal * taxRate;

        // 3. Calculate Total
        double total = subtotal + tax + shippingRate;

        // Rounding to 2 decimal places to ensure clean data
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        tax = Math.round(tax * 100.0) / 100.0;
        total = Math.round(total * 100.0) / 100.0;

        // Create Cart Items based on quantity
        List<CartItem> items = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            items.add(new CartItem("prod_" + idSuffix + "_" + i,
                    "Large Cookiegram",
                    i == 0 ? "Happy Birthday!" : "Congrats!", // Vary message slightly
                    "Classic White",
                    "#FFFFFF",
                    pricePerCookie));
        }

        // Handle Date Logic (Nov vs Dec)
        int month = (dayOfMonth >= 29) ? 11 : 12; // 11=Nov, 12=Dec
        int year = 2025;
        LocalDateTime orderDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, 0);

        return Order.builder()
                .id("ORD" + idSuffix)
                .userId(user.getId())
                .items(items)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(shippingRate)
                .total(total)
                .orderDate(orderDateTime)
                .deliveryDate(orderDateTime.toLocalDate().plusDays(3))
                .status(status)
                .shippingAddress(ShippingAddress.builder()
                        .name(user.getName())
                        .addressLine1("123 Demo Lane")
                        .city("Toronto")
                        .province("ON")
                        .postalCode("M5V 2H1")
                        .country("Canada")
                        .build())
                .billingAddress(BillingAddress.builder()
                        .name(user.getName())
                        .addressLine1("123 Demo Lane")
                        .city("Toronto")
                        .province("ON")
                        .postalCode("M5V 2H1")
                        .country("Canada")
                        .build())
                .build();
    }
}