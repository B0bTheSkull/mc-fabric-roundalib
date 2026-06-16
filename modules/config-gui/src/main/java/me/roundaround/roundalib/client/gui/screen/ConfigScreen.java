package me.roundaround.roundalib.client.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.config.ConfigListWidget;
import me.roundaround.roundalib.config.manage.ModConfig;
import me.roundaround.roundalib.observable.Subscription;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
  protected final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  protected final Screen parent;
  protected final String modId;
  protected final double prevScrollAmount;
  protected final ArrayList<ModConfig> configs = new ArrayList<>();
  protected final ArrayList<Subscription> subscriptions = new ArrayList<>();

  protected ConfigListWidget configListWidget;
  protected CloseAction closeAction = CloseAction.NOOP;

  public ConfigScreen(Screen parent, String modId, ModConfig... configs) {
    this(Component.translatable(modId + ".config.title"), parent, modId, configs);
  }

  public ConfigScreen(Screen parent, String modId, Iterable<ModConfig> configs) {
    this(Component.translatable(modId + ".config.title"), parent, modId, configs);
  }

  public ConfigScreen(Component title, Screen parent, String modId, ModConfig... configs) {
    this(title, parent, modId, List.of(configs));
  }

  public ConfigScreen(Component title, Screen parent, String modId, Iterable<ModConfig> configs) {
    this(title, parent, modId, configs, 0);
  }

  protected ConfigScreen(Component title, Screen parent, String modId, Iterable<ModConfig> configs, double scrollAmount) {
    super(title);
    this.parent = parent;
    this.modId = modId;
    this.prevScrollAmount = scrollAmount;

    for (ModConfig config : configs) {
      if (config.isReady()) {
        this.configs.add(config);
      }
    }
  }

  public String getModId() {
    return this.modId;
  }

  public ConfigScreen copy() {
    return new ConfigScreen(
        this.getTitle(), this.parent, this.modId, this.configs, this.configListWidget.getScrollAmount());
  }

  @Override
  protected void init() {
    this.initHeader();
    this.initBody();
    this.initFooter();

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  protected void initHeader() {
    this.layout.addHeader(this.font, this.title);
  }

  protected void initBody() {
    this.configListWidget = this.layout.addBody(
        new ConfigListWidget(this.minecraft, this.layout, this.modId, this.configs), (parent, self) -> {
          self.setRectangle(parent.getWidth(), parent.getHeight(), parent.getX(), parent.getY());
        });
    this.configListWidget.setScrollAmount(this.prevScrollAmount);
    this.subscriptions.addAll(this.configListWidget.collectSubscriptions());
  }

  protected void initFooter() {
    this.layout.addFooter(Button.builder(CommonComponents.GUI_CANCEL, (button) -> this.cancel()).build());
    this.layout.addFooter(Button.builder(CommonComponents.GUI_DONE, (button) -> this.done()).build());
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  @Override
  public void onClose() {
    Objects.requireNonNull(this.minecraft).setScreenAndShow(this.parent);
  }

  @Override
  public void removed() {
    this.subscriptions.forEach(Subscription::close);
    this.subscriptions.clear();

    this.configs.forEach((config) -> {
      this.closeAction.run(config);
    });
  }

  @Override
  public void tick() {
    this.configListWidget.tick();
  }

  protected void cancel() {
    this.closeAction = ModConfig::readFromStore;
    this.onClose();
  }

  protected void done() {
    this.closeAction = ModConfig::writeToStore;
    this.onClose();
  }

  @FunctionalInterface
  protected interface CloseAction {
    CloseAction NOOP = (config) -> {
    };

    void run(ModConfig config);
  }
}
