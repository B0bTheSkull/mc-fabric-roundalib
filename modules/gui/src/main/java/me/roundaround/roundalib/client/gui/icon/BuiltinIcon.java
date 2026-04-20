package me.roundaround.roundalib.client.gui.icon;

import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum BuiltinIcon implements Icon {
  BACK_13("back", 13),
  BACK_18("back", 18),
  BRUSH_18("brush", 18),
  CANCEL_13("cancel", 13),
  CANCEL_18("cancel", 18),
  CHECKMARK_13("checkmark", 13),
  CHECKMARK_18("checkmark", 18),
  CLOSE_13("close", 13),
  CLOSE_18("close", 18),
  DOWN_9("down", 9),
  DOWN_13("down", 13),
  DOWN_18("down", 18),
  FILTER_13("filter", 13),
  FILTER_18("filter", 18),
  FIX_13("fix", 13),
  FIX_18("fix", 18),
  FORWARD_13("forward", 13),
  FORWARD_18("forward", 18),
  GEAR_18("gear", 18),
  HELP_13("help", 13),
  HELP_18("help", 18),
  HIDE_18("hide", 18),
  LEFT_9("left", 9),
  LEFT_13("left", 13),
  LEFT_18("left", 18),
  MINUS_9("minus", 9),
  MINUS_13("minus", 13),
  MINUS_18("minus", 18),
  MOVE_13("move", 13),
  MOVE_18("move", 18),
  NEXT_13("next", 13),
  NEXT_18("next", 18),
  PLUS_9("plus", 9),
  PLUS_13("plus", 13),
  PLUS_18("plus", 18),
  PREV_13("prev", 13),
  PREV_18("prev", 18),
  REDO_13("redo", 13),
  REDO_18("redo", 18),
  RIGHT_9("right", 9),
  RIGHT_13("right", 13),
  RIGHT_18("right", 18),
  ROTATE_13("rotate", 13),
  ROTATE_18("rotate", 18),
  SHOW_18("show", 18),
  SLIDERS_13("sliders", 13),
  SLIDERS_18("sliders", 18),
  UNDO_13("undo", 13),
  UNDO_18("undo", 18),
  UP_9("up", 9),
  UP_13("up", 13),
  UP_18("up", 18);

  public final String name;
  public final int size;

  BuiltinIcon(String name, int size) {
    this.name = name;
    this.size = size;
  }

  @Override
  public Identifier getTexture(String modId) {
    return Identifier.fromNamespaceAndPath("roundalib", String.format("icon/roundalib/%s-%s", this.name, this.size));
  }

  @Override
  public int getSize() {
    return this.size;
  }

  public String getI18nKey(String modId) {
    return String.format("%s.roundalib.icon.%s", modId, this.name);
  }

  public Component getDisplayText(String modId) {
    return Component.translatable(this.getI18nKey(modId));
  }

  public static List<BuiltinIcon> valuesOfSize(int size) {
    return Arrays.stream(values()).filter((icon) -> icon.size == size).toList();
  }
}
