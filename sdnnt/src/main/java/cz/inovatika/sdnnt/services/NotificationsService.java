package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;
import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.SimpleNotification;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;

import java.util.List;

/**
 * This service is responsible for managing notification
 * @author happy
 */
public interface NotificationsService {

	/** 
	 * Save simple notification
	 * @param notification Notification to save 
	 * @throws NotificationsException The service cannot save the notification
	 */
    public SimpleNotification saveSimpleNotification(SimpleNotification notification) throws NotificationsException;

    
    /**
     * Save notification rule
     * @param notificationRule
     * @throws NotificationsException
     */
    public RuleNotification saveNotificationRule(RuleNotification notificationRule) throws NotificationsException;

    /**
     * Delete rule notifications
     * @param ruleNotifications
     * @throws NotificationsException
     */
    public void deleteRuleNotifications(List<RuleNotification> ruleNotifications) throws NotificationsException;

    
    
    /**
     * Find notifiation by given user
     * @param username User name
     * @return
     * @throws NotificationsException
     */
    public List<AbstractNotification> findNotificationsByUser(String username) throws NotificationsException;

    /**
     * Finds notifications  by given username and type
     * @param username Username 
     * @param type Type
     * @return
     * @throws NotificationsException
     */
    public List<AbstractNotification> findNotificationsByUser(String username, TYPE type) throws NotificationsException;

    
    /**
     * Find notification by username and id
     * @param username User name
     * @param id Identifier
     * @return
     * @throws NotificationsException
     */
    public AbstractNotification findNotificationByUserAndId(String username, String id) throws NotificationsException;

    /**
     * Finds notifications by given interval
     * @param interval Notification interval
     * @return
     * @throws NotificationsException
     */
    public List<AbstractNotification> findNotificationsByInterval(NotificationInterval interval) throws NotificationsException;

    /**
     * Finds notification by given interval and type
     * @param interval Notification interval
     * @param type Notification's type
     * @return
     * @throws NotificationsException
     */
    public List<AbstractNotification> findNotificationByInterval(NotificationInterval interval, TYPE type) throws NotificationsException;
    
    /**
     * Find notification by user name and interval
     * @param username Username
     * @param interval Interval
     * @return
     * @throws NotificationsException
     */
    public List<AbstractNotification> findNotificationsByUserAndInterval(String username, NotificationInterval interval) throws NotificationsException;

    /**
     * Finds notification by username, interval and type
     * @param username Username
     * @param interval Interval
     * @param type Type
     * @return
     * @throws NotificationsException
     */
    public List<AbstractNotification> findNotificationsByUserAndInterval(String username, NotificationInterval interval, TYPE type) throws NotificationsException;

    
    /**
     * Finds notification by interval
     * @param interval
     * @throws UserControlerException
     * @throws NotificationsException
     */
    public void processNotifications(NotificationInterval interval) throws UserControlerException, NotificationsException;
}
