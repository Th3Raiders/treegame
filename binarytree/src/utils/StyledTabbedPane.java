package utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public class StyledTabbedPane extends JTabbedPane {

	private static final Color SELECTED_COLOR = new Color(20, 20, 20);      // MODIFIÉ : noir, cohérent avec les boutons
	private static final Color UNSELECTED_COLOR = new Color(235, 235, 238);
	private static final Color TEXT_COLOR_SELECTED = Color.WHITE;
	private static final Color TEXT_COLOR_UNSELECTED = new Color(100, 100, 100);
	private static final Font TAB_FONT = new Font("Segoe UI", Font.BOLD, 14);
	private static final int ARC = 20; // NOUVEAU : rayon des coins arrondis

	public StyledTabbedPane() {
		super();
		setFont(TAB_FONT);
		setBackground(UNSELECTED_COLOR);
		setForeground(TEXT_COLOR_UNSELECTED);

		setUI(new BasicTabbedPaneUI() {
			@Override
			protected void installDefaults() {
				super.installDefaults();
				lightHighlight = SELECTED_COLOR;
				shadow = UNSELECTED_COLOR;
				darkShadow = UNSELECTED_COLOR;
				focus = SELECTED_COLOR;
				tabInsets.set(12, 24, 12, 24);
				contentBorderInsets.set(0, 0, 0, 0);
			}

			// MODIFIÉ : fond arrondi au lieu d'un rectangle plein
			@Override
			protected void paintTabBackground(Graphics g, int tabPlacement,
					int tabIndex, int x, int y, int w, int h, boolean isSelected) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(isSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
				// NOUVEAU : arrondi uniquement en haut, comme un vrai onglet
				g2.fillRoundRect(x, y, w, h + ARC, ARC, ARC);
				g2.dispose();
			}

			// NOUVEAU : supprime le contour par défaut dessiné autour de chaque onglet
			@Override
			protected void paintTabBorder(Graphics g, int tabPlacement,
					int tabIndex, int x, int y, int w, int h, boolean isSelected) {
				// volontairement vide
			}

			@Override
			protected void paintText(Graphics g, int tabPlacement,
					Font font, java.awt.FontMetrics metrics, int tabIndex,
					String title, java.awt.Rectangle textRect, boolean isSelected) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setFont(font);
				g2.setColor(isSelected ? TEXT_COLOR_SELECTED : TEXT_COLOR_UNSELECTED);
				g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
				g2.dispose();
			}

			@Override
			protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
				return 0;
			}

			@Override
			protected void paintFocusIndicator(Graphics g, int tabPlacement,
					java.awt.Rectangle[] rects, int tabIndex,
					java.awt.Rectangle iconRect, java.awt.Rectangle textRect, boolean isSelected) {
				// volontairement vide
			}

			// NOUVEAU : supprime la bordure du contenu sous les onglets (le cadre gris par défaut)
			@Override
			protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
				// volontairement vide
			}
		});
	}
}