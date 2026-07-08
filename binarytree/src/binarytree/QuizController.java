package binarytree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class QuizController {

	private static final int MIN_TREE_VALUES = 8;
	private static final int MAX_TREE_VALUES = 12;
	private static final int VALUE_MIN = 1;
	private static final int VALUE_MAX = 99;

	private final Random random = new Random();

	private AbstractTreePanel<?> panel;
	private int score = 0;
	private int totalQuestions = 0;
	private QuizQuestion currentQuestion = null;

	private Runnable onQuestionChanged;
	private BiConsumer<Boolean, String> onAnswerChecked;

	public void setOnQuestionChanged(Runnable listener) {
		this.onQuestionChanged = listener;
	}

	public void setOnAnswerChecked(BiConsumer<Boolean, String> listener) {
		this.onAnswerChecked = listener;
	}

	public int getScore() {
		return score;
	}

	public int getTotalQuestions() {
		return totalQuestions;
	}

	public QuizQuestion getCurrentQuestion() {
		return currentQuestion;
	}

	public void start(AbstractTreePanel<?> panel) {
		this.panel = panel;
		this.score = 0;
		this.totalQuestions = 0;
		generateRandomTree();
		nextQuestion();
	}

	public void stop() {
		if (panel != null) {
			panel.setOnNodeClicked(null);
		}
		panel = null;
		currentQuestion = null;
	}

	private void generateRandomTree() {
		panel.clearTree();
		int count = MIN_TREE_VALUES + random.nextInt(MAX_TREE_VALUES - MIN_TREE_VALUES + 1);
		List<Integer> used = new ArrayList<>();
		int attempts = 0;
		while (used.size() < count && attempts < count * 20) {
			int v = VALUE_MIN + random.nextInt(VALUE_MAX - VALUE_MIN + 1);
			attempts++;
			if (!used.contains(v)) {
				used.add(v);
				panel.quietAdd(v);
			}
		}
		panel.repaint();
	}

	public void nextQuestion() {
		panel.setOnNodeClicked(null);

		List<QuizQuestionType> types = new ArrayList<>();
		types.add(QuizQuestionType.INSERT_LOCATION);
		types.add(QuizQuestionType.FIND_VALUE);
		types.add(QuizQuestionType.NODE_DEPTH);
		types.add(QuizQuestionType.SUBTREE_HEIGHT);
		if (panel.supportsBalanceFactorQuestions()) {
			types.add(QuizQuestionType.BALANCE_FACTOR);
		}

		QuizQuestionType type = types.get(random.nextInt(types.size()));
		currentQuestion = buildQuestion(type);

		if (currentQuestion == null) {
			nextQuestion(); // valeurs incompatibles avec ce type de question (rare) : on retire une autre question
			return;
		}

		if (type == QuizQuestionType.INSERT_LOCATION || type == QuizQuestionType.FIND_VALUE) {
			panel.setOnNodeClicked(this::handleNodeClickAnswer);
		}

		if (onQuestionChanged != null) {
			onQuestionChanged.run();
		}
	}

	private QuizQuestion buildQuestion(QuizQuestionType type) {
		List<Integer> presentValues = panel.getAllValues();
		if (presentValues.isEmpty()) {
			return null;
		}

		switch (type) {
			case INSERT_LOCATION: {
				int value;
				int attempts = 0;
				do {
					value = VALUE_MIN + random.nextInt(VALUE_MAX - VALUE_MIN + 1);
					attempts++;
				} while (presentValues.contains(value) && attempts < 50);
				if (presentValues.contains(value)) {
					return null;
				}
				String text = "Clique sur le nœud qui deviendrait le PARENT de " + value
						+ " si on l'insérait maintenant.";
				return new QuizQuestion(type, value, text, null);
			}
			case FIND_VALUE: {
				int value = presentValues.get(random.nextInt(presentValues.size()));
				String text = "Clique sur le nœud contenant la valeur " + value + ".";
				return new QuizQuestion(type, value, text, null);
			}
			case NODE_DEPTH: {
				int value = presentValues.get(random.nextInt(presentValues.size()));
				Integer depth = panel.getDepthOfValue(value);
				String text = "Quelle est la profondeur du nœud " + value + " (racine = profondeur 0) ?";
				return new QuizQuestion(type, value, text, depth);
			}
			case SUBTREE_HEIGHT: {
				int value = presentValues.get(random.nextInt(presentValues.size()));
				Integer height = panel.getSubtreeHeightOfValue(value);
				String text = "Quelle est la hauteur du sous-arbre enraciné en " + value + " ?";
				return new QuizQuestion(type, value, text, height);
			}
			case BALANCE_FACTOR: {
				int value = presentValues.get(random.nextInt(presentValues.size()));
				Integer bf = panel.getBalanceFactorOfValue(value);
				String text = "Quel est le facteur d'équilibre du nœud " + value + " ?";
				return new QuizQuestion(type, value, text, bf);
			}
			default:
				return null;
		}
	}

	private void handleNodeClickAnswer(Integer clickedValue) {
		if (currentQuestion == null) {
			return;
		}
		boolean correct;
		String message;

		if (currentQuestion.type == QuizQuestionType.INSERT_LOCATION) {
			Integer expectedParent = panel.getInsertionParentValue(currentQuestion.targetValue);
			correct = expectedParent != null && expectedParent.equals(clickedValue);
			message = correct
					? "Correct ! " + currentQuestion.targetValue + " s'insérerait bien sous " + clickedValue + "."
					: "Non, la bonne réponse était le nœud " + expectedParent + ".";
		} else {
			correct = clickedValue.equals(currentQuestion.targetValue);
			message = correct ? "Correct !" : "Non, ce n'était pas le bon nœud.";
		}

		registerAnswer(correct, message, clickedValue);
	}

	public void submitNumericAnswer(int answer) {
		if (currentQuestion == null || currentQuestion.correctNumericAnswer == null) {
			return;
		}
		boolean correct = currentQuestion.correctNumericAnswer.equals(answer);
		String message = correct
				? "Correct !"
				: "Non, la bonne réponse était " + currentQuestion.correctNumericAnswer + ".";
		registerAnswer(correct, message, currentQuestion.targetValue);
	}

	private void registerAnswer(boolean correct, String message, Integer highlightValue) {
		totalQuestions++;
		if (correct) {
			score++;
		}
		panel.setOnNodeClicked(null);
		panel.flashFeedback(highlightValue, correct);

		if (onAnswerChecked != null) {
			onAnswerChecked.accept(correct, message);
		}
	}
}