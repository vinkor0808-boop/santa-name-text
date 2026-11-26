package de.windfwm.ui;

import de.windfwm.WindFwm;
import de.windfwm.core.GameContext;
import de.windfwm.core.Keybinds;
import de.windfwm.ui.modules.ChatWindow;
import de.windfwm.ui.modules.DashboardView;
import de.windfwm.ui.modules.MoneyStatsWindow;
import de.windfwm.ui.modules.MoneyTrackerWindow;
import de.windfwm.ui.modules.PlayerWindow;
import de.windfwm.ui.modules.SettingsWindow;
import de.windfwm.ui.modules.ShopWindow;
import de.windfwm.ui.modules.WaypointWindow;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Hauptmenü: linke Navigation, Dashboard in der Mitte, Module als verschiebbare Fenster. */
public final class WindScreen extends Screen {
    public static final int SIDEBAR_W = 118;

    private final UI ui = new UI();
    /** Reihenfolge = Zeichenreihenfolge (letztes Element liegt oben). */
    private final List<Window> windows = new ArrayList<>();
    private final List<Window> navOrder = new ArrayList<>();
    private final DashboardView dashboard = new DashboardView();
    private Window dragging, resizing;
    private int dragOffX, dragOffY;
    private int mouseX, mouseY;

    public WindScreen() {
        super(Text.literal("WindFWM"));
        navOrder.add(new MoneyTrackerWindow());
        navOrder.add(new MoneyStatsWindow());
        navOrder.add(new ShopWindow());
        navOrder.add(new PlayerWindow());
        navOrder.add(new WaypointWindow());
        navOrder.add(new ChatWindow());
        navOrder.add(new SettingsWindow());
        windows.addAll(navOrder);
        ui.setAnim("screen", 0f);
    }

    @Override
    protected void init() {
        for (int i = 0; i < navOrder.size(); i++) navOrder.get(i).loadState(width, height, i);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Hintergrund wird in render() selbst gezeichnet (kein Weichzeichner, keine Doppelzeichnung).
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        mouseX = mx;
        mouseY = my;
        updateDrag();
        ui.hoverOwner = ownerAt(mx, my);
        ui.begin(ctx, mx, my);
        float a = ui.anim("screen", 1f, 12f);
        Draw.alpha = a;

        Draw.rect(ctx, 0, 0, width, height, Theme.BACKDROP);
        renderSidebar(ctx);
        int x0 = SIDEBAR_W + 14;
        dashboard.render(ui, this, x0, 12, width - x0 - 14, height - 24);

        for (Window w : new ArrayList<>(windows)) w.render(ui, this);
        Draw.alpha = 1f;
        ui.end();
    }

    // ------------------------------------------------------------------ Navigation

    private void renderSidebar(DrawContext ctx) {
        ui.owner = "main";
        Draw.rect(ctx, 0, 0, SIDEBAR_W, height, Theme.SIDEBAR);
        Draw.rect(ctx, SIDEBAR_W, 0, 1, height, Theme.BORDER_SOFT);

        // Logo
        Draw.round(ctx, 12, 12, 20, 20, 5, Theme.accent());
        Draw.textCentered(ctx, "W", 22, 18, 0xFF0E1114);
        Draw.text(ctx, "WindFWM", 38, 13, Theme.TEXT);
        Draw.text(ctx, "v" + WindFwm.VERSION, 38, 23, Theme.TEXT_FAINT);

        int y = 46;
        ui.sectionTitle("Übersicht", 12, y);
        y += 12;
        boolean noneOpen = windows.stream().noneMatch(w -> w.open);
        if (navItem(ctx, y, Icons.DASHBOARD, "Dashboard", noneOpen, "nav-dash")) {
            for (Window w : windows) if (w.open) closeWindow(w);
        }
        y += 22;
        ui.sectionTitle("Module", 12, y);
        y += 12;
        for (Window w : navOrder) {
            if (w instanceof SettingsWindow) continue;
            if (navItem(ctx, y, w.icon, w.title, w.open, "nav-" + w.id)) toggleWindow(w);
            y += 21;
        }

        // unten: Server-Status + Einstellungen
        Window settings = navOrder.get(navOrder.size() - 1);
        int by = height - 54;
        boolean wind = GameContext.isWindSmp();
        Draw.dot(ctx, 16, by + 4, wind ? Theme.GREEN : Theme.TEXT_FAINT);
        Draw.textTrim(ctx, GameContext.serverLabel(), 24, by, SIDEBAR_W - 30, wind ? Theme.TEXT : Theme.TEXT_DIM);
        if (navItem(ctx, height - 36, Icons.GEAR, "Einstellungen", settings.open, "nav-settings")) toggleWindow(settings);
    }

    private boolean navItem(DrawContext ctx, int y, Icons icon, String label, boolean active, String key) {
        int x = 6, w = SIDEBAR_W - 12, h = 19;
        boolean hov = ui.hover(x, y, w, h);
        float t = ui.anim(key + "h", hov ? 1 : 0, 18f);
        float act = ui.anim(key + "a", active ? 1 : 0, 14f);
        if (act > 0.01f) Draw.round(ctx, x, y, w, h, 4, Theme.accent(Math.round(32 * act)));
        if (t > 0.01f) Draw.round(ctx, x, y, w, h, 4, Theme.withAlpha(0xFFFFFF, Math.round(t * 10)));
        if (act > 0.01f) Draw.round(ctx, x, y + 4, 2, Math.max(2, Math.round((h - 8) * act)), 1, Theme.accent());
        int col = Theme.mix(Theme.mix(Theme.TEXT_DIM, Theme.TEXT, t), Theme.accent(), act);
        Draw.icon(ctx, icon, x + 8, y + 5, col);
        Draw.textTrim(ctx, label, x + 23, y + 6, w - 28, active ? Theme.TEXT : col);
        return ui.pressed(x, y, w, h);
    }

    // ------------------------------------------------------------------ Fensterverwaltung

    public Window window(String id) {
        for (Window w : navOrder) if (w.id.equals(id)) return w;
        return null;
    }

    public void openWindow(String id) {
        Window w = window(id);
        if (w == null) return;
        if (!w.open) {
            w.open = true;
            ui.setAnim("win#" + w.id, 0f);
        }
        bringToFront(w);
        w.saveState();
    }

    public void toggleWindow(Window w) {
        if (w.open && isFront(w)) closeWindow(w);
        else openWindow(w.id);
    }

    public void closeWindow(Window w) {
        w.open = false;
        if (dragging == w) dragging = null;
        if (resizing == w) resizing = null;
        w.saveState();
    }

    public boolean isFront(Window w) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).open) return windows.get(i) == w;
        }
        return false;
    }

    private void bringToFront(Window w) {
        windows.remove(w);
        windows.add(w);
    }

    private String ownerAt(int x, int y) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.hitTest(x, y)) return w.id;
        }
        return "main";
    }

    public void beginDrag(Window w, int offX, int offY) {
        dragging = w;
        dragOffX = offX;
        dragOffY = offY;
    }

    public void beginResize(Window w) {
        resizing = w;
    }

    private void updateDrag() {
        if (dragging != null) {
            dragging.x = mouseX - dragOffX;
            dragging.y = mouseY - dragOffY;
            dragging.clamp(width, height);
        }
        if (resizing != null) {
            resizing.w = mouseX - resizing.x + 4;
            resizing.h = mouseY - resizing.y + 4;
            resizing.clamp(width, height);
        }
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = (int) x;
        mouseY = (int) y;
        updateDrag();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        String owner = ownerAt(mouseX, mouseY);
        Window w = window(owner);
        if (w != null) bringToFront(w);
        ui.clickOwner = owner;
        ui.onClick(mouseX, mouseY, click.button());
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) dragging.saveState();
        if (resizing != null) resizing.saveState();
        dragging = null;
        resizing = null;
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        ui.onScroll(vertical);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (ui.capture != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                ui.capture = null;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                Keybinds.rebind(ui.capture, -1);
                ui.capture = null;
            } else {
                Keybinds.rebind(ui.capture, key);
                ui.capture = null;
            }
            return true;
        }
        if (ui.hasFocus()) {
            ui.onKey(key, input.modifiers());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || Keybinds.openMenu.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ui.hasFocus()) {
            ui.onChar(input.asString());
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        for (Window w : navOrder) w.saveState();
        WindFwm.data().config.saveAsync();
        super.close();
    }
}
