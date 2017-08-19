/**
 * @author Sunny Kamleshbhai Shah
 * Student ID: 1001358145
 * Login ID: sks8145
 */ 

package lab2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;


public class BullyProcess extends Application {
	
	// All the Process variables are declared as below--------------------------------------------------------------------------------->
	
	// Defines a final port number, to connect through
	private static final Integer port = 7478;
	
	// Defines the final host to  establish connect to
	private static final String host = "localhost";
	
	// Track the node number of the current process
	private static Integer currentNode;
	
	// Opens the output stream the the server to send messages through network
	private static PrintWriter os;
	
	// Opens the input stream from the server to receives messages through network
	private static BufferedReader is;
	
	// Socket to establish the connection to mentioned host and port
	private static Socket server;
	
	// Manages the interrupting of sending multiple heart beats in one go.
	private static ArrayList<Boolean> isCoordinator = new ArrayList<Boolean>();
	
	// Store the most recent messages received from server
	private static String message;
	
	// set the coordinator flag to the current node
	private static Integer iAmCoordinator;
	
	// flag to keep track of the timeout of the coordinator
	private static Integer coordinatorTimeout = 0;
	
	// Used to facilitate the interruption of multiple hear beats
	private static Integer threadCount;
	
	// Used in the case where node is waiting for a heart beat and receives message other that that
	private static Integer checkResponse = 0;
	
	// Used by coordinator to keep a rack of number of active nodes
	private static Integer totalActiveNodes;

	// Flag the keeps the track of network connection status
	private static Integer serverDown = 1;
	
	// Flag set when quitting application, to halt all other operations
	private static Integer quittingApplication  = 0;
	
	// Coordinator heart beat wait, timeout variable
	private static Integer heartbeatWaitTime;
	
	private static Integer disconnected = 0;
	//--------------------------------------------------------------------------------------------------------------------------------->
	
	// All the GUI variables are declared as below ------------------------------------------------------------------------------------>
	
	//JavaFX GUI primary stage, where the layout will be loaded
	private Stage primaryStage;
	// JavaFX root layout, where the FXML is binded to.
	private GridPane rootLayout;
	// JavaFX FXML that will act as the loader, which sets controlling class for FXML
	// and tells JVM where to find FXML.
	private FXMLLoader loader;
	
	/* @FXML are the variable defined in FXML file, with the exact
	 * same name as stated below, and act as a variable binding between 
	 * FXML and java class BullyProcess
	 */
	
	// Starts the server, when clicked
	@FXML
	public Button connectHost;

	// Shuts the server down, when clicked
	@FXML
	public Button disconnectHost;
	
	// Quits the application when clicked
	@FXML
	public Button quit;
	
	// Output area that will display incoming messages.
	@FXML
	public TextArea nodeOutput;
	
	// TextField that takes the node id from the user
	@FXML
	public TextField enterNodeId;
	
	// Output for the node number and coordinator in the header section
	@FXML
	public Label nodeHeader;
	
	//--------------------------------------------------------------------------------------------------------------------------------->
	
	// All the GUI listeners and methods are declared as below------------------------------------------------------------------------->
	
	public TextArea getNodeOutput() {
		return this.nodeOutput;
	}
	
	public FXMLLoader getController() {
		return this.loader.getController();
	}
	
	/* This method does the main work of connecting to the host.
	 * If the server is alive, it will display the connection message and bully algorithm
	 * process thereafter starts
	 * This method gets invoked automatically in a new thread, by the 
	 * connectHostListener() function
	 */
	public void connectHost() {
		System.out.println("Current node: " + currentNode);
		
		// try block to attempt, connection and start the processing
		// exception thrown when server socket not available
		try {
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.setText("Trying to establish connection to server......\n");
				}
			});
			System.out.println("Trying to establish connection to server......");
			
			// tries the connection to network. if successful, open I/O streams and display greetings
			server = new Socket(host,port);
			os = new PrintWriter(server.getOutputStream(), true);
			is = new BufferedReader(new InputStreamReader(server.getInputStream()));
			
			// track the server status, in further executions
			serverDown = 0;
			disconnected = 0;
			
			os.println(currentNode);
			message = is.readLine();
			
			// Updating the GUI to display node number in header section and
			// application title
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					disconnectHost.setDisable(false);
					nodeOutput.appendText("\n" + message + "\n");
					nodeHeader.setText("Node Id: " + currentNode);
					primaryStage.setTitle("Node [" + currentNode + "] Panel");
				}
			});
			System.out.println("\n" + message);
			
			
			// election message, to initiate the election
			os.println("election");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// initializes the heart beat broadcast thread counter to 0
			threadCount = 0;
			
			/* This block of loop runs indefinitely listening to the network
			 * messages until the server is up and running
			 */
			while(!message.equals("serverdown") && message != null) {
				checkResponse = 0;
				
					System.out.println("\nBefore Read: " + message);
					message = is.readLine();
					System.out.println("After Read: " + message + "\n");
					
					/* This is the heart of the application where all of the responses
					 * are checked and actions are triggered in response to that message
					 */
					inspectResponseMessage();
					System.out.println("Check Response : " + checkResponse);
				
				
				// block to clear the overhead response, when waiting for the the heart beat
				while(checkResponse.equals(1)) {
					inspectResponseMessage();
				}
			}
			
			/* Exception is raised when the server closes abruptly, and connection is disrupted
			 * to the network. UI Updates performed to handle the disruption
			 */
		} catch(IOException e) {
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			
			if(!quittingApplication.equals(1) && !disconnected.equals(1)) {
				disconnected = 0;
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						nodeOutput.appendText("\n\nConnection Error! Try again or quit\n");
					}
				});
				System.out.println("Connection Error! Try again or quit");
				
				serverDown();
			}
			
		} catch (NullPointerException npe) {
			
			if(!disconnected.equals(1)) {
				disconnected = 0;
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						nodeOutput.appendText("\n\nCoonection Lost\n");
					}
				});
				System.out.println("Connection Lost");
				
				serverDown();
			}
		}
	}
	
	/* This listener is binded to the connect host button in the FXML itself.
	 * So, as soon as connect host button is clicked this method will be invoked and
	 * will simply in sequence initiate a thread to start to the connection.
	 */
	public void connectHostListener() {
		// Disables the connect host button and input, so as to prevent,
		// from sending multiple requests, when process is
		// still executing
		connectHost.setDisable(true);
		enterNodeId.setDisable(true);
		
		try {
			currentNode = Integer.parseInt(enterNodeId.getText());
			enterNodeId.clear();
			
			/* Java in-built implementation interface, which implements the run method and
			 * starts the startServer() function on a new thread when invoked.
			 */
			Runnable task = new Runnable() {
				@Override
				public void run() {
					connectHost();
				}
			};
			
			// Thread that is used to invoke the Runnable implementation
			// and start the thread
			Thread connectHost = new Thread(task);
			connectHost.setDaemon(false);
			connectHost.start();
			
		} catch (NumberFormatException nfe) {
			nodeOutput.setText("Not a proper node id! Numeric Only");
			connectHost.setDisable(false);
			enterNodeId.setDisable(false);
			System.out.println("Number Exception");
		}
	}
	
	/* This method does the main work of disconnecting host.
	 * Doing so, will not quit the application. Application will still be
	 * running and give a chance to start a new connection instance.
	 * 
	 * This method gets invoked automatically in a new thread, by the 
	 * disconnectHostListener() function
	 */
	public void disconnectHost() {
		// sends the disconnection message to the server
		os.println("exit");		
		
		// shuts the server connection and perform the UI updates accordingly
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		serverDown();
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeOutput.appendText("\n\nConnection successfully terminated!\nPress connect to start a connection");
			}
		});
		disconnected = 1;
	}
	
	/* This listener is binded to the disconnect host button in the FXML itself.
	 * So, as soon as disconnect host button is clicked this method will be invoked and
	 * will simply in sequence initiate a function to disconnect from the host.
	 */
	public void disconnectHostListener() {
		
		// Disables the disable host button, so as to prevent,
		// from sending multiple requests, when process is
		// still executing
		disconnectHost.setDisable(true);
		
		/* Java in-built implementation interface, which implements the run method and
		 * starts the killServer() function on a new thread when invoked.
		 */
		Runnable task = new Runnable() {
			@Override
			public void run() {
				disconnectHost();
			}
		};
		
		// Thread that is used to invoke the Runnable implementation
		// and start the thread
		Thread disconnectHost = new Thread(task);
		disconnectHost.setDaemon(false);
		disconnectHost.start();
	}
	
	// Closes the network connection, and shutdown the application
	public void quit() {

		quittingApplication = 1;
		
		if(!serverDown.equals(1)) {
			// sends the disconnection message to the server
			os.println("exit");		
			
			// shuts the server connection and perform the UI updates accordingly
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.setText("\nClosing connection to the system....");
				}
			});
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeOutput.setText("\nClosing Application....");
			}
		});
		
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	/* This listener is binded to the quit button in the FXML itself.
	 * So, as soon as quit button is clicked this method will be invoked and
	 * will simply in sequence initiate a function to quit application.
	 */
	public void quitListener() {
		/* Java in-built implementation interface, which implements the run method and
		 * starts the killServer() function on a new thread when invoked.
		 */
		
		quit.setDisable(true);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				quit();
			}
		};
		
		// Thread that is used to invoke the Runnable implementation
		// and start the thread
		Thread quit = new Thread(task);
		quit.setDaemon(false);
		quit.start();
	}
	
	/* (non-Javadoc) @see javafx.application.Application#start(javafx.stage.Stage)
	 * 
	 *  JavaFX implementation method that sets and initializes the primary stage,
	 *  that is to be displayed when the application starts up 
	 */
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		primaryStage.setTitle("Node Panel");
		initRootLayout();
	}
	
	/* Invoked by the start method of the JavaFX process.
	 * Loads the FXML file and sets the LexiconServer class as it's 
	 * controller.
	 * Initializes the rootLayout.
	 * Loads the rootLayout on to the primary stage
	 * and displays that scene on the application
	 */
	public void initRootLayout() {
		try {
			// FXML loader variable, that will be used to load GUI elements
            loader = new FXMLLoader();
			// Sets the location, where to find the Layout file for GUI
            loader.setLocation(BullyProcess.class.getResource("ProcessView.fxml"));
			// Sets the LexiconClient class as the GUI Controller
            loader.setController(this);
			// Initializes the root layout, as the default layout variable
			// where the layout is actually binded, to be accessed programmatically.
            rootLayout = (GridPane) loader.load();
        	
            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
            
            // Initial state of application. 
            // kill server button, is disabled. So, the flow of
            // of application is never disrupted, and always performs as expected
            disconnectHost.setDisable(true);
            nodeOutput.clear();
		} catch(IOException ie){
			ie.printStackTrace();
		}
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------->
	
	// All internal methods that handles the individual responses are declared as below------------------------------------------------>
		
	/* This method is invoked as soon as the process wins the election
	 * and declares itself the coordinator, starts sending the heart beat
	 * every 15 seconds
	 */
	private synchronized void sendAliveMessage(Integer threadCount) {
		//System.out.println("Inside send alive! count : " + threadCount);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeOutput.appendText("\n");
			}
		});
		
		// task start a new thread to send messages, every time a call is made to sendAliveMessage method
		// threadCount is used to keep track of those multiple threads that gets created during the multiple,
		// elections wins, and invalidates the older election wins, essentially halting all the threads except
		// the most recent election win thread
		Runnable task = new Runnable() {
			@Override
			public void run() {
				//System.out.println("Alive Thread: " + threadCount + " -> " + Thread.currentThread().getState());
				
				/* This loops also runs indefinitely, sending heart beats to all the active nodes on the
				 * network, until a different process takes over as a coordinator, and notifies the
				 * current node, that it lost it's rights as a coordinator
				 */
				while(isCoordinator.get(threadCount).equals(true) && !serverDown.equals(1)) {
					try {
						Thread.sleep(15000);
						if(iAmCoordinator.equals(1) && !serverDown.equals(1)) {
							os.println("alive");
							System.out.println("node " + currentNode + " : heartbeat sent");
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									nodeOutput.appendText("Hearbeat sent!\n");
								}
							});
						}
					} catch (InterruptedException e) {

					}
				}
			}
		};
		
		Thread t = new Thread(task);
		t.start();
	}

	/* Method is called after the election has started and no other processes
	 * with responds back, hence declaring itself as coordinator, and 
	 * sending the new coordinator update to the active processes
	 */
	private synchronized void youAreCoodinator() {
		//System.out.println("Inside you are the coordinator function");
		
		iAmCoordinator = 1;
		
		// halts the older send alive message threads, if they ever were created			
		if(threadCount > 0)
			isCoordinator.set(threadCount - 1, false);
			
		// add the value to the array list, which is used a condition for different
		// alive threads to be halted or run
		isCoordinator.add(true);

		nodeOutput.appendText("\n");
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Total active nodes: " + totalActiveNodes);
		//nodeOutput.appendText("Server: " + winner);
		//System.out.println("Server: " + winner + "\n");
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeHeader.setText("Node Id: " + currentNode + " (COORDINATOR)");
				nodeOutput.appendText("\nStarting Heartbeats\n");
			}
		});
		
		// start the heart beat invocation, and initializes a new thread on every invocation
		sendAliveMessage(threadCount);
		
		// increments the heart beat thread counter
		threadCount++;
	}

	/* As name suggests, this method checks for heart beat from
	 * the coordinator, with a different timeout after every
	 * heart beat.
	 * Special thing is - while listening for a heart beat, it might 
	 * receive different messages too, this method provides the basis to
	 * handles those message to the caller method.
	 */
	private synchronized void isCoordinatorAlive() {
		
		// resets the timeout status
		coordinatorTimeout = 0;
		Random random = new Random();
		
		// checks for alive messages only till a response is received and 
		// server is still running, or if the response timer times out
		while(coordinatorTimeout.equals(0) && !serverDown.equals(1)) {
			
			// initializes the wait time every run
			heartbeatWaitTime = 20 + random.nextInt(30);
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.appendText("\nWait Time: " + heartbeatWaitTime + " seconds\n");
				}
			});
			
			// converts the wait time to millis
			long timeOut = heartbeatWaitTime * 1000;
			
			// tries to read the message till the timer is running
			// else throws a timeout exception
			try {
				message = waitTimer(is, timeOut);
				
				// keeps the loop on if messages is a heart beat
				if(message.contains("ALIVE")) {
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							nodeOutput.appendText(message + "\n");
						}
					});
					
					// if different message, sets the flag, so as to check for response, without
					// reading another message, sp to prevent message loss
				} else {
					checkResponse = 1;
					coordinatorTimeout = 1;
				}
				
				// exception raised, when the timer expires,
				// sends election message to all active nodes
			} catch(TimeoutException te) {
				
				coordinatorTimeout = 1;
				os.println("election");
				
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						nodeOutput.appendText("\nCoordinator Timed-Out\n");
					}
				});
				
			} catch(IOException ie) {
				
			}
		}
	}
	
	/* invoked when a node receives coordinator message from other node,
	 * display those messages and then invoke isCoordinatorAlive method,
	 *  to listen for alive messages from the coordinator
	 */
	private synchronized void coordinatorMessage(Integer coordinatorUpdated, String coordinatorUpdatedMessage) {
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeOutput.appendText("\n" + coordinatorUpdatedMessage + "\n");
			}
		});
		System.out.println("\n" + coordinatorUpdatedMessage + "\n");
		
		isCoordinatorAlive();
		
	}
	
	/* This method is a root method along with the inspectResponseMessage as this method
	 * is invoked, when a nodes receives a message coordinator, that a new one
	 * is now the boss or node itself has won the election
	 * 
	 * After which it initiates the heart beat listeners for the coordinators and handles 
	 * all the incoming messages which are not a heart beat but are indispensable oOR
	 * treat the receiving node as the coordinator
	 */
	private synchronized void checkNewCoordinator() {
		Integer coordinatorUpdated = null;
		String coordinatorUpdatedMessage = null;
		try {
			// read coordinator node id
			coordinatorUpdated = Integer.parseInt(is.readLine());
			
			// read coordinator node
			coordinatorUpdatedMessage = is.readLine();
			
			// read total active nodes on network
			totalActiveNodes = Integer.parseInt(is.readLine());
			System.out.println("New coordinator: " + coordinatorUpdated);
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// if the node itself has won election, and is coordinator,
		// declares itself as coordinator, start alive messages
		if(coordinatorUpdated.equals(currentNode)) {
			youAreCoodinator();	
			
			// if node itself is not coordinator,
			// then treat the receiving nodes as coordinator
			// and wait for the alive messages, with finite intervals
		} else {
			iAmCoordinator = 0;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeHeader.setText("Node Id: " + currentNode);
				}
			});
			coordinatorMessage(coordinatorUpdated, coordinatorUpdatedMessage);
		}
	}
	
	/* method invoked after all the election messages has been broadcasted to all
	 * active nodes, and then has to wait for a response for other nodes.
	 * 
	 * waits for 15s seconds to receive a response, on timeout declares,
	 * itself coordinator, and broadcast the message
	 */
	private synchronized void waitForOK() {
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// sets the wait time
		Integer waitTime = 15;
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeOutput.appendText("\nElection Wait Response: " + waitTime + " seconds\n");
			}
		});
		
		// converts wait time from seconds to millis
		long waitOut = 15000;
		
		try {
			
			// listens to receive the response till the timer times out
			// throws exception on time out
			message = waitTimer(is,waitOut);
			
			// discard and throw exception, on a waste extra heart received
			// happens because threads tend to be alive in background
			if(message.contains("ALIVE")) {
				throw new TimeoutException();
				
				// if not, then set the flag to check for the response
			} else {
				checkResponse = 1;
			}
			
			// exception thrown on timer timeout
		} catch (TimeoutException te) {
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.appendText("\nNo response from processes! Declaring Coordinator\n");
				}
			});
			//System.out.println("OK WAIT BEFORE MESSAGE: " + message);
			
			// declares it self as coordinator,
			// printing the message on network along with it's id
			checkResponse = 0;
			os.println("coordinator");
			os.println(currentNode);
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}
	
	/* wait timer that takes the node's socket input stream and waiting time as a 
	 * parameter to the method invocation
	 * 
	 * returns if message received, else throws an timeout exception
	 */
	private synchronized String waitTimer(BufferedReader br, long timeOut) throws TimeoutException, IOException {
		long start = System.currentTimeMillis();
		
		// runs till the timeout time, till the input stream is ready
		while(!br.ready()) {
			if(System.currentTimeMillis() - start > timeOut) {
				throw new TimeoutException();
			}
		}
		
		return br.readLine();
	}
	
	// method to be ignored, no use in current code
	public void checkElectionMessage() {
		if(!message.contains("[" + currentNode + "]")) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.appendText(message + "\n");
				}
			});
			System.out.println(message);
			
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		checkResponse = 0;
	}
	
	// displays the elections start affirmation
	private synchronized void startingElection() {
		try {
			Thread.sleep(800);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				nodeOutput.appendText("\n" + message + "\n");
			}
		});
		System.out.println("\n" + message + "\n");
		
		checkResponse = 0;
	}
	
	/* This method executed when a message to start an election is received from some 
	 * other active node, to which this node then sends a OK message, to halt the
	 * calling process, from further elections
	 */
	private synchronized void electionMessageReceived() {
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		iAmCoordinator = 0;
		try {	
			
			if(threadCount > 0)
				isCoordinator.set(threadCount - 1, false);
				
			Integer node = Integer.parseInt(is.readLine());
			System.out.println("Election from: " + node);
			String electionMessage = is.readLine();
			System.out.println("Election Message: " + electionMessage);
			
			// checks if the election messages is from the node itself,
			// if the case, it simply ignores it,
			// if it is from some other node, then it determines,
			// if ok messages has to be sent or it's time to give up the 
			// election
			if(!node.equals(currentNode)) {
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						nodeOutput.appendText("\n" + electionMessage + "\n");
					}	
				});
				System.out.println(electionMessage);
				
				// checks if the node has a higher id than the,
				// receiving node, if found sends the OK message,
				// else withdraws from election
				if(currentNode > node) {
					
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					
					// prints OK to the receiving node
					os.println("sendOK");
					os.println(node);
					os.println("election");
					
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							nodeOutput.appendText("\nMessage to [" + node + "] : OK\n");	
						}	
					});
					System.out.println("Message to [" + node + "] : OK");
					
					// withdrawal from election
				} else {
					
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							nodeOutput.appendText("Cannot be part of the election\n");
						}	
					});
				}
				
				// ignoring the election message to self
			} else {
				
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		} catch(IOException ie) {
			ie.printStackTrace();
		}
		
		checkResponse = 0;
	}
	
	/* when the server connection is lost,
	 * this method is invoked to update the UI components
	 * and sets the required flags
	 */
	private synchronized void serverDown() {
		
		serverDown = 1;
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				enterNodeId.setDisable(false);
				enterNodeId.setText(currentNode.toString());
				nodeHeader.setText("");
				primaryStage.setTitle("Node Panel");
				connectHost.setDisable(false); 
				disconnectHost.setDisable(true);
			}
		});
	}
	
	public synchronized void inspectResponseMessage() {
		
		System.out.println("Inspect Response: " + message);
		
		// this gets executed when the server shutdowns,
		// it sends an shutdown message to all active processes
		// which are intercepted here, and updates are done accordingly
		if(message.equals("serverdown")) {
			System.out.println("Inside server down");
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.appendText("\nServer Down! Quit or Try Later");
				}
			});
			System.out.println("Server Down! Quit & Try Later");

			serverDown();
			
			checkResponse = 0;
			
			// this gets executed when no processes respond back,
			// the node declares itself as coordinator, sends out 
			// coordinator messages and starts the heart beat 
		} else if(message.equals("You are the coordinator")) {
			System.out.println("inside you are the coordinator");
			
			youAreCoodinator();
			checkResponse = 0;
			
			// This method executed when a message to start an election is 
			// received from some other active node
		} else if(message.equals("election")) {
			//System.out.println("Inside election");

			electionMessageReceived();
			// This method executed when an OK message is received 
			// from the process, to which election was sent	
		} else if(message.equals("ok")) {
			System.out.println("Inside ok");
			try {
				message = is.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					nodeOutput.appendText("\n" + message + "\n");
				}
			});
			System.out.println(message);
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			checkResponse = 0;
			
		} else if(message.equals("Starting Election!")) {
			
			startingElection();
			
		} else if(message.contains("ELECTION!") && message.contains("to")) {
			//System.out.println("Inside election to");
			
			checkElectionMessage();
			
			// executed when a node receives a coordinator message
			// from other node, indicating a new coordinator has been elected
		} else if (message.equals("electionBroadcasted")) {
			
			waitForOK();			
			
		} else if(message.equals("coordinator")) {
			
			checkNewCoordinator();
		}
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------->
	
	// Launches and start the application and loads the GUI
	public static void main(String[] args) {
		launch(args);
	}
}


