package com.lutiano.api.proxy;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.lutiano.api.repository.notification.DeviceTokenRepository;
import com.lutiano.api.config.AppProperties;

/**
 * The one place that sends push notifications, usable by any module: give it a person, a title, a text and optional data, and it
 * reaches that person's phones. It knows nothing about events, categories or preferences (callers decide whether to send).
 * Sends push notifications through Firebase Cloud Messaging to a person's phones. It is on only when the Firebase service-account
 * file exists; otherwise every call does nothing. Phones whose token Firebase no longer accepts are forgotten.
 */
@Component
public class PushSender {
  private static final Logger log = LoggerFactory.getLogger(PushSender.class);

  private final DeviceTokenRepository devices;
  private final TaskExecutor executor;
  private final String credentials;
  private FirebaseMessaging messaging;

  public PushSender(DeviceTokenRepository devices, TaskExecutor applicationTaskExecutor, AppProperties.Push push) {
    this.devices = devices; this.executor = applicationTaskExecutor; this.credentials = push.credentials();
  }

  @PostConstruct
  void init() {
    if (credentials == null || credentials.isBlank() || !Files.isRegularFile(Path.of(credentials))) {
      log.info("Push notifications are off: no Firebase service-account file at '{}'.", credentials);
      return;
    }
    try (FileInputStream in = new FileInputStream(credentials)) {
      GoogleCredentials google = GoogleCredentials.fromStream(in);
      String project = google instanceof com.google.auth.oauth2.ServiceAccountCredentials sa ? sa.getProjectId() : null;
      FirebaseApp app = FirebaseApp.getApps().isEmpty()
          ? FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(google).setProjectId(project).build())
          : FirebaseApp.getInstance();
      messaging = FirebaseMessaging.getInstance(app);
      log.info("Push notifications are on (Firebase project {}).", app.getOptions().getProjectId());
    } catch (Exception e) {
      log.error("Push notifications are off: the Firebase service-account file could not be used: {}", e.toString());
    }
  }

  public boolean enabled() { return messaging != null; }

  // ----- sending -----

  /** Tells all of a person's phones, after the current transaction commits. {@code data} reaches the app when the notification is opened. */
  public void notifyAfterCommit(String userId, String title, String body, Map<String, String> data) {
    if (!enabled()) return;
    Runnable task = () -> executor.execute(() -> send(userId, title, body, data, false));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { task.run(); }
      });
    } else task.run();
  }

  /** Sends now; with {@code dryRun}, Firebase checks the message and the credentials but delivers nothing. Returns how many phones accepted it. */
  public int send(String userId, String title, String body, Map<String, String> data, boolean dryRun) {
    if (!enabled()) return 0;
    int accepted = 0;
    for (String token : devices.tokens(userId)) {
      try {
        messaging.send(Message.builder().setToken(token).setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data).setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build()).build(), dryRun);
        accepted++;
      } catch (FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
          devices.forget(token);   // the phone no longer has the app, or the token is not one
        } else log.warn("Could not send a push notification: {}", e.toString());
      } catch (RuntimeException e) {
        log.warn("Could not send a push notification: {}", e.toString());
      }
    }
    return accepted;
  }
}
