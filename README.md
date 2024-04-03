# AI Autopilot Controller for Plane Navigation Project

## Overview
This project develops an autopilot controller for a plane navigating through an endlessly scrolling tunnel using a neural network, aiming to demonstrate the practical application of neural networks in real-time control systems.

## Design and Extraction of Training Data

### Data Collection
Training data were collected manually and through an autopilot mode in a game environment represented on a 30x20 grid, with the plane's X-axis position fixed. The `GameView` class facilitated the generation of diverse training samples.

### Feature Engineering
The neural network input is derived from game state features, sampled every game tick:
- Distance to the nearest obstacle above and below the plane.
- Presence of an obstacle directly ahead of the plane.
- The normalized vertical position of the plane.
- Presence of an obstacle in the second column ahead.

These features provide a concise yet comprehensive view of the immediate surroundings for making navigation decisions.

## Neural Network Topography and Configuration

### Network Architecture
The network, implemented in the `NeuralNetworkTrainer` class, includes an input layer (5 features), two hidden layers (34 and 12 neurons), and an output layer (3 neurons for actions: move up, stay, move down), using the TANH activation function.

### Configuration
The network uses the Encog framework, designed to process game state inputs and predict the plane's best action.

### Data Preprocessing
Input features are normalized to a 0-1 scale based on the game grid's size, ensuring appropriately scaled data for network training.

## Training Hyperparameters
Training used the Backpropagation algorithm with a 0.001 learning rate and 0.9 momentum, proceeding until achieving an error rate below 0.01 or reaching 30,000 epochs.

## Testing and Validation
Initial tests with ReLU and RProp did not exceed 35% accuracy. Switching to tanh and Backpropagation improved learning efficiency and model generalization. Successful autonomous navigation for at least 30 seconds indicated a well-trained model, confirmed by a decreasing error rate in `performance_accuracy.txt`.

## Conclusion
The project demonstrates successful real-time game control using a neural network, with potential future work to explore more complex scenarios and enhance performance.

*The best run achieved with this trained model was 99 seconds.*
