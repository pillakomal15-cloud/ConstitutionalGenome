package com.constitutionalgenome;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class Main extends Application {

    private BorderPane root;
    private VBox content;
    private TextField searchField;
    private final Set<String> processedDocumentHashes = new HashSet<>();
    private String latestExtractedText = "";
    private Process ttsProcess;
    private String latestArchiveId = "";
    private String latestArchiveTitle = "";
    private String latestArchiveType = "";
    private String latestArchivePeriod = "";
    private String latestArchiveLanguage = "";
    private String latestArchiveTopics = "";

    private final String BG = "#0B1220";
    private final String SIDEBAR = "#080E19";
    private final String CARD = "#111B2D";
    private final String CARD2 = "#152238";
    private final String GOLD = "#D8B56A";
    private final String GOLD_LIGHT = "#F0D79A";
    private final String WHITE = "#F4F1E8";
    private final String MUTED = "#9DA9B8";
    private final String LINE = "#26344A";

    @Override
    public void start(Stage stage) {

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        root.setTop(createTopBar(stage));
        root.setLeft(createSidebar());

        content = new VBox();
        content.setPadding(new Insets(28));
        content.setSpacing(24);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: " + BG + ";" +
                "-fx-background-color: " + BG + ";"
        );

        root.setCenter(scrollPane);

        showHome();

        Scene scene = new Scene(root, 1450, 900);

        stage.setTitle("Constitutional Genome");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private HBox createTopBar(Stage stage) {

        HBox top = new HBox(18);
        top.setPadding(new Insets(16, 24, 16, 24));
        top.setAlignment(Pos.CENTER_LEFT);

        top.setStyle(
                "-fx-background-color: #0A111E;" +
                "-fx-border-color: #1C293B;" +
                "-fx-border-width: 0 0 1 0;"
        );

        VBox logoBox = new VBox(2);

        Label logo = new Label("CONSTITUTIONAL");
        logo.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;"
        );

        Label logo2 = new Label("GENOME");
        logo2.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;"
        );

        Label tagline = new Label("PEOPLE  •  IDEAS  •  DEBATES  •  A NATION");
        tagline.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 9px;"
        );

        logoBox.getChildren().addAll(logo, logo2, tagline);

        // Search
        searchField = new TextField();
        searchField.setPromptText(
                "Search writings, speeches, debates, people, concepts..."
        );

        searchField.setStyle(
                "-fx-background-color: #111C2C;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #6F7D91;" +
                "-fx-border-color: #29374C;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 11px;"
        );

        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("Search");
        searchButton.setStyle(goldButtonStyle());

        searchButton.setOnAction(e -> performSearch());

        searchField.setOnAction(e -> performSearch());

        Button uploadButton = new Button("＋ Upload Document");
        uploadButton.setStyle(darkButtonStyle());

        uploadButton.setOnAction(e -> openUploadDialog(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.NEVER);

        Label avatar = new Label("K");
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setStyle(
                "-fx-background-color: " + GOLD + ";" +
                "-fx-background-radius: 50%;" +
                "-fx-text-fill: #111827;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 15px;"
        );

        top.getChildren().addAll(
                logoBox,
                searchField,
                searchButton,
                uploadButton,
                avatar
        );

        return top;
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private VBox createSidebar() {

        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(245);
        sidebar.setPadding(new Insets(24, 15, 20, 15));

        sidebar.setStyle(
                "-fx-background-color: " + SIDEBAR + ";" +
                "-fx-border-color: #1C293B;" +
                "-fx-border-width: 0 1 0 0;"
        );

        Label menuTitle = new Label("ARCHIVE");
        menuTitle.setPadding(new Insets(0, 0, 12, 10));

        menuTitle.setStyle(
                "-fx-text-fill: #64748B;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
        );

        sidebar.getChildren().add(menuTitle);

        Button home = sidebarButton("⌂", "Home");
        Button archive = sidebarButton("▣", "Digital Archive");
        Button ask = sidebarButton("◉", "Ask Archive");
        Button lineage = sidebarButton("⌁", "Idea Lineage");
        Button debate = sidebarButton("♟", "Debate Explorer");
        Button timeline = sidebarButton("◷", "Timeline");
        Button graph = sidebarButton("◇", "Knowledge Graph");
        Button ocr = sidebarButton("▤", "OCR & Upload");
        Button multi = sidebarButton("◎", "Multilingual");
        Button multimedia = sidebarButton("▦", "Multimedia Archive");
        Button about = sidebarButton("ⓘ", "About");

        home.setOnAction(e -> {
            setSelected(home);
            showHome();
        });

        archive.setOnAction(e -> {
            setSelected(archive);
            showArchive();
        });

        ask.setOnAction(e -> {
            setSelected(ask);
            showAskArchive();
        });

        lineage.setOnAction(e -> {
            setSelected(lineage);
            showLineage();
        });

        debate.setOnAction(e -> {
            setSelected(debate);
            showDebate();
        });

        timeline.setOnAction(e -> {
            setSelected(timeline);
            showTimeline();
        });

        graph.setOnAction(e -> {
            setSelected(graph);
            showGraph();
        });

        ocr.setOnAction(e -> {
            setSelected(ocr);
            showOCR();
        });

        multi.setOnAction(e -> {
            setSelected(multi);
            showMultilingual();
        });

        multimedia.setOnAction(e -> {
            setSelected(multimedia);
            showMultimediaArchive();
        });

        about.setOnAction(e -> {
            setSelected(about);
            showAbout();
        });

        sidebar.getChildren().addAll(
                home,
                archive,
                ask,
                lineage,
                debate,
                timeline,
                graph,
                ocr,
                multi,
                multimedia,
                about
        );

        Region space = new Region();
        VBox.setVgrow(space, Priority.ALWAYS);

        Label quote = new Label(
                "\"A Constitution lives through\n" +
                "the conversations of its people.\""
        );

        quote.setWrapText(true);
        quote.setPadding(new Insets(15));

        quote.setStyle(
                "-fx-text-fill: #A8B3C3;" +
                "-fx-font-size: 12px;" +
                "-fx-font-style: italic;" +
                "-fx-border-color: #2A384B;" +
                "-fx-border-width: 1 0 0 0;"
        );

        Label footer = new Label("CONSTITUTIONAL GENOME");
        footer.setPadding(new Insets(10, 15, 0, 15));

        footer.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
        );

        sidebar.getChildren().addAll(
                space,
                quote,
                footer
        );

        setSelected(home);

        return sidebar;
    }

    private Button sidebarButton(String icon, String text) {

        Button button = new Button(icon + "    " + text);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12, 15, 12, 15));

        button.setStyle(sidebarNormalStyle());

        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("#1B2A40")) {
                button.setStyle(sidebarHoverStyle());
            }
        });

        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains(GOLD)) {
                button.setStyle(sidebarNormalStyle());
            }
        });

        return button;
    }

    private void setSelected(Button selected) {

        if (root.getLeft() instanceof VBox sidebar) {

            for (javafx.scene.Node node : sidebar.getChildren()) {

                if (node instanceof Button button) {
                    button.setStyle(sidebarNormalStyle());
                }
            }
        }

        selected.setStyle(
                "-fx-background-color: #1B2A40;" +
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-background-radius: 9;" +
                "-fx-border-color: " + GOLD + ";" +
                "-fx-border-width: 0 0 0 3;" +
                "-fx-border-radius: 9;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );
    }

    // =========================================================
    // HOME
    // =========================================================

    private void showHome() {

        content.getChildren().clear();

        HBox hero = createHero();
        content.getChildren().add(hero);

        content.getChildren().add(createStats());

        Label topicTitle = sectionTitle(
                "Explore by Topic",
                "Browse constitutional ideas, themes and institutions"
        );

        content.getChildren().add(topicTitle);
        content.getChildren().add(createTopics());

        Label heritageTitle = sectionTitle(
                "Heritage Highlights",
                "Historical documents and visual records"
        );

        content.getChildren().add(heritageTitle);
        content.getChildren().add(createHeritageHighlights());

        HBox lower = new HBox(20);

        VBox recent = createRecentDocuments();
        VBox timeline = createTimelineCard();
        VBox personality = createPersonalityCard();

        HBox.setHgrow(recent, Priority.ALWAYS);
        HBox.setHgrow(timeline, Priority.ALWAYS);
        HBox.setHgrow(personality, Priority.ALWAYS);

        lower.getChildren().addAll(
                recent,
                timeline,
                personality
        );

        content.getChildren().add(lower);
    }

    // =========================================================
    // HERO
    // =========================================================

    private HBox createHero() {

        HBox hero = new HBox();

        hero.setMinHeight(270);
        hero.setPadding(new Insets(28));

        hero.setSpacing(25);

        hero.setStyle(
                "-fx-background-color: linear-gradient(to right, #101B2C, #17243A);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #2A394F;" +
                "-fx-border-radius: 18;"
        );

        VBox left = new VBox(12);

        Label small = new Label(
                "EXPLORE   •   UNDERSTAND   •   CONNECT"
        );

        small.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        Label title = new Label(
                "CONSTITUTIONAL\nGENOME"
        );

        title.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 38px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Uncover the ideas, people and debates that shaped a nation."
        );

        description.setWrapText(true);

        description.setStyle(
                "-fx-text-fill: #C2CBD8;" +
                "-fx-font-size: 15px;"
        );

        Label sub = new Label(
                "An AI-powered journey through India's constitutional history."
        );

        sub.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        Button explore = new Button("Start Exploring  →");
        explore.setStyle(goldButtonStyle());

        explore.setOnAction(e -> showArchive());

        left.getChildren().addAll(
                small,
                title,
                description,
                sub,
                explore
        );

        HBox.setHgrow(left, Priority.ALWAYS);

        // Hero image
        StackPane imagePane = new StackPane();

        ImageView heroImage = loadImage(
                "hero.jpg",
                450,
                220
        );

        imagePane.getChildren().add(heroImage);

        Label imageLabel = new Label(
                "INDIA  •  HERITAGE  •  CONSTITUTION"
        );

        imageLabel.setStyle(
                "-fx-background-color: rgba(5,10,18,0.78);" +
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-padding: 9px 13px;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
        );

        StackPane.setAlignment(imageLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(imageLabel, new Insets(0, 0, 12, 12));

        imagePane.getChildren().add(imageLabel);

        hero.getChildren().addAll(
                left,
                imagePane
        );

        return hero;
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private HBox createStats() {

        HBox box = new HBox(15);

        box.getChildren().addAll(
                statCard("1,250", "Documents", "Writings, drafts, reports"),
                statCard("320", "Speeches", "Historical speeches"),
                statCard("180", "Debates", "Constituent Assembly"),
                statCard("75", "Personalities", "People & thinkers"),
                statCard("8", "Languages", "Multilingual archive")
        );

        return box;
    }

    private VBox statCard(
            String number,
            String title,
            String description
    ) {

        VBox card = new VBox(5);

        card.setPadding(new Insets(18));
        card.setPrefHeight(105);

        card.setStyle(cardStyle());

        Label numberLabel = new Label(number);

        numberLabel.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;"
        );

        Label titleLabel = new Label(title);

        titleLabel.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(description);

        desc.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        card.getChildren().addAll(
                numberLabel,
                titleLabel,
                desc
        );

        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    // =========================================================
    // TOPICS
    // =========================================================

    private HBox createTopics() {

        HBox row = new HBox(14);

        String[][] topics = {
                {"Fundamental Rights", "assembly.jpg"},
                {"Directive Principles", "constitution.jpg"},
                {"Governance & Institutions", "hero.jpg"},
                {"Social Justice", "ambedkar.jpg"},
                {"Amendments", "constitution.jpg"},
                {"Federalism", "assembly.jpg"},
                {"Citizenship", "hero.jpg"},
                {"Judiciary", "constitution.jpg"}
        };

        for (String[] topic : topics) {

            VBox card = createTopicCard(
                    topic[0],
                    topic[1]
            );

            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(row);

        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        scroll.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;"
        );

        return row;
    }

    private VBox createTopicCard(
            String title,
            String imageName
    ) {

        VBox card = new VBox();

        card.setPrefWidth(150);
        card.setMinWidth(150);

        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #27364A;" +
                "-fx-border-radius: 12;"
        );

        ImageView image = loadImage(
                imageName,
                150,
                85
        );

        Label label = new Label(title);
        label.setWrapText(true);

        label.setPadding(new Insets(10));

        label.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        card.getChildren().addAll(
                image,
                label
        );

        return card;
    }

    // =========================================================
    // HERITAGE HIGHLIGHTS
    // =========================================================

    private HBox createHeritageHighlights() {

        HBox row = new HBox(18);

        row.getChildren().addAll(
                imageFeatureCard(
                        "The Constitution of India",
                        "Original constitutional document",
                        "constitution.jpg"
                ),
                imageFeatureCard(
                        "Constituent Assembly",
                        "The assembly that shaped the Constitution",
                        "assembly.jpg"
                )
        );

        return row;
    }

    private VBox imageFeatureCard(
            String title,
            String description,
            String imageName
    ) {

        VBox card = new VBox();

        card.setPrefHeight(180);
        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #29384D;" +
                "-fx-border-radius: 14;"
        );

        ImageView image = loadImage(
                imageName,
                310,
                120
        );

        Label titleLabel = new Label(title);

        titleLabel.setPadding(
                new Insets(10, 12, 2, 12)
        );

        titleLabel.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(description);

        desc.setPadding(
                new Insets(0, 12, 10, 12)
        );

        desc.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        card.getChildren().addAll(
                image,
                titleLabel,
                desc
        );

        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    // =========================================================
    // RECENT DOCUMENTS
    // =========================================================

    private VBox createRecentDocuments() {

        VBox card = new VBox(14);

        card.setPadding(new Insets(18));

        card.setStyle(cardStyle());

        Label title = new Label("Recent Documents");

        title.setStyle(sectionHeadingStyle());

        card.getChildren().add(title);

        card.getChildren().add(
                documentRow(
                        "Constitution of India",
                        "Original constitutional document",
                        "constitution.jpg"
                )
        );

        card.getChildren().add(
                documentRow(
                        "Constituent Assembly Records",
                        "Historical assembly archive",
                        "assembly.jpg"
                )
        );

        card.getChildren().add(
                documentRow(
                        "Ambedkar Archives",
                        "Writings and speeches",
                        "ambedkar.jpg"
                )
        );

        return card;
    }

    private HBox documentRow(
            String title,
            String description,
            String imageName
    ) {

        HBox row = new HBox(12);

        row.setAlignment(Pos.CENTER_LEFT);

        ImageView image = loadImage(
                imageName,
                80,
                60
        );

        VBox text = new VBox(4);

        Label titleLabel = new Label(title);

        titleLabel.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(description);

        desc.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        text.getChildren().addAll(
                titleLabel,
                desc
        );

        row.getChildren().addAll(
                image,
                text
        );

        return row;
    }

    // =========================================================
    // TIMELINE
    // =========================================================

    private VBox createTimelineCard() {

        VBox card = new VBox(15);

        card.setPadding(new Insets(18));

        card.setStyle(cardStyle());

        Label title = new Label("Historical Timeline");

        title.setStyle(sectionHeadingStyle());

        card.getChildren().add(title);

        card.getChildren().add(
                timelineItem(
                        "1920s–1930s",
                        "Early writings and social reform"
                )
        );

        card.getChildren().add(
                timelineItem(
                        "1946",
                        "Constituent Assembly begins"
                )
        );

        card.getChildren().add(
                timelineItem(
                        "1948",
                        "Draft Constitution presented"
                )
        );

        card.getChildren().add(
                timelineItem(
                        "1950",
                        "Constitution comes into force"
                )
        );

        return card;
    }

    private HBox timelineItem(
            String year,
            String text
    ) {

        HBox row = new HBox(12);

        Label yearLabel = new Label(year);

        yearLabel.setMinWidth(90);

        yearLabel.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        Label textLabel = new Label(text);

        textLabel.setWrapText(true);

        textLabel.setStyle(
                "-fx-text-fill: #C1CAD6;" +
                "-fx-font-size: 11px;"
        );

        row.getChildren().addAll(
                yearLabel,
                textLabel
        );

        return row;
    }

    // =========================================================
    // PERSONALITY
    // =========================================================

    private VBox createPersonalityCard() {

        VBox card = new VBox(10);

        card.setPadding(new Insets(18));

        card.setStyle(cardStyle());

        Label title = new Label("Featured Personality");

        title.setStyle(sectionHeadingStyle());

        ImageView image = loadImage(
                "ambedkar.jpg",
                190,
                135
        );

        Label name = new Label("Dr. B. R. Ambedkar");

        name.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Explore writings, speeches, debates and ideas "
                + "connected through the archive."
        );

        description.setWrapText(true);

        description.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        card.getChildren().addAll(
                title,
                image,
                name,
                description
        );

        return card;
    }

    // =========================================================
    // DIGITAL ARCHIVE
    // =========================================================

    private void showArchive() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Digital Archive",
                        "Explore documents, speeches, manuscripts and historical records."
                )
        );

        HBox cards = new HBox(20);

        cards.getChildren().addAll(
                archiveCard(
                        "Documents",
                        "1,250",
                        "constitution.jpg"
                ),
                archiveCard(
                        "Speeches",
                        "320",
                        "ambedkar.jpg"
                ),
                archiveCard(
                        "Debates",
                        "180",
                        "assembly.jpg"
                )
        );

        content.getChildren().add(cards);

        VBox searchPanel = new VBox(15);

        searchPanel.setPadding(new Insets(22));
        searchPanel.setStyle(cardStyle());

        Label heading = new Label("Archive Search");

        heading.setStyle(sectionHeadingStyle());

        TextField field = new TextField();

        field.setPromptText(
                "Search documents, writings, speeches..."
        );

        field.setStyle(inputStyle());

        Button search = new Button("Search Archive");

        search.setStyle(goldButtonStyle());

        Label result = new Label(
                "Enter a keyword to search the digital heritage collection."
        );

        result.setStyle(
                "-fx-text-fill: " + MUTED + ";"
        );

        search.setOnAction(e -> {
            result.setText(
                    "Searching archive for: " + field.getText()
            );
        });

        searchPanel.getChildren().addAll(
                heading,
                field,
                search,
                result
        );

        content.getChildren().add(searchPanel);
    }

    private VBox archiveCard(
            String title,
            String number,
            String image
    ) {

        VBox card = new VBox(8);

        card.setPadding(new Insets(15));

        card.setStyle(cardStyle());

        ImageView imageView = loadImage(
                image,
                220,
                100
        );

        Label numberLabel = new Label(number);

        numberLabel.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;"
        );

        Label titleLabel = new Label(title);

        titleLabel.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );

        card.getChildren().addAll(
                imageView,
                numberLabel,
                titleLabel
        );

        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    // =========================================================
    // ASK ARCHIVE
    // =========================================================

    private void showAskArchive() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Ask Archive",
                        "AI Research Orchestrator — route a question through archive search, idea lineage, debate reconstruction, evidence verification and the knowledge graph."
                )
        );

        VBox main = new VBox(18);
        main.setPadding(new Insets(22));

        VBox orchestrator = new VBox(14);
        orchestrator.setPadding(new Insets(22));
        orchestrator.setStyle(cardStyle());

        Label title = new Label("🔎 ARCHIVE RESEARCH ORCHESTRATOR");
        title.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Ask a research question. The orchestrator identifies the main topic, searches the latest processed source text, and creates an evidence-oriented research plan."
        );
        description.setWrapText(true);
        description.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        TextField question = new TextField();
        question.setPromptText(
                "Ask: How did equality develop through constitutional debates?"
        );
        question.setStyle(inputStyle());
        HBox.setHgrow(question, Priority.ALWAYS);

        Button run = new Button("▶ Run Research");
        run.setStyle(goldButtonStyle());

        HBox questionRow = new HBox(10);
        questionRow.setAlignment(Pos.CENTER_LEFT);
        questionRow.getChildren().addAll(question, run);

        HBox quickTopics = new HBox(8);
        String[] topics = {
                "Equality",
                "Fundamental Rights",
                "Social Justice",
                "Constitutional Safeguards"
        };

        for (String topic : topics) {
            Button b = new Button(topic);
            b.setStyle(darkButtonStyle());
            b.setOnAction(e -> question.setText(topic));
            quickTopics.getChildren().add(b);
        }

        orchestrator.getChildren().addAll(
                title,
                description,
                questionRow,
                quickTopics
        );

        // ---------------------------------------------------------
        // LIVE RESULT CARD — deliberately placed ABOVE the plan so
        // the user immediately sees the result after clicking Run.
        // ---------------------------------------------------------
        VBox resultCard = new VBox(10);
        resultCard.setPadding(new Insets(20));
        resultCard.setStyle(cardStyle());

        Label resultTitle = new Label("📋 RESEARCH RESULT");
        resultTitle.setStyle(sectionHeadingStyle());

        Label result = new Label(
                "Enter a research question above and click ▶ Run Research."
        );
        result.setWrapText(true);
        result.setMaxWidth(Double.MAX_VALUE);
        result.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        resultCard.getChildren().addAll(resultTitle, result);

        VBox plan = new VBox(12);
        plan.setPadding(new Insets(22));
        plan.setStyle(cardStyle());

        Label planTitle = new Label("RESEARCH PLAN");
        planTitle.setStyle(sectionHeadingStyle());

        VBox stages = new VBox(9);

        Label archiveStage = researchStage(
                "1. ARCHIVE SEARCH",
                "Locate relevant documents and extracted source text.",
                "READY"
        );

        Label lineageStage = researchStage(
                "2. IDEA LINEAGE",
                "Connect the concept across historical stages.",
                "READY"
        );

        Label debateStage = researchStage(
                "3. DEBATE RECONSTRUCTION",
                "Identify documented constitutional discussions.",
                "READY"
        );

        Label evidenceStage = researchStage(
                "4. EVIDENCE VERIFICATION",
                "Check source text and require human verification.",
                "HUMAN REVIEW"
        );

        Label graphStage = researchStage(
                "5. KNOWLEDGE GRAPH",
                "Connect verified people, ideas, documents and debates.",
                "READY"
        );

        stages.getChildren().addAll(
                archiveStage,
                lineageStage,
                debateStage,
                evidenceStage,
                graphStage
        );

        plan.getChildren().addAll(
                planTitle,
                stages
        );

        HBox navigation = new HBox(10);

        Button lineage = new Button("🧬 Open Idea Lineage");
        lineage.setStyle(darkButtonStyle());
        lineage.setOnAction(e -> showLineage());

        Button debate = new Button("⚖ Open Debate Explorer");
        debate.setStyle(darkButtonStyle());
        debate.setOnAction(e -> showDebate());

        Button graph = new Button("◇ Open Knowledge Graph");
        graph.setStyle(darkButtonStyle());
        graph.setOnAction(e -> showGraph());

        navigation.getChildren().addAll(
                lineage,
                debate,
                graph
        );

        run.setOnAction(e -> {

            String q = question.getText();

            if (q == null || q.isBlank()) {
                result.setText("⚠ Please enter a research question first.");
                result.setStyle(
                        "-fx-text-fill: #FFB36B;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
                );
                return;
            }

            String topic = detectResearchTopic(q);
            String evidence = findResearchEvidence(q);
            boolean sourceReady = latestExtractedText != null && !latestExtractedText.isBlank();

            result.setText(
                    "RESEARCH REQUEST\n" +
                    "Question: " + q + "\n" +
                    "Detected topic: " + topic + "\n\n" +
                    "ORCHESTRATOR EXECUTION\n" +
                    "✓ Archive Search — " +
                    (sourceReady ? "SOURCE TEXT FOUND" : "READY — process a document for live evidence") + "\n" +
                    "✓ Idea Lineage — READY\n" +
                    "✓ Debate Reconstruction — READY\n" +
                    "⚠ Evidence Verification — HUMAN REVIEW REQUIRED\n" +
                    "✓ Knowledge Graph — READY\n\n" +
                    "SOURCE EVIDENCE\n" +
                    evidence + "\n\n" +
                    "EVIDENCE FIREWALL\n" +
                    "AI interpretation is kept separate from archival source text. " +
                    "No historical claim is marked verified automatically."
            );

            result.setStyle(
                    "-fx-text-fill: " + GOLD_LIGHT + ";" +
                    "-fx-font-size: 12px;"
            );
        });

        question.setOnAction(e -> run.fire());

        main.getChildren().addAll(
                orchestrator,
                resultCard,
                plan,
                navigation
        );

        content.getChildren().add(main);
    }

    private String detectResearchTopic(String question) {

        String q = question == null ? "" : question.toLowerCase();

        if (q.contains("equality") || q.contains("equal")) {
            return "Equality";
        }
        if (q.contains("fundamental right") || q.contains("rights")) {
            return "Fundamental Rights";
        }
        if (q.contains("social justice") || q.contains("justice")) {
            return "Social Justice";
        }
        if (q.contains("safeguard") || q.contains("constitutional protection")) {
            return "Constitutional Safeguards";
        }
        if (q.contains("citizenship") || q.contains("citizen")) {
            return "Citizenship";
        }

        return "General Constitutional Theme";
    }

    private String findResearchEvidence(String question) {

        if (latestExtractedText == null || latestExtractedText.isBlank()) {
            return "No processed source text is available yet. Go to OCR & Upload, process a PDF/TXT document, and return here to run the research workflow against the extracted text.";
        }

        String text = latestExtractedText.replaceAll("\\s+", " ").trim();
        String[] keywords = (question == null ? "" : question.toLowerCase()).split("[^a-zA-Z]+" );

        int best = -1;
        int bestScore = 0;

        for (String keyword : keywords) {
            if (keyword.length() < 4) continue;

            int index = text.toLowerCase().indexOf(keyword);
            if (index >= 0) {
                int score = 1;
                if (keyword.equals("equality") || keyword.equals("justice") || keyword.equals("rights") || keyword.equals("constitutional")) {
                    score += 2;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = index;
                }
            }
        }

        if (best < 0) {
            return "The processed document is available, but no direct keyword match was found for this question. Review the document manually before drawing a conclusion.";
        }

        int start = Math.max(0, best - 180);
        int end = Math.min(text.length(), best + 520);
        String excerpt = text.substring(start, end);

        if (start > 0) {
            excerpt = "…" + excerpt;
        }
        if (end < text.length()) {
            excerpt = excerpt + "…";
        }

        return excerpt;
    }

    private Label researchStage(
            String title,
            String description,
            String status
    ) {

        Label label = new Label(
                title + "   •   " + status + "\n" +
                description
        );

        label.setWrapText(true);
        label.setPadding(new Insets(12));
        label.setStyle(
                "-fx-background-color: #17263B;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: #C8D1DE;" +
                "-fx-font-size: 12px;"
        );

        return label;
    }

    // =========================================================
    // IDEA LINEAGE
    // =========================================================

    private void showLineage() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Idea Lineage Agent",
                        "Trace how a constitutional idea evolved across time."
                )
        );

        VBox panel = new VBox(18);
        panel.setPadding(new Insets(25));
        panel.setStyle(cardStyle());

        Label idea = new Label("Evolution of an Idea");
        idea.setStyle(sectionHeadingStyle());

        TextField field = new TextField();
        field.setPromptText(
                "Enter an idea: equality, reservation, citizenship..."
        );
        field.setStyle(inputStyle());

        Button trace = new Button("Trace Idea");
        trace.setStyle(goldButtonStyle());

        Label result = new Label(
                "Enter a constitutional idea and click Trace Idea to build its evidence-oriented lineage."
        );
        result.setWrapText(true);
        result.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        HBox timeline = new HBox(15);
        timeline.setAlignment(Pos.CENTER);

        timeline.getChildren().addAll(
                lineageNode("1920s", "Early Writings"),
                arrow(),
                lineageNode("1946–49", "Assembly Debates"),
                arrow(),
                lineageNode("1948", "Draft Provisions"),
                arrow(),
                lineageNode("1950", "Final Constitution")
        );

        // This is the actual action behind the Trace Idea button.
        trace.setOnAction(e -> traceLineageIdea(field, idea, result, trace));

        // Pressing Enter in the search box performs the same action.
        field.setOnAction(e -> traceLineageIdea(field, idea, result, trace));

        panel.getChildren().addAll(
                idea,
                field,
                trace,
                result,
                timeline
        );

        content.getChildren().add(panel);
    }

    private void traceLineageIdea(
            TextField field,
            Label idea,
            Label result,
            Button trace
    ) {
        String ideaText = field.getText();

        if (ideaText == null || ideaText.isBlank()) {
            ideaText = "Selected constitutional idea";
        } else {
            ideaText = ideaText.trim();
        }

        idea.setText("Evolution of: " + ideaText);

        result.setText(
                "✓ Lineage traced for: " + ideaText + "\n\n" +
                "The prototype connects the idea across four historical stages: " +
                "early writings, Constituent Assembly debates, draft provisions, " +
                "and the final Constitution. Archival source verification is required " +
                "before treating a displayed statement as a historical quotation."
        );

        trace.setText("✓ Idea Traced");
    }

    private VBox lineageNode(
            String year,
            String title
    ) {

        VBox node = new VBox(5);

        node.setAlignment(Pos.CENTER);
        node.setPadding(new Insets(18));

        node.setMinWidth(170);

        node.setStyle(
                "-fx-background-color: #17263B;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #3A4A60;" +
                "-fx-border-radius: 12;"
        );

        Label y = new Label(year);

        y.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-weight: bold;"
        );

        Label t = new Label(title);

        t.setWrapText(true);

        t.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 11px;"
        );

        node.getChildren().addAll(
                y,
                t
        );

        return node;
    }

    private Label arrow() {

        Label arrow = new Label("→");

        arrow.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 25px;"
        );

        return arrow;
    }

    // =========================================================
    // DEBATE EXPLORER
    // =========================================================

    private void showDebate() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Debate Reconstructor",
                        "Reconstruct constitutional discussions using documented archival evidence."
                )
        );

        VBox searchPanel = new VBox(14);
        searchPanel.setPadding(new Insets(22));
        searchPanel.setStyle(cardStyle());

        Label heading = new Label("CONSTITUENT ASSEMBLY DEBATE RECONSTRUCTOR");
        heading.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Select a constitutional topic to reconstruct how the discussion evolved through documented debates."
        );
        description.setWrapText(true);
        description.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        HBox searchRow = new HBox(12);

        TextField topicField = new TextField();
        topicField.setPromptText("Search topic: equality, citizenship, fundamental rights...");
        topicField.setStyle(inputStyle());
        HBox.setHgrow(topicField, Priority.ALWAYS);

        Button reconstruct = new Button("Reconstruct Debate");
        reconstruct.setStyle(goldButtonStyle());

        searchRow.getChildren().addAll(topicField, reconstruct);

        VBox debateContent = new VBox(20);
        debateContent.setPadding(new Insets(22));
        debateContent.setStyle(cardStyle());

        HBox topics = new HBox(10);
        String[] topicNames = {
                "Fundamental Rights",
                "Equality",
                "Citizenship",
                "Social Justice",
                "Constitutional Safeguards"
        };

        for (String topic : topicNames) {
            Button topicButton = new Button(topic);
            topicButton.setStyle(secondaryButtonStyle());
            topicButton.setOnAction(e -> {
                topicField.setText(topic);
                loadDebateTopic(topic, debateContent);
            });
            topics.getChildren().add(topicButton);
        }

        searchPanel.getChildren().addAll(heading, description, searchRow, topics);

        Label initialTitle = new Label("Select a topic to begin");
        initialTitle.setStyle(sectionHeadingStyle());

        Label initialText = new Label(
                "The Debate Reconstructor organizes documented discussions chronologically " +
                "and separates archival evidence from AI-generated explanation."
        );
        initialText.setWrapText(true);
        initialText.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 13px;"
        );

        debateContent.getChildren().addAll(initialTitle, initialText);

        reconstruct.setOnAction(e -> {
            String topic = topicField.getText();
            if (topic == null || topic.isBlank()) {
                topic = "Fundamental Rights";
                topicField.setText(topic);
            }
            loadDebateTopic(topic, debateContent);
        });

        topicField.setOnAction(e -> reconstruct.fire());

        content.getChildren().addAll(searchPanel, debateContent);
    }

    private void loadDebateTopic(String topic, VBox debateContent) {
        debateContent.getChildren().clear();

        Label title = new Label("Debate Reconstruction: " + topic);
        title.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 19px;" +
                "-fx-font-weight: bold;"
        );

        Label explanation = new Label(
                "The system organizes the available archival material into a chronological evidence trail. " +
                "Historical quotations should be taken only from verified source documents."
        );
        explanation.setWrapText(true);
        explanation.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        Label timelineTitle = new Label("DEBATE TIMELINE");
        timelineTitle.setStyle(sectionHeadingStyle());

        VBox timeline = new VBox(12);
        timeline.getChildren().addAll(
                debateEvent("1946", "Initial Constitutional Discussions",
                        "Early discussions concerning the structure and principles of the future Constitution.",
                        "ARCHIVAL RECORD"),
                debateEvent("1947", "Constitutional Committee Discussions",
                        "The topic is examined in relation to constitutional principles, rights and institutional safeguards.",
                        "ARCHIVAL RECORD"),
                debateEvent("1948", "Draft Constitution Discussions",
                        "The issue is considered during discussions surrounding the Draft Constitution.",
                        "DRAFTING STAGE"),
                debateEvent("1949", "Final Constitutional Deliberation",
                        "The reconstructed timeline follows the documented discussion toward adoption of the Constitution.",
                        "FINAL DELIBERATION")
        );

        VBox evidence = new VBox(12);
        evidence.setPadding(new Insets(18));
        evidence.setStyle(
                "-fx-background-color: #17263B;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #35465D;" +
                "-fx-border-radius: 14;"
        );

        Label evidenceTitle = new Label("EVIDENCE & SOURCE VERIFICATION");
        evidenceTitle.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Label evidenceText = new Label(
                "Evidence status: ARCHIVAL SOURCE REQUIRED\n\n" +
                "Topic: " + topic + "\n" +
                "Source type: Constituent Assembly records\n" +
                "Processing: Topic matching → chronology → evidence mapping\n" +
                "Verification: Human/source verification recommended before publication."
        );
        evidenceText.setWrapText(true);
        evidenceText.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 12px;"
        );
        evidence.getChildren().addAll(evidenceTitle, evidenceText);

        HBox actions = new HBox(12);

        Button traceIdea = new Button("Trace to Constitutional Genome");
        traceIdea.setStyle(goldButtonStyle());
        traceIdea.setOnAction(e -> showLineage());

        Button graphButton = new Button("Open Knowledge Graph");
        graphButton.setStyle(secondaryButtonStyle());
        graphButton.setOnAction(e -> showGraph());

        Button evidenceButton = new Button("View Evidence Trail");
        evidenceButton.setStyle(secondaryButtonStyle());
        evidenceButton.setOnAction(e -> showEvidenceTrail(topic));

        actions.getChildren().addAll(traceIdea, graphButton, evidenceButton);

        debateContent.getChildren().addAll(
                title, explanation, timelineTitle, timeline, evidence, actions
        );
    }

    private VBox debateEvent(String year, String title, String description, String status) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #152238;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #304158;" +
                "-fx-border-radius: 12;"
        );

        Label yearLabel = new Label(year);
        yearLabel.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        Label statusLabel = new Label("● " + status);
        statusLabel.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
        );

        card.getChildren().addAll(yearLabel, titleLabel, descriptionLabel, statusLabel);
        return card;
    }

    private void showEvidenceTrail(String topic) {
        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Evidence Trail",
                        "Trace how archival evidence is connected to the reconstructed debate."
                )
        );

        VBox panel = new VBox(18);
        panel.setPadding(new Insets(25));
        panel.setStyle(cardStyle());

        Label title = new Label("Evidence Chain: " + topic);
        title.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        Label chain = new Label(
                "DOCUMENT\n" +
                "   ↓\n" +
                "OCR / TEXT EXTRACTION\n" +
                "   ↓\n" +
                "TOPIC MATCHING\n" +
                "   ↓\n" +
                "DEBATE SEGMENT\n" +
                "   ↓\n" +
                "SPEAKER / DATE METADATA\n" +
                "   ↓\n" +
                "EVIDENCE VERIFICATION\n" +
                "   ↓\n" +
                "CONSTITUTIONAL IDEA\n" +
                "   ↓\n" +
                "KNOWLEDGE GRAPH"
        );
        chain.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );
        chain.setWrapText(true);

        Label warning = new Label(
                "Evidence Firewall: AI-generated explanations must not be treated as archival quotations. " +
                "Source text remains the authoritative evidence layer."
        );
        warning.setWrapText(true);
        warning.setPadding(new Insets(15));
        warning.setStyle(
                "-fx-background-color: #201B12;" +
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 12px;"
        );

        Button back = new Button("Back to Debate Reconstructor");
        back.setStyle(secondaryButtonStyle());
        back.setOnAction(e -> showDebate());

        panel.getChildren().addAll(title, chain, warning, back);
        content.getChildren().add(panel);
    }

    private VBox speakerCard(
            String name,
            String role,
            String imageName
    ) {

        VBox card = new VBox(8);

        card.setPadding(new Insets(12));

        card.setStyle(
                "-fx-background-color: #17263B;" +
                "-fx-background-radius: 12;"
        );

        ImageView image = loadImage(
                imageName,
                170,
                105
        );

        Label nameLabel = new Label(name);

        nameLabel.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-weight: bold;"
        );

        Label roleLabel = new Label(role);

        roleLabel.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        card.getChildren().addAll(
                image,
                nameLabel,
                roleLabel
        );

        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    // =========================================================
    // TIMELINE PAGE
    // =========================================================

    private void showTimeline() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Historical Timeline",
                        "A chronological journey through India's constitutional history."
                )
        );

        VBox timeline = new VBox(15);

        String[][] events = {
                {"1920–1930", "Early writings and social reform"},
                {"1932", "Poona Pact"},
                {"1946", "Constituent Assembly"},
                {"1947", "Independence"},
                {"1948", "Draft Constitution"},
                {"1949", "Constitution adopted"},
                {"1950", "Constitution comes into force"}
        };

        for (String[] event : events) {

            HBox item = new HBox(20);

            item.setPadding(new Insets(18));

            item.setAlignment(Pos.CENTER_LEFT);

            item.setStyle(cardStyle());

            Label year = new Label(event[0]);

            year.setMinWidth(130);

            year.setStyle(
                    "-fx-text-fill: " + GOLD + ";" +
                    "-fx-font-size: 15px;" +
                    "-fx-font-weight: bold;"
            );

            Label eventText = new Label(event[1]);

            eventText.setStyle(
                    "-fx-text-fill: " + WHITE + ";" +
                    "-fx-font-size: 13px;"
            );

            item.getChildren().addAll(
                    year,
                    eventText
            );

            timeline.getChildren().add(item);
        }

        content.getChildren().add(timeline);
    }

    // =========================================================
    // KNOWLEDGE GRAPH
    // =========================================================

    private void showGraph() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Constitutional Knowledge Graph",
                        "Dynamically connect the processed source document with people, ideas, debates and evidence."
                )
        );

        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(18));
        wrapper.setStyle(cardStyle());

        Label status = new Label();
        status.setWrapText(true);
        status.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        String source = latestExtractedText == null ? "" : latestExtractedText.trim();

        if (source.isBlank()) {
            status.setText(
                    "● DEMO GRAPH — No processed document is loaded yet. " +
                    "Process a PDF/TXT document in OCR & Upload to populate the graph."
            );
        } else {
            status.setText(
                    "● LIVE PROTOTYPE GRAPH — Built from the latest extracted source text."
            );
        }

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button refresh = new Button("↻ Refresh Graph");
        refresh.setStyle(goldButtonStyle());
        refresh.setOnAction(e -> showGraph());

        Button lineage = new Button("⌁ Open Idea Lineage");
        lineage.setStyle(secondaryButtonStyle());
        lineage.setOnAction(e -> showLineage());

        Button evidence = new Button("✓ Evidence Trail");
        evidence.setStyle(secondaryButtonStyle());
        evidence.setOnAction(e -> showEvidenceTrail("equality"));

        controls.getChildren().addAll(refresh, lineage, evidence);

        Pane graph = new Pane();
        graph.setPrefHeight(570);
        graph.setMinHeight(570);
        graph.setStyle(
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #29384D;" +
                "-fx-border-radius: 18;"
        );

        // Core node
        VBox center = graphNode("CONSTITUTIONAL GENOME", "Core Knowledge");
        center.setLayoutX(450);
        center.setLayoutY(220);

        // Source document node
        String documentTitle = detectGraphDocumentTitle(source);
        VBox document = graphNode(
                truncateGraphLabel(documentTitle, 30),
                source.isBlank() ? "No source loaded" : "Processed Source Document"
        );
        document.setLayoutX(55);
        document.setLayoutY(45);

        // Person node
        VBox person = graphNode("B. R. AMBEDKAR", "Person / Author");
        person.setLayoutX(760);
        person.setLayoutY(45);

        // Debate node
        VBox debates = graphNode("ASSEMBLY DEBATES", "Historical Discussions");
        debates.setLayoutX(55);
        debates.setLayoutY(385);

        // Evidence node
        VBox evidenceNode = graphNode("EVIDENCE", "Source-backed Claims");
        evidenceNode.setLayoutX(760);
        evidenceNode.setLayoutY(385);

        graph.getChildren().addAll(
                graphLine(245, 100, 505, 255),
                graphLine(950, 100, 595, 255),
                graphLine(245, 440, 505, 300),
                graphLine(950, 440, 595, 300),
                document, person, debates, evidenceNode, center
        );

        // Dynamic idea nodes based on the extracted document.
        List<String> topics = detectGraphTopics(source);
        double topicX = 330;
        double topicY = 55;
        int index = 0;

        for (String topic : topics) {
            VBox topicNode = graphNode(topic.toUpperCase(), "Detected Idea");
            topicNode.setMinWidth(150);
            topicNode.setLayoutX(topicX + (index % 2) * 155);
            topicNode.setLayoutY(topicY + (index / 2) * 82);
            graph.getChildren().add(topicNode);

            double nodeCenterX = topicNode.getLayoutX() + 75;
            double nodeCenterY = topicNode.getLayoutY() + 28;
            graph.getChildren().add(
                    graphLine(nodeCenterX, nodeCenterY, 525, 220)
            );
            index++;

            if (index >= 4) break;
        }

        Label graphInfo = new Label(
                "GRAPH RELATIONSHIPS\n" +
                "Source Document → Evidence → Constitutional Genome\n" +
                "Source Document → Detected Ideas → Constitutional Genome\n" +
                "Ambedkar → Debates → Constitutional Genome"
        );
        graphInfo.setWrapText(true);
        graphInfo.setLayoutX(25);
        graphInfo.setLayoutY(505);
        graphInfo.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );
        graph.getChildren().add(graphInfo);

        wrapper.getChildren().addAll(status, controls, graph);
        content.getChildren().add(wrapper);
    }

    private String detectGraphDocumentTitle(String text) {
        if (text == null || text.isBlank()) return "NO DOCUMENT";

        String[] lines = text.split("\\R");
        for (String line : lines) {
            String cleaned = line.trim();
            if (cleaned.toLowerCase().startsWith("document title:")) {
                return cleaned.substring(cleaned.indexOf(':') + 1).trim();
            }
        }

        return "PROCESSED DOCUMENT";
    }

    private String truncateGraphLabel(String value, int max) {
        if (value == null || value.isBlank()) return "DOCUMENT";
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private List<String> detectGraphTopics(String text) {
        List<String> topics = new ArrayList<>();
        String lower = text == null ? "" : text.toLowerCase();

        String[][] candidates = {
                {"Equality", "equality", "equal"},
                {"Social Justice", "social justice", "social"},
                {"Fundamental Rights", "fundamental rights", "rights"},
                {"Constitutional Safeguards", "constitutional safeguards", "safeguard"},
                {"Citizenship", "citizenship", "citizen"},
                {"Democracy", "democracy", "democratic"},
                {"Representation", "representation", "representative"}
        };

        for (String[] candidate : candidates) {
            if (lower.contains(candidate[1]) || lower.contains(candidate[2])) {
                topics.add(candidate[0]);
            }
        }

        if (topics.isEmpty()) {
            topics.add("Constitutional History");
            topics.add("Historical Archive");
        }

        return topics;
    }

    private Line graphLine(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.web("#52657D"));
        line.setStrokeWidth(1.5);
        line.setMouseTransparent(true);
        return line;
    }

    private VBox graphNode(
            String title,
            String subtitle
    ) {

        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setMinWidth(190);
        box.setMaxWidth(210);
        box.setStyle(
                "-fx-background-color: #1A2B43;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + GOLD + ";" +
                "-fx-border-radius: 14;"
        );

        Label t = new Label(title);
        t.setWrapText(true);
        t.setAlignment(Pos.CENTER);
        t.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        Label s = new Label(subtitle);
        s.setWrapText(true);
        s.setAlignment(Pos.CENTER);
        s.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 9px;"
        );

        box.getChildren().addAll(t, s);
        return box;
    }

    // =========================================================
    // OCR
    // =========================================================

    private void showOCR() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Autonomous Curator Agent",
                        "Convert a newly received historical document into a processed archival record."
                )
        );

        VBox uploadPanel = new VBox(14);
        uploadPanel.setPadding(new Insets(22));
        uploadPanel.setStyle(cardStyle());

        Label title = new Label("DOCUMENT INTAKE");
        title.setStyle(sectionHeadingStyle());

        Label description = new Label(
                "Select a PDF, TXT or scanned image. Text PDFs are extracted with Apache PDFBox. "
                + "Scanned images and image-only PDFs are processed through the locally installed Tesseract OCR engine. "
                + "Metadata, entities, topics and duplicate status are then derived from the extracted text."
        );
        description.setWrapText(true);
        description.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        HBox selectRow = new HBox(12);
        selectRow.setAlignment(Pos.CENTER_LEFT);

        Button choose = new Button("＋ Select Document");
        choose.setStyle(goldButtonStyle());

        Label selected = new Label("No document selected.");
        selected.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );
        HBox.setHgrow(selected, Priority.ALWAYS);

        selectRow.getChildren().addAll(choose, selected);
        uploadPanel.getChildren().addAll(title, description, selectRow);

        VBox intelligence = new VBox(14);
        intelligence.setPadding(new Insets(22));
        intelligence.setStyle(cardStyle());

        Label intelligenceTitle = new Label("DOCUMENT INTELLIGENCE");
        intelligenceTitle.setStyle(sectionHeadingStyle());

        GridPane metadata = new GridPane();
        metadata.setHgap(18);
        metadata.setVgap(12);

        Label titleValue = new Label("—");
        Label typeValue = new Label("—");
        Label periodValue = new Label("—");
        Label languageValue = new Label("—");
        Label entityValue = new Label("—");
        Label topicValue = new Label("—");
        Label confidenceValue = new Label("—");
        Label duplicateValue = new Label("—");

        Label[] valueLabels = {
                titleValue, typeValue, periodValue, languageValue,
                entityValue, topicValue, confidenceValue, duplicateValue
        };

        for (Label value : valueLabels) {
            value.setStyle(
                    "-fx-text-fill: " + WHITE + ";" +
                    "-fx-font-size: 12px;"
            );
            value.setWrapText(true);
        }

        String[] fieldNames = {
                "Title", "Document Type", "Historical Period", "Language",
                "Entities", "Topics", "Confidence", "Duplicate Check"
        };

        for (int i = 0; i < fieldNames.length; i++) {
            Label key = new Label(fieldNames[i] + ":");
            key.setStyle(
                    "-fx-text-fill: " + GOLD_LIGHT + ";" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
            );
            metadata.add(key, 0, i);
            metadata.add(valueLabels[i], 1, i);
        }

        ColumnConstraints keyColumn = new ColumnConstraints();
        keyColumn.setMinWidth(145);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        metadata.getColumnConstraints().addAll(keyColumn, valueColumn);

        intelligence.getChildren().addAll(intelligenceTitle, metadata);

        VBox workflow = new VBox(12);
        workflow.setPadding(new Insets(22));
        workflow.setStyle(cardStyle());

        Label workflowTitle = new Label("CURATOR WORKFLOW");
        workflowTitle.setStyle(sectionHeadingStyle());

        VBox workflowStages = new VBox(8);

        Label stage1 = curatorStage("1", "Document received", "WAITING");
        Label stage2 = curatorStage("2", "Text extraction", "WAITING");
        Label stage3 = curatorStage("3", "Metadata generated", "WAITING");
        Label stage4 = curatorStage("4", "Topic classification", "WAITING");
        Label stage5 = curatorStage("5", "Duplicate check", "WAITING");
        Label stage6 = curatorStage("6", "Evidence verification", "WAITING");
        Label stage7 = curatorStage("7", "Archive decision", "WAITING");

        workflowStages.getChildren().addAll(
                stage1, stage2, stage3, stage4, stage5, stage6, stage7
        );

        workflow.getChildren().addAll(workflowTitle, workflowStages);

        VBox extractedPanel = new VBox(10);
        extractedPanel.setPadding(new Insets(18));
        extractedPanel.setStyle(cardStyle());

        Label extractedTitle = new Label("EXTRACTED TEXT PREVIEW");
        extractedTitle.setStyle(sectionHeadingStyle());

        TextArea extractedText = new TextArea();
        extractedText.setEditable(false);
        extractedText.setWrapText(true);
        extractedText.setPrefRowCount(9);
        extractedText.setPromptText("Extracted document text will appear here.");
        extractedText.setStyle(
                "-fx-control-inner-background: #0B1220;" +
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-prompt-text-fill: #6F7D91;" +
                "-fx-border-color: #29374C;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );

        extractedPanel.getChildren().addAll(extractedTitle, extractedText);

        VBox decision = new VBox(12);
        decision.setPadding(new Insets(18));
        decision.setStyle(
                "-fx-background-color: #17263B;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #35465D;" +
                "-fx-border-radius: 12;"
        );

        Label decisionTitle = new Label("ARCHIVE DECISION");
        decisionTitle.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Label decisionText = new Label(
                "Select a PDF or TXT document to begin real local text extraction."
        );
        decisionText.setWrapText(true);
        decisionText.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 12px;"
        );

        HBox actions = new HBox(12);

        Button verify = new Button("✓ Verify Metadata");
        verify.setStyle(secondaryButtonStyle());
        verify.setDisable(true);

        Button archive = new Button("＋ Add to Archive");
        archive.setStyle(goldButtonStyle());
        archive.setDisable(true);

        Button evidenceButton = new Button("🛡 Verify Evidence");
        evidenceButton.setStyle(goldButtonStyle());
        evidenceButton.setDisable(true);

        actions.getChildren().addAll(verify, evidenceButton, archive);
        decision.getChildren().addAll(decisionTitle, decisionText, actions);

        final File[] selectedFile = new File[1];
        final CuratorRecord[] record = new CuratorRecord[1];

        choose.setOnAction(e -> {

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Historical Document");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(
                            "Processable Documents", "*.pdf", "*.txt"
                    ),
                    new FileChooser.ExtensionFilter(
                            "Image Files (OCR not connected yet)", "*.png", "*.jpg", "*.jpeg"
                    ),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File file = chooser.showOpenDialog(content.getScene().getWindow());

            if (file == null) {
                return;
            }

            selectedFile[0] = file;
            selected.setText("Selected: " + file.getName());

            updateCuratorStage(stage1, "✓", "Document received", "DONE");
            updateCuratorStage(stage2, "→", "Text extraction", "PROCESSING");
            updateCuratorStage(stage3, "→", "Metadata generated", "WAITING");
            updateCuratorStage(stage4, "→", "Topic classification", "WAITING");
            updateCuratorStage(stage5, "→", "Duplicate check", "WAITING");
            updateCuratorStage(stage6, "→", "Evidence verification", "WAITING");
            updateCuratorStage(stage7, "→", "Archive decision", "WAITING");

            verify.setDisable(true);
            evidenceButton.setDisable(true);
            archive.setDisable(true);
            extractedText.clear();

            try {
                String text = extractDocumentText(file);

                if (text.isBlank()) {
                    throw new IOException(
                            "No text could be extracted. If this is a scanned image/PDF, "
                            + "connect an OCR engine such as Tesseract."
                    );
                }

                latestExtractedText = text;

                String normalized = text.replaceAll("\\s+", " ").trim();
                CuratorRecord result = analyzeDocument(file, normalized);
                record[0] = result;

                titleValue.setText(result.title);
                typeValue.setText(result.documentType);
                periodValue.setText(result.period);
                languageValue.setText(result.language);
                entityValue.setText(result.entities);
                topicValue.setText(result.topics);
                confidenceValue.setText(String.format("%.0f%%", result.confidence));

                boolean duplicate = processedDocumentHashes.contains(result.hash);
                duplicateValue.setText(
                        duplicate ? "⚠ Duplicate detected" : "✓ No duplicate in current session"
                );

                extractedText.setText(
                        text.length() > 9000
                                ? text.substring(0, 9000) + "\n\n[Preview truncated]"
                                : text
                );

                updateCuratorStage(stage2, "✓", "Text extraction", "DONE");
                updateCuratorStage(stage3, "✓", "Metadata generated", "DONE");
                updateCuratorStage(stage4, "✓", "Topic classification", "DONE");
                updateCuratorStage(
                        stage5,
                        duplicate ? "⚠" : "✓",
                        "Duplicate check",
                        duplicate ? "DUPLICATE" : "CLEAR"
                );
                updateCuratorStage(stage6, "→", "Evidence verification", "HUMAN REVIEW");
                updateCuratorStage(stage7, "→", "Archive decision", "WAITING");

                decisionText.setText(
                        "DOCUMENT PROCESSED\n\n" +
                        "✓ Text extracted locally\n" +
                        "✓ Metadata inferred from extracted text\n" +
                        "✓ Topics detected using keyword analysis\n" +
                        "✓ Entity detection completed\n" +
                        (duplicate
                                ? "⚠ A duplicate hash was found in this session\n"
                                : "✓ No duplicate found in this session\n") +
                        "⚠ Archival source verification is still required before publication."
                );

                verify.setDisable(false);
                evidenceButton.setDisable(false);
                archive.setDisable(duplicate);

            } catch (Exception ex) {

                titleValue.setText(file.getName());
                typeValue.setText(documentType(file));
                periodValue.setText("Extraction failed");
                languageValue.setText("—");
                entityValue.setText("—");
                topicValue.setText("—");
                confidenceValue.setText("0%");
                duplicateValue.setText("Not checked");

                updateCuratorStage(stage2, "✕", "Text extraction", "FAILED");
                updateCuratorStage(stage3, "→", "Metadata generated", "BLOCKED");
                updateCuratorStage(stage4, "→", "Topic classification", "BLOCKED");
                updateCuratorStage(stage5, "→", "Duplicate check", "BLOCKED");
                updateCuratorStage(stage6, "→", "Evidence verification", "BLOCKED");
                updateCuratorStage(stage7, "→", "Archive decision", "BLOCKED");

                evidenceButton.setDisable(true);

                decisionText.setText(
                        "PROCESSING FAILED\n\n" +
                        ex.getMessage()
                );

                Alert alert = new Alert(
                        Alert.AlertType.WARNING,
                        ex.getMessage(),
                        ButtonType.OK
                );
                alert.setTitle("Document Processing");
                alert.setHeaderText("The document could not be processed");
                alert.showAndWait();
            }
        });

        evidenceButton.setOnAction(e -> {

            if (record[0] == null) {
                return;
            }

            showEvidenceVerification(
                    record[0],
                    extractedText.getText()
            );
        });

        verify.setOnAction(e -> {

            if (record[0] == null) {
                return;
            }

            updateCuratorStage(
                    stage6,
                    "⚠",
                    "Evidence verification",
                    "HUMAN REVIEW REQUIRED"
            );

            decisionText.setText(
                    "METADATA REVIEW COMPLETED\n\n" +
                    "The extracted text and automatically inferred metadata have been reviewed "
                    + "inside the prototype.\n\n" +
                    "Important: this action does not certify historical authenticity. "
                    + "The original archival source must still be checked by a human reviewer."
            );

            archive.setDisable(false);
        });

        archive.setOnAction(e -> {

            if (record[0] == null) {
                return;
            }

            processedDocumentHashes.add(record[0].hash);

            updateCuratorStage(
                    stage7,
                    "✓",
                    "Archive decision",
                    "READY FOR ARCHIVE"
            );

            decisionText.setText(
                    "DOCUMENT READY FOR ARCHIVE\n\n" +
                    "✓ Text extraction completed\n" +
                    "✓ Metadata generated\n" +
                    "✓ Topics classified\n" +
                    "✓ Entities detected\n" +
                    "✓ Duplicate check completed\n" +
                    "⚠ Source authenticity remains a human verification responsibility"
            );

            archive.setDisable(true);
            duplicateValue.setText("✓ Archived in current prototype session");
        });

        content.getChildren().addAll(
                uploadPanel,
                intelligence,
                workflow,
                extractedPanel,
                decision
        );
    }


    // =========================================================
    // AGENT 4 — EVIDENCE VERIFICATION AGENT
    // =========================================================

    private void showEvidenceVerification(
            CuratorRecord record,
            String extractedText
    ) {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Evidence Verification",
                        "Verify source-backed evidence before it enters the Constitutional Knowledge Graph."
                )
        );

        VBox main = new VBox(18);
        main.setPadding(new Insets(22));

        // SOURCE DOCUMENT
        VBox sourceCard = new VBox(10);
        sourceCard.setPadding(new Insets(18));
        sourceCard.setStyle(cardStyle());

        Label sourceTitle = new Label("SOURCE DOCUMENT");
        sourceTitle.setStyle(sectionHeadingStyle());

        Label source = new Label(record.title);
        source.setWrapText(true);
        source.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Label sourceStatus = new Label(
                "✓ Document processed\n" +
                "✓ Text extracted locally\n" +
                "✓ Metadata generated\n" +
                "✓ SHA-256 hash available"
        );
        sourceStatus.setStyle(
                "-fx-text-fill: #B9C6D8;" +
                "-fx-font-size: 12px;"
        );

        sourceCard.getChildren().addAll(
                sourceTitle,
                source,
                sourceStatus
        );

        // EVIDENCE ANALYSIS
        VBox evidenceCard = new VBox(12);
        evidenceCard.setPadding(new Insets(18));
        evidenceCard.setStyle(cardStyle());

        Label evidenceTitle = new Label("EVIDENCE ANALYSIS");
        evidenceTitle.setStyle(sectionHeadingStyle());

        TextField claimField = new TextField();
        claimField.setPromptText(
                "Enter a topic or claim to search in the extracted source..."
        );
        claimField.setStyle(inputStyle());

        Button analyze = new Button("Analyze Evidence");
        analyze.setStyle(goldButtonStyle());

        Label result = new Label(
                "Enter a topic or claim, then click Analyze Evidence."
        );
        result.setWrapText(true);
        result.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        TextArea evidencePreview = new TextArea();
        evidencePreview.setEditable(false);
        evidencePreview.setWrapText(true);
        evidencePreview.setPrefRowCount(7);
        evidencePreview.setPromptText(
                "Matching source text will appear here."
        );
        evidencePreview.setStyle(
                "-fx-control-inner-background: #0B1220;" +
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-prompt-text-fill: #6F7D91;" +
                "-fx-border-color: #29374C;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );

        analyze.setOnAction(e -> {

            String claim = claimField.getText();

            if (claim == null || claim.isBlank()) {
                claim = "Equality";
                claimField.setText(claim);
            }

            String sourceText = extractedText == null
                    ? ""
                    : extractedText;

            String lowerSource = sourceText.toLowerCase();
            String lowerClaim = claim.toLowerCase();

            if (lowerSource.contains(lowerClaim)) {

                int position = lowerSource.indexOf(lowerClaim);

                int start = Math.max(0, position - 220);
                int end = Math.min(
                        sourceText.length(),
                        position + claim.length() + 420
                );

                String match = sourceText.substring(start, end);

                evidencePreview.setText(
                        match
                );

                result.setText(
                        "Claim / Topic: " + claim + "\n\n" +
                        "Source Match: FOUND\n" +
                        "Evidence Location: Extracted document text\n" +
                        "Confidence: " + String.format(
                                "%.0f%%",
                                record.confidence
                        ) + "\n" +
                        "Status: HUMAN REVIEW REQUIRED"
                );

            } else {

                evidencePreview.setText(
                        "No exact text match found for this claim."
                );

                result.setText(
                        "Claim / Topic: " + claim + "\n\n" +
                        "Source Match: NOT FOUND\n" +
                        "Status: Do not treat the claim as source-backed evidence."
                );
            }
        });

        evidenceCard.getChildren().addAll(
                evidenceTitle,
                claimField,
                analyze,
                result,
                evidencePreview
        );

        // EVIDENCE FIREWALL
        VBox firewall = new VBox(10);
        firewall.setPadding(new Insets(18));
        firewall.setStyle(
                "-fx-background-color: #201B12;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #80652C;" +
                "-fx-border-radius: 12;"
        );

        Label firewallTitle = new Label(
                "🛡 EVIDENCE FIREWALL"
        );
        firewallTitle.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Label firewallText = new Label(
                "SOURCE TEXT\n" +
                "✓ Authoritative evidence layer\n\n" +
                "AI EXPLANATION\n" +
                "⚠ Interpretive layer — requires human/source verification\n\n" +
                "AI-generated text must never be presented as an archival quotation."
        );
        firewallText.setWrapText(true);
        firewallText.setStyle(
                "-fx-text-fill: #E4D7B5;" +
                "-fx-font-size: 12px;"
        );

        firewall.getChildren().addAll(
                firewallTitle,
                firewallText
        );

        // DECISION
        VBox decision = new VBox(12);
        decision.setPadding(new Insets(18));
        decision.setStyle(cardStyle());

        Label decisionTitle = new Label(
                "VERIFICATION DECISION"
        );
        decisionTitle.setStyle(sectionHeadingStyle());

        Label status = new Label(
                "● HUMAN REVIEW REQUIRED"
        );
        status.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-weight: bold;"
        );

        HBox actions = new HBox(10);

        Button verifyEvidence = new Button(
                "✓ Verify Evidence"
        );
        verifyEvidence.setStyle(goldButtonStyle());

        Button rejectEvidence = new Button(
                "✕ Reject"
        );
        rejectEvidence.setStyle(darkButtonStyle());

        Button graphButton = new Button(
                "◇ Add to Knowledge Graph"
        );
        graphButton.setStyle(darkButtonStyle());
        graphButton.setDisable(true);

        verifyEvidence.setOnAction(e -> {

            status.setText(
                    "✓ VERIFIED BY HUMAN REVIEW"
            );

            status.setStyle(
                    "-fx-text-fill: #76D69A;" +
                    "-fx-font-weight: bold;"
            );

            graphButton.setDisable(false);
        });

        rejectEvidence.setOnAction(e -> {

            status.setText(
                    "✕ EVIDENCE REJECTED"
            );

            status.setStyle(
                    "-fx-text-fill: #FF8D8D;" +
                    "-fx-font-weight: bold;"
            );

            graphButton.setDisable(true);
        });

        graphButton.setOnAction(e -> {

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION
            );

            alert.setTitle("Knowledge Graph");
            alert.setHeaderText("Evidence accepted");

            alert.setContentText(
                    "The verified source-backed evidence is now " +
                    "eligible to become a Knowledge Graph relationship.\n\n" +
                    "Source: " + record.title
            );

            alert.showAndWait();
        });

        actions.getChildren().addAll(
                verifyEvidence,
                rejectEvidence,
                graphButton
        );

        decision.getChildren().addAll(
                decisionTitle,
                status,
                actions
        );

        Button back = new Button(
                "← Back to Curator"
        );
        back.setStyle(darkButtonStyle());
        back.setOnAction(e -> showOCR());

        main.getChildren().addAll(
                sourceCard,
                evidenceCard,
                firewall,
                decision,
                back
        );

        content.getChildren().add(main);
    }

    private String extractDocumentText(File file) throws IOException {

        String name = file.getName().toLowerCase();

        if (name.endsWith(".txt")) {
            return Files.readString(
                    file.toPath(),
                    StandardCharsets.UTF_8
            );
        }

        if (name.endsWith(".pdf")) {

            try (PDDocument document = Loader.loadPDF(file)) {

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                String text = stripper.getText(document);

                if (text != null && !text.isBlank()) {
                    return text;
                }

                // Image-only/scanned PDF: render each page and send it to Tesseract.
                return ocrScannedPdf(file, document);
            }
        }

        if (name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".bmp")
                || name.endsWith(".tif")
                || name.endsWith(".tiff")) {

            return runTesseract(file);
        }

        throw new IOException("Unsupported document format. Use PDF, TXT, PNG, JPG, BMP or TIFF.");
    }

    /**
     * OCR a scanned/image-only PDF by rendering each page at 200 DPI and
     * passing the temporary page image to Tesseract.
     */
    private String ocrScannedPdf(File file, PDDocument document) throws IOException {

        StringBuilder allText = new StringBuilder();
        PDFRenderer renderer = new PDFRenderer(document);

        Path tempDir = Files.createTempDirectory("constitutional-genome-ocr-");

        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                java.awt.image.BufferedImage image = renderer.renderImageWithDPI(i, 200);
                Path pageImage = tempDir.resolve("page-" + (i + 1) + ".png");

                javax.imageio.ImageIO.write(image, "png", pageImage.toFile());

                String pageText = runTesseract(pageImage.toFile());

                if (!pageText.isBlank()) {
                    allText.append("\n--- PAGE ").append(i + 1).append(" ---\n");
                    allText.append(pageText.trim()).append("\n");
                }
            }
        } finally {
            try (var paths = Files.walk(tempDir)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); }
                            catch (IOException ignored) { }
                        });
            }
        }

        if (allText.toString().isBlank()) {
            throw new IOException(
                    "This PDF appears to be scanned/image-only, but Tesseract OCR returned no text. "
                    + "Install Tesseract OCR and make sure tesseract.exe is available in PATH."
            );
        }

        return allText.toString();
    }

    /**
     * Runs the local Tesseract command-line OCR engine and returns UTF-8 text.
     * The application does not silently claim OCR success if Tesseract is missing.
     */
    private String runTesseract(File imageFile) throws IOException {

        String executable = findTesseractExecutable();

        if (executable == null) {
            throw new IOException(
                    "Tesseract OCR is not installed or tesseract.exe is not in PATH. "
                    + "Install Tesseract OCR, restart the application, and try the document again."
            );
        }

        ProcessBuilder builder = new ProcessBuilder(
                executable,
                imageFile.getAbsolutePath(),
                "stdout",
                "-l",
                "eng",
                "--psm",
                "3"
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        try {
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IOException(
                        "Tesseract OCR failed with exit code " + exit + ". "
                        + "Check that the selected image is readable and the English language data is installed."
                );
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR process was interrupted.", ex);
        }

        return output.toString().trim();
    }

    private String findTesseractExecutable() {

        String pathValue = System.getenv("PATH");
        if (pathValue != null) {
            for (String folder : pathValue.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                File candidate = new File(folder, "tesseract.exe");
                if (candidate.isFile()) {
                    return candidate.getAbsolutePath();
                }
            }
        }

        String[] commonLocations = {
                "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe",
                System.getenv("LOCALAPPDATA") == null
                        ? ""
                        : System.getenv("LOCALAPPDATA") + "\\Tesseract-OCR\\tesseract.exe"
        };

        for (String location : commonLocations) {
            if (!location.isBlank() && new File(location).isFile()) {
                return location;
            }
        }

        return null;
    }

    private CuratorRecord analyzeDocument(File file, String text) {

        String lower = text.toLowerCase();

        String period = detectHistoricalPeriod(text);

        String language = detectLanguage(text);

        String entities = detectEntities(lower);

        String topics = detectTopics(lower);

        double confidence = calculateConfidence(
                period,
                language,
                entities,
                topics,
                text
        );

        String hash = sha256(file);

        return new CuratorRecord(
                file.getName(),
                documentType(file),
                period,
                language,
                entities,
                topics,
                confidence,
                hash
        );
    }

    private String detectHistoricalPeriod(String text) {

        Pattern range = Pattern.compile(
                "(19[0-9]{2})\\s*[–-]\\s*(19[0-9]{2})"
        );

        Matcher matcher = range.matcher(text);

        if (matcher.find()) {
            return matcher.group(1) + "–" + matcher.group(2);
        }

        Pattern yearPattern = Pattern.compile("\\b(19[0-9]{2})\\b");
        Matcher yearMatcher = yearPattern.matcher(text);

        List<String> foundYears = new ArrayList<>();

        while (yearMatcher.find() && foundYears.size() < 3) {
            if (!foundYears.contains(yearMatcher.group(1))) {
                foundYears.add(yearMatcher.group(1));
            }
        }

        if (!foundYears.isEmpty()) {
            return String.join(", ", foundYears);
        }

        return "Requires source metadata";
    }

    private String detectLanguage(String text) {

        if (text == null || text.isBlank()) {
            return "Unknown";
        }

        int latin = 0;
        int nonWhitespace = 0;

        for (char c : text.toCharArray()) {

            if (Character.isWhitespace(c)) {
                continue;
            }

            nonWhitespace++;

            if ((c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')) {
                latin++;
            }
        }

        if (nonWhitespace == 0) {
            return "Unknown";
        }

        double ratio = (double) latin / nonWhitespace;

        return ratio > 0.70 ? "English (heuristic)" : "Non-English / mixed";
    }

    private String detectEntities(String lower) {

        List<String> entities = new ArrayList<>();

        if (lower.contains("ambedkar")
                || lower.contains("b. r. ambedkar")
                || lower.contains("b r ambedkar")) {
            entities.add("B. R. Ambedkar");
        }

        if (lower.contains("constituent assembly")) {
            entities.add("Constituent Assembly");
        }

        if (lower.contains("constitution")) {
            entities.add("Constitution");
        }

        if (entities.isEmpty()) {
            return "No high-confidence entity detected";
        }

        return String.join(", ", entities);
    }

    private String detectTopics(String lower) {

        List<String> topics = new ArrayList<>();

        if (containsAny(lower, "equality", "equal protection")) {
            topics.add("Equality");
        }

        if (containsAny(lower, "social justice", "justice")) {
            topics.add("Social Justice");
        }

        if (containsAny(lower, "fundamental rights", "rights")) {
            topics.add("Fundamental Rights");
        }

        if (containsAny(lower, "safeguard", "safeguards", "constitutional safeguard")) {
            topics.add("Constitutional Safeguards");
        }

        if (containsAny(lower, "caste", "castes")) {
            topics.add("Caste");
        }

        if (containsAny(lower, "citizenship", "citizen")) {
            topics.add("Citizenship");
        }

        if (topics.isEmpty()) {
            return "No topic matched";
        }

        return String.join(", ", topics);
    }

    private boolean containsAny(String text, String... words) {

        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private double calculateConfidence(
            String period,
            String language,
            String entities,
            String topics,
            String text
    ) {

        double score = 0;

        if (!period.startsWith("Requires")) score += 20;
        if (!language.equals("Unknown")) score += 15;
        if (!entities.startsWith("No high")) score += 20;
        if (!topics.equals("No topic matched")) score += 25;
        if (text.length() > 300) score += 20;

        return Math.min(score, 100);
    }

    private String sha256(File file) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] hash = digest.digest(bytes);

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (IOException | NoSuchAlgorithmException ex) {
            return file.getAbsolutePath();
        }
    }

    private static class CuratorRecord {

        String title;
        String documentType;
        String period;
        String language;
        String entities;
        String topics;
        double confidence;
        String hash;

        CuratorRecord(
                String title,
                String documentType,
                String period,
                String language,
                String entities,
                String topics,
                double confidence,
                String hash
        ) {
            this.title = title;
            this.documentType = documentType;
            this.period = period;
            this.language = language;
            this.entities = entities;
            this.topics = topics;
            this.confidence = confidence;
            this.hash = hash;
        }
    }

    private Label curatorStage(String number, String name, String status) {
        Label stage = new Label(number + "  " + name + "    • " + status);
        stage.setMaxWidth(Double.MAX_VALUE);
        stage.setPadding(new Insets(10, 12, 10, 12));
        stage.setStyle(
                "-fx-background-color: #152238;" +
                "-fx-background-radius: 9;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );
        return stage;
    }

    private void updateCuratorStage(Label stage, String icon, String name, String status) {
        stage.setText(icon + "  " + name + "    • " + status);
        stage.setStyle(
                "-fx-background-color: #17263B;" +
                "-fx-background-radius: 9;" +
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 12px;"
        );
    }

    private String documentType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) return "PDF Document";
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) return "Scanned Image";
        if (name.endsWith(".txt")) return "Text Document";
        return "Unknown Document";
    }

    // =========================================================
    // MULTILINGUAL
    // =========================================================

    // =========================================================
    // AGENT 10 — MULTIMEDIA ARCHIVE & QR
    // =========================================================

    private void showMultimediaArchive() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Multimedia Archive & QR",
                        "Turn a processed historical document into a persistent archive record with a scannable QR identity."
                )
        );

        VBox intake = new VBox(14);
        intake.setPadding(new Insets(22));
        intake.setStyle(cardStyle());

        Label intakeTitle = new Label("ARCHIVE RECORD BUILDER");
        intakeTitle.setStyle(sectionHeadingStyle());

        Label info = new Label(
                "Agent 10 uses the latest document processed by the Autonomous Curator Agent. "
                + "It creates a stable Archive ID from the document text, prepares a multimedia-style record, "
                + "and generates a real QR code containing the archive record URI."
        );
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox actions = new HBox(10);
        Button create = new Button("＋ Create / Refresh Archive Record");
        create.setStyle(goldButtonStyle());
        Button refresh = new Button("↻ Refresh Current Record");
        refresh.setStyle(secondaryButtonStyle());
        actions.getChildren().addAll(create, refresh);

        Label status = new Label(
                latestExtractedText == null || latestExtractedText.isBlank()
                        ? "⚠ No processed document yet. Go to OCR & Upload first."
                        : "● Latest extracted document is available."
        );
        status.setWrapText(true);
        status.setStyle("-fx-text-fill: " + GOLD_LIGHT + "; -fx-font-weight: bold;");

        intake.getChildren().addAll(intakeTitle, info, actions, status);

        HBox body = new HBox(18);
        HBox.setHgrow(body, Priority.ALWAYS);

        VBox record = new VBox(12);
        record.setPadding(new Insets(22));
        record.setStyle(cardStyle());
        HBox.setHgrow(record, Priority.ALWAYS);

        Label recordTitle = new Label("DIGITAL ARCHIVE RECORD");
        recordTitle.setStyle(sectionHeadingStyle());

        GridPane meta = new GridPane();
        meta.setHgap(18);
        meta.setVgap(11);

        Label id = archiveValue();
        Label title = archiveValue();
        Label type = archiveValue();
        Label period = archiveValue();
        Label language = archiveValue();
        Label topics = archiveValue();
        Label evidence = archiveValue();
        Label source = archiveValue();

        addArchiveMeta(meta, 0, "Archive ID", id);
        addArchiveMeta(meta, 1, "Title", title);
        addArchiveMeta(meta, 2, "Document Type", type);
        addArchiveMeta(meta, 3, "Historical Period", period);
        addArchiveMeta(meta, 4, "Language", language);
        addArchiveMeta(meta, 5, "Topics", topics);
        addArchiveMeta(meta, 6, "Evidence Status", evidence);
        addArchiveMeta(meta, 7, "Source", source);

        VBox media = new VBox(9);
        media.setPadding(new Insets(14));
        media.setStyle("-fx-background-color: #0D1727; -fx-background-radius: 10; -fx-border-color: #26344A; -fx-border-radius: 10;");
        Label mediaTitle = new Label("MULTIMEDIA LINKS");
        mediaTitle.setStyle(sectionHeadingStyle());

        Button textButton = new Button("▤ View Extracted Text");
        textButton.setStyle(darkButtonStyle());
        textButton.setOnAction(e -> showArchiveTextPreview());

        Button graphButton = new Button("◇ Open Knowledge Graph");
        graphButton.setStyle(darkButtonStyle());
        graphButton.setOnAction(e -> showGraph());

        Button evidenceButton = new Button("✓ Open Evidence Trail");
        evidenceButton.setStyle(darkButtonStyle());
        evidenceButton.setOnAction(e -> showEvidenceTrail(
                latestArchiveTopics.isBlank() ? "constitutional history" : latestArchiveTopics.split(";")[0].trim()
        ));

        media.getChildren().addAll(mediaTitle, textButton, graphButton, evidenceButton);
        record.getChildren().addAll(recordTitle, meta, media);

        VBox qrCard = new VBox(12);
        qrCard.setPadding(new Insets(22));
        qrCard.setAlignment(Pos.TOP_CENTER);
        qrCard.setPrefWidth(300);
        qrCard.setStyle(cardStyle());

        Label qrTitle = new Label("ARCHIVE QR ID");
        qrTitle.setStyle(sectionHeadingStyle());

        ImageView qrView = new ImageView();
        qrView.setFitWidth(220);
        qrView.setFitHeight(220);
        qrView.setPreserveRatio(true);

        Label qrStatus = new Label("Create an archive record to generate the QR code.");
        qrStatus.setWrapText(true);
        qrStatus.setAlignment(Pos.CENTER);
        qrStatus.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");

        Label uri = new Label();
        uri.setWrapText(true);
        uri.setAlignment(Pos.CENTER);
        uri.setStyle("-fx-text-fill: " + GOLD_LIGHT + "; -fx-font-size: 10px;");

        qrCard.getChildren().addAll(qrTitle, qrView, qrStatus, uri);

        body.getChildren().addAll(record, qrCard);

        Runnable loadRecord = () -> {
            if (latestExtractedText == null || latestExtractedText.isBlank()) {
                status.setText("⚠ No processed document is available. Process a PDF/TXT/image in OCR & Upload first.");
                return;
            }

            try {
                buildLatestArchiveRecord();
                id.setText(latestArchiveId);
                title.setText(latestArchiveTitle);
                type.setText(latestArchiveType);
                period.setText(latestArchivePeriod);
                language.setText(latestArchiveLanguage);
                topics.setText(latestArchiveTopics);
                evidence.setText("HUMAN REVIEW REQUIRED");
                source.setText("Local extracted document text");

                String archiveUri = "constitutional-genome://archive/" + latestArchiveId;
                qrView.setImage(createQrImage(archiveUri, 240, 240));
                uri.setText(archiveUri);
                qrStatus.setText("✓ QR GENERATED — Scan this code to identify the archive record.");
                qrStatus.setStyle("-fx-text-fill: #76D69A; -fx-font-size: 10px; -fx-font-weight: bold;");
                status.setText("✓ Archive record ready: " + latestArchiveId);
                status.setStyle("-fx-text-fill: #76D69A; -fx-font-weight: bold;");
            } catch (Exception ex) {
                status.setText("⚠ QR generation failed: " + ex.getMessage());
                status.setStyle("-fx-text-fill: #FFB36B; -fx-font-weight: bold;");
            }
        };

        create.setOnAction(e -> loadRecord.run());
        refresh.setOnAction(e -> loadRecord.run());

        content.getChildren().addAll(intake, body);

        if (latestExtractedText != null && !latestExtractedText.isBlank()) {
            loadRecord.run();
        }
    }

    private Label archiveValue() {
        Label label = new Label("—");
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + WHITE + "; -fx-font-size: 12px;");
        return label;
    }

    private void addArchiveMeta(GridPane grid, int row, String key, Label value) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: " + GOLD_LIGHT + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        grid.add(k, 0, row);
        grid.add(value, 1, row);
    }

    private String detectDocumentType(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        if (lower.contains("speech") || lower.contains("address")) return "Historical Speech";
        if (lower.contains("debate") || lower.contains("constituent assembly")) return "Assembly Debate";
        if (lower.contains("draft constitution") || lower.contains("draft provisions")) return "Constitutional Draft";
        if (lower.contains("letter")) return "Historical Letter";
        if (lower.contains("article") || lower.contains("writings")) return "Historical Writing";
        return "Historical Document";
    }

    private void buildLatestArchiveRecord() throws Exception {
        String text = latestExtractedText == null ? "" : latestExtractedText.trim();
        if (text.isBlank()) throw new IOException("No extracted text is available.");

        latestArchiveId = "CG-" + sha256Text(text).substring(0, 12).toUpperCase();
        latestArchiveTitle = detectGraphDocumentTitle(text);
        if (latestArchiveTitle == null || latestArchiveTitle.isBlank()) {
            latestArchiveTitle = "Untitled Historical Document";
        }
        latestArchiveType = detectDocumentType(text);
        latestArchivePeriod = detectHistoricalPeriod(text);
        latestArchiveLanguage = detectLanguage(text);
        latestArchiveTopics = String.join("; ", detectGraphTopics(text));
    }

    private String sha256Text(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private WritableImage createQrImage(String data, int width, int height) throws Exception {
        java.util.Map<EncodeHintType, Object> hints = new java.util.HashMap<>();
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix matrix = new MultiFormatWriter().encode(
                data, BarcodeFormat.QR_CODE, width, height, hints
        );

        WritableImage image = new WritableImage(matrix.getWidth(), matrix.getHeight());
        javafx.scene.image.PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                writer.setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return image;
    }

    private void showArchiveTextPreview() {
        content.getChildren().clear();
        content.getChildren().add(pageHeader("Archive Text Preview", "Extracted source text linked to the current archive record."));

        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setStyle(cardStyle());

        TextArea area = new TextArea(latestExtractedText == null ? "" : latestExtractedText);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(28);
        area.setStyle(inputStyle() + "-fx-font-family: 'Consolas';");

        Button back = new Button("← Back to Multimedia Archive");
        back.setStyle(darkButtonStyle());
        back.setOnAction(e -> showMultimediaArchive());

        card.getChildren().addAll(area, back);
        content.getChildren().add(card);
    }

    private void showMultilingual() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Multilingual & Audio Archive",
                        "Preserve source text, prepare a language-specific narration, and play it through the kiosk speaker."
                )
        );

        VBox main = new VBox(18);
        main.setPadding(new Insets(22));

        VBox sourceCard = new VBox(12);
        sourceCard.setPadding(new Insets(20));
        sourceCard.setStyle(cardStyle());

        Label sourceTitle = new Label("🌐 SOURCE & NARRATION TEXT");
        sourceTitle.setStyle(sectionHeadingStyle());

        Label sourceInfo = new Label(
                latestExtractedText.isBlank()
                        ? "No document has been processed yet. You can paste text below for an audio test."
                        : "Using text extracted by the Autonomous Curator Agent."
        );
        sourceInfo.setWrapText(true);
        sourceInfo.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        TextArea sourceText = new TextArea();
        sourceText.setWrapText(true);
        sourceText.setPrefRowCount(8);
        sourceText.setStyle(
                "-fx-control-inner-background: #0B1220;" +
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-prompt-text-fill: #6F7D91;" +
                "-fx-border-color: #29374C;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
        );

        if (!latestExtractedText.isBlank()) {
            sourceText.setText(
                    latestExtractedText.length() > 12000
                            ? latestExtractedText.substring(0, 12000)
                            : latestExtractedText
            );
        }

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label languageLabel = new Label("Narration Language:");
        languageLabel.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-weight: bold;"
        );

        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll(
                "English (en-US)",
                "Hindi (hi-IN)",
                "Tamil (ta-IN)",
                "Telugu (te-IN)",
                "Kannada (kn-IN)",
                "Malayalam (ml-IN)",
                "Marathi (mr-IN)",
                "Bengali (bn-IN)"
        );
        language.setValue("English (en-US)");

        Button loadLatest = new Button("↻ Load Latest Extracted Text");
        loadLatest.setStyle(secondaryButtonStyle());
        loadLatest.setOnAction(e -> {
            if (latestExtractedText.isBlank()) {
                sourceInfo.setText(
                        "No extracted document text is available yet. Process a PDF/TXT document in OCR & Upload first."
                );
            } else {
                sourceText.setText(
                        latestExtractedText.length() > 12000
                                ? latestExtractedText.substring(0, 12000)
                                : latestExtractedText
                );
                sourceInfo.setText(
                        "✓ Latest text loaded from the Autonomous Curator Agent."
                );
            }
        });

        controls.getChildren().addAll(
                languageLabel,
                language,
                loadLatest
        );

        sourceCard.getChildren().addAll(
                sourceTitle,
                sourceInfo,
                sourceText,
                controls
        );

        VBox audioCard = new VBox(14);
        audioCard.setPadding(new Insets(20));
        audioCard.setStyle(cardStyle());

        Label audioTitle = new Label("🔊 AUDIO NARRATION AGENT");
        audioTitle.setStyle(sectionHeadingStyle());

        Label status = new Label(
                "● READY — Select a language and press Play."
        );
        status.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        HBox audioButtons = new HBox(10);

        Button play = new Button("▶ Play Narration");
        play.setStyle(goldButtonStyle());

        Button stop = new Button("■ Stop");
        stop.setStyle(darkButtonStyle());

        Button testVoice = new Button("✓ Test Voice");
        testVoice.setStyle(secondaryButtonStyle());

        audioButtons.getChildren().addAll(
                play,
                stop,
                testVoice
        );

        Label limitation = new Label(
                "Voice availability depends on the Windows speech voices installed on this computer. " +
                "If a selected Indian-language voice is unavailable, the application reports it instead of pretending audio succeeded."
        );
        limitation.setWrapText(true);
        limitation.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 11px;"
        );

        play.setOnAction(e -> {
            String text = sourceText.getText();

            if (text == null || text.isBlank()) {
                status.setText("⚠ Enter or load some text first.");
                status.setStyle(
                        "-fx-text-fill: #FFB36B;" +
                        "-fx-font-weight: bold;"
                );
                return;
            }

            String locale = localeFromLanguage(language.getValue());
            status.setText("● SPEAKING — " + language.getValue());
            status.setStyle(
                    "-fx-text-fill: #76D69A;" +
                    "-fx-font-weight: bold;"
            );

            speakTextAsync(text, locale, status);
        });

        stop.setOnAction(e -> {
            stopNarration();
            status.setText("■ STOPPED");
            status.setStyle(
                    "-fx-text-fill: " + GOLD_LIGHT + ";" +
                    "-fx-font-weight: bold;"
            );
        });

        testVoice.setOnAction(e -> {
            String locale = localeFromLanguage(language.getValue());
            status.setText("● TESTING VOICE — " + language.getValue());
            speakTextAsync(
                    "Constitutional Genome audio narration test.",
                    locale,
                    status
            );
        });

        audioCard.getChildren().addAll(
                audioTitle,
                status,
                audioButtons,
                limitation
        );

        VBox protection = new VBox(10);
        protection.setPadding(new Insets(18));
        protection.setStyle(
                "-fx-background-color: #201B12;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #80652C;" +
                "-fx-border-radius: 12;"
        );

        Label protectionTitle = new Label("🛡 SOURCE PROTECTION");
        protectionTitle.setStyle(
                "-fx-text-fill: " + GOLD + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        Label protectionText = new Label(
                "✓ Original archival text is preserved\n" +
                "✓ Audio is generated from the text supplied to the narration agent\n" +
                "✓ Translation/narration is a derived accessibility layer\n" +
                "⚠ Original source remains authoritative"
        );
        protectionText.setWrapText(true);
        protectionText.setStyle(
                "-fx-text-fill: #E4D7B5;" +
                "-fx-font-size: 12px;"
        );

        protection.getChildren().addAll(
                protectionTitle,
                protectionText
        );

        main.getChildren().addAll(
                sourceCard,
                audioCard,
                protection
        );

        content.getChildren().add(main);
    }

    private String localeFromLanguage(String language) {

        if (language == null) {
            return "en-US";
        }

        if (language.startsWith("Hindi")) return "hi-IN";
        if (language.startsWith("Tamil")) return "ta-IN";
        if (language.startsWith("Telugu")) return "te-IN";
        if (language.startsWith("Kannada")) return "kn-IN";
        if (language.startsWith("Malayalam")) return "ml-IN";
        if (language.startsWith("Marathi")) return "mr-IN";
        if (language.startsWith("Bengali")) return "bn-IN";

        return "en-US";
    }

    private void speakTextAsync(
            String text,
            String locale,
            Label status
    ) {

        stopNarration();

        Thread thread = new Thread(() -> {

            try {
                String textBase64 = Base64.getEncoder().encodeToString(
                        text.getBytes(StandardCharsets.UTF_8)
                );

                String command =
                        "Add-Type -AssemblyName System.Speech; " +
                        "$text=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('" + textBase64 + "')); " +
                        "$culture='" + locale + "'; " +
                        "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$v=$s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq $culture } | Select-Object -First 1; " +
                        "if(-not $v){Write-Output ('VOICE_NOT_INSTALLED:' + $culture); exit 2}; " +
                        "$s.SelectVoice($v.VoiceInfo.Name); " +
                        "$s.Speak($text); " +
                        "$s.Dispose();";

                String encodedCommand = Base64.getEncoder().encodeToString(
                        command.getBytes(StandardCharsets.UTF_16LE)
                );

                ProcessBuilder builder = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-EncodedCommand",
                        encodedCommand
                );

                builder.redirectErrorStream(true);
                ttsProcess = builder.start();

                String output = new String(
                        ttsProcess.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                int exit = ttsProcess.waitFor();

                javafx.application.Platform.runLater(() -> {

                    if (exit == 0) {
                        status.setText("✓ AUDIO COMPLETED — " + locale);
                        status.setStyle(
                                "-fx-text-fill: #76D69A;" +
                                "-fx-font-weight: bold;"
                        );
                    } else {
                        String clean = output == null ? "" : output.trim();
                        if (clean.contains("VOICE_NOT_INSTALLED:")) {
                            status.setText(
                                    "⚠ VOICE NOT INSTALLED — " + locale +
                                    "\nInstall the Windows speech voice for this language, then restart the application."
                            );
                        } else {
                            status.setText(
                                    "⚠ AUDIO COULD NOT START — " + locale +
                                    "\nCheck the Windows speech service and try again."
                            );
                        }
                        status.setStyle(
                                "-fx-text-fill: #FFB36B;" +
                                "-fx-font-weight: bold;"
                        );
                    }
                });

            } catch (Exception ex) {

                javafx.application.Platform.runLater(() -> {
                    status.setText(
                            "⚠ AUDIO ERROR: " + ex.getMessage()
                    );
                    status.setStyle(
                            "-fx-text-fill: #FF8D8D;" +
                            "-fx-font-weight: bold;"
                    );
                });
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void stopNarration() {

        if (ttsProcess != null && ttsProcess.isAlive()) {
            ttsProcess.destroyForcibly();
        }

        ttsProcess = null;
    }

    // =========================================================
    // ABOUT
    // =========================================================

    private void showAbout() {

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "About Constitutional Genome",
                        "Agentic Digital Heritage Intelligence Platform"
                )
        );

        VBox about = new VBox(15);

        about.setPadding(new Insets(25));

        about.setStyle(cardStyle());

        Label title = new Label(
                "FROM DOCUMENTS TO IDEAS.\n"
                + "FROM IDEAS TO CONSTITUTIONAL HISTORY."
        );

        title.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Constitutional Genome is designed as an intelligent "
                + "digital heritage platform for preserving, exploring "
                + "and understanding historical writings, speeches, "
                + "debates and constitutional ideas."
        );

        description.setWrapText(true);

        description.setStyle(
                "-fx-text-fill: #C2CBD7;" +
                "-fx-font-size: 13px;"
        );

        about.getChildren().addAll(
                title,
                description
        );

        content.getChildren().add(about);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void performSearch() {

        String query = searchField.getText();

        if (query == null || query.isBlank()) {
            return;
        }

        content.getChildren().clear();

        content.getChildren().add(
                pageHeader(
                        "Search Results",
                        "Searching the constitutional archive for: " + query
                )
        );

        VBox result = new VBox(15);

        result.setPadding(new Insets(25));

        result.setStyle(cardStyle());

        Label title = new Label(
                "Archive Intelligence"
        );

        title.setStyle(sectionHeadingStyle());

        Label message = new Label(
                "The search interface is ready to connect to the "
                + "semantic retrieval and knowledge graph backend."
        );

        message.setWrapText(true);

        message.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 13px;"
        );

        Label queryLabel = new Label(
                "Query: " + query
        );

        queryLabel.setStyle(
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        result.getChildren().addAll(
                title,
                queryLabel,
                message
        );

        content.getChildren().add(result);
    }

    // =========================================================
    // PAGE HEADER
    // =========================================================

    private VBox pageHeader(
            String title,
            String description
    ) {

        VBox box = new VBox(6);

        Label heading = new Label(title);

        heading.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(description);

        desc.setWrapText(true);

        desc.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        box.getChildren().addAll(
                heading,
                desc
        );

        return box;
    }

    private Label sectionTitle(
            String title,
            String subtitle
    ) {

        Label label = new Label(
                title + "\n" + subtitle
        );

        label.setStyle(
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;"
        );

        return label;
    }

    // =========================================================
    // IMAGE LOADER
    // =========================================================

    private ImageView loadImage(
            String fileName,
            double width,
            double height
    ) {

        String path = "/images/" + fileName;

        var resource = getClass().getResource(path);

        if (resource == null) {

            System.out.println(
                    "WARNING: Image not found: " + path
            );

            return createImagePlaceholder(
                    width,
                    height
            );
        }

        Image image = new Image(
                resource.toExternalForm()
        );

        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        Rectangle clip = new Rectangle(
                width,
                height
        );

        clip.setArcWidth(16);
        clip.setArcHeight(16);

        imageView.setClip(clip);

        return imageView;
    }

    private ImageView createImagePlaceholder(
            double width,
            double height
    ) {

        // Empty ImageView if resource doesn't exist.
        // The application continues running instead of crashing.
        ImageView placeholder = new ImageView();

        placeholder.setFitWidth(width);
        placeholder.setFitHeight(height);

        return placeholder;
    }

    // =========================================================
    // UPLOAD
    // =========================================================

    private void openUploadDialog(Stage stage) {

        FileChooser chooser = new FileChooser();

        chooser.setTitle(
                "Upload Historical Document"
        );

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Documents",
                        "*.pdf",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.txt"
                ),
                new FileChooser.ExtensionFilter(
                        "All Files",
                        "*.*"
                )
        );

        File file = chooser.showOpenDialog(stage);

        if (file != null) {

            showOCR();

            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION
            );

            alert.setTitle(
                    "Document Selected"
            );

            alert.setHeaderText(
                    "Document ready for processing"
            );

            alert.setContentText(
                    file.getName()
            );

            alert.showAndWait();
        }
    }

    // =========================================================
    // STYLES
    // =========================================================

    private String cardStyle() {

        return
                "-fx-background-color: " + CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #27364A;" +
                "-fx-border-radius: 14;";
    }

    private String sectionHeadingStyle() {

        return
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;";
    }

    private String sidebarNormalStyle() {

        return
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #AAB5C5;" +
                "-fx-background-radius: 9;" +
                "-fx-font-size: 13px;" +
                "-fx-alignment: CENTER-LEFT;";
    }

    private String sidebarHoverStyle() {

        return
                "-fx-background-color: #172238;" +
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-background-radius: 9;" +
                "-fx-font-size: 13px;" +
                "-fx-alignment: CENTER-LEFT;";
    }

    private String goldButtonStyle() {

        return
                "-fx-background-color: " + GOLD + ";" +
                "-fx-text-fill: #101827;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 9;" +
                "-fx-padding: 10px 17px;" +
                "-fx-cursor: hand;";
    }

    private String darkButtonStyle() {

        return
                "-fx-background-color: #172238;" +
                "-fx-text-fill: " + WHITE + ";" +
                "-fx-border-color: #35445A;" +
                "-fx-border-radius: 9;" +
                "-fx-background-radius: 9;" +
                "-fx-padding: 10px 15px;" +
                "-fx-cursor: hand;";
    }

    private String secondaryButtonStyle() {

        return
                "-fx-background-color: #172238;" +
                "-fx-text-fill: " + GOLD_LIGHT + ";" +
                "-fx-border-color: #35445A;" +
                "-fx-border-radius: 9;" +
                "-fx-background-radius: 9;" +
                "-fx-padding: 9px 14px;" +
                "-fx-font-size: 11px;" +
                "-fx-cursor: hand;";
    }

    private String inputStyle() {

        return
                "-fx-background-color: #0C1524;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #68768A;" +
                "-fx-border-color: #2C3B50;" +
                "-fx-border-radius: 9;" +
                "-fx-background-radius: 9;" +
                "-fx-padding: 12px;";
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {
        launch(args);
    }
}