package com.inspiration.catcher.manager;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.util.Optional;

public class StatusManager {
    public void showNotification(String message) {
        Notifications.create()
                .title("提示")
                .text(message)
                .hideAfter(Duration.seconds(3))
                .owner(null)
                .showInformation();
    }

    public void showSuccess(String message) {
        Notifications.create()
                .title("成功")
                .text(message)
                .hideAfter(Duration.seconds(3))
                .owner(null)
                .showConfirm();
    }

    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void showError(String title, String message) {
        Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(5))
                .owner(null)
                .showError();
    }
    public boolean showConfirmDialog(String title, String header, String content) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(header);
        confirm.setContentText(content);

        Optional<ButtonType> result = confirm.showAndWait();
        return result.orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}