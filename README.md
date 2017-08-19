# Bully Leader Algorithm

This project is a **Java** implementation that aims to select the leader using the bully election method, where the node with the highest priorities becomes the leader. In order to give a proper view on how this election works and the nodes communicate, user **GUI** using **JavaFX** has been implemented, which shows the concise communication messages between all the participating nodes.

## _Basic Algorithm Mechanism_

- When a node joins the network, it will start the election.

- Send the **election** message to every active node on the network and wait for response from the nodes.

- On receiving **election** message, compare the priority of the node
    1. if a lower priority than that of the election initiating node, take no action and wait for election to complete.
    2. if a higher priority than that of the election initiating node, send an **OK** acknowledgement message, indicating a take over.

- If there is no response from the nodes and the timer times out, indicates that no node exists with a higher priority. So, declare itself leader and pass on the **leader** message to other nodes. Start sending out **keep alive** message at a fixed intervals to indicate the liveliness.

After a leader is elected, if the leader crashes, a new leader has to be elected again. The node which first detects that the leader is  dead, shall initiate a new election and the process stated above, repeats.

## Requirements to run the program
* Java 1.8 SDK is a minimum requirement
* JavaFX API to be imported to project folder

## Run instructions for Eclipse IDE
1) File -> Import -> Select the whole folder

2) You might need to download the JavaFX package to build application. Follow instruction in the link 
   below to install JavaFX in Eclipse
   
   https://www.eclipse.org/efxclipse/install.html
3) After installing, you need to add this SDK to the imported project.

4) Right click project folder -> Properties -> Java Build Path -> Select Libraries Tab -> Add 
   Library -> Select JavaFX SDK

5) After this you can run the files and play around with the application
