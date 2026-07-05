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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.Timer;

import node.StdAVLNode;
import tree.BSTTree;
import tree.StdAVLTree;

public class AVLPanel extends JPanel {

	// ATTRIBUTS

	private static final int NODE_DIAMETER = 30;
	private static final int VERTICAL_GAP = 60;
	private static final int HORIZONTAL_GAP = 40;

	private static final int TRANSITION_DURATION_MS = 500; // NOUVEAU : durée de la translation
	private static final int TRANSITION_FPS = 60;          // NOUVEAU : fluidité de l'animation

	private double scale = 1.0;
	private double offsetX = 0;
	private double offsetY = 0;

	private int lastMouseX;
	private int lastMouseY;

	private BSTTree<StdAVLNode<Integer>, Integer> model =
			new StdAVLTree<StdAVLNode<Integer>, Integer>(Comparator.naturalOrder(), StdAVLNode::new);

	private final Map<StdAVLNode<Integer>, Point2D.Double> positions = new IdentityHashMap<>();
	private int inorderCounter;

	private boolean animationMode = false;
	private Integer highlightedValue = null;
	private Integer replacementValue = null;

	// NOUVEAU : état de la translation des nœuds (rotation)
	private boolean transitioning = false;
	private double currentT = 0; // avancement de la translation, entre 0 et 1
	private Map<StdAVLNode<Integer>, Point2D.Double> animStart = null;
	private Map<StdAVLNode<Integer>, Point2D.Double> animEnd = null;

	// CONSTRUCTEURS
	public AVLPanel() {
		setupNavigation();
		setBackground();
	}

	// REQUETES

	public BSTTree<StdAVLNode<Integer>, Integer> getModel() {
		return model;
	}

	// NOUVEAU : pour que Treegame puisse désactiver les boutons pendant une animation
	public boolean isBusy() {
		return transitioning;
	}

	// COMMANDES

	// MODIFIÉ : la mutation réelle du modèle est maintenant différée après le surlignage
	public void addInt(Integer i) {
		if (isBusy()) {
			return;
		}
		if (!animationMode) {
			model.add(i);
			repaint();
			return;
		}
		List<Integer> path = computeSearchPath(i);
		Map<StdAVLNode<Integer>, Point2D.Double> before = snapshotLayout(); // NOUVEAU
		animatePath(path, () -> {
			model.add(i); // NOUVEAU : la vraie insertion (+ rotations) n'a lieu qu'ici
			Map<StdAVLNode<Integer>, Point2D.Double> after = snapshotLayout(); // NOUVEAU
			animateTransition(before, after); // NOUVEAU
		});
	}

	// MODIFIÉ : même principe pour la suppression
	public void deleteInt(Integer i) {
		if (isBusy()) {
			return;
		}
		if (!animationMode) {
			model.delete(i);
			repaint();
			return;
		}
		if (!model.isIn(i)) {
			return;
		}
		List<Integer> path = computeDeletePath(i);
		Map<StdAVLNode<Integer>, Point2D.Double> before = snapshotLayout(); // NOUVEAU
		animatePath(path, () -> {
			replacementValue = null;
			model.delete(i); // NOUVEAU : la vraie suppression (+ rotations) n'a lieu qu'ici
			Map<StdAVLNode<Integer>, Point2D.Double> after = snapshotLayout(); // NOUVEAU
			animateTransition(before, after); // NOUVEAU
		});
	}

	public boolean searchInt(Integer i) {
		return model.isIn(i);
	}

	// NOUVEAU : calcule la position de chaque nœud SANS toucher au champ `positions` affiché
	private Map<StdAVLNode<Integer>, Point2D.Double> snapshotLayout() {
		Map<StdAVLNode<Integer>, Point2D.Double> snapshot = new IdentityHashMap<>();
		inorderCounter = 0;
		snapshotLayoutAux(model.getRoot(), 0, snapshot);
		return snapshot;
	}

	private void snapshotLayoutAux(StdAVLNode<Integer> node, int depth, Map<StdAVLNode<Integer>, Point2D.Double> target) {
		if (node == null) {
			return;
		}
		snapshotLayoutAux(node.getLeft(), depth + 1, target);
		double x = inorderCounter * HORIZONTAL_GAP + 40;
		double y = depth * VERTICAL_GAP + 40;
		target.put(node, new Point2D.Double(x, y));
		inorderCounter++;
		snapshotLayoutAux(node.getRight(), depth + 1, target);
	}

	// NOUVEAU : lance la translation progressive entre deux layouts
	private void animateTransition(Map<StdAVLNode<Integer>, Point2D.Double> before,
			Map<StdAVLNode<Integer>, Point2D.Double> after) {
		animStart = before;
		animEnd = after;
		transitioning = true;
		currentT = 0;

		long startTime = System.currentTimeMillis();
		Timer timer = new Timer(1000 / TRANSITION_FPS, null);
		timer.addActionListener(e -> {
			long elapsed = System.currentTimeMillis() - startTime;
			currentT = Math.min(1.0, elapsed / (double) TRANSITION_DURATION_MS);
			repaint();
			if (currentT >= 1.0) {
				((Timer) e.getSource()).stop();
				transitioning = false;
				positions.clear();
				positions.putAll(after); // on fige le layout final exact
				repaint();
			}
		});
		timer.start();
	}

	// NOUVEAU : position interpolée d'un nœud donné à l'instant courant de la translation
	private Point2D.Double interpolatedPosition(StdAVLNode<Integer> node) {
		Point2D.Double end = animEnd.get(node);
		Point2D.Double start = animStart.get(node);
		if (start == null) {
			start = end; // nœud tout juste créé : pas de départ, il apparaît directement à sa place
		}
		double x = start.x + (end.x - start.x) * currentT;
		double y = start.y + (end.y - start.y) * currentT;
		return new Point2D.Double(x, y);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.translate(offsetX, offsetY);
		g2.scale(scale, scale);

		if (transitioning) { // NOUVEAU : mode translation
			StdAVLNode<Integer> root = model.getRoot();
			if (root != null) {
				drawTreeInterpolated(g2, root);
			}
			return;
		}

		StdAVLNode<Integer> root = model.getRoot();
		if (root != null) {
			positions.clear();
			inorderCounter = 0;
			computeLayout(root, 0);
			drawTree(g2, root);
		}
	}

	private void computeLayout(StdAVLNode<Integer> node, int depth) {
		if (node == null) {
			return;
		}
		computeLayout(node.getLeft(), depth + 1);
		double x = inorderCounter * HORIZONTAL_GAP + 40;
		double y = depth * VERTICAL_GAP + 40;
		positions.put(node, new Point2D.Double(x, y));
		inorderCounter++;
		computeLayout(node.getRight(), depth + 1);
	}

	private void drawTree(Graphics g, StdAVLNode<Integer> node) {
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

		drawNodeCircle(g, node, p); // MODIFIÉ : extrait dans une méthode partagée
	}

	// NOUVEAU : dessine l'arbre pendant la translation, en utilisant les positions interpolées
	private void drawTreeInterpolated(Graphics g, StdAVLNode<Integer> node) {
		Point2D.Double p = interpolatedPosition(node);

		if (node.getLeft() != null) {
			Point2D.Double childP = interpolatedPosition(node.getLeft());
			g.drawLine((int) p.x, (int) p.y, (int) childP.x, (int) childP.y);
			drawTreeInterpolated(g, node.getLeft());
		}
		if (node.getRight() != null) {
			Point2D.Double childP = interpolatedPosition(node.getRight());
			g.drawLine((int) p.x, (int) p.y, (int) childP.x, (int) childP.y);
			drawTreeInterpolated(g, node.getRight());
		}

		drawNodeCircle(g, node, p);
	}

	// NOUVEAU : factorisation du dessin d'un seul nœud (cercle + texte), réutilisée par les deux méthodes ci-dessus
	private void drawNodeCircle(Graphics g, StdAVLNode<Integer> node, Point2D.Double p) {
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

	public void resetView() {
		scale = 1.0;
		offsetX = 0;
		offsetY = 0;
		repaint();
	}

	public void setBackground() {
		this.setBackground(Color.WHITE);
	}

	public void setAnimationMode(boolean enabled) {
		this.animationMode = enabled;
	}

	// MODIFIÉ : accepte maintenant un callback exécuté à la fin du surlignage
	private void animatePath(List<Integer> path, Runnable onFinished) {
		final int[] index = {0};
		Timer timer = new Timer(500, null);
		timer.addActionListener(e -> {
			if (index[0] < path.size()) {
				highlightedValue = path.get(index[0]);
				repaint();
				index[0]++;
			} else {
				((Timer) e.getSource()).stop();
				highlightedValue = null;
				repaint();
				if (onFinished != null) {
					onFinished.run(); // NOUVEAU : déclenche la suite (mutation + translation)
				}
			}
		});
		timer.start();
	}

	private List<Integer> computeSearchPath(int v) {
		List<Integer> path = new ArrayList<>();
		StdAVLNode<Integer> current = model.getRoot();
		Comparator<Integer> cmp = model.getComparator();
		while (current != null) {
			path.add(current.getValue());
			int c = cmp.compare(v, current.getValue());
			if (c == 0) {
				break;
			}
			current = (c < 0) ? current.getLeft() : current.getRight();
		}
		path.add(v);
		return path;
	}

	private List<Integer> computeDeletePath(int v) {
		List<Integer> path = new ArrayList<>();
		StdAVLNode<Integer> current = model.getRoot();
		Comparator<Integer> cmp = model.getComparator();

		while (current != null) {
			path.add(current.getValue());
			int c = cmp.compare(v, current.getValue());
			if (c == 0) {
				break;
			}
			current = (c < 0) ? current.getLeft() : current.getRight();
		}

		replacementValue = null;

		if (current != null && current.getLeft() != null && current.getRight() != null) {
			StdAVLNode<Integer> successor = current.getRight();
			while (successor.getLeft() != null) {
				path.add(successor.getValue());
				successor = successor.getLeft();
			}
			path.add(successor.getValue());
			replacementValue = successor.getValue();
		}

		return path;
	}
}