package ie.atu.sw;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.Timer;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

public class GameWindow implements KeyListener {
	private GameView view;
	private Timer logTimer;
	/*
	 * Set this to false to be able to do autopilot with trained data To collect
	 * data set it to true, can also set GameView to false to collect manual data
	 */
	private boolean manualPlayMode = false;

	public GameWindow() throws Exception {
		view = new GameView(true); // Use true to get the plane to fly in autopilot mode...
		init();
		loadSprites();

		// Create a timer to log game state periodically
		logTimer = new Timer(1000, new ActionListener() { // Adjust the interval as needed
			public void actionPerformed(ActionEvent e) {
				logGameState(); // Log game state periodically
			}
		});
		logTimer.start(); // Start the log timer
	}

	// Method to log game state
	public void logGameState() {
		// Sample the current state before any movement
		double[] gameState = view.sample();

		// Log the state and action only if the game is not over and in manual play mode
		if (!view.isGameOver() && manualPlayMode) {
			view.logData(gameState, 0); // Log action as 0 (no movement)
		}
	}

	/*
	 * Build and display the GUI.
	 */
	public void init() throws Exception {
		var f = new JFrame("ATU - B.Sc. in Software Development");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.addKeyListener(this);
		f.getContentPane().setLayout(new FlowLayout());
		f.add(view);
		f.setSize(1000, 1000);
		f.setLocation(100, 100);
		f.pack();
		f.setVisible(true);
	}

	/*
	 * Load the sprite graphics from the image directory
	 */
	public void loadSprites() throws Exception {
		var player = new Sprite("Player", 2, "images/0.png", "images/1.png");
		view.setSprite(player);

		var explosion = new Sprite("Explosion", 7, "images/2.png", "images/3.png", "images/4.png", "images/5.png",
				"images/6.png", "images/7.png", "images/8.png");
		view.setDyingSprite(explosion);
	}

	/*
	 * KEYBOARD OPTIONS ---------------- UP Arrow Key: Moves plane up DOWN Arrow
	 * Key: Moves plane down S: Resets and restarts the game
	 * 
	 * Maybe consider adding options for "start sampling" and "end sampling"
	 * 
	 */
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_S) { // Press "S" to restart
			view.reset(); // Reset the view and bail out
			manualPlayMode = true; // Set to manual play mode after restarting
			return;
		}

		int step = switch (e.getKeyCode()) {
		case KeyEvent.VK_UP -> -1; // Press "UP Arrow"
		case KeyEvent.VK_DOWN -> 1; // Press "DOWN Arrow"
		default -> 0; // No change. Fly straight
		};

		// Before moving, sample the current state before it changes due to the action
		// This step is crucial as it captures the state that led to the decision
		double[] gameState = view.sample(); 
		
		// Perform the move
		view.move(step);

		// Log the state and action after the move is made
		if (manualPlayMode) {
			view.logData(gameState, step); // Log data only in manual play mode
		}
	}

	public void keyReleased(KeyEvent e) {
	} // Ignore

	public void keyTyped(KeyEvent e) {
	} // Ignore
}