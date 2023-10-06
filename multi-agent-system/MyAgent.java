package multi-agent-system;
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
	
	public static String[] directions = {"left", "right", "up", "down", "here"};
	public static int[][] currBlock = new int[Grid.numberOfAgents][2];  // the current position of all the agents
	public static int[][] endBlock = new int[Grid.numberOfAgents][2];  // the goal position of all the agents (ATTENTION: not always the same with some client's goal position)
	public static boolean[] isWaiting = new boolean[Grid.numberOfClients];
	
	
	/*************************************************************************  SETUP METHOD  ************************************************************************************/
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

        
        /***********************************************************************  AGENT'S BEHAVIOUR  *****************************************************************************/
        addBehaviour(new CyclicBehaviour(this) {  //while(1): runs until the agent drops out
        	
        	private static final long serialVersionUID = 2L;
        	
        	//declares the possible actions during this current behavior
            public void action() {

                ACLMessage msg = null;
                msg = blockingReceive();  //blocks agent until message appears in queue (waits for message)

                
                /**********************************************************  MESSAGE: PICK DIRECTION  *************************************************************************/
                if(msg.getContent().equals("Pick a Direction")){  //case: direction-message
                    String direction = pickDirection();
                    
                    //the agent creates a message to "inform" the receiver about his action
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    
                    message.addReceiver(new AID("ThisGrid", AID.ISLOCALNAME));  /*grid is the receiver of the
                    message | reference to receiver by local name*/
                    message.setContent(direction);  //informs the grid about his decision
                    
                    send(message);  //the message now looks like this e.x: {Receiver: "Grid", Content: "left"}
                }

                
                /********************************************************  MESSAGE: GREET YOUR FELLOW DRIVER  ******************************************************************/
                else if (msg.getContent().contains("You can see,")){  //case: see-message
                    String[] array = msg.getContent().split(",");  //array is e.x: ["You can see", "26"]
                    
                    //the agent creates a message to "tell" the receiver hello
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    
                    message.addReceiver(new AID("Agent-"+array[1], AID.ISLOCALNAME));  /*Some other agent is the
                    receiver of the message | reference to receiver by local name*/
                    message.setContent("Hello");  //greets the other agent
                    
                    send(message);  //the message now looks like this e.x: {Receiver: "Agent-26", Content: "Hello"}
                }

                
                /******************************************************  MESSAGE: RESPOND TO YOUR FELLOW DRIVER  ****************************************************************/
                else if (msg.getContent().equals("Hello")){  // case: hello-message
                	
                	int id = Integer.parseInt(getLocalName().replaceAll("[\\D]", ""));
                	//the agent says outloud that some other agent just greeted him
                	if (!Grid.DEBUG) {
                		System.out.println(msg.getSender().getLocalName().toUpperCase()+": Hello A" + id + "!");
					}
                    //the output looks like this e.x.: >> Agent-37: Agent-17 says Hello
                }

                
                /***************************************************************  MESSAGE: AGENT TERMINATING  *******************************************************************/
                else if(msg.getContent().equals("End")){  //case: end-message
                	
                	//the agent says outloud that he is now out of order
                	if(!Grid.DEBUG) {
                		System.out.println(getLocalName().toUpperCase()+": terminating...");
                	}
                    //the output looks like this e.x.: >> Agent-37 terminating
                    takeDown();  //takes down from registry, agent is no longer available
                    doDelete();  //terminates
                }
            }
        });
    }

    
    /*********************************************************************  METHOD: PICK DIRECTION  **********************************************************************************/
	private String pickDirection() {
	   
	    AStar aStar = new AStar();
	    
	    // SOME IMPORTANT AGENT VARIABLES
		String[] desires = new String[Grid.clients.size()];  // desires = his need to serve every client on the grid
		String intention;  // the best choice among all his desires
		int num = Integer.parseInt(getLocalName().replaceAll("[\\D]", ""));  //num = agents' id
		String agentName = "A" + String.valueOf(num);
		boolean found = false;
		int[] choices = new int[Grid.clients.size()];
		
		// SOME IMPORTANT GRID VARIABLES
		int[][] nextBlock = new int[directions.length][2];  // all possible next positions of each agent (refers to 1 agent at a time)
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
					   
					   //the current position of the agent
					   currBlock[num][0] = i; 
					   currBlock[num][1] = j;
					   
					    // defines all the possible (x,y) positions where the agent can move to from his current position (stores them in coordinates array)
						for(int dr = 0; dr < directions.length; dr++) {  // for each one of these directions: left, right, up & down
							
							nextBlock[dr] = Grid.directionsToCoordinates(directions[dr], i, j); /* e.x: if current position = {1,3}
						    nextBlock array look like: {{1,2}, {1,4}, {0,3}, {2,3}} one pair for each d in directions array */
						}
						
						//IS THE AGENT BLOCKED?
						if (Grid.roundsBlocked.containsKey(num)) {
							optDir = directions[4];  //directions[4] = "here"
							update_agentsXcells(num, optDir);
							return optDir;
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
										
									optDir = aStar.optimalPosition(agentName, nextBlock);  /*gets the best move for the agent that will get
									him closer to his client-partner*/
									break;
								}
									
								else if (!allFalse(Grid.agentsXclients[num])) {  // if the agent is serving some client, but not client k
									continue;
								}
									
								else {  // if the agent is not serving any client
									
									desires[k] = (Grid.clients.get(k).get(0)).toString();
									choices[k] = Grid.heuristics[num][AStar.getArrayIndex(Grid.colours, firstPos)][i][j] + Grid.heuristics[num][AStar.getArrayIndex(Grid.colours, firstPos)][goalPos[0]][goalPos[1]];
										
									if (k == Grid.clients.size() - 1) {
										int minCost = getBiasedMin(choices);
										intention = getBiasedCorrespondingString(minCost, choices, desires);  //contains the name of the best client according to the agent's knowledge base
										
										if(intention == null) {
											optDir = directions[4];  //directions[4] = "here"
											update_agentsXcells(num, optDir);
											return optDir;
										}
										
										int intentionId = Integer.parseInt(intention.replaceAll("[\\D]", "")); 
										
										if(!Grid.DEBUG) {
											System.out.println("AGENT-" + num + ": I want to serve C" + intentionId + ".");
										}
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
											
											//AGENT'S GOAL IS TO REACH THE CLIENT'S GOAL SO THAT THEY BOTH GET DISMISSED
											endBlock[num][0] = goalPos[0];
											endBlock[num][1] = goalPos[1];
											
											if (i == goalPos[0] && j == goalPos[1]) {  // VERY EXTREME CASE: the goal position is where client & agent lie
												optDir = directions[4];  //directions[4] = "here"
												update_agentsXcells(num, optDir);
												return optDir;
											}
										}
										optDir = aStar.optimalPosition(agentName, nextBlock);  /*gets the best move for the agent that will get
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
										
									optDir = aStar.optimalPosition(agentName, nextBlock);  /*gets the best move for the agent that will get
									him closer to his client's goal destination*/
									break;
								}
								else {  //if this client is not the one that the agent carries (some other must be, so go find him)
									continue;
								}
							}
						}
						update_agentsXcells(num, optDir);
					    break;  //leaves after having checked out all the agents of the grid
				   }
				   else {
					   continue;
				   }
			}
		}
		return optDir;
	}
	
	
	/*************************************************************  METHOD: UPDATE AGENTS X CELLS  ******************************************************************************/
	public static void update_agentsXcells(int id, String dir) {
		int[] nextPos = Grid.directionsToCoordinates(dir, currBlock[id][0], currBlock[id][1]);
		if(nextPos[0] < 5 && nextPos[0] > -1 && nextPos[1] < 5 && nextPos[1] > -1) {  //if nextPos is not out of bounds (because there can't be any conflict when out of bounds) {
			if(Grid.isAllowed(currBlock[id], nextPos)) {
				Grid.agentsXcells[id][currBlock[id][0]][currBlock[id][1]] = false;
				Grid.agentsXcells[id][nextPos[0]][nextPos[1]] = true;
			}
		}
	}
	
	
	/*********************************************************  METHOD: REVERSE UPDATE AGENTS X CELLS  *************************************************************************/
	public static void reverse_update_agentsXcells(int id, String dir) {
		int[] nextPos = Grid.directionsToCoordinates(dir, currBlock[id][0], currBlock[id][1]);
		if(nextPos[0] < 5 && nextPos[0] > -1 && nextPos[1] < 5 && nextPos[1] > -1) {  //if nextPos is not out of bounds (because there can't be any conflict when out of bounds) {
			if(Grid.isAllowed(currBlock[id], nextPos)) {
				Grid.agentsXcells[id][nextPos[0]][nextPos[1]] = false;
				Grid.agentsXcells[id][currBlock[id][0]][currBlock[id][1]] = true;
			}
		}
	}


	/*********************************************************************  METHOD: ALL FALSE  **********************************************************************************/
	public static boolean allFalse (boolean[] array) {  // returns true if all values in a boolean array are false
	    for (boolean value : array) {
	        if (value)
	            return false;
	    }
	    return true;
	}
	
	
	/*********************************************************************  METHOD: GET BIASED MIN  ******************************************************************************/
	public int getBiasedMin(int[] inputArray) {  // biased method for getting the minimum value of an array ignoring some of its elements
		int biasedMinValue = 83748621; 
		for(int i = 0; i < inputArray.length; i++){
			if(inputArray[i] < biasedMinValue && !Grid.servedClients.contains(i) && isWaiting[i]) {
				biasedMinValue = inputArray[i];
			}
		}
		return biasedMinValue;
	}
	
	
	/************************************************************  METHOD: GET BIASED CORRESPONDING STRING  *********************************************************************/
	public String getBiasedCorrespondingString(int minValue, int[] intArray, String[] strArray){  /* biased method for getting the string which
	corresponds to the minimum value of an array */
		String biasedBest = null;
		
		//returns null if there is no other client waiting (if it's impossible for agentsXclients[agId] to contain any true values)
		if(minValue == 83748621) {
			return null;
		}
		
		for(int bcs = 0; bcs < intArray.length; bcs++) { 
			int id = Integer.parseInt(strArray[bcs].replaceAll("[\\D]", ""));
			if(intArray[bcs] == minValue && !Grid.servedClients.contains(id) && isWaiting[id]) {
				biasedBest = strArray[bcs];
				break;
			}
		}
		return biasedBest;
	}
}
