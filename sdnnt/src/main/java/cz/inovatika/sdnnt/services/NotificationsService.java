package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.Notification;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;

import java.util.List;

public interface NotificationsService {


    public void saveNotification(Notification notification) throws NotificationsException;

    public List<Notification> findNotificationsByUser(String username) throws NotificationsException;
    public List<Notification> findNotificationsByInterval(NotificationInterval interval) throws NotificationsException;


    public void processNotifications(NotificationInterval interval) throws UserControlerException, NotificationsException;

}
