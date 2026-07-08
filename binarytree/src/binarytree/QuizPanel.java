package binarytree;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.StyledButton;

public class QuizPanel extends JPanel {

	private final JLabel questionLabel = new JLabel(" ");
	private final JLabel scoreLabel = new JLabel("Score : 0 / 0");
	private final JTextField answerField = new JTextField(6);
	private final JButton validateButton = new StyledButton("Valider");
	private final JButton nextButton = new StyledButton("Question suivante");
	private final JButton stopButton = new StyledButton("Arrêter le quiz");

	public QuizPanel() {
		setLayout(new BorderLayout());

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(questionLabel);
		add(top, BorderLayout.NORTH);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bottom.add(answerField);
		bottom.add(validateButton);
		bottom.add(nextButton);
		bottom.add(stopButton);
		bottom.add(scoreLabel);
		add(bottom, BorderLayout.SOUTH);

		nextButton.setEnabled(false);
	}

	public JLabel getQuestionLabel() {
		return questionLabel;
	}

	public JLabel getScoreLabel() {
		return scoreLabel;
	}

	public JTextField getAnswerField() {
		return answerField;
	}

	public JButton getValidateButton() {
		return validateButton;
	}

	public JButton getNextButton() {
		return nextButton;
	}

	public JButton getStopButton() {
		return stopButton;
	}

	public void setNumericInputVisible(boolean visible) {
		answerField.setVisible(visible);
		validateButton.setVisible(visible);
		if (visible) {
			answerField.setText("");
			answerField.requestFocusInWindow();
		}
	}

	public void setAnswered(boolean answered) {
		nextButton.setEnabled(answered);
		validateButton.setEnabled(!answered);
		answerField.setEnabled(!answered);
	}
}