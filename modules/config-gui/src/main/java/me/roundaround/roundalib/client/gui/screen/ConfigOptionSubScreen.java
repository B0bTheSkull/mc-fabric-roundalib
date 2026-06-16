package me.roundaround.roundalib.client.gui.screen;

import me.roundaround.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.roundalib.client.gui.layout.NonPositioningLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.roundalib.config.option.ConfigOption;
import me.roundaround.roundalib.observable.Subject;
import me.roundaround.roundalib.observable.Subscription;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ConfigOptionSubScreen<D, O extends ConfigOption<D>> extends Screen {
  protected final ConfigScreen parent;
  protected final O option;
  protected final String modId;
  protected final Component helpShortText;
  protected final Component helpCloseText;
  protected final Component helpResetText;
  protected Subject<Boolean> shiftState;
  protected final NonPositioningLayoutWidget nonPositioningRoot = new NonPositioningLayoutWidget();
  protected final List<Subscription> subscriptions = new ArrayList<>();

  protected LabelWidget titleLabel;
  protected LabelWidget helpLabel;
  protected LinearLayoutWidget actionRow;

  protected ConfigOptionSubScreen(Component title, ConfigScreen parent, O option) {
    super(title);
    this.parent = parent;
    this.option = option;
    this.modId = option.getModId();

    this.helpShortText = Component.translatable(this.modId + ".roundalib.help.short");
    this.helpCloseText = Component.translatable(this.modId + ".roundalib.help.close");
    this.helpResetText = InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY ?
        Component.translatable(this.modId + ".roundalib.help.reset.mac") :
        Component.translatable(this.modId + ".roundalib.help.reset.win");

    this.shiftState = Subject.of(false);
  }

  @Override
  protected void init() {
    this.initElements();
    this.nonPositioningRoot.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  protected void initElements() {
    this.titleLabel = this.createTitleLabel();
    this.placeTitleLabel(this.titleLabel);

    this.helpLabel = this.createHelpLabel();
    this.placeHelpLabel(this.helpLabel);

    this.actionRow = this.createActionRow();
    this.placeActionRow(this.actionRow);
  }

  @Override
  protected void repositionElements() {
    this.nonPositioningRoot.setPositionAndDimensions(0, 0, this.width, this.height);
    this.nonPositioningRoot.arrangeElements();
  }

  @Override
  public void onClose() {
    this.subscriptions.forEach(Subscription::close);
    this.subscriptions.clear();

    Objects.requireNonNull(this.minecraft).setScreenAndShow(this.parent.copy());
  }

  @Override
  public boolean keyPressed(KeyEvent input) {
    this.shiftState.set(input.hasShiftDown());

    if (input.input() == GLFW.GLFW_KEY_R) {
      if (input.hasControlDown()) {
        this.resetToDefault();
        return true;
      }
    }

    return super.keyPressed(input);
  }

  @Override
  public boolean keyReleased(KeyEvent input) {
    this.shiftState.set(input.hasShiftDown());
    return super.keyReleased(input);
  }

  protected List<Component> getCurrentHelp() {
    return this.getCurrentHelp(this.shiftState.get());
  }

  protected List<Component> getCurrentHelp(boolean shiftDown) {
    return this.shiftState.get() ? this.getHelpLong() : this.getHelpShort();
  }

  protected List<Component> getHelpShort() {
    return List.of(this.helpShortText);
  }

  protected List<Component> getHelpLong() {
    return List.of(this.helpCloseText, this.helpResetText);
  }

  protected O getOption() {
    return this.option;
  }

  protected void setValue(D value) {
    this.getOption().setValue(value);
  }

  protected D getValue() {
    return this.getOption().getPendingValue();
  }

  protected String getValueAsString() {
    return this.getOption().getPendingValueAsString();
  }

  protected void resetToDefault() {
    this.getOption().setDefault();
  }

  protected boolean isDirty() {
    return this.getOption().isDirty();
  }

  protected LabelWidget createTitleLabel() {
    return this.createTitleLabel(this.getTitle());
  }

  protected LabelWidget createTitleLabel(Component text) {
    return LabelWidget.builder(this.font, text)
        .alignTextCenterX()
        .alignTextCenterY()
        .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
        .hideBackground()
        .showShadow()
        .build();
  }

  protected void placeTitleLabel(LabelWidget titleLabel) {
    this.nonPositioningRoot.add(
        titleLabel, (parent, self) -> {
          self.setRectangle(
              parent.getWidth(),
              HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT,
              parent.getX(),
              parent.getY()
          );
        }
    );
  }

  protected LabelWidget createHelpLabel() {
    LabelWidget helpLabel = LabelWidget.builder(this.font, this.getCurrentHelp())
        .alignTextLeft()
        .alignTextBottom()
        .lineSpacing(2)
        .hideBackground()
        .showShadow()
        .build();
    this.subscriptions.add(this.shiftState.subscribe((shiftDown) -> {
      helpLabel.setText(this.getCurrentHelp(shiftDown));
    }));
    return helpLabel;
  }

  protected void placeHelpLabel(LabelWidget helpLabel) {
    this.nonPositioningRoot.add(
        helpLabel, (parent, self) -> {
          self.setRectangle(
              parent.getWidth() - 2 * GuiUtil.PADDING,
              parent.getHeight() - 2 * GuiUtil.PADDING,
              parent.getX() + GuiUtil.PADDING,
              parent.getY() + GuiUtil.PADDING
          );
        }
    );
  }

  protected LinearLayoutWidget createActionRow() {
    LinearLayoutWidget actionRow = LinearLayoutWidget.horizontal()
        .spacing(GuiUtil.PADDING)
        .defaultOffAxisContentAlignCenter();

    actionRow.add(this.createBackButton());
    actionRow.add(this.createResetButton());

    return actionRow;
  }

  protected IconButtonWidget createBackButton() {
    return IconButtonWidget.builder(BuiltinIcon.BACK_18, this.modId)
        .onPress((button) -> this.onClose())
        .messageAndTooltip(Component.translatable(this.modId + ".roundalib.back.tooltip"))
        .build();
  }

  protected IconButtonWidget createResetButton() {
    IconButtonWidget resetButton = IconButtonWidget.builder(BuiltinIcon.UNDO_18, this.modId)
        .onPress((button) -> this.resetToDefault())
        .messageAndTooltip(Component.translatable(this.modId + ".roundalib.reset.tooltip"))
        .build();
    this.subscriptions.add(this.getOption().isPendingDefault.subscribe((isPendingDefault) -> {
      resetButton.active = !isPendingDefault;
    }));
    return resetButton;
  }

  protected void placeActionRow(LinearLayoutWidget actionRow) {
    this.nonPositioningRoot.add(
        actionRow.alignSelfRight().alignSelfBottom(), (parent, self) -> {
          self.setPosition(
              parent.getX() + parent.getWidth() - GuiUtil.PADDING,
              parent.getY() + parent.getHeight() - GuiUtil.PADDING
          );
        }
    );
  }
}
