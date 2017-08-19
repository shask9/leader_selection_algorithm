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
import java.util.concurrent.locks.*;

import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

public class BullyServer extends Application {

	// All the BullyServer class variable are declared below-------------------------------------------------------------------------->
	
	// final port number, where server listens for new connections
	private static final Integer port = 7478;
	
	// defines the server socket
	private static ServerSocket server;
	
	// Map that stores the key-Value pairs of the nodes connected, with their sockets
	public static HashMap<Integer,ProcessThread> threadMap = new HashMap<Integer,ProcessThread>();
	
	// Object of ProcessThread class
	private static ProcessThread pt;
	
	// tracker for the current coordinator node
	public static Integer coordinatorNode;
	
	// temporary id assigned to the new connection on the network
	private static Integer tempId;
	
	// server status tracking flag
	public static Integer serverFlag = 0;
	
	//-------------------------------------------------------------------------------------------------------------------------------->
	
	// All the GUI variables and methods are declared as below ----------------------------------------------------------------------->
	
	//JavaFX GUI primary stage, where the layout will be loaded
	private Stage primaryStage;
	// JavaFX root layout, where the FXML is binded to.
	private GridPane rootLayout;
	// JavaFX FXML that will act as the loader, which sets controlling class for FXML
	// and tells JVM where to find FXML.
	private FXMLLoader loader;
	
	/* @FXML are the variable defined in FXML file, with the exact
	 * same name as stated below, and act as a variable binding between 
	 * FXML and  java class LexiconServer
	 */
	
	// Starts the server, when clicked
	@FXML
	Button startServer;

	// Shuts the server down, when clicked
	@FXML
	Button killServer;
	
	// Quits the application when clicked
	@FXML
	Button quit;
	
	// Output area that will display incoming client requests.
	@FXML
	public TextArea scrollView;
	
	public synchronized void setLogger(String message) {
		this.scrollView.appendText(message);
	}
	
	/* This method does the main work of starting the server.
	 * If the server is alive, it will display the connection message and wait
	 * for nodes to join and then send them initial greeting or instructions
	 * Whereas, if the server start fails, it will display the corresponding message
	 * and shutdown/quit the client application.
	 * 
	 * This method gets invoked automatically in a new thread, by the 
	 * startServerListener() function
	 */
	public void startServer() {
		scrollView.setText("");
		try {
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					scrollView.appendText("Starting server on port: " + port + "\n\n");
				}
			});
			System.out.println("Starting server on port: " + port + "\n");
			
			// starts the server on the port and sets the server flag
			server = new ServerSocket(port);
			serverFlag = 1;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					killServer.setDisable(false);
					scrollView.appendText("Server Started!\nWaiting for connection...\n\n");
				}
			});
			System.out.println("Server Started!\nWaiting for connection...\n");
			
			Random random = new Random();
			
			// runs indefinitely listening for new connections on the network
			while(true) {
				tempId = 100 + random.nextInt(1000);
				pt = null;
				try {
					// stores the new accepted connection in the process thread object and saves them in the map
					pt = new ProcessThread(server.accept(),tempId, (BullyServer) loader.getController());
					pt.start();
					threadMap.put(tempId,pt);
				} catch(IOException e) {
					//e.printStackTrace();
				}
			}
		} catch(IOException e) {
		e.printStackTrace();
		}
	}
	
	/* This listener is binded to the start server button in the FXML itself.
	 * So, as soon as start server button is clicked this method will be invoked and
	 * will simply in sequence initiate a thread to start to the server.
	 */
	public void startServerListener() {
		
		// Disables to start server button, so as to prevent,
		// from sending multiple start requests, when process is
		// still executing
		startServer.setDisable(true);
		
		/* Java in-built implementation interface, which implements the run method and
		 * starts the startServer() function on a new thread when invoked.
		 */
		Runnable task = new Runnable() {
			@Override
			public void run() {
				startServer();
			}
		};
		
		// Thread that is used to invoke the Runnable implementation
		// and start the thread
		Thread startServer = new Thread(task);
		startServer.setDaemon(false);
		startServer.start();
	}
	
	/* This method does the main work of shutting the server.
	 * Doing so, will not quit the application. Application will still be
	 * running and give a chance to start a new server instance.
	 * 
	 * This method gets invoked automatically in a new thread, by the 
	 * killServerListener() function
	 */
	public void killServer() {
		try {
			
			// sends the shutdown message to all the active connections
			for(Integer shutServer : threadMap.keySet()) {
				Socket demo = threadMap.get(shutServer).getSocket();
				PrintWriter pw = new PrintWriter(demo.getOutputStream(), true);
				pw.println("serverdown");
				pw.close();
			}
			
			// Empties the active nodes map
			BullyServer.threadMap.clear();
			
			// closes the server and resets the serverFlag
			server.close();
			serverFlag = 0;
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch(InterruptedException ie) {
				ie.printStackTrace();
			}
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					scrollView.setText("Server killed! Press 'Start Server' to fire up the server\n");
					// enables the start server button
					startServer.setDisable(false);
				}
			});
			System.out.println("\n\nServer killed! Press 'Start Server' to fire up the server");
		} catch(IOException ie) {
			ie.printStackTrace();
		}
	}
	
	/* This listener is binded to the kill server button in the FXML itself.
	 * So, as soon as kill server button is clicked this method will be invoked and
	 * will simply in sequence initiate a function to kill to the server.
	 */
	public void killServerListener() {
		
		// Disables to kill server button, so as to prevent,
		// from sending multiple kill requests, when process is
		// still executing
		killServer.setDisable(true);
		
		/* Java in-built implementation interface, which implements the run method and
		 * starts the killServer() function on a new thread when invoked.
		 */
		Runnable task = new Runnable() {
			@Override
			public void run() {
				killServer();
			}
		};
		
		// Thread that is used to invoke the Runnable implementation
		// and start the thread
		Thread killServer = new Thread(task);
		killServer.setDaemon(false);
		killServer.start();
	}

	public void quit() {
		if(serverFlag.equals(1)) {
			try {
				
				for(Integer shutServer : threadMap.keySet()) {
					Socket demo = threadMap.get(shutServer).getSocket();
					PrintWriter pw = new PrintWriter(demo.getOutputStream(), true);
					pw.println("serverdown");
					pw.close();
				}
				
				BullyServer.threadMap.clear();
				server.close();
				serverFlag = 0;
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		}
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				scrollView.setText("\nClosing Application....");
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
	@Override
	public void start(Stage primaryStage) throws Exception {
		 this.primaryStage = primaryStage;
	     this.primaryStage.setTitle("Server Panel");
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
            loader.setLocation(BullyServer.class.getResource("ServerView.fxml"));
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
            killServer.setDisable(true);
            scrollView.clear();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
	
	// Return the primary stage of the application, that is running the GUI and controls it
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    //-------------------------------------------------------------------------------------------------------------------------------->
    
    public static void main(String[] args) {
		launch(args);
	}
	
}

//------------------------------------------------------------------------------------------------------------------------------------->

// ProcessThread class, where each new connection is started and manages in it's own different thread
class ProcessThread extends Thread {
	
	// All the variables of ProcessThread class are as below -------------------------------------------------------------------------->
	
	// Public lock, which is used to lock the resources, when one thread is accessing it.
	public static ReentrantLock lock = new ReentrantLock();
	
	// BullyServer class object, that is used to access the UI Components
	private BullyServer ui;
	
	// Socket for the connection
	private Socket processNode;
	
	// Node id as read from the user itself
	private String nodeId;
	
	// Stores the numeric node id of the node id sent by user
	private Integer node;
	
	// Input stream for the connection
	private BufferedReader is;
	
	// Output stream for the connection
	private PrintWriter os;
	
	// stores the messages received during the I/O operations, one at a time
	private String message;
	
	// Used to stores the tempId, generated for the new connection
	private Integer tempId;
	
	// coordinator flag for the node
	private Boolean coordinator = true;
	
	// Node disconnection tracker
	private Boolean nodeDisconnected;
	
	//--------------------------------------------------------------------------------------------------------------->
	
	// constructor for the process thread class
	public ProcessThread(Socket processNode, Integer tempId, BullyServer ui) {
		this.processNode = processNode;
		this.tempId = tempId;
		this.ui = ui;
	}
	
	public synchronized Boolean getCoordinator() {
		return this.coordinator;
	}
	
	public synchronized void setCoordinator(Boolean flag) {
		this.coordinator = flag;
	}
	
	public synchronized Socket getSocket() {
		return this.processNode;
	}
	
	/* When the process node sends the message OK, it will be intercepted
	 * and send to the node from which election was received.
	 */
	private synchronized void sendOkMessage(Integer sendOkTo) {
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// get the I/O stream to the election call node
		ProcessThread pt = BullyServer.threadMap.get(sendOkTo);
		Socket demo = pt.getSocket();
		
		// writing the OK to the calling node
		try {
			PrintWriter pw = new PrintWriter(demo.getOutputStream(), true);
			pw.println("ok");
			pw.println("Message from [" + node + "] : OK");
			
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ui.setLogger("\nMessage from [" + node + "] to [" + sendOkTo + "] : OK\n");
				}
			});
			System.out.println("Message from [" + node + "] to [" + sendOkTo + "] : OK");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* This method intercepts the alive message from the coordinator,
	 * and broadcast it to all the active nodes in the network
	 */
	private synchronized void sendAliveMessage() {
		
		System.out.println("Node [" + node + "] : COORDINATOR ALIVE");
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ui.setLogger("\nNode [" + node + "] : COORDINATOR ALIVE\n");
				ui.setLogger("Broadcasting to:");
			}
		});
		System.out.println("Node [" + node + "] : COORDINATOR ALIVE");
		System.out.print("Broadcasting to:");
		
		
		// iterating the thread map to traverse all active connections
		for(Integer sendAliveTo : BullyServer.threadMap.keySet()) {
			
			if(sendAliveTo != node) {
				
				// extracting the socket connection information to the node
				ProcessThread pt = BullyServer.threadMap.get(sendAliveTo);
				Socket demo = pt.getSocket();
			
				// writing the alive message to the node
				try {
					PrintWriter pw = new PrintWriter(demo.getOutputStream(), true);
					pw.println("Message from [" + node + "] : COORDINATOR ALIVE");
					
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							ui.setLogger(" [" + sendAliveTo + "]");
						}
					});
					System.out.print(" [" + sendAliveTo + "]");
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ui.setLogger("\n");
			}
		});
		System.out.println("\n");
	}
	
	/* get connection to all active nodes and 
	 * Broadcasts the new coordinator message to all of active
	 * nodes
	 */
	private synchronized void broadcastCoordinatorMessage() {
		Integer newCoordinator = null;
		try {
			newCoordinator = Integer.parseInt(is.readLine());
			System.out.println("New coordinator: " + newCoordinator);
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		BullyServer.coordinatorNode = newCoordinator;
	
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ui.setLogger("\nMessage from [" + node + "] : COORDINATOR\n");	
			}
		});
		
		// iterating the thread map to traverse all active connections
		for(Integer broadcastCoordinator: BullyServer.threadMap.keySet()) {
			
				// extracting the socket connection information to the node
				ProcessThread pt = BullyServer.threadMap.get(broadcastCoordinator);
				Socket demo = pt.getSocket();
			
				// writing the new coordinator message to the node
				try {
					PrintWriter pw = new PrintWriter(demo.getOutputStream(), true);
					pw.println("coordinator");
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					
					pw.println(newCoordinator);
					pw.println("Message from [" + node + "] : COORDINATOR");
					pw.println(BullyServer.threadMap.size());
					System.out.println("[" + node + "] : COORDINATOR TO ["+broadcastCoordinator+"]\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/* gets invoked when a election request is received from a node to
	 * the server, which is them simply forwarded to all the
	 * active nodes
	 * 
	 * Invoked by startElection method
	 */
	public synchronized void broadcastElectionMessage() {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ui.setLogger("\n");
			}
		});
		System.out.println("");
		
		// iterate through the map, to get sockets, to the active nodes
		for(Integer sendElection: BullyServer.threadMap.keySet()) {
				//os.println("Message to [" + sendElection + "] : ELECTION!");
				
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				// storing the socket information in temporary variable
				ProcessThread pt = BullyServer.threadMap.get(sendElection);
				Socket demo = pt.getSocket();
				
				// getting output stream, and writing to the node, one a t a time
				try {
					PrintWriter pw = new PrintWriter(demo.getOutputStream(), true);
					pw.println("election");
					pw.println(node);
					pw.println("Message from [" + node + "] : ELECTION");
					
	
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							ui.setLogger("Message from [" + node + "] to [" + sendElection + "] : ELECTION\n");
						}
					});
					System.out.println("Message from [" + node + "] to [" + sendElection + "] : ELECTION");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
		}
		
		// affirm the caller that election has been broadcasted
		os.println("electionBroadcasted");
	}
	
	/* This is the main block, that handles the initiating of an election,
	 * where it sends the election messages to all the active network
	 * nodes
	 */
	private synchronized void startElection() {
		
		/* sends the message back to caller, to affirm the initiating of an election
		 * and logs the messages accordingly
		 */
		os.println("Starting Election!");
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ui.setLogger("\nNode [" + nodeId + "] : Starting Election!\n");
			}
		});
		System.out.println("Node [" + node + "] : Starting Election!");
		
		// blocks the broadcasting to one node only,
		// i.e. when one process has a lock, only that that start
		// election. This is done to handle multiple multiple 
		// election requests received concurrently
		ReentrantLock lock = new ReentrantLock();
		synchronized (message) {
			lock.lock();
			
			/* this block opens output streams to the active nodes and send election message
			 * only to nodes higher than calling node
			 */
			broadcastElectionMessage();
			lock.unlock();
		}
		//System.out.println("Node [" + node + "] SEND ELECTION LOCK : " + lock.isHeldByCurrentThread());
	}
	
	@Override
	public void run() {
		try {
			// Opens the socket I/O streams to the particular connected node
			is = new BufferedReader(new InputStreamReader(processNode.getInputStream()));
			os = new PrintWriter(processNode.getOutputStream(),true);
		} catch(IOException ie) {
			ie.printStackTrace();
		}
		
		try {
			
			// reads the first message i.e. the the nodeID which is send by the client
			message = is.readLine();
			nodeId = message;
			
			// sets the name of this thread to the nodeID received
			Thread.currentThread().setName(nodeId);
			
			// Updates the node key in the HashMap, which has a record for all active nodes
			synchronized (BullyServer.threadMap) {
				lock.lock();
				node = Integer.parseInt(nodeId);
				ProcessThread pt = BullyServer.threadMap.get(tempId);
				BullyServer.threadMap.remove(tempId);
				BullyServer.threadMap.put(node, pt);
				lock.unlock();	
			}

			// printing connection message to the node
			os.println("Network Connection Successful");
			
			// displays the connection messages and all active nodes, when a node connects
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ui.setLogger("\nNode [" + node + "] : Connected\n");
					ui.setLogger("\nActive Nodes: " + BullyServer.threadMap.keySet() + "\n");
				}
			});
			System.out.println("Node [" + node + "] : Connected");
			System.out.println("Active Nodes: " + BullyServer.threadMap.keySet());
			
			/* This loop sets the server to always listen to the incoming node messages,
			 * intercepts them and takes the action accordingly.
			 */
			while(true) {
					message = is.readLine();
					System.out.println("Message: " + message);
					
					synchronized (message) {
						lock.lock();
						nodeDisconnected = false;

						if(message.equals("election")) {

							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							startElection();
							
						} else if(message.equals("alive") && BullyServer.coordinatorNode.equals(node) && BullyServer.serverFlag == 1) {

							sendAliveMessage();
								
							
						} else if(message.equals("sendOK")) {
							
							String sendOkToNode = is.readLine();
							sendOkMessage(Integer.parseInt(sendOkToNode));
							
							
						} else if(message.equals("coordinator")) {
							
							broadcastCoordinatorMessage();
							
						} else if(message.equals("exit")) {
							
							BullyServer.threadMap.remove(node);
							processNode.close();
							nodeDisconnected = true;
							
						}
						lock.unlock();
					}
					
					
					if(nodeDisconnected.equals(true)) {
						if(BullyServer.coordinatorNode.equals(node)) {

							// Displays the node disconnected message, when node closed
							ui.setLogger("\nNode [" + node + "] : Disconnected\n");
						}
		
						// checks if any active nodes in system, and displays the message as below
						if(BullyServer.threadMap.size() == 0) {

							ui.setLogger("\nActive Nodes: " + BullyServer.threadMap.keySet() + "\n");
						}
						System.out.println("Node [" + node + "] : Disconnected\n");
						break;
					}
			}
		} catch (IOException e) {
			System.out.println("\nServer Down\n");
			//e.printStackTrace();
			
			/* The exception below occurs, when the server is waiting for a request from node
			 * and node abruptly closes, throwing a null pointer exception on read operation
			 */
		} catch(NullPointerException npe) {
			BullyServer.threadMap.remove(node);
			ui.setLogger("\nNode [" + node + "] : Crashed\n");
			System.out.println("Node [" + node + "] : Crashed");
		}
	}
}
