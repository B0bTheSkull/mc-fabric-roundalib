package me.roundaround.roundalib.client.gui.widget.config;

import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import me.roundaround.roundalib.config.option.ConfigOption;
import me.roundaround.roundalib.config.panic.IllegalStatePanic;
import me.roundaround.roundalib.config.panic.Panic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Function;

public class SubScreenControl<D, O extends ConfigOption<D>> extends Control<D, O> {
  private final Function<O, Component> messageFactory;
  private final SubScreenFactory<D, O> subScreenFactory;
  private final Button button;

  public SubScreenControl(
      Minecraft client, O option, int width, int height, SubScreenFactory<D, O> subScreenFactory
  ) {
    this(client, option, width, height, (value) -> Component.translatable(option.getModId() + ".roundalib.subscreen.label"),
        subScreenFactory
    );
  }

  public SubScreenControl(
      Minecraft client,
      O option,
      int width,
      int height,
      Function<O, Component> messageFactory,
      SubScreenFactory<D, O> subScreenFactory
  ) {
    super(client, option, width, height);
    this.messageFactory = messageFactory;
    this.subScreenFactory = subScreenFactory;

    this.button = this.add(Button.builder(this.messageFactory.apply(this.getOption()), (button) -> {
      if (!(client.gui.screen() instanceof ConfigScreen screen)) {
        Panic.panic(new IllegalStatePanic("Sub-screens can only be created from ConfigScreens."), option.getModId());
        return;
      }
      GuiUtil.setScreen(this.subScreenFactory.create(screen, this.getOption()));
    }).build(), (parent, self) -> self.setSize(parent.getWidth(), parent.getHeight()));
  }

  @Override
  protected void update(D value, boolean isDisabled) {
    this.button.active = !isDisabled;
    this.button.setMessage(this.messageFactory.apply(this.getOption()));
  }

  public static <D, O extends ConfigOption<D>> Function<O, Component> getValueDisplayMessageFactory() {
    return (option) -> Component.nullToEmpty(option.getPendingValueAsString());
  }

  @FunctionalInterface
  public interface SubScreenFactory<D, O extends ConfigOption<D>> {
    Screen create(ConfigScreen parent, O option);
  }
}
