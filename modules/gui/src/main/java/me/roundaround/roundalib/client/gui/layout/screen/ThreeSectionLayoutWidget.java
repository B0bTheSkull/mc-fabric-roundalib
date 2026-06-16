package me.roundaround.roundalib.client.gui.layout.screen;

import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.LayoutHookWithParent;
import me.roundaround.roundalib.client.gui.layout.NonPositioningLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.SizableLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutCellConfigurator;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

import static net.minecraft.client.gui.layouts.HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT;

public class ThreeSectionLayoutWidget extends SizableLayoutWidget {
  private final Screen screen;
  private final LinearLayoutWidget header;
  private final LinearLayoutWidget body;
  private final LinearLayoutWidget footer;
  private final NonPositioningLayoutWidget nonPositioned;

  private int headerHeight;
  private int footerHeight;
  private int bodyHeight;
  private LayoutHookWithParent<ThreeSectionLayoutWidget, LinearLayoutWidget> headerLayoutHook;
  private LayoutHookWithParent<ThreeSectionLayoutWidget, LinearLayoutWidget> bodyLayoutHook;
  private LayoutHookWithParent<ThreeSectionLayoutWidget, LinearLayoutWidget> footerLayoutHook;

  public ThreeSectionLayoutWidget(Screen screen) {
    this(screen, DEFAULT_HEADER_AND_FOOTER_HEIGHT);
  }

  public ThreeSectionLayoutWidget(Screen screen, int headerFooterHeight) {
    this(screen, headerFooterHeight, headerFooterHeight);
  }

  public ThreeSectionLayoutWidget(Screen screen, int headerHeight, int footerHeight) {
    super(0, 0, screen.width, screen.height);

    this.screen = screen;
    this.headerHeight = headerHeight;
    this.footerHeight = footerHeight;
    this.calculateBodyHeight();

    this.header = LinearLayoutWidget.vertical(0, 0, this.width, this.headerHeight)
        .mainAxisContentAlignCenter()
        .defaultOffAxisContentAlignCenter()
        .spacing(GuiUtil.PADDING / 2);
    this.body = LinearLayoutWidget.vertical(0, this.headerHeight, this.width, this.bodyHeight)
        .mainAxisContentAlignCenter()
        .defaultOffAxisContentAlignCenter()
        .spacing(GuiUtil.PADDING);
    this.footer = LinearLayoutWidget.horizontal(0, this.height - this.footerHeight, this.width, this.footerHeight)
        .mainAxisContentAlignCenter()
        .defaultOffAxisContentAlignCenter()
        .spacing(2 * GuiUtil.PADDING);
    this.nonPositioned = new NonPositioningLayoutWidget();
  }

  @Override
  public void visitChildren(Consumer<LayoutElement> consumer) {
    this.header.visitChildren(consumer);
    this.body.visitChildren(consumer);
    this.footer.visitChildren(consumer);
    this.nonPositioned.visitChildren(consumer);
  }

  @Override
  public void arrangeElements() {
    this.setDimensions(this.screen.width, this.screen.height);
    this.calculateBodyHeight();

    this.header.setPositionAndDimensions(0, 0, this.width, this.headerHeight);
    this.body.setPositionAndDimensions(0, this.headerHeight, this.width, this.bodyHeight);
    this.footer.setPositionAndDimensions(0, this.height - this.footerHeight, this.width, this.footerHeight);

    if (this.headerLayoutHook != null) {
      this.headerLayoutHook.run(this, this.header);
    }
    if (this.bodyLayoutHook != null) {
      this.bodyLayoutHook.run(this, this.body);
    }
    if (this.footerLayoutHook != null) {
      this.footerLayoutHook.run(this, this.footer);
    }

    this.header.arrangeElements();
    this.body.arrangeElements();
    this.footer.arrangeElements();
    this.nonPositioned.arrangeElements();
  }

  public int getHeaderHeight() {
    return this.headerHeight;
  }

  public void setHeaderHeight(int headerHeight) {
    this.headerHeight = headerHeight;
    this.calculateBodyHeight();
  }

  public int getBodyHeight() {
    return this.bodyHeight;
  }

  public int getFooterHeight() {
    return this.footerHeight;
  }

  public void setFooterHeight(int footerHeight) {
    this.footerHeight = footerHeight;
    this.calculateBodyHeight();
  }

  public LinearLayoutWidget getHeader() {
    return this.header;
  }

  public LinearLayoutWidget getBody() {
    return this.body;
  }

  public LinearLayoutWidget getFooter() {
    return this.footer;
  }

  public void clearChildren() {
    this.header.clearChildren();
    this.body.clearChildren();
    this.footer.clearChildren();
  }

  @Override
  public void removeChildren() {
    this.clearChildren();
  }

  public LabelWidget addHeader(Font textRenderer, Component title) {
    return this.header.add(LabelWidget.builder(textRenderer, title)
        .alignTextCenterX()
        .alignTextCenterY()
        .hideBackground()
        .showShadow()
        .build());
  }

  public <T extends LayoutElement> T addHeader(T widget) {
    return this.header.add(widget);
  }

  public <T extends LayoutElement> T addHeader(
      T widget, LayoutHookWithParent<LinearLayoutWidget, T> layoutHook
  ) {
    return this.header.add(widget, layoutHook);
  }

  public <T extends LayoutElement> T addHeader(
      T widget, Consumer<LinearLayoutCellConfigurator<T>> configure
  ) {
    return this.header.add(widget, configure);
  }

  public <T extends LayoutElement> T addBody(T widget) {
    return this.body.add(widget);
  }

  public <T extends LayoutElement> T addBody(
      T widget, LayoutHookWithParent<LinearLayoutWidget, T> layoutHook
  ) {
    return this.body.add(widget, layoutHook);
  }

  public <T extends LayoutElement> T addBody(
      T widget, Consumer<LinearLayoutCellConfigurator<T>> configure
  ) {
    return this.body.add(widget, configure);
  }

  public <T extends LayoutElement> T addFooter(T widget) {
    return this.footer.add(widget);
  }

  public <T extends LayoutElement> T addFooter(
      T widget, LayoutHookWithParent<LinearLayoutWidget, T> layoutHook
  ) {
    return this.footer.add(widget, layoutHook);
  }

  public <T extends LayoutElement> T addFooter(
      T widget, Consumer<LinearLayoutCellConfigurator<T>> configure
  ) {
    return this.footer.add(widget, configure);
  }

  public <T extends LayoutElement> T addNonPositioned(T widget) {
    return this.nonPositioned.add(widget);
  }

  public <T extends LayoutElement> T addNonPositioned(T widget, LayoutHookWithParent<NonPositioningLayoutWidget, T> layoutHook) {
    return this.nonPositioned.add(widget, layoutHook);
  }

  public void setHeaderLayoutHook(LayoutHookWithParent<ThreeSectionLayoutWidget, LinearLayoutWidget> layoutHook) {
    this.headerLayoutHook = layoutHook;
  }

  public void setBodyLayoutHook(LayoutHookWithParent<ThreeSectionLayoutWidget, LinearLayoutWidget> layoutHook) {
    this.bodyLayoutHook = layoutHook;
  }

  public void setFooterLayoutHook(LayoutHookWithParent<ThreeSectionLayoutWidget, LinearLayoutWidget> layoutHook) {
    this.footerLayoutHook = layoutHook;
  }

  private void calculateBodyHeight() {
    this.bodyHeight = this.height - this.headerHeight - this.footerHeight;
  }
}
