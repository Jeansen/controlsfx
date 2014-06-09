/*
 * @(#)FontPopupContent.java 5/19/2013
 *
 * Copyright 2002 - 2013 JIDE Software Inc. All rights reserved.
 */

package org.controlsfx.jidefx.scene.control.field.popup;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;

import org.controlsfx.jidefx.scene.control.decoration.DecorationPane;
import org.controlsfx.jidefx.scene.control.field.NumberField;
import org.controlsfx.jidefx.scene.control.searchable.ComboBoxSearchable;
import org.controlsfx.jidefx.utils.FXUtils;
import org.controlsfx.jidefx.utils.FontUtils;
import org.controlsfx.jidefx.utils.LazyLoadUtils;
import org.tbee.javafx.scene.layout.MigPane;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

/**
 * The content of the Popup to edit a Font.
 */
public class FontPopupContent extends DecorationPane implements PopupContent<Font> {
    private static final String STYLE_CLASS_DEFAULT = "popup-content"; //NON-NLS

    private ComboBox<String> _fontFamilyComboBox;
    private NumberField _sizeSpinner;
    private ChoiceBox<String> _styleChoiceBox;
    private Label _previewLabel;

    public FontPopupContent(Font font) {
        super(new MigPane(new LC().maxWidth("230px").maxHeight("180px").insets("10 10 10 10"), //NON-NLS
                new AC().align("right", 0).size("160px", 1).fill(1).grow(1).gap("10px"), //NON-NLS
                new AC().gap("6px"))); //NON-NLS
        getStylesheets().add(PopupContent.class.getResource("PopupContent.css").toExternalForm()); //NON-NLS
        getStyleClass().add(STYLE_CLASS_DEFAULT);
        initializeComponents(font == null ? Font.getDefault() : font);
        installListeners();
        updateValues();
    }

    private void initializeComponents(Font font) {
        // create components
        BorderPane previewPanel = new BorderPane();
        previewPanel.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

        _previewLabel = new Label(getResourceString("preview"));
        _previewLabel.setPrefSize(210, 60);
        _previewLabel.setAlignment(Pos.CENTER);
        _previewLabel.setFont(font);
        previewPanel.setCenter(_previewLabel);

        _fontFamilyComboBox = new ComboBox<>();
        LazyLoadUtils.install(_fontFamilyComboBox, new Callback<ComboBox<String>, ObservableList<String>>() {
            @Override
            public ObservableList<String> call(ComboBox<String> comboBox) {
                return FXCollections.observableArrayList(Font.getFamilies());
            }
        });
        _fontFamilyComboBox.setPrefWidth(160); // Without setting it, it will come up with a small size then expand when setItems is called
        new ComboBoxSearchable<>(_fontFamilyComboBox);

        DecimalFormat integerFormat = (DecimalFormat) DecimalFormat.getNumberInstance();
        integerFormat.setMaximumIntegerDigits(3);
        integerFormat.setMaximumFractionDigits(1);

        _sizeSpinner = new NumberField();
        _sizeSpinner.setDecimalFormat(integerFormat);
        _sizeSpinner.setSpinnersVisible(true);
        _sizeSpinner.setValue(font.getSize());

        _styleChoiceBox = new ChoiceBox<>();

        // layout components
        MigPane content = (MigPane) getContent();
        content.add(previewPanel, new CC().grow().span(2).wrap().gapBottom("6px")); //NON-NLS
        content.add(new Label(getResourceString("font")));
        content.add(_fontFamilyComboBox, new CC().wrap());
        Label sizeLabel = new Label(getResourceString("size"));
        content.add(sizeLabel);
        content.add(_sizeSpinner, new CC().split().maxWidth("60px").wrap()); //NON-NLS
        _sizeSpinner.installAdjustmentMouseHandler(sizeLabel);
        content.add(new Label(getResourceString("style")));
        content.add(_styleChoiceBox, new CC().grow().wrap());
    }

    private void updateValues() {
        Font font = getValue();
        _fontFamilyComboBox.setValue(font.getFamily());
        _styleChoiceBox.setValue(font.getStyle());
    }

    private void installListeners() {
        _fontFamilyComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                updateStyleChoiceBox();
            }
        });
        _sizeSpinner.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                updateStyleChoiceBox();
            }
        });
        _styleChoiceBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                _previewLabel.setFont(FontUtils.createFont(_fontFamilyComboBox.getValue(), newValue, _sizeSpinner.getValue().doubleValue()));
            }
        });
    }

    private void updateStyleChoiceBox() {
        LazyLoadUtils.install(_styleChoiceBox, new Callback<ChoiceBox<String>, ObservableList<String>>() {
            @Override
            public ObservableList<String> call(final ChoiceBox<String> comboBox) {
                final List<String> supportedFontStyle = FontUtils.getSupportedFontStyles(_fontFamilyComboBox.getValue(), _sizeSpinner.getValue().doubleValue());
                FXUtils.runThreadSafe(new Runnable() {
                    @Override
                    public void run() {
                        comboBox.setDisable(supportedFontStyle.size() <= 1);
                    }
                });
                return FXCollections.observableArrayList(supportedFontStyle);
            }
        });
    }

    @Override
    public final Font getValue() {
        return _previewLabel.getFont();
    }

    @Override
    public final void setValue(Font value) {
        _previewLabel.setFont(value);
    }

    @Override
    public final ObjectProperty<Font> valueProperty() {
        return _previewLabel.fontProperty();
    }

    /**
     * Gets the localized string from resource bundle. Subclass can override it to provide its own string. Available
     * keys are defined in grid.properties that begins with "Filter." and lucene.properties that begins with "Lucene".
     *
     * @param key the key to the resource.
     * @return the localized string.
     */
    public String getResourceString(String key) {
        if (key == null) {
            return "";
        }
        return PopupsResource.getResourceBundle(Locale.getDefault()).getString(key);
    }
}
