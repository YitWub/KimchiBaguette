package engine;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import engine.DrawManager.SpeechBubble;

/**
 * Checks the boss speech bubble layout: wrapping, clamping to the screen,
 * flipping under the boss near the HUD and nothing drawn without a line.
 */
public final class BossSpeechBubbleTest {

	private static final int SCREEN_WIDTH = 448;
	private static final int SCREEN_HEIGHT = 520;
	/** Where GameScreen spawns the boss. */
	private static final int BOSS_SPAWN_Y = 78;
	/** Longest line the dialogue table may use (PRD 4.2). */
	private static final String LONGEST_LINE =
			"Witness the end of your pathetic small fleet";

	private BossSpeechBubbleTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static FontMetrics gameFontMetrics() throws Exception {
		try (InputStream in = BossSpeechBubbleTest.class.getClassLoader()
				.getResourceAsStream("font.ttf")) {
			check(in != null, "Game font is on the classpath");
			Font font = Font.createFont(Font.TRUETYPE_FONT, in)
					.deriveFont(14f);
			Graphics2D graphics = new BufferedImage(1, 1,
					BufferedImage.TYPE_INT_RGB).createGraphics();
			try {
				return graphics.getFontMetrics(font);
			} finally {
				graphics.dispose();
			}
		}
	}

	private static void checkInsideScreen(final SpeechBubble bubble,
			final String where) {
		check(bubble.getX() >= SpeechBubble.SCREEN_MARGIN,
				where + ": left edge stays on screen");
		check(bubble.getX() + bubble.getWidth()
				<= SCREEN_WIDTH - SpeechBubble.SCREEN_MARGIN,
				where + ": right edge stays on screen");
		check(bubble.getY() >= SpeechBubble.HUD_BOTTOM,
				where + ": bubble stays under the top HUD");
		check(Math.min(bubble.getY(), bubble.getTailTipY())
				>= SpeechBubble.HUD_BOTTOM,
				where + ": tail stays under the top HUD");
		check(bubble.getY() + bubble.getHeight() <= SCREEN_HEIGHT,
				where + ": bottom edge stays on screen");
		check(bubble.getTailTipX() > bubble.getX()
				&& bubble.getTailTipX() < bubble.getX() + bubble.getWidth(),
				where + ": tail starts inside the box");
	}

	public static void main(final String[] args) throws Exception {
		FontMetrics metrics = gameFontMetrics();

		// Nothing to say: nothing is laid out, so nothing is drawn.
		check(SpeechBubble.layout(null, metrics, 224, 200, SCREEN_WIDTH,
				SCREEN_HEIGHT) == null, "null line draws nothing");
		check(SpeechBubble.layout("   ", metrics, 224, 200, SCREEN_WIDTH,
				SCREEN_HEIGHT) == null, "blank line draws nothing");

		// A 44-character line fits on two lines without being cut.
		check(LONGEST_LINE.length() == 44, "Sample line is 44 characters");
		List<String> lines = SpeechBubble.wrap(LONGEST_LINE, metrics,
				SpeechBubble.MAX_TEXT_WIDTH);
		check(lines.size() == 2, "44-character line wraps to two lines");
		check(String.join(" ", lines).equals(LONGEST_LINE),
				"Wrapping keeps every word");
		for (String line : lines)
			check(metrics.stringWidth(line) <= SpeechBubble.MAX_TEXT_WIDTH,
					"Wrapped line fits the bubble: " + line);

		// A short line stays on one line.
		check(SpeechBubble.wrap("You cannot stop me.", metrics,
				SpeechBubble.MAX_TEXT_WIDTH).size() == 1,
				"Short line stays on one line");

		// Too much text is cut at two lines and marked as cut.
		List<String> cut = SpeechBubble.wrap(
				LONGEST_LINE + " and every pilot who dares to follow it",
				metrics, SpeechBubble.MAX_TEXT_WIDTH);
		check(cut.size() == SpeechBubble.MAX_LINES, "Long text keeps two lines");
		check(cut.get(1).endsWith(SpeechBubble.ELLIPSIS),
				"Cut text ends with an ellipsis");
		check(metrics.stringWidth(cut.get(1)) <= SpeechBubble.MAX_TEXT_WIDTH,
				"Ellipsis still fits the bubble");

		// One word wider than the bubble is split instead of overflowing.
		List<String> longWord = SpeechBubble.wrap(
				"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", metrics,
				SpeechBubble.MAX_TEXT_WIDTH);
		for (String line : longWord)
			check(metrics.stringWidth(line) <= SpeechBubble.MAX_TEXT_WIDTH,
					"Long word is split to fit");

		// Boss in the middle, low enough: bubble above it, tail at the boss.
		SpeechBubble middle = SpeechBubble.layout(LONGEST_LINE, metrics,
				SCREEN_WIDTH / 2, 200, SCREEN_WIDTH, SCREEN_HEIGHT);
		checkInsideScreen(middle, "middle");
		check(!middle.isBelowBoss(), "Bubble goes above the boss when it fits");
		check(middle.getY() + middle.getHeight() <= 200,
				"Bubble does not cover the boss");
		check(middle.getTailTipX() == SCREEN_WIDTH / 2,
				"Tail points at the boss");
		check(middle.getTailTipY() < 200, "Tail ends above the boss");

		// Boss at either edge: bubble is pushed back inside the screen.
		int[] edgeCenters = { 1 + 16, SCREEN_WIDTH - 1 - 16 };
		for (int centerX : edgeCenters) {
			SpeechBubble edge = SpeechBubble.layout(LONGEST_LINE, metrics,
					centerX, 200, SCREEN_WIDTH, SCREEN_HEIGHT);
			checkInsideScreen(edge, "edge x=" + centerX);
		}

		// Boss at its spawn height, near the HUD: bubble flips under it.
		for (int centerX : new int[] { 17, SCREEN_WIDTH / 2, 431 }) {
			SpeechBubble top = SpeechBubble.layout(LONGEST_LINE, metrics,
					centerX, BOSS_SPAWN_Y, SCREEN_WIDTH, SCREEN_HEIGHT);
			checkInsideScreen(top, "top x=" + centerX);
			check(top.isBelowBoss(), "Bubble flips under a boss near the HUD");
			check(top.getY() >= BOSS_SPAWN_Y + SpeechBubble.BOSS_HEIGHT,
					"Flipped bubble does not cover the boss");
			check(top.getTailTipY() >= BOSS_SPAWN_Y + SpeechBubble.BOSS_HEIGHT,
					"Flipped tail points up at the boss");
		}

		System.out.println("BossSpeechBubbleTest passed");
	}
}
