package screen;

import java.awt.event.KeyEvent;

import audio.AudioManager;
import engine.Cooldown;
import engine.Core;
import engine.GameplayConfig;

public class GameplaySettingsScreen extends Screen {
    private static final int OPTION_COUNT = 3;
    private static final int SELECTION_TIME = 200;

    private final Cooldown selectionCooldown;
    private int selectedOption;

    public GameplaySettingsScreen(final int width, final int height,
            final int fps) {
        super(width, height, fps);
        this.returnCode = 4;
        this.selectedOption = GameplayConfig.getDifficulty();
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
    }

    @Override
    public int run() {
        super.run();
        return this.returnCode;
    }

    @Override
    protected void update() {
        super.update();
        draw();

        if (!selectionCooldown.checkFinished()
                || !inputDelay.checkFinished()) {
            return;
        }

        if (inputManager.isKeyDown(KeyEvent.VK_UP)
                || inputManager.isKeyDown(KeyEvent.VK_W)) {
            selectedOption = (selectedOption - 1 + OPTION_COUNT)
                    % OPTION_COUNT;
            selectionCooldown.reset();
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        } else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                || inputManager.isKeyDown(KeyEvent.VK_S)) {
            selectedOption = (selectedOption + 1) % OPTION_COUNT;
            selectionCooldown.reset();
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        } else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            GameplayConfig.setDifficulty(selectedOption);
            returnCode = 4;
            isRunning = false;
        } else if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            returnCode = 4;
            isRunning = false;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawGameplaySettings(this, selectedOption);
        drawManager.completeDrawing(this);
    }
}