package utils;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JCheckBox;

public class StyledCheckBox extends JCheckBox {

	private static final Color ON_COLOR = new Color(255, 40, 40);   // rouge, comme ton image
	private static final Color OFF_COLOR = new Color(200, 200, 205);
	private static final Color THUMB_COLOR = Color.WHITE;
	private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);

	private static final int TRACK_WIDTH = 52;
	private static final int TRACK_HEIGHT = 28;
	private static final int THUMB_MARGIN = 3;

	public StyledCheckBox(String text) {
		super(text);
		setFont(LABEL_FONT);
		setIcon(new javax.swing.Icon() { // NOUVEAU : remplace l'icône carrée par le toggle custom
			@Override
			public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
				paintToggle(g, x, y);
			}
			@Override
			public int getIconWidth() {
				return TRACK_WIDTH;
			}
			@Override
			public int getIconHeight() {
				return TRACK_HEIGHT;
			}
		});
		setFocusPainted(false);
		setContentAreaFilled(false); // pas de fond derrière le texte
		setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	// NOUVEAU : dessine la piste arrondie + le rond
	private void paintToggle(Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color trackColor = isSelected() ? ON_COLOR : OFF_COLOR;
		g2.setColor(trackColor);
		g2.fillRoundRect(x, y, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

		int thumbDiameter = TRACK_HEIGHT - 2 * THUMB_MARGIN;
		int thumbX = isSelected()
				? x + TRACK_WIDTH - thumbDiameter - THUMB_MARGIN
				: x + THUMB_MARGIN;
		int thumbY = y + THUMB_MARGIN;

		g2.setColor(THUMB_COLOR);
		g2.fillOval(thumbX, thumbY, thumbDiameter, thumbDiameter);

		g2.dispose();
	}
}