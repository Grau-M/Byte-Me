package ca.sheridan.byteme.services;

import ca.sheridan.byteme.beans.Order;
import ca.sheridan.byteme.beans.User;
import ca.sheridan.byteme.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Simple stub for order status change notifications.
 * Right now it just checks the user's preferences and logs to the console.
 * You can later plug in real email/SMS/push logic here.
 */
@Service
public class NotificationService {

    private final UserRepository userRepository;

    public NotificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void sendOrderStatusUpdateNotification(Order order) {
        if (order == null || order.getUserId() == null) {
            return;
        }

        Optional<User> userOpt = userRepository.findById(order.getUserId());
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        // Respect notification preferences
        if (!user.isEmailNotifications()
                && !user.isInAppNotifications()
                && !user.isPushNotifications()) {
            return;
        }

        // 🔧 Stub: just log for now (no actual email/SMS)
        System.out.printf(
                "Stub notification: would notify %s about order %s status change to %s%n",
                user.getEmail(),
                order.getId(),
                order.getStatus()
        );
    }
}
