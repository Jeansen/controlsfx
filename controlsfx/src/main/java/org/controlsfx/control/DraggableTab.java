/**
 * Copyright (c) 2014, ControlsFX All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of ControlsFX, any associated
 * website, nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.controlsfx.control;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * A draggable tab that can optionally be detached from its tab pane and shown
 * in a separate window. This can be added to any normal TabPane, however a
 * TabPane with draggable tabs must *only* have DraggableTabs, normal tabs and
 * DrragableTabs mixed will cause issues!
 */
public class DraggableTab extends Tab {

    private static final Set<TabPane> tabPanes = new HashSet<>();
    private Label nameLabel;
    private final Text dragText;
    private static final Stage markerStage;
    private final Stage dragStage;
    private final SimpleBooleanProperty detachableProperty;
    private final SimpleObjectProperty<Callable<Boolean>> callbackProperty;

    static {
        markerStage = new Stage();
        markerStage.initStyle(StageStyle.UNDECORATED);
        Rectangle dummy = new Rectangle(3, 10, Color.web("#555555"));
        StackPane markerStack = new StackPane();
        markerStack.getChildren().add(dummy);
        markerStage.setScene(new Scene(markerStack));
    }

    /**
     * Create a new draggable tab. This can be added to any normal TabPane,
     * however a TabPane with draggable tabs must *only* have DraggableTabs,
     * normal tabs and DragableTabs mixed will cause issues!
     * <p>
     * @param text the text to appear on the tag label.
     */
    public DraggableTab(String text) {
        super(text);
        setupLabelRef();
        detachableProperty = new SimpleBooleanProperty(true);
        callbackProperty = new SimpleObjectProperty<>();
        dragStage = new Stage();
        dragStage.initStyle(StageStyle.UNDECORATED);
        StackPane dragStagePane = new StackPane();
        dragStagePane.setStyle("-fx-background-color:#DDDDDD;");
        dragText = new Text(text);
        dragText.textProperty().bind(textProperty());
        StackPane.setAlignment(dragText, Pos.CENTER);
        dragStagePane.getChildren().add(dragText);
        dragStage.setScene(new Scene(dragStagePane));
    }

    /**
     * Set the callback that will be called upon attempting to move or detach
     * this tab. If the callback returns false then the tab will be returned to
     * its original position, if it returns true the drag operation will
     * complete.
     *
     * @param callback the callback to execute to determine whether or not to
     * move or detach this tab.
     */
    public final void setMoveCallback(Callable<Boolean> callback) {
        callbackProperty.set(callback);
    }

    /**
     * Get the callback that will be called upon attempting to move or detach
     * this tab. If the callback returns false then the tab will be returned to
     * its original position, if it returns true the drag operation will
     * complete.
     *
     * @return the callback to execute to determine whether or not to move or
     * detach this tab.
     */
    public final Callable<Boolean> getMoveCallback() {
        return callbackProperty.get();
    }

    /**
     * Specifies the callback that will be called upon attempting to move or
     * detach this tab. If the callback returns false then the tab will be
     * returned to its original position, if it returns true the drag operation
     * will complete.
     *
     * @return the callback property.
     */
    public final SimpleObjectProperty<Callable<Boolean>> callbackProperty() {
        return callbackProperty;
    }

    /**
     * Set whether it's possible to detach the tab from its pane and move it to
     * another pane or another window. Defaults to true.
     * <p>
     * @param detachable true if the tab should be detachable, false otherwise.
     */
    public final void setDetachable(boolean detachable) {
        detachableProperty.set(detachable);
    }

    /**
     * Determine whether it's possible to detach the tab from its pane and move
     * it to another pane or another window.
     * <p>
     * @return the state of the detachable property.
     */
    public final boolean isDetachable() {
        return detachableProperty.get();
    }

    /**
     * Specifies whether it's possible to detach the tab from its pane and move
     * it to another pane or another window. Defaults to true.
     *
     * @return the detachable property.
     */
    public final BooleanProperty detachableProperty() {
        return detachableProperty;
    }

    private InsertData getInsertData(Point2D screenPoint) {
        for (TabPane tabPane : tabPanes) {
            Rectangle2D tabAbsolute = getAbsoluteRect(tabPane);
            if (tabAbsolute.contains(screenPoint)) {
                int tabInsertIndex = 0;
                if (!tabPane.getTabs().isEmpty()) {
                    Rectangle2D firstTabRect = getAbsoluteRect(tabPane.getTabs().get(0));
                    if (firstTabRect.getMaxY() + 60 < screenPoint.getY() || firstTabRect.getMinY() > screenPoint.getY()) {
                        return null;
                    }
                    Rectangle2D lastTabRect = getAbsoluteRect(tabPane.getTabs().get(tabPane.getTabs().size() - 1));
                    if (screenPoint.getX() < (firstTabRect.getMinX() + firstTabRect.getWidth() / 2)) {
                        tabInsertIndex = 0;
                    } else if (screenPoint.getX() > (lastTabRect.getMaxX() - lastTabRect.getWidth() / 2)) {
                        tabInsertIndex = tabPane.getTabs().size();
                    } else {
                        for (int i = 0; i < tabPane.getTabs().size() - 1; i++) {
                            Tab leftTab = tabPane.getTabs().get(i);
                            Tab rightTab = tabPane.getTabs().get(i + 1);
                            if (leftTab instanceof DraggableTab && rightTab instanceof DraggableTab) {
                                Rectangle2D leftTabRect = getAbsoluteRect(leftTab);
                                Rectangle2D rightTabRect = getAbsoluteRect(rightTab);
                                if (betweenX(leftTabRect, rightTabRect, screenPoint.getX())) {
                                    tabInsertIndex = i + 1;
                                    break;
                                }
                            }
                        }
                    }
                }
                return new InsertData(tabInsertIndex, tabPane);
            }
        }
        return null;
    }

    private boolean useLabel(Label label) {
        if (label == null) {
            return false;
        }
        nameLabel = label;
        nameLabel.setOnMouseDragged(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                dragStage.setWidth(nameLabel.getWidth() + 10);
                dragStage.setHeight(nameLabel.getHeight() + 10);
                dragStage.setX(t.getScreenX());
                dragStage.setY(t.getScreenY());
                dragStage.show();
                Point2D screenPoint = new Point2D(t.getScreenX(), t.getScreenY());
                tabPanes.add(getTabPane());
                InsertData data = getInsertData(screenPoint);
                if (data == null || data.getInsertPane().getTabs().isEmpty()) {
                    markerStage.hide();
                } else {
                    int index = data.getIndex();
                    boolean end = false;
                    if (index == data.getInsertPane().getTabs().size()) {
                        end = true;
                        index--;
                    }
                    Rectangle2D rect = getAbsoluteRect(data.getInsertPane().getTabs().get(index));
                    if (end) {
                        markerStage.setX(rect.getMaxX() + 13);
                    } else {
                        markerStage.setX(rect.getMinX());
                    }
                    markerStage.setY(rect.getMaxY() + 10);
                    markerStage.show();
                }
            }
        });
        nameLabel.setOnMouseReleased(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                markerStage.hide();
                dragStage.hide();
                try {
                    if (callbackProperty.get() != null && !callbackProperty.get().call()) {
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //Exception in callback
                }
                if (!t.isStillSincePress()) {
                    Point2D screenPoint = new Point2D(t.getScreenX(), t.getScreenY());
                    TabPane oldTabPane = getTabPane();
                    int oldIndex = oldTabPane.getTabs().indexOf(DraggableTab.this);
                    tabPanes.add(oldTabPane);
                    InsertData insertData = getInsertData(screenPoint);
                    if (insertData != null) {
                        int addIndex = insertData.getIndex();
                        if (oldTabPane == insertData.getInsertPane() && oldTabPane.getTabs().size() == 1) {
                            return;
                        }
                        oldTabPane.getTabs().remove(DraggableTab.this);
                        if (oldIndex < addIndex && oldTabPane == insertData.getInsertPane()) {
                            addIndex--;
                        }
                        if (addIndex > insertData.getInsertPane().getTabs().size()) {
                            addIndex = insertData.getInsertPane().getTabs().size();
                        }
                        insertData.getInsertPane().getTabs().add(addIndex, DraggableTab.this);
                        insertData.getInsertPane().selectionModelProperty().get().select(addIndex);
                        setupLabelRef();
                        return;
                    }
                    if (!detachableProperty.get()) {
                        return;
                    }
                    final Stage newStage = new Stage();
                    final TabPane pane = new TabPane();
                    tabPanes.add(pane);
                    newStage.setOnHiding(new EventHandler<WindowEvent>() {

                        @Override
                        public void handle(WindowEvent t) {
                            tabPanes.remove(pane);
                        }
                    });
                    getTabPane().getTabs().remove(DraggableTab.this);
                    pane.getTabs().add(DraggableTab.this);
                    pane.getTabs().addListener(new ListChangeListener<Tab>() {

                        @Override
                        public void onChanged(ListChangeListener.Change<? extends Tab> change) {
                            if (pane.getTabs().isEmpty()) {
                                newStage.hide();
                            }
                        }
                    });
                    newStage.setScene(new Scene(pane));
                    newStage.initStyle(StageStyle.UTILITY);
                    newStage.setX(t.getScreenX());
                    newStage.setY(t.getScreenY());
                    newStage.show();
                    pane.requestLayout();
                    pane.requestFocus();
                    setupLabelRef();
                }
            }
        });
        return true;
    }

    private void setupLabelRef() {
        if(getTabPane()==null) {
            tabPaneProperty().addListener(new ChangeListener<TabPane>() {

                @Override
                public void changed(ObservableValue<? extends TabPane> ov, TabPane oldPane, final TabPane newPane) {
                    setupUseLabelListeners(newPane);
                    tabPaneProperty().removeListener(this);
                }
            });
        }
        else {
            setupUseLabelListeners(getTabPane());
        }
    }
    
    private void setupUseLabelListeners(final TabPane tabPane) {
        Scene scene = tabPane.getScene();
        if (scene != null) {
            useLabel(getLabelFromSkin(tabPane.getSkin().getNode()));
        }
        if (nameLabel == null) {
            tabPane.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {

                @Override
                public void onChanged(ListChangeListener.Change<? extends Node> change) {
                    if (useLabel(getLabelFromSkin(tabPane.getSkin().getNode()))) {
                        tabPane.getChildrenUnmodifiable().removeListener(this);
                    }
                }
            });
        }
    }

    private Label getLabelFromSkin(final Node p) {
        if (p instanceof Label) {
            Label label = (Label)p;
            if (label.getText().equals(getText()) && label.getScene()!=null) {
                return label;
            }
        }
        if (p instanceof Parent) {
            for (Node node : ((Parent) p).getChildrenUnmodifiable()) {
                Label ret = getLabelFromSkin(node);
                if(ret!=null) {
                    return ret;
                }
            }
        }
        return null;
    }

    private Rectangle2D getAbsoluteRect(Control node) {
        return new Rectangle2D(node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getX() + node.getScene().getWindow().getX(),
                node.localToScene(node.getLayoutBounds().getMinX(), node.getLayoutBounds().getMinY()).getY() + node.getScene().getWindow().getY(),
                node.getWidth(),
                node.getHeight());
    }

    private Rectangle2D getAbsoluteRect(Tab tab) {
        DraggableTab draggable = (DraggableTab)tab;
        Control node = draggable.nameLabel;
        if(node.getScene() == null) {
            draggable.setupLabelRef();
        }
        node = draggable.nameLabel;
        return getAbsoluteRect(node);
    }

    private boolean betweenX(Rectangle2D r1, Rectangle2D r2, double xPoint) {
        double lowerBound = r1.getMinX() + r1.getWidth() / 2;
        double upperBound = r2.getMaxX() - r2.getWidth() / 2;
        return xPoint >= lowerBound && xPoint <= upperBound;
    }

    private static class InsertData {

        private final int index;
        private final TabPane insertPane;

        public InsertData(int index, TabPane insertPane) {
            this.index = index;
            this.insertPane = insertPane;
        }

        public int getIndex() {
            return index;
        }

        public TabPane getInsertPane() {
            return insertPane;
        }

    }
}
