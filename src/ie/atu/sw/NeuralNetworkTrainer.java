package ie.atu.sw;

import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ReadCSV;
import java.io.FileWriter; // Import FileWriter
import java.io.IOException;  // Import the IOException class to handle errors

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NeuralNetworkTrainer {

	// Size of the input and output layers
	private final int inputSize = 5;
	private final int outputSize = 3;

	// Creates a new neural network with the specified architecture.
	public BasicNetwork createNetwork() {
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, true, inputSize)); // Same as the features
		network.addLayer(new BasicLayer(new ActivationTANH(), true, 34)); // First hidden layer of 30 neurons
		network.addLayer(new BasicLayer(new ActivationTANH(), true, 12)); // Second hidden layer of 15 neurons
		network.addLayer(new BasicLayer(new ActivationTANH(), false, outputSize)); // Output is however many actions
																					// possible (up, down, stay)
		network.getStructure().finalizeStructure();
		network.reset();
		return network;
	}

	// Loads the training data from a CSV file.
	// filePath The path to the CSV file containing training data.
	// A MLDataSet object containing the loaded training data.
	private MLDataSet loadCSV(String filePath) {
		List<double[]> inputData = new ArrayList<>();
		List<double[]> idealData = new ArrayList<>();
		ReadCSV csv = new ReadCSV(filePath, true, CSVFormat.DECIMAL_POINT);

		while (csv.next()) {
			double[] input = new double[inputSize];
			double[] ideal = new double[outputSize];
			for (int i = 0; i < inputSize; i++) {
				input[i] = csv.getDouble(i);
			}
			int action = (int) csv.getDouble(inputSize); // Next value after the input
			ideal[action + 1] = 1; // One-hot encoding
			inputData.add(input);
			idealData.add(ideal);
		}

		return new BasicMLDataSet(inputData.toArray(new double[0][]), idealData.toArray(new double[0][]));
	}

	// Train a neural network model using the provided training data file.
	public void trainModel(String dataFilePath) {
		// Create a new neural network
		BasicNetwork network = createNetwork();
		// Load the training data from the specified CSV file
		MLDataSet dataSet = loadCSV(dataFilePath);

		// Train the network using Backpropagation algorithm
		Backpropagation train = new Backpropagation(network, dataSet, 0.001, 0.9);
		int epoch = 1;

		// Iterate until the error is sufficiently low or the maximum number of epochs
		// is reached
		do {
			train.iteration();
			System.out.println("Epoch #" + epoch + " Error:" + train.getError());
			epoch++;
		} while (train.getError() > 0.01 && epoch <= 30000);

		// Finalize the training process
		train.finishTraining();

		// Evaluate the trained model
		evaluateModel(network, dataSet);

		// Save the trained model
		EncogDirectoryPersistence.saveObject(new File("resources/trainingModel.eg"), network);
	}

	// Evaluates the trained neural network model using the provided dataset to
	// measure accuracy and other performance metrics.
	private void evaluateModel(BasicNetwork network, MLDataSet dataSet) {
		// Initialize variables to track performance metrics
		int correctPredictions = 0;
		int[] truePositives = new int[3];
		int[] falsePositives = new int[3];
		int[] falseNegatives = new int[3];

		// Iterate through each data pair in the dataset
		for (MLDataPair pair : dataSet) {
			// Compute the output from the neural network for the input data
			final MLData output = network.compute(pair.getInput());
			int predictedIndex = getMaxIndex(output.getData());
			int actualIndex = getMaxIndex(pair.getIdeal().getData());

			// Update performance metrics based on the predicted and actual classes
			if (predictedIndex == actualIndex) {
				correctPredictions++;
				truePositives[predictedIndex]++;
			} else {
				falsePositives[predictedIndex]++;
				falseNegatives[actualIndex]++;
			}
		}
		// Calculate accuracy
		double accuracy = (double) correctPredictions / dataSet.size();
		System.out.println("Validation Accuracy: " + accuracy);

		// Calculate and print precision, recall, and F1 for each class
		// This is necessary because originally my data collection would not record
		// zeros (stay) so I added these metrics since we've done this in the Gesture
		// Based AI module, to find if it was recognising all classes, which proved it
		// wasn't recognising Class 1 which is stay because 1 - 1 = 0 when it's not one
		// hot encoded.
		try {
	        // Open a file writer in append mode
	        FileWriter writer = new FileWriter("resources/performance_accuracy.txt", false);
	        // Write the accuracy to the file
	        writer.write("Validation Accuracy: " + accuracy + "\n");

	        // Calculate and write precision, recall, and F1 for each class to the file
	        for (int i = 0; i < 3; i++) {
	            double precision = (double) truePositives[i] / (truePositives[i] + falsePositives[i]);
	            double recall = (double) truePositives[i] / (truePositives[i] + falseNegatives[i]);
	            double f1 = 2 * (precision * recall) / (precision + recall);

	            // Write performance metrics for each class
	            writer.write("Class " + i + ": Precision = " + precision + ", Recall = " + recall + ", F1 = " + f1 + "\n");
	            // Print performance metrics for each class
				System.out.println("Class " + i + ": Precision = " + precision + ", Recall = " + recall + ", F1 = " + f1);
	        }

	        // Close the file writer
	        writer.close();

	    } catch (IOException e) {
	        System.out.println("An error occurred while writing to performance_accuracy.txt.");
	        e.printStackTrace();
	    }
	}

	// Helper method to find the index of the maximum value in a double array
	private int getMaxIndex(double[] array) {
		 // Initialize the index of the maximum value
		int maxIndex = 0;
		
		// Iterate through the array to find the index of the maximum value
		for (int i = 1; i < array.length; i++) {
			if (array[i] > array[maxIndex]) {
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	// Main method to execute the neural network training process
	public static void main(String[] args) {
		// Create an instance of NeuralNetworkTrainer
		NeuralNetworkTrainer trainer = new NeuralNetworkTrainer();
		// Path to the CSV data file
		String dataFilePath = "resources/game_data.csv";
		
		// Train the neural network model using the specified data file
		trainer.trainModel(dataFilePath);
	}
}
