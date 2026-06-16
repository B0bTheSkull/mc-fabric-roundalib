package me.roundaround.roundalib.client.gui.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.roundaround.roundalib.client.gui.render.state.HorizontalColoredQuadGuiElementRenderState;
import me.roundaround.roundalib.mixin.GuiGraphicsExtractorAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class GuiUtil {
  private GuiUtil() {
  }

  public static final int LABEL_COLOR = genColorInt(1f, 1f, 1f);
  public static final int ERROR_COLOR = genColorInt(1f, 0.15f, 0.15f);
  public static final int BACKGROUND_COLOR = genColorInt(0f, 0f, 0f, 0.5f);
  public static final int CROSSHAIR_COLOR = genColorInt(1f, 1f, 1f, 0.7f);
  public static final int TRANSPARENT_COLOR = genColorInt(0f, 0f, 0f, 0f);
  public static final int PADDING = 4;
  public static final int SCROLLBAR_WIDTH = 6;
  public static final int DEFAULT_HEADER_FOOTER_HEIGHT = 33;
  public static final int COMPACT_HEADER_HEIGHT = 17;

  private static Minecraft client = null;

  public static Minecraft getClient() {
    if (client == null) {
      client = Minecraft.getInstance();
    }
    return client;
  }

  public static int getScaledWindowWidth() {
    return getScaledWindowWidth(getClient());
  }

  public static int getScaledWindowWidth(Minecraft client) {
    return client.getWindow().getGuiScaledWidth();
  }

  public static int getScaledWindowHeight() {
    return getScaledWindowHeight(getClient());
  }

  public static int getScaledWindowHeight(Minecraft client) {
    return client.getWindow().getGuiScaledHeight();
  }

  public static int getDisplayWidth() {
    return getDisplayWidth(getClient());
  }

  public static int getDisplayWidth(Minecraft client) {
    return client.getWindow().getScreenWidth();
  }

  public static int getDisplayHeight() {
    return getDisplayHeight(getClient());
  }

  public static int getDisplayHeight(Minecraft client) {
    return client.getWindow().getScreenHeight();
  }

  public static double getScaleFactor() {
    return getScaleFactor(getClient());
  }

  public static double getScaleFactor(Minecraft client) {
    return client.getWindow().getGuiScale();
  }

  public static Screen getCurrentScreen() {
    return getCurrentScreen(getClient());
  }

  public static Screen getCurrentScreen(Minecraft client) {
    return client.gui.screen();
  }

  public static void setScreen(Screen screen) {
    getClient().setScreenAndShow(screen);
  }

  public static void drawText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow
  ) {
    drawText(context, textRenderer, text.getVisualOrderText(), x, y, color, shadow);
  }

  public static void drawText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      FormattedCharSequence text,
      int x,
      int y,
      int color,
      boolean shadow
  ) {
    drawText(context, textRenderer, text, x, y, color, shadow, 0, Alignment.START);
  }

  public static void drawText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth,
      Alignment alignment
  ) {
    drawText(context, textRenderer, text.getVisualOrderText(), x, y, color, shadow, viewWidth, alignment);
  }

  public static void drawText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      FormattedCharSequence text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth,
      Alignment alignment
  ) {
    int textWidth = textRenderer.width(text);
    context.text(textRenderer, text, alignment.getPosInContainer(x, viewWidth, textWidth), y, color, shadow);
  }

  public static void drawTruncatedText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth
  ) {
    drawTruncatedText(context, textRenderer, text, x, y, color, shadow, viewWidth, Alignment.START);
  }

  public static void drawTruncatedText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth,
      Alignment alignment
  ) {
    if (textRenderer.width(text) < viewWidth) {
      drawText(context, textRenderer, text, x, y, color, shadow, viewWidth, alignment);
      return;
    }

    MutableComponent ellipsis = CommonComponents.ELLIPSIS.copy().setStyle(text.getStyle());
    FormattedText trimmed = textRenderer.substrByWidth(text, viewWidth - textRenderer.width(ellipsis));
    trimmed = FormattedText.composite(trimmed, ellipsis);

    drawText(
        context,
        textRenderer,
        Language.getInstance().getVisualOrder(trimmed),
        x,
        y,
        color,
        shadow,
        viewWidth,
        alignment
    );
  }

  public static void drawWrappedText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth
  ) {
    drawWrappedText(context, textRenderer, text, x, y, color, shadow, viewWidth, 0, 0);
  }

  public static void drawWrappedText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth,
      int lineSpacing,
      int maxLines
  ) {
    drawWrappedText(
        context,
        textRenderer,
        text,
        x,
        y,
        color,
        shadow,
        viewWidth,
        maxLines,
        lineSpacing,
        Alignment.START
    );
  }

  public static void drawWrappedText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth,
      int maxLines,
      int lineSpacing,
      Alignment alignment
  ) {
    if (textRenderer.width(text) < viewWidth) {
      drawText(context, textRenderer, text, x, y, color, shadow, viewWidth, alignment);
      return;
    }

    List<FormattedCharSequence> lines = textRenderer.split(text, viewWidth);
    int cursorY = y;
    for (FormattedCharSequence line : lines.subList(0, Math.min(lines.size(), maxLines))) {
      int lineWidth = textRenderer.width(line);
      int lineX = alignment.getPosInContainer(x, viewWidth, lineWidth);
      drawText(context, textRenderer, line, lineX, cursorY, color, shadow, viewWidth, alignment);
      cursorY += textRenderer.lineHeight + lineSpacing;
    }
  }

  public static Dimensions measureWrappedText(
      Font textRenderer,
      Component text,
      int maxWidth,
      int maxLines,
      int lineSpacing
  ) {
    if (maxWidth <= 0) {
      return Dimensions.of(textRenderer.width(text), textRenderer.lineHeight);
    }

    List<FormattedCharSequence> lines = textRenderer.split(text, maxWidth);
    if (lines.size() <= 1) {
      return Dimensions.of(textRenderer.width(text), textRenderer.lineHeight);
    }

    int lineCount = Math.min(lines.size(), maxLines);
    return Dimensions.of(
        lines.stream().mapToInt(textRenderer::width).max().orElse(0),
        lineCount * textRenderer.lineHeight + (lineCount - 1) * lineSpacing
    );
  }

  public static void drawScrollingText(
      GuiGraphicsExtractor context,
      Font textRenderer,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow,
      int viewWidth,
      Alignment alignment
  ) {
    int textWidth = textRenderer.width(text);
    if (textWidth < viewWidth) {
      drawText(context, textRenderer, text, x, y, color, shadow, viewWidth, alignment);
      return;
    }

    // Based largely on the scrolling text from ClickableWidget.
    double X = (double) textWidth - viewWidth;
    double t = Util.getMillis() / 1000.0;
    double T = Math.max(X / 2, 3);
    double c = Math.sin((Math.PI / 2) * Math.cos(2 * Math.PI * (t + alignment.floatValue()) / T)) / 2 + 0.5;
    double dx = c * X;

    context.enableScissor(x, y - textRenderer.lineHeight, x + viewWidth, y + 2 * textRenderer.lineHeight);
    drawText(context, textRenderer, text, x - (int) dx, y, color, shadow);
    context.disableScissor();
  }

  public static int getLineYOffset(Font textRenderer, int index, int lineSpacing) {
    return index * (textRenderer.lineHeight + lineSpacing);
  }

  public static void fill(GuiGraphicsExtractor context, IntRect rect, int color) {
    context.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), color);
  }

  public static void fillHorizontalGradient(GuiGraphicsExtractor context, IntRect rect, int colorStart, int colorEnd) {
    fillHorizontalGradient(context, rect.left(), rect.top(), rect.right(), rect.bottom(), colorStart, colorEnd);
  }

  public static void fillHorizontalGradient(
      GuiGraphicsExtractor context,
      int startX,
      int startY,
      int endX,
      int endY,
      int colorStart,
      int colorEnd
  ) {
    ((GuiGraphicsExtractorAccessor) context).getGuiRenderState().addGuiElement(new HorizontalColoredQuadGuiElementRenderState(
        RenderPipelines.GUI,
        TextureSetup.noTexture(),
        new Matrix3x2f(context.pose()),
        startX,
        startY,
        endX,
        endY,
        colorStart,
        colorEnd,
        ((GuiGraphicsExtractorAccessor) context).getScissorStack().peek()
    ));
  }

  public static void drawStrokedRectangle(GuiGraphicsExtractor context, IntRect rect, int color) {
    drawStrokedRectangle(context, rect, color, false);
  }

  public static void drawStrokedRectangle(GuiGraphicsExtractor context, IntRect rect, int color, boolean outside) {
    if (outside) {
      rect = rect.expand(1);
    }
    context.outline(rect.left(), rect.top(), rect.getWidth(), rect.getHeight(), color);
  }

  public static TextureAtlasSprite getSprite(Identifier texture) {
    return getClient().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(texture);
  }

  public static void drawSpriteNineSliced(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      int width,
      int height,
      int texWidth,
      int texHeight,
      int color,
      int border
  ) {
    drawSpriteNineSliced(
        context,
        pipeline,
        texture,
        x,
        y,
        width,
        height,
        texWidth,
        texHeight,
        color,
        Spacing.of(border)
    );
  }

  public static void drawSpriteNineSliced(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      int width,
      int height,
      int texWidth,
      int texHeight,
      int color,
      Spacing border
  ) {
    drawSpriteNineSliced(
        context,
        pipeline,
        getSprite(texture),
        x,
        y,
        width,
        height,
        texWidth,
        texHeight,
        color,
        border
    );
  }

  public static void drawSpriteNineSliced(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      TextureAtlasSprite sprite,
      int x,
      int y,
      int width,
      int height,
      int texWidth,
      int texHeight,
      int color,
      Spacing border
  ) {
    GuiSpriteScaling.NineSlice nineSlice = new GuiSpriteScaling.NineSlice(
        texWidth,
        texHeight,
        new GuiSpriteScaling.NineSlice.Border(border.left(), border.top(), border.right(), border.bottom()),
        false
    );
    ((GuiGraphicsExtractorAccessor) context).invokeBlitNineSlicedSprite(
        pipeline,
        sprite,
        nineSlice,
        x,
        y,
        width,
        height,
        color
    );
  }

  public static void drawSpriteRegion(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      TextureAtlasSprite sprite,
      int textureWidth,
      int textureHeight,
      int u,
      int v,
      int x,
      int y,
      int width,
      int height,
      int color
  ) {
    ((GuiGraphicsExtractorAccessor) context).invokeBlitSprite(
        pipeline,
        sprite,
        textureWidth,
        textureHeight,
        u,
        v,
        x,
        y,
        width,
        height,
        color
    );
  }

  public static void drawTexturedQuad(GuiGraphicsExtractor context, Identifier sprite, int x1, int x2, int y1, int y2) {
    drawTexturedQuad(context, sprite, x1, x2, y1, y2, 0, 1, 0, 1);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2
  ) {
    drawTexturedQuad(context, pipeline, sprite, x1, x2, y1, y2, 0, 1, 0, 1);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2,
      float u1,
      float u2,
      float v1,
      float v2
  ) {
    drawTexturedQuad(context, sprite, x1, x2, y1, y2, u1, u2, v1, v2, CommonColors.WHITE);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2,
      float u1,
      float u2,
      float v1,
      float v2
  ) {
    drawTexturedQuad(context, pipeline, sprite, x1, x2, y1, y2, u1, u2, v1, v2, CommonColors.WHITE);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2,
      int color
  ) {
    drawTexturedQuad(context, sprite, x1, x2, y1, y2, 0, 1, 0, 1, color);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2,
      int color
  ) {
    drawTexturedQuad(context, pipeline, sprite, x1, x2, y1, y2, 0, 1, 0, 1, color);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2,
      float u1,
      float u2,
      float v1,
      float v2,
      int color
  ) {
    drawTexturedQuad(context, RenderPipelines.GUI_TEXTURED, sprite, x1, x2, y1, y2, u1, u2, v1, v2, color);
  }

  public static void drawTexturedQuad(
      GuiGraphicsExtractor context,
      RenderPipeline pipeline,
      Identifier sprite,
      int x1,
      int x2,
      int y1,
      int y2,
      float u1,
      float u2,
      float v1,
      float v2,
      int color
  ) {
    ((GuiGraphicsExtractorAccessor) context).invokeInnerBlit(
        pipeline,
        sprite,
        x1,
        x2,
        y1,
        y2,
        u1,
        u2,
        v1,
        v2,
        color
    );
  }

  public static void enableScissor(GuiGraphicsExtractor context, IntRect rect) {
    context.enableScissor(rect.left(), rect.top(), rect.right(), rect.bottom());
  }

  public static void disableScissor(GuiGraphicsExtractor context) {
    context.disableScissor();
  }

  public static int genColorInt(float r, float g, float b) {
    return genColorInt(r, g, b, 1f);
  }

  public static int genColorInt(int r, int g, int b) {
    return genColorInt(r, g, b, 255);
  }

  public static int genColorInt(float r, float g, float b, float a) {
    return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
  }

  public static int genColorInt(int r, int g, int b, int a) {
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  public static void playSound(SoundManager soundManager, SoundEvent soundEvent, float volume) {
    soundManager.play(SimpleSoundInstance.forUI(soundEvent, volume));
  }

  public static void playSound(SoundEvent soundEvent, float volume) {
    playSound(getClient().getSoundManager(), soundEvent, volume);
  }

  public static void playClickSound() {
    playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f);
  }

  public static void playClickSound(SoundManager soundManager) {
    playSound(soundManager, SoundEvents.UI_BUTTON_CLICK.value(), 1f);
  }
}
