package me.roundaround.roundalib.client.gui.widget.drawable;

import java.util.function.Consumer;

import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.util.IntRect;
import me.roundaround.roundalib.client.gui.util.Spacing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.inventory.Slot;

public class FrameWidget extends DrawableWidget implements Layout {
  public static final int DEFAULT_OVERFLOW = 2;

  private static final Identifier TEXTURE = Identifier.withDefaultNamespace("hud/hotbar_selection");
  private static final Spacing NINE_SLIDE_BORDER = Spacing.of(4, 4, 3, 4);

  private Slot refSlot = null;
  private LayoutElement refWidget = null;
  private int overflow;

  public FrameWidget() {
    this(0, 0, 0, 0, DEFAULT_OVERFLOW);
  }

  public FrameWidget(int overflow) {
    this(0, 0, 0, 0, overflow);
  }

  public FrameWidget(int width, int height) {
    this(0, 0, width, height, DEFAULT_OVERFLOW);
  }

  public FrameWidget(int width, int height, int overflow) {
    this(0, 0, width, height, overflow);
  }

  public FrameWidget(Slot slot) {
    this(slot, DEFAULT_OVERFLOW);
  }

  public FrameWidget(Slot slot, int overflow) {
    this(IntRect.fromSlot(slot), overflow);
    this.refSlot = slot;
  }

  public FrameWidget(LayoutElement widget) {
    this(widget, DEFAULT_OVERFLOW);
  }

  public FrameWidget(LayoutElement widget, int overflow) {
    this(IntRect.fromWidget(widget), overflow);
    this.refWidget = widget;
  }

  public FrameWidget(IntRect targetBounds) {
    this(targetBounds, DEFAULT_OVERFLOW);
  }

  public FrameWidget(IntRect targetBounds, int overflow) {
    this(targetBounds.left(), targetBounds.top(), targetBounds.getWidth(), targetBounds.getHeight(), overflow);
  }

  public FrameWidget(int x, int y, int width, int height) {
    this(x, y, width, height, DEFAULT_OVERFLOW);
  }

  public FrameWidget(int x, int y, int width, int height, int overflow) {
    super(x, y, width, height);
    this.overflow = overflow;
  }

  @Override
  public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    // Need to get around the fact the vanilla texture is only 23 pixels tall, so
    // actually draw a 9-slice
    // that is height - 1 tall, then draw just the top border of the texture again,
    // but at the bottom.

    int x = this.getX() - this.overflow;
    int y = this.getY() - this.overflow;
    int width = this.getWidth() + 2 * this.overflow;
    int height = this.getHeight() + 2 * this.overflow - 1;

    TextureAtlasSprite sprite = GuiUtil.getSprite(TEXTURE);
    GuiUtil.drawSpriteNineSliced(
        context, RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, 24, 23, CommonColors.WHITE, NINE_SLIDE_BORDER);
    GuiUtil.drawSpriteRegion(
        context, RenderPipelines.GUI_TEXTURED, sprite, 24, 23, 0, 0, x, y + height, width, 1, CommonColors.WHITE);
  }

  @Override
  public void visitChildren(Consumer<LayoutElement> consumer) {
  }

  @Override
  public void removeChildren() {
    // FrameWidget only references external widgets/slots; it owns no children.
    this.refSlot = null;
    this.refWidget = null;
  }

  @Override
  public void arrangeElements() {
    if (this.refSlot != null) {
      this.setBounds(IntRect.fromSlot(this.refSlot));
    } else if (this.refWidget != null) {
      this.setBounds(IntRect.fromWidget(this.refWidget));
    }
  }

  public void setOverflow(int overflow) {
    this.overflow = overflow;
  }

  public void frame(Slot slot) {
    this.refSlot = slot;
    this.refWidget = null;
    this.setBounds(IntRect.fromSlot(slot));
  }

  public void frame(LayoutElement widget) {
    this.refWidget = widget;
    this.refSlot = null;
    this.setBounds(IntRect.fromWidget(this.refWidget));
  }

  public void frame(IntRect targetBounds) {
    this.refSlot = null;
    this.refWidget = null;
    this.setBounds(targetBounds);
  }

  private void setBounds(IntRect targetBounds) {
    this.setPosition(targetBounds.left(), targetBounds.top());
    this.setSize(targetBounds.getWidth(), targetBounds.getHeight());
  }
}
