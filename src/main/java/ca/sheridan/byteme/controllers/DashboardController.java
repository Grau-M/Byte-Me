package ca.sheridan.byteme.controllers;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.Role;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.beans.Status;
import ca.sheridan.byteme.repositories.OrderRepository;
import ca.sheridan.byteme.repositories.UserRepository;
import ca.sheridan.byteme.services.CartService;
import ca.sheridan.byteme.services.OrderService;
import ca.sheridan.byteme.services.PromotionService;
import lombok.AllArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Controller
public class DashboardController {

    private final PromotionService promotionService;
    private final OrderService orderService;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String getDashboard(Model model, Principal principal,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String filter) {

        // --- Add Cart Count ---
        model.addAttribute("cartCount", cartService.getCartCount());

        // --- 1. Dynamic Clock (unchanged) ---
        ZoneId userZone = ZoneId.of("America/Toronto");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            if (currentUser.getTimezone() != null && !currentUser.getTimezone().isEmpty()) {
                userZone = ZoneId.of(currentUser.getTimezone());
            }

            // ---- 2. Only for CUSTOMERS: add promotions and orders fetched from MongoDB ----
            if (currentUser.getRole() == Role.CUSTOMER) {
                model.addAttribute("promotions", promotionService.getActivePromotionsForToday());

                List < Order > orders;
                if (search != null && !search.isEmpty()) {
                    orders = orderService.searchOrders(currentUser.getId(), search);
                    if (orders.isEmpty()) {
                        model.addAttribute("message", "No orders found matching your criteria");
                    }
                } else if (filter != null && !filter.isEmpty()) {
                    orders = orderService.filterOrders(currentUser.getId(), filter);
                    if (orders.isEmpty()) {
                        model.addAttribute("message", "No orders found matching your criteria");
                    }
                } else {
                    orders = orderService.getOrdersForUser(currentUser.getId());
                    if (orders.isEmpty()) {
                        model.addAttribute("message", "You haven't placed any orders yet. Start shopping!");
                    }
                }
                model.addAttribute("orders", orders);
            }
        }

        ZonedDateTime zonedTime = ZonedDateTime.now(userZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        String formattedTime = zonedTime.format(formatter);
        model.addAttribute("currentTime", formattedTime);

        String username = (principal != null) ? principal.getName() : "Guest";
        model.addAttribute("username", username);

        return "dashboard";
    }

    @GetMapping("/adminDashboard")
    public String getAdminDashboard(Model model) {

        // --- 1. KPI CARDS DATA ---

        // Calculate Today's Revenue
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        Double revenueToday = orderRepository.sumTotalByDateRange(startOfToday, LocalDateTime.now());
        revenueToday = (revenueToday == null) ? 0.0 : revenueToday;

        // Calculate Yesterday's Revenue
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = today.atStartOfDay();
        Double revenueYesterday = orderRepository.sumTotalByDateRange(startOfYesterday, endOfYesterday);
        revenueYesterday = (revenueYesterday == null) ? 0.0 : revenueYesterday;

        // Calculate Percentage Change
        double percentageChange = 0.0;
        if (revenueYesterday > 0) {
            percentageChange = ((revenueToday - revenueYesterday) / revenueYesterday) * 100;
        } else if (revenueToday > 0) {
            percentageChange = 100.0; // If yesterday was 0 and today is > 0, it's a 100% increase
        }

        model.addAttribute("revenueToday", revenueToday);
        model.addAttribute("revenueChange", percentageChange);

        // Counts
        long pendingCount = orderRepository.countByStatus(Status.Pending);
        long bakingCount = orderRepository.countByStatus(Status.Baking);
        long totalUsers = userRepository.count();

        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("bakingCount", bakingCount);
        model.addAttribute("totalUsers", totalUsers);

        // --- 2. RECENT ORDERS TABLE ---
        // Fetch top 5 most recent orders
        List < Order > recentOrders = orderRepository.findTop5ByOrderByOrderDateDesc();
        model.addAttribute("recentOrders", recentOrders);

        // --- 3. CHART DATA (Simple Implementation) ---

        // Line Chart: Last 7 Days Labels
        List < String > chartDates = new ArrayList < > ();
        List < Double > chartSales = new ArrayList < > ();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            chartDates.add(date.format(DateTimeFormatter.ofPattern("MMM dd")));

            // You need a repository method to get sum for a specific day
            // This is pseudo-code; implementation depends on your Repo
            Double dailySum = orderRepository.sumTotalByDateRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            chartSales.add(dailySum != null ? dailySum : 0.0);
        }

        model.addAttribute("chartDates", chartDates);
        model.addAttribute("chartSales", chartSales);

        // Doughnut Chart: [Pending, Baking, Completed]
        // You can group 'Shipped' and 'Delivered' into 'Completed' for the chart
        long completedCount = orderRepository.countByStatus(Status.Delivered) + orderRepository.countByStatus(Status.Shipped);
        List < Long > statusCounts = Arrays.asList(pendingCount, bakingCount, completedCount);
        model.addAttribute("chartStatusCounts", statusCounts);

        return "adminDashboard";
    }
}