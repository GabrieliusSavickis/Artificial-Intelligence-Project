package ie.atu.sw;

import javax.swing.SwingUtilities;
import static java.lang.System.*;

public class Runner {
	public static void main(String[] args) throws Exception {
		
		/*
		 * Always run a GUI in a separate thread from the main thread.
		 */
		SwingUtilities.invokeAndWait(() -> { //Sounds like the Command Pattern at work!
			try {
				new GameWindow();
			} catch (Exception e) {
				out.println("[ERROR] Yikes...problem starting up " + e.getMessage());
			}
		});
	}
}