package ie.atu.sw;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.ThreadLocalRandom.current;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GameView extends JPanel implements ActionListener {
	// Some constants
	private static final long serialVersionUID = 1L;
	private static final int MODEL_WIDTH = 30;
	private static final int MODEL_HEIGHT = 20;
	private static final int SCALING_FACTOR = 30;

	private static final int MIN_TOP = 2;
	private static final int MIN_BOTTOM = 18;
	private static final int PLAYER_COLUMN = 15;
	private static final int TIMER_INTERVAL = 100;

	private static final byte ONE_SET = 1;
	private static final byte ZERO_SET = 0;

	/*
	 * The 30x20 game grid is implemented using a linked list of 30 elements, where
	 * each element contains a byte[] of size 20.
	 */
	private LinkedList<byte[]> model = new LinkedList<>();

	// These two variables are used by the cavern generator.
	private int prevTop = MIN_TOP;
	private int prevBot = MIN_BOTTOM;

	// Once the timer stops, the game is over
	private Timer timer;
	private long time;

	private int playerRow = 11;
	private int index = MODEL_WIDTH - 1; // Start generating at the end
	private Dimension dim;

	// Some fonts for the UI display
	private Font font = new Font("Dialog", Font.BOLD, 50);
	private Font over = new Font("Dialog", Font.BOLD, 100);

	// The player and a sprite for an exploding plane
	private Sprite sprite;
	private Sprite dyingSprite;

	private boolean auto;

	public GameView(boolean auto) throws Exception {
		this.auto = auto; // Use the autopilot
		setBackground(Color.LIGHT_GRAY);
		setDoubleBuffered(true);

		// Creates a viewing area of 900 x 600 pixels
		dim = new Dimension(MODEL_WIDTH * SCALING_FACTOR, MODEL_HEIGHT * SCALING_FACTOR);
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);

		initModel();

		timer = new Timer(TIMER_INTERVAL, this); // Timer calls actionPerformed() every second
		timer.start();

		// Ensure the data file exists or create it with the header
		initializeDataFile();

		loadModel();
	}

	// Build our game grid
	private void initModel() {
		for (int i = 0; i < MODEL_WIDTH; i++) {
			model.add(new byte[MODEL_HEIGHT]);
		}
	}

	public void setSprite(Sprite s) {
		this.sprite = s;
	}

	public void setDyingSprite(Sprite s) {
		this.dyingSprite = s;
	}

	// Called every second by actionPerformed(). Paint methods are usually ugly.
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		var g2 = (Graphics2D) g;

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, dim.width, dim.height);

		int x1 = 0, y1 = 0;
		for (int x = 0; x < MODEL_WIDTH; x++) {
			for (int y = 0; y < MODEL_HEIGHT; y++) {
				x1 = x * SCALING_FACTOR;
				y1 = y * SCALING_FACTOR;

				if (model.get(x)[y] != 0) {
					if (y == playerRow && x == PLAYER_COLUMN) {
						timer.stop(); // Crash...
					}
					g2.setColor(Color.BLACK);
					g2.fillRect(x1, y1, SCALING_FACTOR, SCALING_FACTOR);
				}

				if (x == PLAYER_COLUMN && y == playerRow) {
					if (timer.isRunning()) {
						g2.drawImage(sprite.getNext(), x1, y1, null);
					} else {
						g2.drawImage(dyingSprite.getNext(), x1, y1, null);
					}

				}
			}
		}

		/*
		 * Not pretty, but good enough for this project... The compiler will tidy up and
		 * optimise all of the arithmetics with constants below.
		 */
		g2.setFont(font);
		g2.setColor(Color.RED);
		g2.fillRect(1 * SCALING_FACTOR, 15 * SCALING_FACTOR, 400, 3 * SCALING_FACTOR);
		g2.setColor(Color.WHITE);
		g2.drawString("Time: " + (int) (time * (TIMER_INTERVAL / 1000.0d)) + "s", 1 * SCALING_FACTOR + 10,
				(15 * SCALING_FACTOR) + (2 * SCALING_FACTOR));

		if (!timer.isRunning()) {
			g2.setFont(over);
			g2.setColor(Color.RED);
			g2.drawString("Game Over!", MODEL_WIDTH / 5 * SCALING_FACTOR, MODEL_HEIGHT / 2 * SCALING_FACTOR);
		}
	}

	BasicNetwork network;

	// Load the trained model by NeuralNetworkTrainer
	public void loadModel() {
		network = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File("resources/trainingModel.eg"));
	}

	// Move the plane up or down
	public void move(int step) {
		playerRow += step;
	}

	/*
	 * ---------- AUTOPILOT! ---------- The following implementation randomly picks
	 * a -1, 0, 1 to control the plane. You should plug the trained neural network
	 * in here. This method is called by the timer every TIMER_INTERVAL units of
	 * time from actionPerformed(). There are other ways of wiring your neural
	 * network into the application, but this way might be the easiest.
	 * 
	 */
	private void autoMove() {
		// Sample the current game state before making a move
		double[] currentState = sample();

		// Encog requires MLData for input
		MLData input = new BasicMLData(currentState);
		MLData output = network.compute(input);

		// Determine the action with the highest output value
		int actionIndex = getMaxIndex(output.getData());
		int action = indexToAction(actionIndex); // Convert index to action (-1, 0, 1)

		// Validate the action before moving
		if (action >= -1 && action <= 1) {
			move(action);
		} else {
			// Error handling if incase conversion is incorrect
			System.err.println("Warning: Invalid action received: " + action);
		}

		// Remove this commented out code to launch the game in autopilot with random
		// movement
		// Decide on a move: -1 (up), 0 (nowhere), 1 (down)
		// int action = current().nextInt(-1, 2);
		// Log the game state and the action taken
		// logData(currentState, action);
	}

	private int indexToAction(int index) {
		// Map the neural network's output index back to game action
		return index - 1; // indexes 0, 1, 2 map to actions -1, 0, 1
	}

	// Find the index of the maximum value in the array
	private int getMaxIndex(double[] array) {
		int maxIndex = 0;
		// Iterate through the array starting from the second element
		for (int i = 1; i < array.length; i++) {
			// Check if the current element is greater than the maximum value
			if (array[i] > array[maxIndex]) {
				// Update the index of the maximum value
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	// Called every second by the timer
	public void actionPerformed(ActionEvent e) {
		time++; // Update our timer
		this.repaint(); // Repaint the cavern

		// Update the next index to generate
		index++;
		index = (index == MODEL_WIDTH) ? 0 : index;

		generateNext(); // Generate the next part of the cave
		if (auto)
			autoMove();

		/*
		 * Use something like the following to extract training data. It might be a good
		 * idea to submit the double[] returned by the sample() method to an executor
		 * and then write it out to file. You'll need to label the data too and perhaps
		 * add some more features... Finally, you do not have to sample the data every
		 * TIMER_INTERVAL units of time. Use some modular arithmetic as shown below.
		 * Alternatively, add a key stroke to fire an event that starts the sampling.
		 */
		if (time % 10 == 0) {
			/*
			 * double[] trainingRow = sample();
			 * System.out.println(Arrays.toString(trainingRow));
			 */
		}
	}

	/*
	 * Generate the next layer of the cavern. Use the linked list to move the
	 * current head element to the tail and then randomly decide whether to increase
	 * or decrease the cavern.
	 */
	private void generateNext() {
		var next = model.pollFirst();
		model.addLast(next); // Move the head to the tail
		Arrays.fill(next, ONE_SET); // Fill everything in

		// Flip a coin to determine if we could grow or shrink the cave
		var minspace = 4; // Smaller values will create a cave with smaller spaces
		prevTop += current().nextBoolean() ? 1 : -1;
		prevBot += current().nextBoolean() ? 1 : -1;
		prevTop = max(MIN_TOP, min(prevTop, prevBot - minspace));
		prevBot = min(MIN_BOTTOM, max(prevBot, prevTop + minspace));

		// Fill in the array with the carved area
		Arrays.fill(next, prevTop, prevBot, ZERO_SET);
	}

	/*
	 * Use this method to get a snapshot of the 30x20 matrix of values that make up
	 * the game grid. The grid is flat-mapped into a single dimension double
	 * array... (somewhat) ready to be used by a neural net. You can experiment
	 * around with how much of this you actually will need. The plane is always
	 * somewhere in column PLAYER_COLUMN and you probably do not need any of the
	 * columns behind this. You can consider all of the columns ahead of
	 * PLAYER_COLUMN as your horizon and this value can be reduced to save space and
	 * time if needed, e.g. just look 1, 2 or 3 columns ahead.
	 * 
	 * You may also want to track the last player movement, i.e. up, down or no
	 * change. Depending on how you design your neural network, you may also want to
	 * label the data as either okay or dead. Alternatively, the label might be the
	 * movement (up, down or straight).
	 * 
	 */

	// Samples the current game state and generates a feature vector for the neural
	// network input.
	public double[] sample() {
		// Initialize the feature vector with 5 elements
		var vector = new double[5];

		// Initialize variables to track obstacle positions and distances
		boolean obstacleDirectlyAhead = false;
		double distanceAbove = 0.0;
		double distanceBelow = 0.0;

		// Calculate distance to the nearest obstacle above and below in the next column
		byte[] nextColumn = model.get((PLAYER_COLUMN + 1) % MODEL_WIDTH);
		for (int i = playerRow - 1; i >= 0; i--) {
			if (nextColumn[i] == ONE_SET) {
				break;
			}
			distanceAbove += 1.0;
		}

		for (int i = playerRow + 1; i < MODEL_HEIGHT; i++) {
			if (nextColumn[i] == ONE_SET) {
				break;
			}
			distanceBelow += 1.0;
		}

		// Check if there's an obstacle directly ahead in the immediate next column
		if (nextColumn[playerRow] == ONE_SET) {
			obstacleDirectlyAhead = true;
		}

		// Normalize the distances
		distanceAbove /= MODEL_HEIGHT; // Normalize
		distanceBelow /= MODEL_HEIGHT; // Normalize

		// Populate the feature vector
		vector[0] = distanceAbove; // Normalized distance to the nearest obstacle above
		vector[1] = distanceBelow; // Normalized distance to the nearest obstacle below
		vector[2] = obstacleDirectlyAhead ? 1.0 : 0.0; // Binary flag for immediate obstacle
		vector[3] = playerRow / (double) MODEL_HEIGHT; // Normalized player row position

		// Check if theres an obstacle in the second column ahead
		byte[] secondColumn = model.get((PLAYER_COLUMN + 2) % MODEL_WIDTH);
		boolean obstacleInSecondColumnAhead = secondColumn[playerRow] == ONE_SET;
		vector[4] = obstacleInSecondColumnAhead ? 1.0 : 0.0; // Binary flag for obstacle in the second column ahead

		return vector;
	}

	/*
	 * Resets and restarts the game when the "S" key is pressed
	 */
	public void reset() {
		model.stream() // Zero out the grid
				.forEach(n -> Arrays.fill(n, 0, n.length, ZERO_SET));
		playerRow = 11; // Centre the plane
		time = 0; // Reset the clock
		timer.restart(); // Start the animation
	}

	// Path to the CSV file used for logging game data.
	private static final String DATA_FILE_PATH = "resources/game_data.csv";

	// Logs the game state data along with the action taken by the player.
	public void logData(double[] gameState, int action) {
		try (FileWriter fw = new FileWriter(DATA_FILE_PATH, true)) {
			// Write the game state features and the action to the CSV file
			for (double value : gameState) {
				fw.append(Double.toString(value)).append(",");
			}
			fw.append(Integer.toString(action)).append("\n");
			// Debug log
			System.out.println("Logged game data: " + Arrays.toString(gameState) + ", Action: " + action);
		} catch (IOException e) {
			System.err.println("Error writing to game data file: " + e.getMessage());
			e.printStackTrace(); // Print stack trace for debugging
		}
	}

	// Initializes the game data file if it does not exist, adding headers for the
	// features and action.
	private void initializeDataFile() {
		Path path = Paths.get(DATA_FILE_PATH);
		if (!Files.exists(path)) {
			try {
				// Create directories for the file if they do not exist
				Files.createDirectories(path.getParent());
				// Create the file
				Files.createFile(path);
				try (FileWriter fw = new FileWriter(DATA_FILE_PATH, true)) {
					// Headers for the 5 features plus the action
					String[] headers = new String[] { "DistanceAbove", "DistanceBelow", "ObstacleDirectlyAhead",
							"PlayerRowNormalized", "ObstacleSecondColumnAhead", "Action" };

					for (int i = 0; i < headers.length; i++) {
						fw.append(headers[i]);
						if (i < headers.length - 1) { // Append comma after all but the last header
							fw.append(",");
						}
					}
					fw.append("\n"); // New line after the header
				}
			} catch (IOException e) {
				System.err.println("Error initializing game data file: " + e.getMessage());
			}
		}
	}

	// Checks if the game is over by determining if the timer is running.
	public boolean isGameOver() {
		return !timer.isRunning();
	}

}