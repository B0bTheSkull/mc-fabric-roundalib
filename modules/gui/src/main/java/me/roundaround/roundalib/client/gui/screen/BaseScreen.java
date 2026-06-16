package me.roundaround.roundalib.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class BaseScreen extends Screen {
  protected final @NotNull ScreenParent parent;
  protected final @NotNull Minecraft minecraft;

  public BaseScreen(
      @NotNull Component title,
      @NotNull ScreenParent parent,
      @NotNull Minecraft minecraft) {
    super(title);
    this.parent = parent;
    this.minecraft = minecraft;
  }

  @Override
  public void onClose() {
    this.minecraft.setScreenAndShow(this.parent.get());
  }

  protected void navigateTo(Screen screen) {
    this.minecraft.setScreenAndShow(screen);
  }

  protected void done(Button button) {
    this.onClose();
  }
}
