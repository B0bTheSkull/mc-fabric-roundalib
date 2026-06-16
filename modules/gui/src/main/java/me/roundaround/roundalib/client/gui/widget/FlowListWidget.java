package me.roundaround.roundalib.client.gui.widget;

import me.roundaround.roundalib.client.gui.layout.LayoutHook;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.util.Spacing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public abstract class FlowListWidget<E extends FlowListWidget.Entry> extends AbstractContainerWidget implements Layout {
  protected static final int VANILLA_LIST_WIDTH_S = 220;
  protected static final int VANILLA_LIST_WIDTH_M = 280;
  protected static final int VANILLA_LIST_WIDTH_L = 340;
  protected static final int DEFAULT_SHADE_STRENGTH = 50;
  protected static final int DEFAULT_SHADE_STRENGTH_STRONG = 150;
  protected static final int DEFAULT_SHADE_FADE_WIDTH = 10;

  protected final Minecraft client;
  protected final ThreeSectionLayoutWidget parentLayout;

  protected E hoveredEntry;
  protected E selected;
  protected Double scrollUnit;

  protected final LinkedList<E> entries = new LinkedList<>();

  protected boolean alternatingRowShading = false;
  protected int shadeFadeWidth = DEFAULT_SHADE_FADE_WIDTH;
  protected int shadeStrength = DEFAULT_SHADE_STRENGTH;
  protected boolean autoPadForShading = true;
  protected int contentHeight = 0;
  protected Spacing contentPadding = Spacing.zero();
  protected int contentMargin = GuiUtil.PADDING;
  protected int rowSpacing = 0;
  protected double scrollAmount = 0;
  protected boolean scrolling = false;
  protected int scrollbarX;

  protected FlowListWidget(Minecraft client, ThreeSectionLayoutWidget layout) {
    super(
        0,
        layout.getHeaderHeight(),
        layout.getWidth(),
        layout.getBodyHeight(),
        CommonComponents.EMPTY,
        AbstractScrollArea.defaultSettings(1)
    );

    this.client = client;
    this.parentLayout = layout;
  }

  protected FlowListWidget(Minecraft client, int x, int y, int width, int height) {
    super(x, y, width, height, CommonComponents.EMPTY, AbstractScrollArea.defaultSettings(1));

    this.client = client;
    this.parentLayout = null;
  }

  public <T extends E> T addEntry(EntryFactory<T> factory) {
    T entry = factory.create(
        this.entries.size(),
        this.getContentLeft(),
        this.getNextEntryTop(),
        this.getContentWidth()
    );

    if (entry == null) {
      return null;
    }

    entry.setDefaultAlternatingRowShading(this.alternatingRowShading);
    entry.setDefaultRowShadeFadeWidth(this.shadeFadeWidth);
    entry.setDefaultRowShadeStrength(this.shadeStrength);
    entry.setDefaultAutoPadForShading(this.autoPadForShading);

    boolean wasScrollbarVisible = this.isScrollbarVisible();

    this.entries.add(entry);

    this.calculateContentHeight();
    if (this.isScrollbarVisible() && !wasScrollbarVisible) {
      this.arrangeElements();
    }

    E selected = this.getSelected();
    if (selected != null) {
      this.ensureVisible(selected);
    }

    return entry;
  }

  public E removeEntry() {
    if (this.entries.isEmpty()) {
      return null;
    }

    boolean wasScrollbarVisible = this.isScrollbarVisible();

    E entry = this.entries.removeLast();
    this.calculateContentHeight();
    if (!this.isScrollbarVisible() && wasScrollbarVisible) {
      this.arrangeElements();
    }

    E selected = this.getSelected();
    if (selected == entry) {
      this.setSelected(null);
    } else if (selected != null) {
      this.ensureVisible(selected);
    }

    return entry;
  }

  public void clearEntries() {
    this.entries.clear();
    this.contentHeight = this.contentPadding.getVertical();
    this.scrollAmount = 0;
    this.selected = null;
  }

  @Override
  public List<E> children() {
    return this.entries;
  }

  public E getEntry(int index) {
    return this.entries.get(index);
  }

  public E getFirst() {
    return this.entries.getFirst();
  }

  public int getEntryCount() {
    return this.entries.size();
  }

  public void forEachEntry(Consumer<E> consumer) {
    this.entries.forEach(consumer);
  }

  @Override
  public void visitChildren(Consumer<LayoutElement> consumer) {
    this.entries.forEach(consumer);
  }

  @Override
  public void removeChildren() {
    this.clearEntries();
  }

  @Override
  public void arrangeElements() {
    if (this.parentLayout != null) {
      this.setRectangle(
          this.parentLayout.getWidth(),
          this.parentLayout.getBodyHeight(),
          0,
          this.parentLayout.getHeaderHeight()
      );
    }

    this.calculateContentHeight();
    this.calculateScrollbarX();

    int entryY = this.getContentTop();
    for (E entry : this.entries) {
      entry.setPosition(this.getContentLeft(), entryY);
      entry.setWidth(this.getContentWidth());

      entryY += entry.getHeight() + this.rowSpacing;
    }

    this.setScrollAmount(this.getScrollAmount());

    Layout.super.arrangeElements();
  }

  protected void calculateContentHeight() {
    this.contentHeight = this.contentPadding.getVertical() + this.entries.stream().mapToInt(Entry::getHeight).sum() +
                         Math.max(0, this.getEntryCount() - 1) * this.rowSpacing;
  }

  protected void calculateScrollbarX() {
    this.scrollbarX = this.getContentRight() + this.contentMargin;
  }

  @Override
  public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    this.hoveredEntry = this.getEntryAtPosition(mouseX, mouseY);

    this.renderListBackground(context);
    this.renderList(context, mouseX, mouseY, delta);
    this.renderListBorders(context);
  }

  protected void renderListBackground(GuiGraphicsExtractor context) {
    context.blit(
        RenderPipelines.GUI_TEXTURED,
        Textures.listBg(this.client),
        this.getX(),
        this.getY(),
        this.getRight(),
        this.getBottom() + (int) this.getScrollAmount(),
        this.getWidth(),
        this.getHeight(),
        32,
        32
    );
  }

  protected void renderList(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    context.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
    this.renderEntries(context, mouseX, mouseY, delta);
    this.renderScrollBar(context);
    context.disableScissor();
  }

  protected void renderEntries(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    this.entries.forEach((entry) -> {
      if (!this.isEntryVisible(entry)) {
        return;
      }
      this.renderEntry(context, mouseX, mouseY, delta, entry);
    });
  }

  protected void renderEntry(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, E entry) {
    entry.extractRenderState(context, mouseX, mouseY, delta);
  }

  protected void renderScrollBar(GuiGraphicsExtractor context) {
    if (!this.isScrollbarVisible()) {
      return;
    }

    int handleHeight = this.getScrollbarHandleHeight();
    double yPercent = this.getScrollAmount() / this.getMaxScroll();
    int movableSpace = this.getHeight() - handleHeight;
    int handleY = this.getY() + (int) Math.round(yPercent * movableSpace);

    context.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        Textures.SCROLLBAR_BG,
        this.scrollbarX,
        this.getY(),
        GuiUtil.SCROLLBAR_WIDTH,
        this.getHeight()
    );
    context.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        Textures.SCROLLBAR,
        this.scrollbarX,
        handleY,
        GuiUtil.SCROLLBAR_WIDTH,
        handleHeight
    );
  }

  protected void renderListBorders(GuiGraphicsExtractor context) {
    context.blit(
        RenderPipelines.GUI_TEXTURED,
        Textures.borderTop(this.client),
        this.getX(),
        this.getY() - 2,
        0,
        0,
        this.getWidth(),
        2,
        32,
        2
    );
    context.blit(
        RenderPipelines.GUI_TEXTURED,
        Textures.borderBottom(this.client),
        this.getX(),
        this.getBottom(),
        0,
        0,
        this.getWidth(),
        2,
        32,
        2
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public E getFocused() {
    return (E) super.getFocused();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setFocused(GuiEventListener focused) {
    super.setFocused(focused);
    if (focused instanceof Entry entry) {
      this.setSelected((E) entry);
      if (this.client.getLastInputType().isKeyboard()) {
        this.ensureVisible((E) entry);
      }
    }
  }

  public E getSelected() {
    return this.selected;
  }

  public void setSelected(E entry) {
    this.selected = entry;
  }

  protected boolean isSelectedEntry(int index) {
    return Objects.equals(this.getSelected(), this.children().get(index));
  }

  public void selectFirst() {
    if (this.getEntryCount() > 0) {
      this.setSelected(this.getEntry(0));
    }
  }

  protected E getNeighboringEntry(ScreenDirection direction) {
    return this.getNeighboringEntry(direction, (entry) -> true);
  }

  protected E getNeighboringEntry(ScreenDirection direction, Predicate<E> predicate) {
    return this.getNeighboringEntry(direction, predicate, this.getSelected());
  }

  protected E getNeighboringEntry(ScreenDirection direction, Predicate<E> predicate, E selected) {
    if (this.entries.isEmpty()) {
      return null;
    }

    int delta = switch (direction) {
      case UP -> -1;
      case DOWN -> 1;
      default -> 0;
    };

    if (delta == 0) {
      return null;
    }

    int index;
    if (selected == null) {
      index = delta > 0 ? 0 : this.entries.size() - 1;
    } else {
      index = selected.getIndex() + delta;
    }

    for (int i = index; i >= 0 && i < this.entries.size(); i += delta) {
      E entry = this.entries.get(i);
      if (predicate.test(entry)) {
        return entry;
      }
    }

    return null;
  }

  @Override
  public NarrationPriority narrationPriority() {
    if (this.isFocused()) {
      return NarrationPriority.FOCUSED;
    } else {
      return this.getHoveredEntry() != null ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }
  }

  protected E getHoveredEntry() {
    return this.hoveredEntry;
  }

  protected void appendNarrations(NarrationElementOutput builder, E entry) {
    int count = this.getEntryCount();
    if (count > 1) {
      int num = entry.getIndex() + 1;
      builder.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", num, count));
    }
  }

  @Override
  public boolean isMouseOver(double mouseX, double mouseY) {
    return mouseX >= this.getX() && mouseX <= this.getRight() && mouseY >= this.getY() && mouseY <= this.getBottom();
  }

  protected boolean isSelectButton(int button) {
    return button == 0;
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
    if (!this.isSelectButton(click.button())) {
      return false;
    }

    this.updateScrollingState(click);

    if (!this.isMouseOver(click.x(), click.y())) {
      return false;
    }

    E entry = this.getEntryAtPosition(click.x(), click.y());
    if (entry != null) {
      if (entry.mouseClicked(click, doubled)) {
        E focused = this.getFocused();
        if (focused != entry && focused instanceof ContainerEventHandler parent) {
          parent.setFocused(null);
        }

        this.setFocused(entry);
        this.setDragging(true);
        return true;
      }
    }

    return this.scrolling;
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent click) {
    if (this.getFocused() != null) {
      this.getFocused().mouseReleased(click);
    }

    return false;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
    if (super.mouseDragged(click, deltaX, deltaY)) {
      return true;
    }

    if (click.button() != 0 || !this.scrolling) {
      return false;
    }

    if (click.y() < this.getY()) {
      this.setScrollAmount(0);
    } else if (click.y() > this.getBottom()) {
      this.setScrollAmount(this.getMaxScroll());
    } else {
      double max = Math.max(1, this.getMaxScroll());
      int movableSpace = this.getHeight() - this.getScrollbarHandleHeight();
      double scale = Math.max(1, max / movableSpace);
      this.setScrollAmount(this.getScrollAmount() + deltaY * scale);
    }

    return true;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (this.allowScrollPassThrough()) {
      E entry = this.getEntryAtPosition(mouseX, mouseY);
      if (entry != null && entry.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
        return true;
      }
    }

    this.setScrollAmount(this.getScrollAmount() - verticalAmount * this.getScrollUnit());
    return true;
  }

  protected boolean allowScrollPassThrough() {
    return false;
  }

  protected E getEntryAtPosition(double x, double y) {
    if (!this.isMouseOver(x, y)) {
      return null;
    }

    for (E entry : this.entries) {
      if (entry.isMouseOver(x, y)) {
        return entry;
      }
    }

    return null;
  }

  protected boolean isEntryVisible(E entry) {
    return entry.getY() <= this.getContentBottom() && entry.getBottom() >= this.getContentTop();
  }

  protected void centerScrollOn(E entry) {
    this.setScrollAmount(entry.getContentCenterY() - this.height * 0.5);
  }

  protected void ensureVisible(E entry) {
    if (entry.getY() < this.getContentTop()) {
      this.scroll(entry.getY() - this.getContentTop());
      return;
    }

    if (entry.getBottom() > this.getContentBottom()) {
      this.scroll(entry.getBottom() - this.getContentBottom());
    }
  }

  protected boolean isScrollbarVisible() {
    return this.getMaxScroll() > 0;
  }

  @Override
  protected int contentHeight() {
    return this.getContentHeight();
  }

  public void setContentPaddingX(int paddingX) {
    this.contentPadding = this.contentPadding.setHorizontal(paddingX);
  }

  public void setContentPaddingY(int paddingX) {
    this.contentPadding = this.contentPadding.setVertical(paddingX);
  }

  public void setContentPadding(int padding) {
    this.contentPadding = Spacing.of(padding);
  }

  public void setContentPadding(Spacing padding) {
    this.contentPadding = padding;
  }

  public void setRowSpacing(int rowSpacing) {
    this.rowSpacing = rowSpacing;
  }

  public int getContentWidth() {
    return Math.min(this.getPreferredContentWidth(), this.getMaxContentWidth());
  }

  public int getContentHeight() {
    return this.contentHeight;
  }

  public int getContentTop() {
    return this.getY() + this.contentPadding.top();
  }

  public int getContentBottom() {
    return this.getBottom() - this.contentPadding.bottom();
  }

  public int getContentLeft() {
    if (this.doesContentFillWidth()) {
      return this.getX() + this.getContentMarginLeft();
    }
    return this.getX() + (this.getWidth() - this.getContentWidth()) / 2;
  }

  public int getContentRight() {
    if (this.doesContentFillWidth()) {
      return this.getRight() - this.getContentMarginRight();
    }
    return this.getX() + (this.getWidth() + this.getContentWidth()) / 2;
  }

  protected int getNextEntryTop() {
    if (this.entries.isEmpty()) {
      return this.getContentTop();
    }
    return this.entries.getLast().getBottom() + this.rowSpacing;
  }

  protected int getPreferredContentWidth() {
    if (this.parentLayout == null || this.getWidth() <= VANILLA_LIST_WIDTH_S) {
      return this.getWidth();
    }
    if (this.getWidth() < VANILLA_LIST_WIDTH_L) {
      return VANILLA_LIST_WIDTH_M;
    }
    return VANILLA_LIST_WIDTH_L;
  }

  protected int getFullGutterWidth() {
    if (!this.isScrollbarVisible()) {
      return this.contentMargin;
    }
    return GuiUtil.SCROLLBAR_WIDTH + 2 * this.contentMargin;
  }

  protected boolean doesContentFillWidth() {
    return this.getPreferredContentWidth() >= this.getWidth() - 2 * this.getFullGutterWidth();
  }

  protected int getGutterWidth() {
    if (!this.isScrollbarVisible()) {
      return this.contentMargin;
    }
    return GuiUtil.SCROLLBAR_WIDTH + (this.doesContentFillWidth() ? 1 : 2) * this.contentMargin;
  }

  protected int getContentMarginLeft() {
    return this.doesContentFillWidth() ? this.contentMargin : this.getGutterWidth();
  }

  protected int getContentMarginRight() {
    return this.getGutterWidth();
  }

  protected int getMaxContentWidth() {
    return this.getWidth() - this.getContentMarginLeft() - this.getContentMarginRight();
  }

  protected int getScrollbarHandleHeight() {
    int height = this.getHeight();
    return Mth.clamp(height * height / this.getContentHeight(), 32, height - 8);
  }

  protected void scroll(int amount) {
    this.setScrollAmount(this.getScrollAmount() + (double) amount);
  }

  public double getScrollAmount() {
    return this.scrollAmount;
  }

  public void setScrollAmount(double amount) {
    this.scrollAmount = Mth.clamp(amount, 0, this.getMaxScroll());
    this.entries.forEach((entry) -> entry.setScrollAmount(this.getScrollAmount()));
  }

  public void refreshScrollAmount() {
    this.scrollAmount = Mth.clamp(this.getScrollAmount(), 0, this.getMaxScroll());
  }

  public int getMaxScroll() {
    return Math.max(0, this.getContentHeight() - this.getHeight());
  }

  protected void updateScrollingState(MouseButtonEvent click) {
    this.scrolling = click.button() == 0 && click.x() >= (double) this.scrollbarX &&
                     click.x() < (this.scrollbarX + GuiUtil.SCROLLBAR_WIDTH);
  }

  @Override
  protected double scrollRate() {
    return this.getScrollUnit();
  }

  protected double getScrollUnit() {
    if (this.scrollUnit != null) {
      return this.scrollUnit;
    }
    double averageHeight = (double) this.contentHeight / this.entries.size();
    return averageHeight / 2;
  }

  protected void setAlternatingRowShading(boolean alternatingRowShading) {
    this.alternatingRowShading = alternatingRowShading;
    this.forEachEntry((entry) -> {
      entry.setAlternatingRowShading(this.alternatingRowShading);
    });
  }

  protected void setRowShadeFadeWidth(int width) {
    this.shadeFadeWidth = Math.max(width, 0);
    this.forEachEntry((entry) -> {
      entry.setRowShadeFadeWidth(this.shadeFadeWidth);
    });
  }

  protected void setRowShadeStrength(int strength) {
    this.shadeStrength = Math.clamp(strength, 0, 255);
    this.forEachEntry((entry) -> {
      entry.setRowShadeStrength(this.shadeStrength);
    });
  }

  protected void setRowShadeStrength(float strength) {
    this.setRowShadeStrength((int) (strength * 255));
  }

  protected void setAutoPadForShading(boolean autoPad) {
    this.autoPadForShading = autoPad;
    this.forEachEntry((entry) -> {
      entry.setAutoPadForShading(this.autoPadForShading);
    });
  }

  @Environment(EnvType.CLIENT)
  public abstract static class Entry implements Renderable, Layout, GuiEventListener {
    protected static final Spacing DEFAULT_MARGIN = Spacing.of(GuiUtil.PADDING / 2);

    protected final ArrayList<LayoutRef<?>> layouts = new ArrayList<>();
    protected final ArrayList<Renderable> drawables = new ArrayList<>();
    protected final int index;
    protected final int contentHeight;

    protected int x;
    protected int y;
    protected int width;
    protected double scrollAmount = 0;
    protected Spacing margin = DEFAULT_MARGIN;

    private boolean defaultForceRowShading = false;
    private boolean defaultAlternatingRowShading = false;
    private int defaultShadeFadeWidth = DEFAULT_SHADE_FADE_WIDTH;
    private int defaultShadeStrength = DEFAULT_SHADE_STRENGTH;
    private boolean defaultAutoPadForShading = true;
    private Boolean forceRowShading = null;
    private Boolean alternatingRowShading = null;
    private Integer shadeFadeWidth = null;
    private Integer shadeStrength = null;
    private Boolean autoPadForShading = null;

    protected Entry(int index, int x, int y, int width, int contentHeight) {
      this.index = index;
      this.x = x;
      this.y = y;
      this.width = width;
      this.contentHeight = contentHeight;
    }

    protected <T extends Layout> T addLayout(T layout) {
      return this.addLayout(layout, LayoutHook.noop());
    }

    protected <T extends Layout> T addLayout(T layout, LayoutHook<T> layoutHook) {
      this.layouts.add(new LayoutRef<>(layout, layoutHook));
      return layout;
    }

    protected <T extends Renderable> T addDrawable(T drawable) {
      this.drawables.add(drawable);
      return drawable;
    }

    protected void clearChildren() {
      this.layouts.clear();
      this.drawables.clear();
    }

    public List<Renderable> drawables() {
      return this.drawables;
    }

    public List<? extends Layout> layoutWidgets() {
      return this.layouts.stream().map(LayoutRef::getLayout).toList();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> consumer) {
      this.layouts.forEach((ref) -> consumer.accept(ref.getLayout()));
    }

    @Override
    public void removeChildren() {
      this.clearChildren();
    }

    @Override
    public void arrangeElements() {
      this.layouts.forEach(LayoutRef::refreshPositions);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      this.renderContent(context, mouseX, mouseY, delta);
      this.renderDecorations(context, mouseX, mouseY, delta);
    }

    protected void renderBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      if (this.hasRowShading()) {
        this.renderRowShade(context);
      }
    }

    protected void renderRowShade(GuiGraphicsExtractor context) {
      renderRowShade(
          context,
          this.getX(),
          this.getY(),
          this.getRight(),
          this.getBottom(),
          this.getRowShadeFadeWidth(),
          this.getRowShadeStrength()
      );
    }

    protected static void renderRowShade(
        GuiGraphicsExtractor context,
        int left,
        int top,
        int right,
        int bottom,
        int fadeWidth,
        int shadeStrength
    ) {
      int shadeColor = GuiUtil.genColorInt(0, 0, 0, shadeStrength);
      GuiUtil.fillHorizontalGradient(context, left, top, left + fadeWidth, bottom, 0, shadeColor);
      context.fill(left + fadeWidth, top, right - fadeWidth, bottom, shadeColor);
      GuiUtil.fillHorizontalGradient(context, right - fadeWidth, top, right, bottom, shadeColor, 0);
    }

    protected void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      this.drawables.forEach((drawable) -> drawable.extractRenderState(context, mouseX, mouseY, delta));
    }

    protected void renderDecorations(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    protected void setScrollAmount(double scrollAmount) {
      this.scrollAmount = scrollAmount;
      this.arrangeElements();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
      return mouseX >= this.getX() && mouseX <= this.getRight() && mouseY >= this.getY() && mouseY <= this.getBottom();
    }

    @Override
    public ScreenRectangle getRectangle() {
      return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    public int getIndex() {
      return this.index;
    }

    @Override
    public int getWidth() {
      return this.width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    @Override
    public int getHeight() {
      return this.getContentHeight() + this.margin.getVertical();
    }

    @Override
    public int getX() {
      return this.x;
    }

    @Override
    public void setX(int x) {
      this.x = x;
    }

    @Override
    public int getY() {
      return this.y - (int) this.scrollAmount;
    }

    @Override
    public void setY(int y) {
      this.y = y;
    }

    public int getRight() {
      return this.getX() + this.getWidth();
    }

    public int getBottom() {
      return this.getY() + this.getHeight();
    }

    public void setMarginY(int margin) {
      this.margin = this.margin.setVertical(margin);
    }

    public void setMarginX(int margin) {
      this.margin = this.margin.setHorizontal(margin);
    }

    public void setMargin(int margin) {
      this.margin = Spacing.of(margin);
    }

    public void setMargin(Spacing margin) {
      this.margin = margin;
    }

    public int getContentLeft() {
      return this.getX() + this.margin.left() + this.getShadingPadding();
    }

    public int getContentRight() {
      return this.getRight() - this.margin.right() - this.getShadingPadding();
    }

    public int getContentWidth() {
      return this.getWidth() - this.margin.getHorizontal() - 2 * this.getShadingPadding();
    }

    public int getContentTop() {
      return this.getY() + this.margin.top();
    }

    public int getContentBottom() {
      return this.getBottom() - this.margin.bottom();
    }

    public int getContentHeight() {
      return this.contentHeight;
    }

    public int getContentCenterX() {
      return this.getContentLeft() + this.getContentWidth() / 2;
    }

    public int getContentCenterY() {
      return this.getContentTop() + this.getContentHeight() / 2;
    }

    public int getShadingPadding() {
      return (this.getAlternatingRowShading() || this.getForceRowShading()) && this.getAutoPadForShading() ?
          this.getRowShadeFadeWidth() :
          0;
    }

    public boolean hasRowShading() {
      return (this.getAlternatingRowShading() && this.index % 2 == 0) || this.getForceRowShading();
    }

    protected void setForceRowShading(boolean forceRowShading) {
      this.forceRowShading = forceRowShading;
    }

    protected void setDefaultForceRowShading(boolean forceRowShading) {
      this.defaultForceRowShading = forceRowShading;
    }

    protected boolean getForceRowShading() {
      return valueOrDefault(this.forceRowShading, this.defaultForceRowShading);
    }

    protected void setAlternatingRowShading(boolean alternatingRowShading) {
      this.alternatingRowShading = alternatingRowShading;
    }

    protected void setDefaultAlternatingRowShading(boolean alternatingRowShading) {
      this.defaultAlternatingRowShading = alternatingRowShading;
    }

    protected boolean getAlternatingRowShading() {
      return valueOrDefault(this.alternatingRowShading, this.defaultAlternatingRowShading);
    }

    protected void setRowShadeFadeWidth(int width) {
      this.shadeFadeWidth = width;
    }

    protected void setDefaultRowShadeFadeWidth(int width) {
      this.defaultShadeFadeWidth = width;
    }

    protected int getRowShadeFadeWidth() {
      return valueOrDefault(this.shadeFadeWidth, this.defaultShadeFadeWidth);
    }

    protected void setRowShadeStrength(int strength) {
      this.shadeStrength = strength;
    }

    protected void setDefaultRowShadeStrength(int strength) {
      this.defaultShadeStrength = strength;
    }

    protected int getRowShadeStrength() {
      return valueOrDefault(this.shadeStrength, this.defaultShadeStrength);
    }

    protected void setAutoPadForShading(boolean autoPad) {
      this.autoPadForShading = autoPad;
    }

    protected void setDefaultAutoPadForShading(boolean autoPad) {
      this.defaultAutoPadForShading = autoPad;
    }

    protected boolean getAutoPadForShading() {
      return valueOrDefault(this.autoPadForShading, this.defaultAutoPadForShading);
    }

    private static <T> T valueOrDefault(T value, T other) {
      return value == null ? other : value;
    }
  }

  @Environment(EnvType.CLIENT)
  @FunctionalInterface
  public interface EntryFactory<E extends Entry> {
    E create(int index, int left, int top, int width);
  }

  @Environment(EnvType.CLIENT)
  public final static class LayoutRef<T extends Layout> {
    private final T layout;
    private final LayoutHook<T> hook;

    public LayoutRef(T layout, LayoutHook<T> hook) {
      this.layout = layout;
      this.hook = hook;
    }

    public T getLayout() {
      return this.layout;
    }

    public void refreshPositions() {
      if (this.hook != null) {
        this.hook.run(this.layout);
      }
      this.layout.arrangeElements();
    }
  }

  @Environment(EnvType.CLIENT)
  public final static class Textures {
    public static final Identifier SCROLLBAR_BG = Identifier.withDefaultNamespace("widget/scroller_background");
    public static final Identifier SCROLLBAR = Identifier.withDefaultNamespace("widget/scroller");
    public static final Identifier LIST_BG = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
    public static final Identifier BORDER_TOP = Screen.HEADER_SEPARATOR;
    public static final Identifier BORDER_BOTTOM = Screen.FOOTER_SEPARATOR;
    public static final Identifier IN_WORLD_LIST_BG = Identifier.withDefaultNamespace(
        "textures/gui/inworld_menu_list_background.png");
    public static final Identifier IN_WORLD_BORDER_TOP = Screen.INWORLD_HEADER_SEPARATOR;
    public static final Identifier IN_WORLD_BORDER_BOTTOM = Screen.INWORLD_FOOTER_SEPARATOR;

    public static Identifier borderTop(Minecraft client) {
      return client.level == null ? BORDER_TOP : IN_WORLD_BORDER_TOP;
    }

    public static Identifier borderBottom(Minecraft client) {
      return client.level == null ? BORDER_BOTTOM : IN_WORLD_BORDER_BOTTOM;
    }

    public static Identifier listBg(Minecraft client) {
      return client.level == null ? LIST_BG : IN_WORLD_LIST_BG;
    }
  }
}
