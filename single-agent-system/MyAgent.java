package single-agent-system;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class MyAgent extends Agent {	
	
	private static final long serialVersionUID = 1L; /*SerialVersionUID is used to ensure that during deserialization
	the same class that was used during serialize process is loaded. Serialization is a mechanism of converting the state
	of an object into a byte stream & deserialization is the reverse process*/
	
	public static String[] directions = {"left", "right", "up", "down"};
	
    public void setup() {  //registers agent to directory facilitator
    	
    	//generates a random id for the agent, so that dfd looks like e.x: {AID: someAID}
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        //defines the type of agent as "agent" & its full name as e.x: Agent-37
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent");
        sd.setName(getLocalName());
        dfd.addServices(sd);  //now dfd looks like e.x: {AID:someAID, (LocalName: "Agent-37", Type: "agent")}
        
        //attempt the registration of the agent
        try {DFService.register(this, dfd);}
        catch (FIPAException fe) {fe.printStackTrace();}

        //while(1): runs until the agent drops out
        addBehaviour(new CyclicBehaviour(this) {
        	
        	private static final long serialVersionUID = 2L;
        	
        	//declares the possible actions during this current behavior
            public void action() {

                ACLMessage msg = null;
                msg = blockingReceive();  //blocks agent until message appears in queue (waits for message)

                if(msg.getContent().equals("Pick a Direction")){  //case: direction-message
                    String direction = pickDirection();
                    
                    //the agent creates a message to "inform" the receiver about his action
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    
                    message.addReceiver(new AID("ThisGrid", AID.ISLOCALNAME));  /*grid is the receiver of the
                    message | reference to receiver by local name*/
                    message.setContent(direction);  //informs the grid about his decision
                    
                    send(message);  //the message now looks like this e.x: {Receiver: "Grid", Content: "left"}
                    //block(1000);
                }

                else if (msg.getContent().contains("You can see,")){  //case: see-message
                    String[] array = msg.getContent().split(",");  //array is e.x: ["You can see", "26"]
                    
                    //the agent creates a message to "tell" the receiver hello
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    
                    message.addReceiver(new AID("Agent-"+array[1], AID.ISLOCALNAME));  /*Some other agent is the
                    receiver of the message | reference to receiver by local name*/
                    message.setContent("Hello");  //greets the other agent
                    
                    send(message);  //the message now looks like this e.x: {Receiver: "Agent-26", Content: "Hello"}
                }

                else if (msg.getContent().equals("Hello")){  // case: hello-message
                	
                	//the agent says outloud that some other agent just greeted him
                    System.out.println(getLocalName()+": "+msg.getSender().getLocalName()+" says Hello");
                    //the output looks like this e.x.: >> Agent-37: Agent-17 says Hello
                }

                else if(msg.getContent().equals("End")){  //case: end-message
                	
                	//the agent says outloud that he is now out of order
                    System.out.println(getLocalName()+" terminating");
                    //the output looks like this e.x.: >> Agent-37 terminating
                    takeDown();  //takes down from registry, agent is no longer available
                    doDelete();  //terminates
                }
            }
        });
    }

    
	private String pickDirection() {
	   
		Grid grid = new Grid();
	    AStar aStar = new AStar();
	    
	    // SOME IMPORTANT AGENT VARIABLES
		String[] desires = new String[Grid.rate];  // desires = his need to serve every client on the grid
		String intention;  // the best choice among all his desires
		int num = Integer.parseInt(getLocalName().replaceAll("[\\D]", ""));
		String agentName = "A" + String.valueOf(num);
		boolean found = false;
		int[] choices = new int[Grid.rate];
		
		// SOME IMPORTANT GRID VARIABLES
		int[][] nextBlock = new int[4][2];  // all possible next positions of each agent (refers to 1 agent at a time)
		int[][] endBlock = new int[Grid.numberOfAgents][2];  // the goal position of all the agents (ATTENTION: not always the same with some client's goal position)
		String optDir = null;
		
		
		// find where the agents lies on the grid
		for(int i = 0; i < Grid.locations.length; i++) {
			// if the agent's coordiantes are already found
    		if(found) {
    			break;
    		}
			for(int j = 0; j < Grid.locations[i].length; j++) {
				
				   if(Grid.locations[i][j].contains(agentName)) {
					   found = true;
					   int[] currBlock = {i, j};  //the current position of the agent
					    // defines all the possible (x,y) positions where the agent can move to from his current position (stores them in coordinates array)
						for(int dr = 0; dr < 4; dr++) {  // for each one of these directions: left, right, up & down
							
							nextBlock[dr] = grid.directionsToCoordinates(directions[dr], i, j); /* e.x: if current position = {1,3}
						    nextBlock array look like: {{1,2}, {1,4}, {0,3}, {2,3}} one pair for each d in directions array */
						}
						
						// IS THE AGENT AVAILABLE BEFORE THE MOVE?
						for (int c = 0; c < Grid.clients.size(); c++) {
							if ((Grid.agentsXclients[num][c] == true) && (Grid.locations[i][j].contains("C" + String.valueOf(c)))) {
								Grid.rewards[num] = Grid.rewards[num] - 1;  // gives a small negative reward for serving this new client
								Grid.availability[num] = false;  //since the agent and his client are on the same position, he has to carry him with him
								break;
							}
							else {
								Grid.availability[num] = true;  // the agent is still available
								continue;
							}
						}
						
						// WHAT KIND OF MOVE WILL THE AGENT MAKE? WHERE DOES HE WANT TO GO?
						for (int k = 0; k < Grid.clients.size(); k++) {  //for each client
							
							int[] firstPos = {Integer.parseInt((Grid.clients.get(k).get(1)).toString()), Integer.parseInt((Grid.clients.get(k).get(2)).toString())};
							int[] goalPos = {Integer.parseInt((Grid.clients.get(k).get(3)).toString()), Integer.parseInt((Grid.clients.get(k).get(4)).toString())};
								
							// IN CASE THE AGENT CARRIES NO CLIENTS
							if (Grid.availability[num]) {
									
								if (Grid.agentsXclients[num][k] == true) {  // if the agent is already serving this client
									//AGENT'S GOAL IS TO REACH THE CLIENT SO THAT THEY BOTH START TRAVELLING TOGETHER
									endBlock[num][0] = firstPos[0];
									endBlock[num][1] = firstPos[1];
										
									optDir = aStar.optimalPosition(agentName, Grid.initialPosition, currBlock, nextBlock, endBlock);  /*gets the best move for the agent that will get
									him closer to his client-partner*/
									break;
								}
									
								else if (!allFalse(Grid.agentsXclients[num])) {  // if the agent is serving some client, but not client k
									continue;
								}
									
								else {  // if the agent is not serving any client
									
									desires[k] = (Grid.clients.get(k).get(0)).toString();
									choices[k] = Math.abs(i-firstPos[0]) + Math.abs(j-firstPos[1]) + Math.abs(firstPos[0]-goalPos[0]) + Math.abs(firstPos[1]-goalPos[1]);
										
									if (k == Grid.rate - 1) {
										int minCost = getBiasedMin(choices);
										intention = getBiasedCorrespondingString(minCost, choices, desires);  //contains the name of the best client according to the agent's knowledge base
										int intentionId = Integer.parseInt(intention.replaceAll("[\\D]", "")); 
										
										System.out.println("Agent-" + num + ": I choose to serve client > " + intention);
										Grid.agentsXclients[num][intentionId] = true;  //announces that agent[num] and client[k] are now partners (soon they'll start travelling together)
									    
										firstPos[0] = Integer.parseInt((Grid.clients.get(intentionId).get(1)).toString());
										firstPos[1] = Integer.parseInt((Grid.clients.get(intentionId).get(2)).toString());
										
										//AGENT'S GOAL IS TO REACH THE CLIENT SO THAT THEY BOTH START TRAVELLING TOGETHER
										endBlock[num][0] = firstPos[0];
										endBlock[num][1] = firstPos[1];
										
										if (i == firstPos[0] && j == firstPos[1]) {  //EXTREME CASE: the client lying on the same position as this available agent
											goalPos[0] = Integer.parseInt((Grid.clients.get(intentionId).get(3)).toString());
											goalPos[1] = Integer.parseInt((Grid.clients.get(intentionId).get(4)).toString());
											
											Grid.availability[num] = false;
											Grid.agentsXclients[num][intentionId] = true;
											//AGENT'S GOAL IS TO REACH THE CLIENT'S GOAL SO THAT THEY BOTH GET DISMISSED
											endBlock[num][0] = goalPos[0];
											endBlock[num][1] = goalPos[1];
											
											if (i == goalPos[0] && j == goalPos[1]) {  // VERY EXTREME CASE: the goal position is where client & agent lie
												//MAKES ALL THE CHANGES AFTER THE FINAL MOVE THAT LEAD TO THE DESTINATION OF THE CLIENT
		                    					Grid.locations[i][j] = Grid.locations[i][j].replace(" " + intention, "");  // removes client from grid
		                    					Grid.servedClients.add(intentionId);
		                    					Grid.leftovers = Grid.leftovers - 1;
		                    					Grid.agentsXclients[num][intentionId] = false;
												String response = "no need to";
												return response;
											}
										}
										optDir = aStar.optimalPosition(agentName, Grid.initialPosition, currBlock, nextBlock, endBlock);  /*gets the best move for the agent that will get
										him closer to his client-partner*/
									}
									else {  // if there are still check-ups to be done before announcing that the agent is not serving any clients
										continue;
									}	
								}
							}
							// IN CASE THE AGENT CARRIES SOME CLIENT (HE IS NOT AVAILABLE)
							else {
									
								if (Grid.agentsXclients[num][k] == true) {  //if this client is the one that the agent carries
									
									//AGENT'S GOAL IS TO REACH THE CLIENT'S GOAL SO THAT THEY BOTH GET DISMISSED
									endBlock[num][0] = goalPos[0];
									endBlock[num][1] = goalPos[1];
										
									optDir = aStar.optimalPosition(agentName, Grid.initialPosition, currBlock, nextBlock, endBlock);  /*gets the best move for the agent that will get
									him closer to his client's goal destination*/
								}
								else {  //if this client is not the one that the agent carries (some other must be, so go find him)
									continue;
								}
							}
						}
					break;  //leaves after having checked out all the clients of the grid
				   }
				   else {
					   continue;
				   }
			}
		}   
		return optDir;
	}
	
	
	// checks if all values in a boolean array are false
	public static boolean allFalse (boolean[] array) {
	    for (boolean value : array) {
	        if (value)
	            return false;
	    }
	    return true;
	}
	
	
	// biased method for getting the minimum value of an array ignoring some of its elements
	public int getBiasedMin(int[] inputArray) {
		int biasedMinValue = 99999; 
		for(int i = 0; i < inputArray.length; i++){
			if(inputArray[i] < biasedMinValue && !Grid.servedClients.contains(i)) {
				biasedMinValue = inputArray[i];
				} 
			} 
		return biasedMinValue;
	}
	
	
	// biased method for getting the direction which corresponds to the minimum value of the distances array
		public String getBiasedCorrespondingString(int minValue, int[] intArray, String[] strArray){
			String biasedBest = null;
			for(int bcs = 0; bcs < intArray.length; bcs++) { 
				int id = Integer.parseInt(strArray[bcs].replaceAll("[\\D]", ""));
				if(intArray[bcs] == minValue && !Grid.servedClients.contains(id)) {
					biasedBest = strArray[bcs];
					break;
				}
			}
			return biasedBest;
		}
}
