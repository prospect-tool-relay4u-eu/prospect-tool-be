package eu.relay4u.prospecting.repository;

import eu.relay4u.prospecting.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
