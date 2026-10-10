package screen;

import java.awt.event.KeyEvent;

import engine.Cooldown;
import engine.Core;
import entity.ShipType;

/**
 * Screen where the player selects a ship type before playing.
 */
public class ShipSelectionScreen extends Screen {

    /** Milliseconds between changes in user selection. */
    private static final int SELECTION_TIME = 200;

    /** Time between changes in user selection. */
    private Cooldown selectionCooldown;
    /** Available ship types. */
    private final ShipType[] shipTypes = ShipType.values();
    /** Currently selected index. */
    private int selectedIndex = 0;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width Screen width.
     * @param height Screen height.
     * @param fps Frames per second.
     */
    public ShipSelectionScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.returnCode = 2;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();

        draw();
        if (this.selectionCooldown.checkFinished()
                && this.inputDelay.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP)
                    || inputManager.isKeyDown(KeyEvent.VK_W)) {
                this.selectedIndex = (this.selectedIndex - 1 + this.shipTypes.length) % this.shipTypes.length;
                this.selectionCooldown.reset();
            }
            if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                    || inputManager.isKeyDown(KeyEvent.VK_S)) {
                this.selectedIndex = (this.selectedIndex + 1) % this.shipTypes.length;
                this.selectionCooldown.reset();
            }
            if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                this.isRunning = false;
            }
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawCenteredBigString(this, "SELECT SHIP", this.height / 5);

        for (int i = 0; i < this.shipTypes.length; i++) {
            boolean isSelected = (i == this.selectedIndex);
            ShipType type = this.shipTypes[i];

            String title = (isSelected ? "> " : "  ") + type.getName();
            String stats = "SPD " + type.getSpeed() + "  HP " + type.getMaxLives();

            drawManager.drawCenteredRegularString(this, title, this.height / 2 + i * 50);
            drawManager.drawCenteredRegularString(this, stats, this.height / 2 + i * 50 + 20);
        }

        drawManager.drawCenteredRegularString(this, "Press SPACE to start", this.height * 9 / 10);
        drawManager.drawShipSelection(this, this.selectedIndex, this.shipTypes);
        drawManager.completeDrawing(this);
    }

    /**
     * Returns the selected ship type.
     *
     * @return Selected ship type.
     */
    public final ShipType getSelectedShip() {
        return this.shipTypes[this.selectedIndex];
    }
}