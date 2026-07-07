package binarytree;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import node.StdAVLNode;
import tree.StdAVLTree;

public class AVLPanel extends AbstractTreePanel<StdAVLNode<Integer>> {

	private static final int SEARCH_STEP_MS = 900;
	private static final int TRANSITION_DURATION_MS = 1200;
	private static final int HOLD_DURATION_MS = 1600;
	private static final int TRANSITION_FPS = 60;

	private boolean transitioning = false;
	private double currentT = 0;
	private Map<StdAVLNode<Integer>, Point2D.Double> animStart = null;
	private Map<StdAVLNode<Integer>, Point2D.Double> animEnd = null;

	private String infoMessage = null;
	private Runnable pendingContinuation = null;
	private Runnable stateListener = null;

	public AVLPanel() {
		super();
		model = new StdAVLTree<StdAVLNode<Integer>, Integer>(Comparator.naturalOrder(), StdAVLNode::new);
	}

	@Override
	public void nextStep() {
		if (pendingContinuation == null) {
			return;
		}
		Runnable r = pendingContinuation;
		pendingContinuation = null;
		r.run();
	}

	@Override
	public boolean isAnimating() {
		return pendingContinuation != null;
	}

	public void setStateListener(Runnable listener) {
		this.stateListener = listener;
	}

	private void notifyState() {
		if (stateListener != null) {
			stateListener.run();
		}
	}

	// ================== MOTEUR D'ANIMATION PAS À PAS ==================

	private interface AnimStep {
		void run(Runnable onDone);
	}

	private class HighlightStep implements AnimStep {
		private final int value;
		private final int pauseMs;
		private final String message;

		HighlightStep(int value, int pauseMs, String message) {
			this.value = value;
			this.pauseMs = pauseMs;
			this.message = message;
		}

		@Override
		public void run(Runnable onDone) {
			highlightedValue = value;
			infoMessage = message;
			repaint();
			if (stepMode) {
				onDone.run();
			} else {
				Timer t = new Timer(pauseMs, e -> {
					((Timer) e.getSource()).stop();
					onDone.run();
				});
				t.setRepeats(false);
				t.start();
			}
		}
	}

	private class TransitionStep implements AnimStep {
		private final Map<StdAVLNode<Integer>, Point2D.Double> from;
		private final Map<StdAVLNode<Integer>, Point2D.Double> to;
		private final String message;
		private final int durationMs;
		private final int holdMs;

		TransitionStep(Map<StdAVLNode<Integer>, Point2D.Double> from,
				Map<StdAVLNode<Integer>, Point2D.Double> to,
				String message, int durationMs, int holdMs) {
			this.from = from;
			this.to = to;
			this.message = message;
			this.durationMs = durationMs;
			this.holdMs = holdMs;
		}

		@Override
		public void run(Runnable onDone) {
			infoMessage = message;
			highlightedValue = null;
			animStart = from;
			animEnd = to;
			transitioning = true;
			currentT = 0;

			long startTime = System.currentTimeMillis();
			Timer timer = new Timer(1000 / TRANSITION_FPS, null);
			timer.addActionListener(e -> {
				long elapsed = System.currentTimeMillis() - startTime;
				currentT = Math.min(1.0, elapsed / (double) durationMs);
				repaint();
				if (currentT >= 1.0) {
					((Timer) e.getSource()).stop();
					transitioning = false;
					positions.clear();
					positions.putAll(to);
					repaint();

					if (stepMode) {
						onDone.run();
					} else {
						Timer hold = new Timer(holdMs, ev -> {
							((Timer) ev.getSource()).stop();
							infoMessage = null;
							repaint();
							onDone.run();
						});
						hold.setRepeats(false);
						hold.start();
					}
				}
			});
			timer.start();
		}
	}

	private void runQueue(Deque<AnimStep> queue, Runnable onAllDone) {
		if (queue.isEmpty()) {
			onAllDone.run();
			return;
		}
		AnimStep step = queue.poll();
		step.run(() -> {
			if (stepMode && !queue.isEmpty()) {
				pendingContinuation = () -> runQueue(queue, onAllDone);
			} else {
				runQueue(queue, onAllDone);
			}
			notifyState();
		});
	}

	private void appendRebalanceSteps(Deque<AnimStep> queue, List<StdAVLNode<Integer>> chain) {
		for (int i = 0; i < chain.size(); i++) {
			final StdAVLNode<Integer> node = chain.get(i);
			final StdAVLNode<Integer> parent = (i + 1 < chain.size()) ? chain.get(i + 1) : null;

			queue.add(new AnimStep() {
				@Override
				public void run(Runnable onDone) {
					Map<StdAVLNode<Integer>, Point2D.Double> before = snapshotLayout();

					updateHeight(node);
					int bf = balanceFactor(node);
					StdAVLNode<Integer> newLocalRoot = node;
					String message = null;

					if (bf > 1) {
						if (balanceFactor(node.getLeft()) < 0) {
							message = "Rotation double Gauche-Droite sur le nœud " + node.getValue()
									+ " (facteur = " + bf + " : trop lourd à gauche, "
									+ "et l'enfant gauche penche lui-même à droite)";
							node.setLeft(rotateLeft(node.getLeft()));
							newLocalRoot = rotateRight(node);
						} else {
							message = "Rotation simple à droite sur le nœud " + node.getValue()
									+ " (facteur = " + bf + " : trop lourd à gauche)";
							newLocalRoot = rotateRight(node);
						}
					} else if (bf < -1) {
						if (balanceFactor(node.getRight()) > 0) {
							message = "Rotation double Droite-Gauche sur le nœud " + node.getValue()
									+ " (facteur = " + bf + " : trop lourd à droite, "
									+ "et l'enfant droit penche lui-même à gauche)";
							node.setRight(rotateRight(node.getRight()));
							newLocalRoot = rotateLeft(node);
						} else {
							message = "Rotation simple à gauche sur le nœud " + node.getValue()
									+ " (facteur = " + bf + " : trop lourd à droite)";
							newLocalRoot = rotateLeft(node);
						}
					}

					if (newLocalRoot == node) {
						infoMessage = "Le nœud " + node.getValue() + " reste équilibré (facteur = " + bf + ")";
						repaint();
						onDone.run();
						return;
					}

					if (parent == null) {
						model.setRoot(newLocalRoot);
					} else if (parent.getLeft() == node) {
						parent.setLeft(newLocalRoot);
					} else {
						parent.setRight(newLocalRoot);
					}

					Map<StdAVLNode<Integer>, Point2D.Double> after = snapshotLayout();
					new TransitionStep(before, after, message, TRANSITION_DURATION_MS, HOLD_DURATION_MS).run(onDone);
				}
			});
		}
	}

	// ================== COMMANDES PRINCIPALES ==================

	public void addInt(Integer v) {
		addInt(v, null);
	}

	public void addInt(Integer v, Runnable onFinished) {
		if (busy) {
			return;
		}
		if (!animationMode) {
			model.add(v);
			repaint();
			if (onFinished != null) {
				onFinished.run();
			}
			return;
		}
		if (model.isIn(v)) {
			Deque<AnimStep> searchQueue = new ArrayDeque<>();
			Comparator<Integer> cmpCheck = model.getComparator();

			List<StdAVLNode<Integer>> ancestorsCheck = new ArrayList<>();
			StdAVLNode<Integer> cur = model.getRoot();
			while (cur != null) {
				ancestorsCheck.add(cur);
				int c = cmpCheck.compare(v, cur.getValue());
				if (c == 0) {
					break;
				}
				cur = (c < 0) ? cur.getLeft() : cur.getRight();
			}
			for (StdAVLNode<Integer> a : ancestorsCheck) {
				searchQueue.add(new HighlightStep(a.getValue(), SEARCH_STEP_MS,
						"Descente : comparaison de " + v + " avec " + a.getValue()));
			}
			searchQueue.add(new AnimStep() {
				@Override
				public void run(Runnable onDone) {
					highlightedValue = null;
					replacementValue = v;
					infoMessage = "Le nœud " + v + " existe déjà, rien à insérer";
					repaint();
					if (stepMode) {
						onDone.run();
					} else {
						Timer hold = new Timer(HOLD_DURATION_MS, e -> {
							((Timer) e.getSource()).stop();
							onDone.run();
						});
						hold.setRepeats(false);
						hold.start();
					}
				}
			});

			busy = true;
			runQueue(searchQueue, () -> {
				busy = false;
				highlightedValue = null;
				replacementValue = null;
				infoMessage = null;
				repaint();
				if (onFinished != null) {
					onFinished.run();
				}
			});
			return;
		}

		busy = true;
		Deque<AnimStep> queue = new ArrayDeque<>();
		Comparator<Integer> cmp = model.getComparator();

		List<StdAVLNode<Integer>> ancestors = new ArrayList<>();
		StdAVLNode<Integer> current = model.getRoot();
		while (current != null) {
			ancestors.add(current);
			current = (cmp.compare(v, current.getValue()) < 0) ? current.getLeft() : current.getRight();
		}
		for (StdAVLNode<Integer> a : ancestors) {
			queue.add(new HighlightStep(a.getValue(), SEARCH_STEP_MS,
					"Descente : comparaison de " + v + " avec " + a.getValue()));
		}

		queue.add(new AnimStep() {
			@Override
			public void run(Runnable onDone) {
				Map<StdAVLNode<Integer>, Point2D.Double> before = snapshotLayout();
				StdAVLNode<Integer> leaf = new StdAVLNode<>(v);
				if (ancestors.isEmpty()) {
					model.setRoot(leaf);
				} else {
					StdAVLNode<Integer> parent = ancestors.get(ancestors.size() - 1);
					if (cmp.compare(v, parent.getValue()) < 0) {
						parent.setLeft(leaf);
					} else {
						parent.setRight(leaf);
					}
				}
				Map<StdAVLNode<Integer>, Point2D.Double> after = snapshotLayout();
				new TransitionStep(before, after, "Insertion du nœud " + v + " en tant que feuille",
						TRANSITION_DURATION_MS, HOLD_DURATION_MS).run(onDone);
			}
		});

		List<StdAVLNode<Integer>> chain = new ArrayList<>(ancestors);
		Collections.reverse(chain);
		appendRebalanceSteps(queue, chain);

		runQueue(queue, () -> {
			busy = false;
			highlightedValue = null;
			replacementValue = null;
			infoMessage = null;
			repaint();
			if (onFinished != null) {
				onFinished.run();
			}
		});
	}

	public void deleteInt(Integer v) {
		deleteInt(v, null);
	}

	public void deleteInt(Integer v, Runnable onFinished) {
		if (busy) {
			return;
		}
		if (!animationMode) {
			model.delete(v);
			repaint();
			if (onFinished != null) {
				onFinished.run();
			}
			return;
		}
		if (!model.isIn(v)) {
			Deque<AnimStep> searchQueue = new ArrayDeque<>();
			Comparator<Integer> cmpCheck = model.getComparator();

			List<StdAVLNode<Integer>> ancestorsCheck = new ArrayList<>();
			StdAVLNode<Integer> cur = model.getRoot();
			while (cur != null) {
				ancestorsCheck.add(cur);
				int c = cmpCheck.compare(v, cur.getValue());
				if (c == 0) {
					break;
				}
				cur = (c < 0) ? cur.getLeft() : cur.getRight();
			}
			for (StdAVLNode<Integer> a : ancestorsCheck) {
				searchQueue.add(new HighlightStep(a.getValue(), SEARCH_STEP_MS,
						"Recherche de " + v + " : comparaison avec " + a.getValue()));
			}
			searchQueue.add(new AnimStep() {
				@Override
				public void run(Runnable onDone) {
					highlightedValue = null;
					infoMessage = "Le nœud " + v + " n'existe pas, rien à supprimer";
					repaint();
					if (stepMode) {
						onDone.run();
					} else {
						Timer hold = new Timer(HOLD_DURATION_MS, e -> {
							((Timer) e.getSource()).stop();
							onDone.run();
						});
						hold.setRepeats(false);
						hold.start();
					}
				}
			});

			busy = true;
			runQueue(searchQueue, () -> {
				busy = false;
				highlightedValue = null;
				replacementValue = null;
				infoMessage = null;
				repaint();
				if (onFinished != null) {
					onFinished.run();
				}
			});
			return;
		}

		busy = true;
		Deque<AnimStep> queue = new ArrayDeque<>();
		Comparator<Integer> cmp = model.getComparator();

		List<StdAVLNode<Integer>> ancestorsToNode = new ArrayList<>();
		StdAVLNode<Integer> current = model.getRoot();
		while (current != null) {
			ancestorsToNode.add(current);
			int c = cmp.compare(v, current.getValue());
			if (c == 0) {
				break;
			}
			current = (c < 0) ? current.getLeft() : current.getRight();
		}
		for (StdAVLNode<Integer> a : ancestorsToNode) {
			queue.add(new HighlightStep(a.getValue(), SEARCH_STEP_MS,
					"Recherche du nœud " + v + " : comparaison avec " + a.getValue()));
		}

		final StdAVLNode<Integer> target = ancestorsToNode.get(ancestorsToNode.size() - 1);

		if (target.getLeft() == null || target.getRight() == null) {
			queue.add(new AnimStep() {
				@Override
				public void run(Runnable onDone) {
					Map<StdAVLNode<Integer>, Point2D.Double> before = snapshotLayout();
					StdAVLNode<Integer> replacement = (target.getLeft() != null) ? target.getLeft() : target.getRight();
					StdAVLNode<Integer> targetParent =
							ancestorsToNode.size() >= 2 ? ancestorsToNode.get(ancestorsToNode.size() - 2) : null;
					if (targetParent == null) {
						model.setRoot(replacement);
					} else if (targetParent.getLeft() == target) {
						targetParent.setLeft(replacement);
					} else {
						targetParent.setRight(replacement);
					}
					Map<StdAVLNode<Integer>, Point2D.Double> after = snapshotLayout();
					new TransitionStep(before, after, "Suppression du nœud " + v + " (0 ou 1 enfant)",
							TRANSITION_DURATION_MS, HOLD_DURATION_MS).run(onDone);
				}
			});

			List<StdAVLNode<Integer>> chain =
					new ArrayList<>(ancestorsToNode.subList(0, ancestorsToNode.size() - 1));
			Collections.reverse(chain);
			appendRebalanceSteps(queue, chain);

		} else {
			List<StdAVLNode<Integer>> pathToSuccessor = new ArrayList<>();
			StdAVLNode<Integer> succ = target.getRight();
			pathToSuccessor.add(succ);
			while (succ.getLeft() != null) {
				succ = succ.getLeft();
				pathToSuccessor.add(succ);
			}
			final StdAVLNode<Integer> successor = succ;
			replacementValue = successor.getValue();

			for (StdAVLNode<Integer> a : pathToSuccessor) {
				queue.add(new HighlightStep(a.getValue(), SEARCH_STEP_MS,
						"Recherche du successeur (plus petit du sous-arbre droit) : on descend à gauche vers " + a.getValue()));
			}

			queue.add(new AnimStep() {
				@Override
				public void run(Runnable onDone) {
					Map<StdAVLNode<Integer>, Point2D.Double> before = snapshotLayout();
					int successorValue = successor.getValue();
					target.setValue(successorValue);
					StdAVLNode<Integer> succReplacement = successor.getRight();
					StdAVLNode<Integer> succParent = pathToSuccessor.size() >= 2
							? pathToSuccessor.get(pathToSuccessor.size() - 2)
							: target;
					if (succParent.getLeft() == successor) {
						succParent.setLeft(succReplacement);
					} else {
						succParent.setRight(succReplacement);
					}
					Map<StdAVLNode<Integer>, Point2D.Double> after = snapshotLayout();
					new TransitionStep(before, after,
							"Le nœud " + v + " a deux enfants : on le remplace par son successeur " + successorValue,
							TRANSITION_DURATION_MS, HOLD_DURATION_MS).run(onDone);
				}
			});

			List<StdAVLNode<Integer>> chain = new ArrayList<>();
			List<StdAVLNode<Integer>> succAncestorsExcludingSucc =
					new ArrayList<>(pathToSuccessor.subList(0, pathToSuccessor.size() - 1));
			Collections.reverse(succAncestorsExcludingSucc);
			chain.addAll(succAncestorsExcludingSucc);
			chain.add(target);
			List<StdAVLNode<Integer>> aboveTarget =
					new ArrayList<>(ancestorsToNode.subList(0, ancestorsToNode.size() - 1));
			Collections.reverse(aboveTarget);
			chain.addAll(aboveTarget);

			appendRebalanceSteps(queue, chain);
		}

		runQueue(queue, () -> {
			busy = false;
			highlightedValue = null;
			replacementValue = null;
			infoMessage = null;
			repaint();
			if (onFinished != null) {
				onFinished.run();
			}
		});
	}

	public void searchInt(Integer v, Runnable onFinished) {
		if (busy) {
			return;
		}
		if (!animationMode) {
			if (onFinished != null) {
				onFinished.run();
			}
			return;
		}

		busy = true;
		Deque<AnimStep> queue = new ArrayDeque<>();
		Comparator<Integer> cmp = model.getComparator();

		List<StdAVLNode<Integer>> ancestors = new ArrayList<>();
		StdAVLNode<Integer> current = model.getRoot();
		boolean found = false;
		while (current != null) {
			ancestors.add(current);
			int c = cmp.compare(v, current.getValue());
			if (c == 0) {
				found = true;
				break;
			}
			current = (c < 0) ? current.getLeft() : current.getRight();
		}
		for (StdAVLNode<Integer> a : ancestors) {
			queue.add(new HighlightStep(a.getValue(), SEARCH_STEP_MS,
					"Recherche de " + v + " : comparaison avec " + a.getValue()));
		}

		final boolean finalFound = found;
		queue.add(new AnimStep() {
			@Override
			public void run(Runnable onDone) {
				highlightedValue = null;
				if (finalFound) {
					replacementValue = v;
					infoMessage = "Valeur " + v + " trouvée !";
				} else {
					infoMessage = "Valeur " + v + " absente de l'arbre.";
				}
				repaint();
				if (stepMode) {
					onDone.run();
				} else {
					Timer hold = new Timer(HOLD_DURATION_MS, e -> {
						((Timer) e.getSource()).stop();
						onDone.run();
					});
					hold.setRepeats(false);
					hold.start();
				}
			}
		});

		runQueue(queue, () -> {
			busy = false;
			highlightedValue = null;
			replacementValue = null;
			infoMessage = null;
			repaint();
			if (onFinished != null) {
				onFinished.run();
			}
		});
	}

	// ================== OUTILS AVL (répliques exactes de StdAVLTree) ==================

	private int height(StdAVLNode<Integer> n) {
		return (n == null) ? 0 : n.getHeight();
	}

	private void updateHeight(StdAVLNode<Integer> n) {
		n.setHeight(1 + Math.max(height(n.getLeft()), height(n.getRight())));
	}

	private int balanceFactor(StdAVLNode<Integer> n) {
		return (n == null) ? 0 : height(n.getLeft()) - height(n.getRight());
	}

	private StdAVLNode<Integer> rotateRight(StdAVLNode<Integer> node) {
		StdAVLNode<Integer> pivot = node.getLeft();
		node.setLeft(pivot.getRight());
		pivot.setRight(node);
		updateHeight(node);
		updateHeight(pivot);
		return pivot;
	}

	private StdAVLNode<Integer> rotateLeft(StdAVLNode<Integer> node) {
		StdAVLNode<Integer> pivot = node.getRight();
		node.setRight(pivot.getLeft());
		pivot.setLeft(node);
		updateHeight(node);
		updateHeight(pivot);
		return pivot;
	}

	// ================== INTERPOLATION (translation des nœuds) ==================

	private Point2D.Double interpolatedPosition(StdAVLNode<Integer> node) {
		Point2D.Double end = animEnd.get(node);
		Point2D.Double start = animStart.get(node);
		if (start == null) {
			start = end;
		}
		double x = start.x + (end.x - start.x) * currentT;
		double y = start.y + (end.y - start.y) * currentT;
		return new Point2D.Double(x, y);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g); // NOTE : appelle celui d'AbstractTreePanel, qui gère déjà le cas non-transitioning
		Graphics2D overlay = (Graphics2D) g.create();

		if (transitioning) {
			Graphics2D g2 = (Graphics2D) g;
			g2.translate(offsetX, offsetY);
			g2.scale(scale, scale);
			StdAVLNode<Integer> root = model.getRoot();
			if (root != null) {
				drawTreeInterpolated(g2, root);
			}
		}

		drawInfoMessage(overlay);
		overlay.dispose();
	}

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

	private void drawInfoMessage(Graphics2D g) {
		if (infoMessage == null) {
			return;
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(new Font("Segoe UI", Font.BOLD, 14));
		FontMetrics fm = g.getFontMetrics();
		int textWidth = fm.stringWidth(infoMessage);
		int boxWidth = textWidth + 24;
		int boxHeight = 34;
		int x = 12;
		int y = 12;

		g.setColor(new Color(20, 20, 20, 230));
		g.fillRoundRect(x, y, boxWidth, boxHeight, 12, 12);
		g.setColor(Color.WHITE);
		g.drawString(infoMessage, x + 12, y + boxHeight - 12);
	}

	// ================== PARCOURS ==================

	private List<Integer> preorderValues(StdAVLNode<Integer> node, List<Integer> acc) {
		if (node == null) {
			return acc;
		}
		acc.add(node.getValue());
		preorderValues(node.getLeft(), acc);
		preorderValues(node.getRight(), acc);
		return acc;
	}

	private List<Integer> inorderValues(StdAVLNode<Integer> node, List<Integer> acc) {
		if (node == null) {
			return acc;
		}
		inorderValues(node.getLeft(), acc);
		acc.add(node.getValue());
		inorderValues(node.getRight(), acc);
		return acc;
	}

	private List<Integer> postorderValues(StdAVLNode<Integer> node, List<Integer> acc) {
		if (node == null) {
			return acc;
		}
		postorderValues(node.getLeft(), acc);
		postorderValues(node.getRight(), acc);
		acc.add(node.getValue());
		return acc;
	}

	private List<Integer> levelOrderValues(StdAVLNode<Integer> root) {
		List<Integer> acc = new ArrayList<>();
		if (root == null) {
			return acc;
		}
		Deque<StdAVLNode<Integer>> bfsQueue = new ArrayDeque<>();
		bfsQueue.add(root);
		while (!bfsQueue.isEmpty()) {
			StdAVLNode<Integer> n = bfsQueue.poll();
			acc.add(n.getValue());
			if (n.getLeft() != null) {
				bfsQueue.add(n.getLeft());
			}
			if (n.getRight() != null) {
				bfsQueue.add(n.getRight());
			}
		}
		return acc;
	}

	public void traversePreorder(Runnable onFinished) {
		runTraversal(preorderValues(model.getRoot(), new ArrayList<>()),
				"Parcours préfixe (Racine → Gauche → Droite)", onFinished);
	}

	public void traverseInorder(Runnable onFinished) {
		runTraversal(inorderValues(model.getRoot(), new ArrayList<>()),
				"Parcours infixe (Gauche → Racine → Droite)", onFinished);
	}

	public void traversePostorder(Runnable onFinished) {
		runTraversal(postorderValues(model.getRoot(), new ArrayList<>()),
				"Parcours postfixe (Gauche → Droite → Racine)", onFinished);
	}

	public void traverseLevelOrder(Runnable onFinished) {
		runTraversal(levelOrderValues(model.getRoot()),
				"Parcours en largeur (niveau par niveau)", onFinished);
	}

	private void runTraversal(List<Integer> order, String label, Runnable onFinished) {
		if (busy) {
			if (onFinished != null) {
				onFinished.run();
			}
			return;
		}
		busy = true;
		Deque<AnimStep> queue = new ArrayDeque<>();
		StringBuilder visited = new StringBuilder();

		for (int i = 0; i < order.size(); i++) {
			final int value = order.get(i);
			final int stepNumber = i + 1;
			queue.add(new AnimStep() {
				@Override
				public void run(Runnable onDone) {
					highlightedValue = value;
					if (visited.length() > 0) {
						visited.append(", ");
					}
					visited.append(value);
					infoMessage = label + " — étape " + stepNumber + "/" + order.size() + " : " + visited;
					repaint();
					if (stepMode) {
						onDone.run();
					} else {
						Timer t = new Timer(SEARCH_STEP_MS, e -> {
							((Timer) e.getSource()).stop();
							onDone.run();
						});
						t.setRepeats(false);
						t.start();
					}
				}
			});
		}

		runQueue(queue, () -> {
			busy = false;
			highlightedValue = null;
			infoMessage = null;
			repaint();
			if (onFinished != null) {
				onFinished.run();
			}
		});
	}
}