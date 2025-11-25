package ca.sheridan.byteme.bootstrap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
            System.out.println("Inserted demo user: " + adminEmail + " / Password123!");
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
            System.out.println("Inserted test user: " + staffEmail + " / Password123!");
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
            System.out.println("Inserted test user: " + customerEmail + " / Password123!");
        } else {
            customer = userRepository.findByEmail(customerEmail).get();
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
            System.out.println("Seeded demo promotions.");
        }

        // --- seed a couple of orders (only if none exist for this customer) ---
        if (orderRepository.findByUserId(customer.getId()).isEmpty()) {
            CartItem item1 = new CartItem("prod_12345", "Large Cookiegram", "Happy Birthday!", "Royal Blue", "#4169E1", 29.99);
            CartItem item2 = new CartItem("prod_67890", "Large Cookiegram", "Get Well Soon!", "Green", "#008000", 24.99);
            CartItem item3 = new CartItem("prod_ABCDE", "Small Cookiegram", "Congratulations!", "Red", "#FF0000", 19.99);

            Order order1 = Order.builder()
                .id("ORD001")
                .userId(customer.getId())
                .items(List.of(item1))
                .subtotal(29.99)
                .tax(3.90)
                .shippingCost(5.00)
                .total(38.89)
                .orderDate(LocalDateTime.now().minusDays(10))
                .deliveryDate(LocalDate.now().minusDays(7))
                .status(Status.Delivered)
                .shippingAddress(ShippingAddress.builder().name("Customer User").addressLine1("123 Main St").city("Toronto").province("ON").postalCode("M1M1M1").country("Canada").build())
                .billingAddress(BillingAddress.builder().name("Customer User").addressLine1("123 Main St").city("Toronto").province("ON").postalCode("M1M1M1").country("Canada").build())
                .build();
            
            Order order2 = Order.builder()
                .id("ORD002")
                .userId(customer.getId())
                .items(List.of(item2))
                .subtotal(24.99)
                .tax(3.25)
                .shippingCost(5.00)
                .total(33.24)
                .orderDate(LocalDateTime.now().minusDays(2))
                .deliveryDate(LocalDate.now().plusDays(1))
                .status(Status.Shipped)
                .shippingAddress(ShippingAddress.builder().name("Customer User").addressLine1("456 Oak St").city("Mississauga").province("ON").postalCode("L5B2C9").country("Canada").build())
                .billingAddress(BillingAddress.builder().name("Customer User").addressLine1("456 Oak St").city("Mississauga").province("ON").postalCode("L5B2C9").country("Canada").build())
                .carrier("FedEx")
                .trackingNumber("123456789")
                .build();

            Order order3 = Order.builder()
                .id("ORD003")
                .userId(customer.getId())
                .items(List.of(item3))
                .subtotal(19.99)
                .tax(2.60)
                .shippingCost(5.00)
                .total(27.59)
                .orderDate(LocalDateTime.now().minusDays(1))
                .deliveryDate(LocalDate.now().plusDays(2))
                .status(Status.Baking)
                .shippingAddress(ShippingAddress.builder().name("Customer User").addressLine1("789 Pine St").city("Brampton").province("ON").postalCode("L6T0G8").country("Canada").build())
                .billingAddress(BillingAddress.builder().name("Customer User").addressLine1("789 Pine St").city("Brampton").province("ON").postalCode("L6T0G8").country("Canada").build())
                .build();

            // Order for another user to test security
            User anotherCustomer = User.builder().name("Another Customer").email("another@cookie.com").password(passwordEncoder.encode("Password123!")).role(Role.CUSTOMER).build();
            userRepository.save(anotherCustomer);
            Order anotherOrder = Order.builder()
                .id("ORD004")
                .userId(anotherCustomer.getId())
                .items(List.of(new CartItem("prod_XYZ", "Large Cookiegram", "Secret Message", "Black", "#000000", 29.99)))
                .subtotal(29.99)
                .tax(3.90)
                .shippingCost(5.00)
                .total(38.89)
                .orderDate(LocalDateTime.now().minusDays(5))
                .deliveryDate(LocalDate.now().minusDays(2))
                .status(Status.Delivered)
                .shippingAddress(ShippingAddress.builder().name("Another Customer").addressLine1("1 Hacker Way").city("Menlo Park").province("CA").postalCode("94025").country("USA").build())
                .billingAddress(BillingAddress.builder().name("Another Customer").addressLine1("1 Hacker Way").city("Menlo Park").province("CA").postalCode("94025").country("USA").build())
                .build();

            orderRepository.saveAll(List.of(order1, order2, order3, anotherOrder));
            System.out.println("Seeded demo orders for customer.");
        }
    }
}
