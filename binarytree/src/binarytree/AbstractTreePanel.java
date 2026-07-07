package binarytree;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.JPanel;

import node.ValuedNode;
import tree.BSTTree;

public abstract class AbstractTreePanel<N extends ValuedNode<N, Integer>> extends JPanel {

	// CONSTANTES DE DESSIN, PARTAGÉES

	protected static final int NODE_DIAMETER = 30;
	protected static final int VERTICAL_GAP = 60;
	protected static final int HORIZONTAL_GAP = 40;

	// NAVIGATION (pan & zoom), IDENTIQUE POUR TOUS LES ARBRES

	protected double scale = 1.0;
	protected double offsetX = 0;
	protected double offsetY = 0;
	private int lastMouseX;
	private int lastMouseY;

	// MODÈLE ET DISPOSITION

	protected BSTTree<N, Integer> model;
	protected final Map<N, Point2D.Double> positions = new IdentityHashMap<>();
	protected int inorderCounter;

	// ÉTAT COMMUN À TOUTE ANIMATION

	protected boolean animationMode = false;
	protected boolean stepMode = false;
	protected boolean busy = false;
	protected Integer highlightedValue = null;
	protected Integer replacementValue = null;

	// CONSTRUCTEURS

	protected AbstractTreePanel() {
		setupNavigation();
		applyDefaultBackground();
	}

	// REQUETES COMMUNES

	public BSTTree<N, Integer> getModel() {
		return model;
	}

	public boolean isBusy() {
		return busy;
	}

	public boolean searchInt(Integer i) {
		return model.isIn(i);
	}

	public abstract boolean isAnimating();

	public abstract void nextStep();

	// COMMANDES COMMUNES

	public void setAnimationMode(boolean enabled) {
		this.animationMode = enabled;
	}

	public void setStepMode(boolean enabled) {
		this.stepMode = enabled;
	}

	public void resetView() {
		scale = 1.0;
		offsetX = 0;
		offsetY = 0;
		repaint();
	}

	protected void applyDefaultBackground() {
		setBackground(Color.WHITE);
	}

	// ================== DISPOSITION (calcul des positions) ==================

	// Recalcule `positions` à partir de la racine actuelle du modèle.
	protected void recomputePositions() {
		positions.clear();
		inorderCounter = 0;
		layoutAux(model.getRoot(), 0, positions);
	}

	// Calcule une disposition dans une map INDÉPENDANTE (utile pour les snapshots avant/après une rotation).
	protected Map<N, Point2D.Double> snapshotLayout() {
		Map<N, Point2D.Double> snapshot = new IdentityHashMap<>();
		inorderCounter = 0;
		layoutAux(model.getRoot(), 0, snapshot);
		return snapshot;
	}

	private void layoutAux(N node, int depth, Map<N, Point2D.Double> target) {
		if (node == null) {
			return;
		}
		layoutAux(node.getLeft(), depth + 1, target);
		double x = inorderCounter * HORIZONTAL_GAP + 40;
		double y = depth * VERTICAL_GAP + 40;
		target.put(node, new Point2D.Double(x, y));
		inorderCounter++;
		layoutAux(node.getRight(), depth + 1, target);
	}

	// ================== DESSIN (commun) ==================

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.translate(offsetX, offsetY);
		g2.scale(scale, scale);

		N root = model.getRoot();
		if (root != null) {
			recomputePositions();
			drawTree(g2, root);
		}
	}

	protected void drawTree(Graphics g, N node) {
		Point2D.Double p = positions.get(node);

		if (node.getLeft() != null) {
			Point2D.Double childP = positions.get(node.getLeft());
			g.drawLine((int) p.x, (int) p.y, (int) childP.x, (int) childP.y);
			drawTree(g, node.getLeft());
		}
		if (node.getRight() != null) {
			Point2D.Double childP = positions.get(node.getRight());
			g.drawLine((int) p.x, (int) p.y, (int) childP.x, (int) childP.y);
			drawTree(g, node.getRight());
		}

		drawNodeCircle(g, node, p);
	}

	protected void drawNodeCircle(Graphics g, N node, Point2D.Double p) {
		String s = String.valueOf(node.getValue());
		FontMetrics fm = g.getFontMetrics();
		int strWidth = fm.stringWidth(s);
		int diameter = Math.max(NODE_DIAMETER, strWidth + 16);

		if (replacementValue != null && replacementValue.equals(node.getValue())) {
			g.setColor(Color.GREEN);
		} else if (highlightedValue != null && highlightedValue.equals(node.getValue())) {
			g.setColor(Color.RED);
		} else {
			g.setColor(Color.WHITE);
		}
		g.fillOval((int) p.x - diameter / 2, (int) p.y - diameter / 2, diameter, diameter);
		g.setColor(Color.BLACK);
		g.drawOval((int) p.x - diameter / 2, (int) p.y - diameter / 2, diameter, diameter);

		g.drawString(s, (int) p.x - strWidth / 2, (int) p.y + fm.getAscent() / 2 - 2);
	}

	// ================== NAVIGATION (pan & zoom) ==================

	private void setupNavigation() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				lastMouseX = e.getX();
				lastMouseY = e.getY();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				offsetX += e.getX() - lastMouseX;
				offsetY += e.getY() - lastMouseY;
				lastMouseX = e.getX();
				lastMouseY = e.getY();
				repaint();
			}
		});

		addMouseWheelListener(this::handleZoom);
	}

	private void handleZoom(MouseWheelEvent e) {
		double zoomFactor = (e.getWheelRotation() < 0) ? 1.1 : 0.9;
		double mouseX = e.getX();
		double mouseY = e.getY();
		offsetX = mouseX - (mouseX - offsetX) * zoomFactor;
		offsetY = mouseY - (mouseY - offsetY) * zoomFactor;
		scale *= zoomFactor;
		repaint();
	}
}