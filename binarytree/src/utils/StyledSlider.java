package utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

public class StyledSlider extends JSlider {

	private static final Color TRACK_COLOR = new Color(230, 230, 235);
	private static final Color FILLED_TRACK_COLOR = new Color(20, 20, 20);
	private static final Color THUMB_COLOR = new Color(20, 20, 20);
	private static final Color THUMB_HOVER_COLOR = new Color(60, 60, 60);
	private static final int TRACK_HEIGHT = 6;
	private static final int THUMB_DIAMETER = 18;

	public StyledSlider(int min, int max, int value) {
		super(min, max, value);
		setOpaque(false);
		setFocusable(true);
		setUI(new StyledSliderUI(this));
	}

	private class StyledSliderUI extends BasicSliderUI {

		private boolean hovering = false;

		StyledSliderUI(JSlider slider) {
			super(slider);
		}

		@Override
		protected void installListeners(JSlider slider) {
			super.installListeners(slider);
			slider.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseEntered(java.awt.event.MouseEvent e) {
					hovering = true;
					slider.repaint();
				}

				@Override
				public void mouseExited(java.awt.event.MouseEvent e) {
					hovering = false;
					slider.repaint();
				}
			});
		}

		@Override
		public void paintTrack(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Rectangle track = trackRect;
			int y = track.y + track.height / 2 - TRACK_HEIGHT / 2;

			g2.setColor(TRACK_COLOR);
			g2.fillRoundRect(track.x, y, track.width, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

			int filledWidth = thumbRect.x + thumbRect.width / 2 - track.x;
			g2.setColor(FILLED_TRACK_COLOR);
			g2.fillRoundRect(track.x, y, Math.max(filledWidth, TRACK_HEIGHT), TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

			g2.dispose();
		}

		@Override
		public void paintThumb(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int cx = thumbRect.x + thumbRect.width / 2 - THUMB_DIAMETER / 2;
			int cy = thumbRect.y + thumbRect.height / 2 - THUMB_DIAMETER / 2;

			g2.setColor(hovering ? THUMB_HOVER_COLOR : THUMB_COLOR);
			g2.fillOval(cx, cy, THUMB_DIAMETER, THUMB_DIAMETER);

			g2.dispose();
		}

		@Override
		protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
			// volontairement vide : pas de graduations mineures, pour un rendu épuré
		}

		@Override
		protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
			// volontairement vide : pas de graduations majeures
		}
	}
}