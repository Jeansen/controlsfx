/*
 * @(#)Rectangle2DField.java 5/19/2013
 *
 * Copyright 2002 - 2013 JIDE Software Inc. All rights reserved.
 */

package org.controlsfx.jidefx.scene.control.field;

import javafx.geometry.Rectangle2D;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.jidefx.scene.control.field.popup.PopupContent;
import org.controlsfx.jidefx.scene.control.field.popup.ValuesPopupContent;
import org.controlsfx.jidefx.scene.control.field.verifier.NumberValuePatternVerifier;
import org.controlsfx.jidefx.utils.converter.ConverterContext;
import org.controlsfx.jidefx.utils.converter.javafx.Rectangle2DConverter;

/**
 * {@code Rectangle2DField} is a {@code FormattedTextField} for {@link Rectangle2D}.
 */
public class Rectangle2DField extends PopupField<Rectangle2D> {
    public Rectangle2DField() {
    }

    private static final String STYLE_CLASS_DEFAULT = "rectangle-2d-field"; //NON-NLS

    @Override
    protected void initializeStyle() {
        super.initializeStyle();
        getStyleClass().addAll(STYLE_CLASS_DEFAULT);
    }

    @Override
    protected void initializePattern() {
        super.initializePattern();
        setStringConverter(new Rectangle2DConverter() {
            @Override
            protected String toString(int i, Double o, ConverterContext context) {
                if (o == null) return "";
                return o.toString();
            }

            @Override
            protected Double fromString(int i, String s, ConverterContext context) {
                if (s == null || s.trim().isEmpty()) return null;
                return Double.valueOf(s);
            }
        }.toStringConverter());

        getPatternVerifiers().put("X", new NumberValuePatternVerifier<Rectangle2D>() { //NON-NLS
            @Override
            public Double toTargetValue(Rectangle2D fieldValue) {
                return fieldValue.getMinX();
            }

            @Override
            public Rectangle2D fromTargetValue(Rectangle2D previousFieldValue, Number value) {
                double x = value.doubleValue();
                return previousFieldValue != null ? new Rectangle2D(x, previousFieldValue.getMinY(), previousFieldValue.getWidth(), previousFieldValue.getHeight())
                        : new Rectangle2D(x, 0, 0, 0);
            }
        });
        getPatternVerifiers().put("Y", new NumberValuePatternVerifier<Rectangle2D>() { //NON-NLS
            @Override
            public Double toTargetValue(Rectangle2D fieldValue) {
                return fieldValue.getMinY();
            }

            @Override
            public Rectangle2D fromTargetValue(Rectangle2D previousFieldValue, Number value) {
                double y = value.doubleValue();
                return previousFieldValue != null ? new Rectangle2D(previousFieldValue.getMinX(), y, previousFieldValue.getWidth(), previousFieldValue.getHeight())
                        : new Rectangle2D(0, y, 0, 0);
            }
        });
        getPatternVerifiers().put("Width", new NumberValuePatternVerifier<Rectangle2D>(0, Double.MAX_VALUE) { //NON-NLS
            @Override
            public Double toTargetValue(Rectangle2D fieldValue) {
                return fieldValue.getWidth();
            }

            @Override
            public Rectangle2D fromTargetValue(Rectangle2D previousFieldValue, Number value) {
                double width = value.doubleValue();
                return previousFieldValue != null ? new Rectangle2D(previousFieldValue.getMinX(), previousFieldValue.getMinY(), width, previousFieldValue.getHeight())
                        : new Rectangle2D(0, 0, width, 0);
            }
        });
        getPatternVerifiers().put("Height", new NumberValuePatternVerifier<Rectangle2D>(0, Double.MAX_VALUE) { //NON-NLS
            @Override
            public Double toTargetValue(Rectangle2D fieldValue) {
                return fieldValue.getHeight();
            }

            @Override
            public Rectangle2D fromTargetValue(Rectangle2D previousFieldValue, Number value) {
                double height = value.doubleValue();
                return previousFieldValue != null ? new Rectangle2D(previousFieldValue.getMinX(), previousFieldValue.getMinY(), previousFieldValue.getWidth(), height)
                        : new Rectangle2D(0, 0, 0, height);
            }
        });
        setPattern("X; Y; Width; Height"); //NON-NLS
    }

    @Override
    protected void initializeTextField() {
        super.initializeTextField();

        setPopupContentFactory(new Callback<Rectangle2D, PopupContent<Rectangle2D>>() {
            @Override
            public PopupContent<Rectangle2D> call(Rectangle2D param) {
                ValuesPopupContent<Rectangle2D, Double> popupContent = new ValuesPopupContent<Rectangle2D, Double>(new String[]{getResourceString("x"), getResourceString("y"), getResourceString("width"), getResourceString("height")}) {
                    @Override
                    public List<Double> toValues(Rectangle2D value) {
                        ArrayList<Double> list = new ArrayList<>(4);
                        list.add(value.getMinX());
                        list.add(value.getMinY());
                        list.add(value.getWidth());
                        list.add(value.getHeight());
                        return list;
                    }

                    @Override
                    public Rectangle2D fromValues(List<Double> values) {
                        return new Rectangle2D(values.get(0), values.get(1), values.get(2), values.get(3));
                    }

                    @Override
                    public FormattedTextField<Double> createTextField(String label) {
                        int fieldIndex = getFieldIndex(label);
                        if (fieldIndex == 2 || fieldIndex == 3) // for width and height
                            return new DoubleField(0, Double.MAX_VALUE);
                        else return new DoubleField();
                    }
                };
                popupContent.setValue(getValue());
                return popupContent;
            }
        });

    }
}
