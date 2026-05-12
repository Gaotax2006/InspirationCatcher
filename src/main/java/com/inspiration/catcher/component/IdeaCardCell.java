package com.inspiration.catcher.component;

import com.inspiration.catcher.model.Idea;
import com.inspiration.catcher.model.Tag;
import com.inspiration.catcher.util.DateUtil;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Renders an Idea as a rich card inside a single TableCell.
 */
public class IdeaCardCell extends TableCell<Idea, Idea> {

    /** 搜索高亮关键词（静态，全局生效） */
    public static String highlightKeyword = "";

    public IdeaCardCell() {
        setStyle("-fx-padding: 4 8;");
    }

    @Override
    protected void updateItem(Idea idea, boolean empty) {
        super.updateItem(idea, empty);
        if (empty || idea == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        setGraphic(createCard(idea));
    }

    private VBox createCard(Idea idea) {
        VBox card = new VBox(5);
        String typeColor = getTypeColor(idea.getType());
        card.setStyle(String.format(
            "-fx-background-color: -fx-bg-raised; -fx-background-radius: 8px; " +
            "-fx-border-color: -fx-border-subtle; -fx-border-radius: 8px; " +
            "-fx-border-width: 1; -fx-padding: 10 12 10 14; " +
            "-fx-effect: dropshadow(gaussian, rgba(44,41,36,0.06), 4, 0, 0, 1);" +
            "-fx-border-insets: 0 0 0 3; -fx-border-color: transparent transparent transparent %s;",
            typeColor
        ));

        // Row 1: Type icon + Title + Importance
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);

        FontIcon typeIcon = createTypeIcon(idea.getType());
        typeIcon.setIconSize(16);
        typeIcon.setIconColor(Color.web(typeColor));

        Node titleNode = buildHighlightedText(
            idea.getTitle() != null && !idea.getTitle().isEmpty() ? idea.getTitle() : "无标题",
            "-fx-font-size: 14px; -fx-font-weight: 600; -fx-fill: -fx-text-primary;"
        );
        HBox.setHgrow(titleNode instanceof Label ? (Label)titleNode : (titleNode instanceof TextFlow ? (TextFlow)titleNode : null), Priority.ALWAYS);

        HBox starsBox = createStars(idea.getImportance());
        row1.getChildren().addAll(typeIcon, titleNode, starsBox);

        // Row 2: Tags + Mood + Time
        HBox row2 = new HBox(6);
        row2.setAlignment(Pos.CENTER_LEFT);

        if (idea.getTags() != null && !idea.getTags().isEmpty()) {
            HBox tagsBox = new HBox(4);
            int maxTags = Math.min(3, idea.getTags().size());
            for (int i = 0; i < maxTags; i++) {
                Tag tag = idea.getTags().get(i);
                Label tagLabel = new Label("#" + tag.getName());
                String tc = tag.getColor() != null ? tag.getColor() : "#C4843C";
                tagLabel.setStyle(String.format(
                    "-fx-background-color: %1$s18; -fx-text-fill: %1$s; -fx-padding: 1 8; " +
                    "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: 500;", tc));
                tagsBox.getChildren().add(tagLabel);
            }
            if (idea.getTags().size() > 3) {
                Label more = new Label("+" + (idea.getTags().size() - 3));
                more.setStyle("-fx-text-fill: -fx-text-tertiary; -fx-font-size: 11px;");
                tagsBox.getChildren().add(more);
            }
            row2.getChildren().add(tagsBox);
        }

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        row2.getChildren().add(spacer2);

        // Mood
        FontIcon moodIcon = createMoodIcon(idea.getMood());
        if (moodIcon != null) {
            moodIcon.setIconSize(13);
            row2.getChildren().add(moodIcon);
        }

        // Privacy badge
        if (idea.getPrivacy() == Idea.PrivacyLevel.PUBLIC) {
            Label pub = new Label("公开");
            pub.setStyle("-fx-text-fill: -fx-green; -fx-font-size: 10px; -fx-padding: 0 4;");
            row2.getChildren().add(pub);
        } else if (idea.getPrivacy() == Idea.PrivacyLevel.ENCRYPTED) {
            Label enc = new Label("🔒");
            enc.setStyle("-fx-font-size: 11px;");
            row2.getChildren().add(enc);
        }

        Label timeLabel = new Label(formatRelativeTime(idea.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: -fx-text-tertiary; -fx-font-size: 11px;");
        row2.getChildren().add(timeLabel);

        // Row 3: Content preview with highlighting
        if (idea.getContent() != null && !idea.getContent().trim().isEmpty()) {
            String previewText = idea.getContent().length() > 120
                ? idea.getContent().substring(0, 120) + "..."
                : idea.getContent();
            Node previewNode = buildHighlightedText(previewText,
                "-fx-fill: -fx-text-tertiary; -fx-font-size: 12px;");
            if (previewNode instanceof TextFlow tf) {
                tf.setTextAlignment(TextAlignment.LEFT);
                card.getChildren().addAll(row1, row2, tf);
            } else {
                card.getChildren().addAll(row1, row2, previewNode);
            }
        } else {
            card.getChildren().addAll(row1, row2);
        }

        return card;
    }

    /** 构建高亮文本（若有关键词则使用 TextFlow 高亮匹配部分） */
    private Node buildHighlightedText(String text, String baseStyle) {
        if (highlightKeyword == null || highlightKeyword.isEmpty() || !text.toLowerCase().contains(highlightKeyword.toLowerCase())) {
            Label label = new Label(text);
            label.setStyle(baseStyle.replace("-fx-fill:", "-fx-text-fill:"));
            return label;
        }
        // 有关键词时用 TextFlow 分段高亮
        TextFlow flow = new TextFlow();
        String lower = text.toLowerCase();
        String kw = highlightKeyword.toLowerCase();
        int idx = 0;
        while (idx < text.length()) {
            int match = lower.indexOf(kw, idx);
            if (match < 0) {
                Text rest = new Text(text.substring(idx));
                rest.setStyle(baseStyle);
                flow.getChildren().add(rest);
                break;
            }
            if (match > idx) {
                Text before = new Text(text.substring(idx, match));
                before.setStyle(baseStyle);
                flow.getChildren().add(before);
            }
            Text highlight = new Text(text.substring(match, match + kw.length()));
            highlight.setStyle("-fx-fill: -fx-primary; -fx-font-weight: bold; " +
                "-fx-background-color: -fx-primary-bg;");
            flow.getChildren().add(highlight);
            idx = match + kw.length();
        }
        return flow;
    }

    private FontIcon createTypeIcon(Idea.IdeaType type) {
        FontAwesomeSolid icon = switch (type) {
            case IDEA -> FontAwesomeSolid.LIGHTBULB;
            case QUOTE -> FontAwesomeSolid.QUOTE_LEFT;
            case QUESTION -> FontAwesomeSolid.QUESTION_CIRCLE;
            case TODO -> FontAwesomeSolid.CHECK_CIRCLE;
            case DISCOVERY -> FontAwesomeSolid.SEARCH;
            case CONFUSION -> FontAwesomeSolid.QUESTION;
            case HYPOTHESIS -> FontAwesomeSolid.FLASK;
        };
        return new FontIcon(icon);
    }

    private FontIcon createMoodIcon(Idea.Mood mood) {
        if (mood == null) return null;
        FontAwesomeSolid icon = switch (mood) {
            case HAPPY -> FontAwesomeSolid.SMILE;
            case EXCITED -> FontAwesomeSolid.GRIN_STARS;
            case CALM -> FontAwesomeSolid.SMILE_BEAM;
            case NEUTRAL -> FontAwesomeSolid.MEH;
            case THOUGHTFUL -> FontAwesomeSolid.COMMENT;
            case CREATIVE -> FontAwesomeSolid.PALETTE;
            case INSPIRED -> FontAwesomeSolid.STAR;
            case CURIOUS -> FontAwesomeSolid.SEARCH;
            case CONFUSED -> FontAwesomeSolid.QUESTION;
            case FRUSTRATED -> FontAwesomeSolid.FROWN;
        };
        return new FontIcon(icon);
    }

    private HBox createStars(int importance) {
        HBox stars = new HBox(1);
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= importance ? "★" : "☆");
            star.setStyle(i <= importance
                ? "-fx-text-fill: #E8A838; -fx-font-size: 13px;"
                : "-fx-text-fill: #D0C8C0; -fx-font-size: 13px;");
            stars.getChildren().add(star);
        }
        return stars;
    }

    private String getTypeColor(Idea.IdeaType type) {
        return switch (type) {
            case IDEA -> "#C4843C";
            case QUOTE -> "#5B7FAF";
            case QUESTION -> "#8B6FAF";
            case TODO -> "#5B8C5A";
            case DISCOVERY -> "#C4A84C";
            case CONFUSION -> "#C45656";
            case HYPOTHESIS -> "#C4843C";
        };
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + "小时前";
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + "天前";
        return DateUtil.formatDate(dateTime);
    }
}
