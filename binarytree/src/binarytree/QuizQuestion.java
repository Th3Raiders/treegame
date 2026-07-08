package binarytree;

public class QuizQuestion {

	public final QuizQuestionType type;
	public final int targetValue;
	public final String text;
	public final Integer correctNumericAnswer; // null si la question attend un clic, pas un nombre

	public QuizQuestion(QuizQuestionType type, int targetValue, String text, Integer correctNumericAnswer) {
		this.type = type;
		this.targetValue = targetValue;
		this.text = text;
		this.correctNumericAnswer = correctNumericAnswer;
	}
}