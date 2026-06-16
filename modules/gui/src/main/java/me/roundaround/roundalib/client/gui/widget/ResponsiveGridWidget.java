package me.roundaround.roundalib.client.gui.widget;

import me.roundaround.roundalib.client.gui.util.Coords;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ResponsiveGridWidget extends AbstractContainerWidget implements Layout {
  private final List<CellWidget<?>> cells = new ArrayList<>();
  private final List<LayoutElement> widgets = new ArrayList<>();
  private final LayoutSettings cellPositioner = LayoutSettings.defaults();
  private final Axis flowAxis;

  private int columnWidth;
  private int rowHeight;
  private int rowSpacing = GuiUtil.PADDING;
  private int columnSpacing = GuiUtil.PADDING;
  private Float relative = null;
  private int contentHeight = 0;

  public ResponsiveGridWidget(int width, int height, int columnWidth, int rowHeight) {
    this(0, 0, width, height, columnWidth, rowHeight, Axis.HORIZONTAL);
  }

  public ResponsiveGridWidget(int x, int y, int width, int height, int columnWidth, int rowHeight) {
    this(x, y, width, height, columnWidth, rowHeight, Axis.HORIZONTAL);
  }

  public ResponsiveGridWidget(int width, int height, int columnWidth, int rowHeight, Axis flowAxis) {
    this(0, 0, width, height, columnWidth, rowHeight, flowAxis);
  }

  public ResponsiveGridWidget(int x, int y, int width, int height, int columnWidth, int rowHeight, Axis flowAxis) {
    super(x, y, width, height, CommonComponents.EMPTY, AbstractScrollArea.defaultSettings(rowHeight / 2));

    this.columnWidth = columnWidth;
    this.rowHeight = rowHeight;
    this.flowAxis = flowAxis;
  }

  @Override
  public List<? extends GuiEventListener> children() {
    return this.widgets.stream()
        .filter((widget) -> widget instanceof GuiEventListener)
        .map((widget) -> (GuiEventListener) widget)
        .toList();
  }

  @Override
  public void visitChildren(Consumer<LayoutElement> consumer) {
    this.widgets.forEach(consumer);
  }

  @Override
  public void arrangeElements() {
    int maxCount = this.getMaxCountForMainAxis();
    int main = 0;
    int other = 0;

    Coords offset = this.getCenteredOffset();

    int numRows = 0;
    for (CellWidget<?> cell : this.cells) {
      int column = this.flowAxis == Axis.HORIZONTAL ? main : other;
      int row = this.flowAxis == Axis.HORIZONTAL ? other : main;

      numRows = Math.max(numRows, row);

      cell.setPosition(
          this.calcPosX(column) + offset.x(),
          this.calcPosY(row) + offset.y(),
          this.columnWidth,
          this.rowHeight
      );

      main++;
      if (main > maxCount - 1) {
        main = 0;
        other++;
      }
    }

    Layout.super.arrangeElements();

    this.contentHeight = numRows * (this.rowHeight + this.rowSpacing) + this.rowSpacing;
  }

  @Override
  protected int contentHeight() {
    return this.contentHeight;
  }

  @Override
  protected double scrollRate() {
    return (double) (this.rowHeight + this.rowSpacing) / 2.0;
  }

  private int getMaxCountForMainAxis() {
    return switch (this.flowAxis) {
      case HORIZONTAL -> (this.width + this.columnSpacing) / (this.columnWidth + this.columnSpacing);
      case VERTICAL -> (this.height + this.rowSpacing) / (this.rowHeight + this.rowSpacing);
    };
  }

  private Coords getCenteredOffset() {
    if (this.relative == null) {
      return Coords.zero();
    }

    int maxCount = this.getMaxCountForMainAxis();
    int actualCount = Math.min(maxCount, this.cells.size());

    if (actualCount <= 0) {
      return Coords.zero();
    }

    int cellSize = this.flowAxis == Axis.HORIZONTAL ? this.columnWidth : this.rowHeight;
    int cellSpacing = this.flowAxis == Axis.HORIZONTAL ? this.columnSpacing : this.rowSpacing;
    int used = actualCount * cellSize + (actualCount - 1) * cellSpacing;
    int available = this.flowAxis == Axis.HORIZONTAL ? this.width : this.height;
    int offset = (int) ((available - used) * this.relative);

    return this.flowAxis == Axis.HORIZONTAL ? Coords.of(offset, 0) : Coords.of(0, offset);
  }

  private int calcPosX(int column) {
    return this.getX() + column * (this.columnWidth + this.columnSpacing);
  }

  private int calcPosY(int row) {
    return this.getY() + row * (this.rowHeight + this.rowSpacing);
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput builder) {
    this.widgets.forEach((widget) -> {
      if (widget instanceof NarrationSupplier narratable) {
        narratable.updateNarration(builder);
      }
    });
  }

  @Override
  protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    this.widgets.forEach((widget) -> {
      if (widget instanceof Renderable drawable) {
        drawable.extractRenderState(context, mouseX, mouseY, delta);
      }
    });
  }

  public ResponsiveGridWidget rowSpacing(int rowSpacing) {
    this.rowSpacing = rowSpacing;
    return this;
  }

  public ResponsiveGridWidget columnSpacing(int columnSpacing) {
    this.columnSpacing = columnSpacing;
    return this;
  }

  public ResponsiveGridWidget spacing(int spacing) {
    this.rowSpacing = spacing;
    this.columnSpacing = spacing;
    return this;
  }

  public ResponsiveGridWidget alignedStart() {
    return this.aligned(0f);
  }

  public ResponsiveGridWidget centered() {
    return this.aligned(0.5f);
  }

  public ResponsiveGridWidget alignedEnd() {
    return this.aligned(1f);
  }

  public ResponsiveGridWidget aligned(float relative) {
    this.relative = relative;
    return this;
  }

  public void setColumnWidth(int columnWidth) {
    this.columnWidth = columnWidth;
  }

  public void setRowHeight(int rowHeight) {
    this.rowHeight = rowHeight;
  }

  public void setCellDimensions(int columnWidth, int rowHeight) {
    this.columnWidth = columnWidth;
    this.rowHeight = rowHeight;
  }

  public LayoutSettings copyCellPositioner() {
    return this.cellPositioner.copy();
  }

  public LayoutSettings getCellPositioner() {
    return this.cellPositioner;
  }

  public <T extends LayoutElement> CellWidget<T> add(T widget) {
    return this.add(widget, this.copyCellPositioner());
  }

  public <T extends LayoutElement> CellWidget<T> add(T widget, Consumer<LayoutSettings> consumer) {
    return this.add(widget, Util.make(this.copyCellPositioner(), consumer));
  }

  public <T extends LayoutElement> CellWidget<T> add(T widget, LayoutSettings positioner) {
    this.widgets.add(widget);
    CellWidget<T> cell = new CellWidget<>(widget, positioner);
    this.cells.add(cell);
    return cell;
  }

  public void clear() {
    this.widgets.clear();
    this.cells.clear();
  }

  @Override
  public void removeChildren() {
    this.clear();
  }

  @Environment(EnvType.CLIENT)
  public enum Axis {
    HORIZONTAL, VERTICAL;
  }

  @Environment(EnvType.CLIENT)
  public static class CellWidget<T extends LayoutElement> {
    private final T child;
    private final LayoutSettings.LayoutSettingsImpl positioner;

    public CellWidget(T child, LayoutSettings positioner) {
      this.child = child;
      this.positioner = positioner.getExposed();
    }

    public T getChild() {
      return this.child;
    }

    public void setPosition(int x, int y, int width, int height) {
      int marginLeft = this.positioner.paddingLeft;
      int childLeft = x + width - this.child.getWidth() - this.positioner.paddingRight;
      int offsetX = Mth.lerpInt(this.positioner.xAlignment, marginLeft, childLeft);

      int marginTop = this.positioner.paddingTop;
      int childTop = y + height - this.child.getHeight() - this.positioner.paddingBottom;
      int offsetY = Mth.lerpInt(this.positioner.yAlignment, marginTop, childTop);

      this.child.setPosition(x + offsetX, y + offsetY);
      if (this.child instanceof Layout layout) {
        layout.arrangeElements();
      }
    }
  }
}
