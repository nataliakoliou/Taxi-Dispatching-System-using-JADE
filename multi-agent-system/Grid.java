package multi-agent-system;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class Grid extends Agent {
	
	private static final long serialVersionUID = 3L; /*SerialVersionUID is used to ensure that during deserialization
	the same class that was used during serialize process is loaded. Serialization is a mechanism of converting the state
	of an object into a byte stream & deserialization is the reverse process*/
	
	public static boolean DEBUG = false;
    private int tries=0;
    public static int maxTries=20;  //number of actions in an episode | each agent makes a total of maxTries actions
    private String[] agentArray;
    public static int numberOfAgents = 3;
    public static int numberOfClients = maxTries*numberOfAgents;
    public static int x = 5;
    public static int y = 5;
    public static String[][] locations = new String[x][y];
    public static ArrayList<ArrayList<Object>> clients = new ArrayList<ArrayList<Object>>();  //list of all the clients data
    private int visibility = 1;  //how far it is possible for an agent to detect another agent
    public static int rate = 4;  // number of clients for each round
    public static int[][] colours = {{0,0},{4,0},{4,3},{0,4}};
    public static int[][] initialPosition = new int[numberOfAgents][2];  // an array with the agents' initial coordinates (x,y) on the grid
    private boolean agentFound = false;
    public static boolean[] availability = new boolean[numberOfAgents]; // defines which agent is available (doesn't carry any client)
    public static boolean[][] agentsXclients = new boolean[numberOfAgents][numberOfClients];  // links each agent with the client he decided to serve
    public static boolean[][][] agentsXcells = new boolean[numberOfAgents][x][y];  // links each agent with the cell he decided to visit
    public static boolean wasSuccessful;  // boolean variable to define whether the move took place
    public static ArrayList<Integer> servedClients = new ArrayList<Integer>();  //contains the  ids' of the served clients
    public static ArrayList<ArrayList<Object>> forbiddenMoves = new ArrayList<ArrayList<Object>>();  //list of all forbidden moves
    public static int[] rewards = new int[numberOfAgents];
    public static int[][][][] heuristics = new int[numberOfAgents][colours.length][x][y];
    private int total_clients = 0;
    public static Hashtable<Integer, Integer> roundsBlocked = new Hashtable<>();  // Key: Agent number -> Value: Rounds until the agent can perform the wanted move
    public boolean firstTime = false;
    public boolean moreClients = false;
    public List<String> thanksgiving = new ArrayList<String>();

    Random rand = new Random();  // creates instance of Random class
    
   
    /*************************************************************************  SETUP METHOD  ************************************************************************************/
    public void setup(){  //registers grid to directory facilitator
    	
    	//set every null value of the grid to "" | value "" indicates that one position is empty
    	for (int r = 0; r < x; r++){
    		for (int c = 0; c < y; c++){
    			locations[r][c] = "";
    		}
    	}

        try {Thread.sleep(50);} catch (InterruptedException ie) {}  //waits so that every agent is registered before the grid's turn
        
        //defines what type of agents it's looking for
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent");  //defines the type of agent as "agent"
        template.addServices(sd);  //now template looks like e.x: {Type: "agent"}
        
        //searches the registry for agents
        try {
            DFAgentDescription[] result = DFService.search(this, template);  /*result: an array full of the agents of type
            "agent" declared in template | result looks like this e.x: [{AID: someAID, (LocalName: "Agent-43", Type: "agent")},
            {AID: someAID, (LocalName: "Agent-98", Type: "agent")}, ... , {AID: someAID, (LocalName: "Agent-6", Type: "agent")}]*/
            
            //creates an array full of the local names of the agents found in result array
            numberOfAgents = result.length;  //stores the length of result array
            agentArray = new String[numberOfAgents];
            for (int i = 0; i < numberOfAgents; ++i) {
                agentArray[i] = result[i].getName().getLocalName();
            }
        }
        catch (FIPAException fe) {fe.printStackTrace();}
        
        //prints all the agents stored in agentArray
        Arrays.sort(agentArray);  //sorts the agentArray according to the agents' id inside the local name

        
        /************************************************************************  GRID'S BEHAVIOUR  *****************************************************************************/
        addBehaviour(new CyclicBehaviour(this) {  //runs until the grid shuts down | loops for maxTries times (tries=0,1,2,3,4)
        	
        	private static final long serialVersionUID = 4L;
        	
            public void action(){

                try {Thread.sleep(80);} catch (InterruptedException ie) {}  //waits for the agents to get ready
                
                if (tries <= maxTries) {
                	
                	/**************************************************************  CASE: FOR EACH TRY  **********************************************************************/
                	System.out.println("*".repeat(106));
                	if (!roundsBlocked.isEmpty()) {  //updates the roundsBlocked HashTable at the beginning of each new try
                		
                		Set<Integer> keys = roundsBlocked.keySet();  // Getting keySets of Hashtable and storing it into Set
                		Iterator<Integer> itr = keys.iterator();  // Creating an Iterator object to iterate over the given Hashtable
                		List<Integer> keyList = new ArrayList<>();
                		
                		//stores all the keys with values less than 1 into a list
                		while (itr.hasNext()) {
                			int key = itr.next();  // Getting key of a particular entry
                        	if (roundsBlocked.get(key) < 2) {  /*if value is 0 (the winner) OR if value 1 (the second winner who soon becomes 0, having already stayed
                        		here for one round | see inside auction function*/
                        		keyList.add(key);
                        	}
                        	roundsBlocked.put(key, roundsBlocked.get(key) - 1);
                		}
                		//removes all the keys of the hashtable contained in keyList
                		for (int k = 0; k < keyList.size(); k++) {
                			roundsBlocked.remove(keyList.get(k));
                		}
                	}
                
                	
                	/********************************************************  CASE: THE VERY FIRST TIME  ************************************************************************/
                	if (tries == 0) {
                		for (int ag = 0; ag < numberOfAgents; ag++) {  //for every agent
                			
                			availability[ag] = true;
                			//declares that in the beginning no agent has decided which cell to visit
                    		for (int r = 0; r < x; r++) {
                    			for (int c = 0; c < y; c++) {
                    				agentsXcells[ag][r][c] = false;
                    			}
                    		}
                    		
                    		//initializes heuristics array
                    		for (int col = 0; col < colours.length; col++) {  //for each colour (goal position)
                    			//for each possible position (i,j) on th grid
                        		for (int i = 0; i < x; i++) {
                        			for (int j = 0; j < y; j++) {
                        				heuristics[ag][col][i][j] = Math.abs(colours[col][0]-i) + Math.abs(colours[col][1]-j);  /*calculates the Manhattan distance between
                        				cell (i,j) and the goal position - this is the initial heuristic value*/
                        			}
                        		}
                        	}
                    	}
                	}
                	
                	
                    /********************************************************  CASE: NEW ROUND NEW CLIENTS  **********************************************************************/
                	if (MyAgent.allFalse(MyAgent.isWaiting)) {
                		
                		// CREATES NEW CLIENTS DATA
                		moreClients = true;
                		int currSize = clients.size();
                    	ArrayList<Object> client = new ArrayList<Object>();  //each client looks like e.x: ["C12",0,0,4,0] where start = {0,0} and goal = {4,0}	
                    	
                    	for (int cl = clients.size(); cl < clients.size() + rate; cl++) {  //for each client
                    		MyAgent.isWaiting[cl] = true;  // no client is taken by any agent yet
                    		
                    		//none of these clients is currently being served by any agent
                    		for (int ag = 0; ag < numberOfAgents; ag++) {
                    			agentsXclients[ag][cl] = false;
                    		}
                    		
                    		//define his name, his starting block and his goal block
                    		String clientName = "C"+String.valueOf(cl);
                    		int[] startingBlock = colours[rand.nextInt(4)];
                    		int[] goalBlock = colours[rand.nextInt(4)];
                    		
                    		//creates client's data list (string values)
                    		client.add(clientName);
                    		client.add(startingBlock[0]);
                    		client.add(startingBlock[1]);
                    		client.add(goalBlock[0]);
                    		client.add(goalBlock[1]);
                    	}
                    	clients.addAll(chopped(client, 5));  //creates clients list, which contains all clients' lists
                        
                        if (tries == 0) {
                        	firstTime = true;  //public non-static variable
                        	moreClients = false;
                        }
                        findStartingLocations(currSize);  //changes (and sometimes prints) the state of the grid (positions of the agents & clients)
                        firstTime = false;
                        moreClients = false;
                	}
                	
                	
                	/**********************************************************  MESSAGE: PICK DIRECTION  ************************************************************************/
                    for (String agentName: agentArray) {  //the grid creates a message to "ask" each agent on it to pick a direction
                    	
                    	agentFound = false;
                    	int id = Integer.parseInt(agentName.replaceAll("[\\D]", ""));  /*stores the digit included
                    	in the agentName e.x: id=4 if agentName="Agent-4"*/
                    	String agId = "A"+String.valueOf(id);  //e.x: if i=4 then agIA=A4
                    	
                    	// SAVES THE INITIAL POSITION OF THIS AGENT INTO INITIALPOSITION ARRAY
                    	if (MyAgent.allFalse(agentsXclients[id])) {
                        	for(int r = 0; r < locations.length; r++){
                    			// if the agent's coordiantes are already found
                        		if(agentFound) {
                        			break;
                        		}
                    			for(int c = 0; c < locations[r].length; c++){
                    				if(locations[r][c].contains(agId)) {
                    					initialPosition[id][0] = r;
                    					initialPosition[id][1] = c;
                    					agentFound = true;
                        				break;
                    				}
                    			}
                    		}
                        }
                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        
                        message.addReceiver(new AID(agentName, AID.ISLOCALNAME));  /*some agent is the receiver of the
                        message | reference to receiver by local name*/
                        message.setContent("Pick a Direction");  //asks the question
                        
                        send(message);  //the message now looks like this e.x: {Receiver: "Agent-6", Content: "Pick a Direction"}
                        
                    }
                    
                    
                    /******************************************************  MESSAGE: GREET YOUR FELLOW DRIVER  ******************************************************************/
                    for (int i = 0; i < numberOfAgents; i++) {  //double for-loop to get the combinations of each pair of agents (agi,agj)
                        for (int j = 0; j < numberOfAgents; j++) {
                            
                            if(i==j) continue;  //there is no point in case of (agi,agi), so avoid it
                            
                            String agI = "A"+String.valueOf(i);  //e.x: if i=4 then agIA=A4 (it indicates that the first agent is agent 4)
                            String agJ = "A"+String.valueOf(j);  //e.x: if j=17 then agJ=A17 (it indicates that the second agent is agent 17)
                            
                            int distance = calculateDistance(agI, agJ);  //distance of agi and agj in steps
                            if(distance <= visibility) {  //if close enough
                            	
                            	//the grid creates a message to "inform" the receiver that he is close to another agent
                                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                                
                                message.addReceiver(new AID(agentArray[i], AID.ISLOCALNAME));  /*Some agent is the
                                receiver of the message | reference to receiver by local name*/
                                message.setContent("You can see,"+ j);  //informs the agent
                                
                                send(message);  //the message now looks like this e.x: {Receiver: "Agent-9", Content: "You can see, Agent-5"}
                            }
                        }
                    }
                    
                    
                    /*************************************************************  MESSAGE: MAKE MOVE  **************************************************************************/
                    String[] moveDir = new String[numberOfAgents];
                    
                    //FOR EACH AGENT SAVE HIS MOVE PREFERENCES INTO MOVEDIR STRING ARRAY
                    for (int i = 0; i < numberOfAgents; i++) {  
                      
                    	//waits until message appears in queue (waits for message)
                        ACLMessage msg = null;
                        msg = blockingReceive();  ///using a blocking receive causes the block of all the behaviours 
                        
                        String senderName = msg.getSender().getLocalName();
                        int id = Integer.parseInt(senderName.replaceAll("[\\D]", ""));  /*stores the digit included
                        in the senderName e.x: id=6 if senderName="Agent-6"*/
                       
                        moveDir[id] = msg.getContent();
                        
                        try {
							TimeUnit.SECONDS.sleep(2);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                    
                    //FOR EACH AGENT OF THE GAME: MOVE HIM WHERE HE IS SUPPOSED TO GO
                    for (int k = 0; k < numberOfAgents; k++) {
                    	
                    	if(!DEBUG) {
                    		System.out.println("AGENT-" + k + ": I'm moving " + moveDir[k] + ", with my total reward being " + rewards[k] + "."); 
                    	}
                    	
                    	//CASE: CLIENT CONFLICT EXISTS
                    	if(clientConflictExists(k)) {
                    		System.out.println("GRID: Attention! Conflict of type 'CLIENT' detected!");
                        	moveDir = getAuctionWinner(k, moveDir);
                    	}
                    	else if(!MyAgent.allFalse(agentsXclients[k])) {  // if there is no conflict AND the agent has claimed a client
                    		int clId = -1;
                    		//finds the client he has claimed
                        	for(int cl = 0; cl < clients.size(); cl++) { 
                        		if(agentsXclients[k][cl] == true) {
                        			clId = cl;
                        			break;
                        		}
                        	}
                    		MyAgent.isWaiting[clId] = false;  //the client has decided to get into the winner taxi-agent (he stops waiting for other agents)
                    	}
                    }
                    
                    int times = 0;
                    while(times < 3) {
                    	for (int k = 0; k < numberOfAgents; k++) {
                        	//CASE: CELL CONFILCT EXISTS
                        	if(cellConflictExists(k, moveDir)) {
                        		System.out.println("GRID: Attention! Conflict of type 'CELL' detected!");
                        		if (times > 0) {
                        			System.out.println("AGENT-" + k + ": I'm moving " + moveDir[k] + ", with my total reward being " + rewards[k] + ".");
                        		}
                        		moveDir = getAuctionSequence(k, moveDir);  //the length of sequence array is variable
                        	}
                        }
                    	times = times + 1;
                    }
                    
                    
                    //MAKES THE MOVE
                    for (int k = 0; k < numberOfAgents; k++) {
                    	 //the output looks like this e.x.: >> Agent-6: I'moving left, with my total reward being 14
                        makeMove(k, moveDir[k]);
                        rewards[k] = rewards[k] - 1;  // gives a relatively bad reward to the agent, according to how fast he reaches the client's destination
                    }
                    //PRINTS ALL THE THANKSGIVING WISHES
                    for(String sentence:thanksgiving) {
                    	if(!DEBUG) {
    						System.out.println(sentence);
    					}
                    }
                    thanksgiving.clear();
                    
                    printGrid();
                   
                        
                    /**********************************************************  CASE: SUCCESSFUL TRANFER  ******************************************************************/
                    for (int k = 0; k < numberOfAgents; k++) {
                		if (availability[k] == false) {
                			for (int cl = 0; cl < clients.size(); cl++) {
                				
                				if (agentsXclients[k][cl] == true) {
                					int[] goalPosition = {Integer.parseInt((clients.get(cl).get(3)).toString()), Integer.parseInt((clients.get(cl).get(4)).toString())};
                					
                					String clName = "C"+String.valueOf(cl);
                					if (locations[goalPosition[0]][goalPosition[1]].contains(clName)) {
                						
                    					int clRow = goalPosition[0];
                    					int clCol = goalPosition[1];
                    					
                    					//MAKES ALL THE CHANGES AFTER THE FINAL MOVE THAT LEAD TO THE DESTINATION OF THE CLIENT
                    					locations[clRow][clCol] = locations[clRow][clCol].replace(" " + clName, "");  // removes client from grid
                    					servedClients.add(cl);
                    					availability[k] = true;  // agent k is available again
                    					agentsXclients[k][cl] = false;
                    					
                    					rewards[k] = rewards[k] + 20;
                    					total_clients = total_clients + 1;
                    					
                    					thanksgiving.add("CLIENT-" + cl + ": Thank you A" + k + " for the ride!");
                					}
                					break; 
                				}
                				else {
                					continue;
                				}
                			}
                		}
                    }
                    tries++;
                }
                
                
                /***************************************************************  MESSAGE: GRID TERMINATING  *********************************************************************/
                else{
                    try {Thread.sleep(50);} catch (InterruptedException ie) {}  //waits for the agents to complete their actions
                    
                    for (String agentName: agentArray) {  //for each agent on the grid
                    	
                    	//the grid creates a message to "inform" the receiver that it shuts down
                        ACLMessage messageFinal = new ACLMessage(ACLMessage.INFORM);
                        
                        messageFinal.addReceiver(new AID(agentName, AID.ISLOCALNAME));  /*Some agent is the
                        receiver of the message | reference to receiver by local name*/
                        messageFinal.setContent("End");  //informs the agent
                        
                        send(messageFinal);  //the message now looks like this e.x: {Receiver: "Agent-88", Content: "End"}
                    }
                    
                    //the grid says outloud that it is shut down
                    if(!DEBUG) {
                    	System.out.println("GRID: The number of clients served is: " + total_clients + " | which corresponds to: " + Math.round(total_clients * 100 / clients.size()) + "% of all the active clients &\nto the "+ Math.round(total_clients * 100 / maxTries) + "% of the maximum amount of clients that could be served in " + maxTries + " tries.");
                    	System.out.println("GRID: The agents' total reward is: " + Arrays.stream(rewards).sum() + ".");
                        System.out.println("GRID: terminating...");
                    }
                    doDelete();  //terminates
                }

            }
        });
    }
    
    
    /*********************************************************************  METHOD: PRINT GRID  **********************************************************************************/
    private void printGrid() {
    	
    	if(!moreClients) {  //AVOIDS TO PRINT: when more clients are added on the grid (when no move takes place) | EXCEPTION: the very first time (initial state)
    		//prints the initial state of the grid (environment, agents & clients)
    		System.out.println("\n".replaceAll("\n", "-".repeat(106)));
        	for (int row = 0; row < locations.length; row++){
        		for (int col = 0; col < locations[row].length; col++){
        			System.out.print("|"+centerString(20, locations[row][col]));
        		}
        		System.out.println("|"+"\n"+"-".repeat(106));
        	}
        	if(firstTime) {
                System.out.println("GRID: This was the initial state.");
                System.out.println("*".repeat(106) + "\n\n\n" + "*".repeat(106));
        	}
        	else {
                System.out.println("GRID: This was try number " + tries + ".");
                System.out.println("*".repeat(106) + "\n\n");
        	}
    	}
    }
    

    /*****************************************************************  METHOD: FIND STARTING LOCATIONS  *************************************************************************/
    private void findStartingLocations(int size) {
    	String agAg = null;
    	
		for(int ag = 0; ag < numberOfAgents; ag++) {
			boolean stayHere = false;
			for(int row = 0; row < locations.length; row++){
        		// if the agent is already found
        		if(stayHere) {
        			break;
        		}
        		// otherwise searches for the agent inside the grid
        		for(int col = 0; col < locations[row].length; col++){
                    agAg = "A" + String.valueOf(ag);
        			if(locations[row][col].contains(agAg)) {
        				stayHere = true;
        				break;
        			}
        		}
        	}
			if(!stayHere) {
				
				//define the 4 discrete positions of the grid (possible clients positions): R,Y,B and G
				locations[0][0]="R";
				locations[4][0]="Y";
				locations[4][3]="B";
				locations[0][4]="G";
		    	
				String[] splitArray = agentArray[ag].split("-");  // splitArray looks like e.x: ["Agent", "17"]
				int randRow = -1;
				int randCol = -1;
				
				do {
					randRow = rand.nextInt(5);
				    randCol = rand.nextInt(5);
				} while (locations[randRow][randCol].contains("A"));
				
				if(locations[randRow][randCol].equals("")) {
					locations[randRow][randCol] = "A"+splitArray[1];  // A1 represents the presence of Agent-1 on the grid
				}
				else {
					locations[randRow][randCol] = locations[randRow][randCol] + " " + "A"+splitArray[1];  /* A1 A2 represents the
					presence of Agent-1 & Agent-2 on the same position on the grid */
				}
				agentsXcells[ag][randRow][randCol] = true;  //agent ag is initially at position [randRow, randCol]
			}
		}
		
		for(int cl = size; cl < size + rate; cl++) {
			// client's name and it's starting position
			String name = (clients.get(cl).get(0)).toString();
			int[] pos = {Integer.parseInt((clients.get(cl).get(1)).toString()), Integer.parseInt((clients.get(cl).get(2)).toString())};
	        
	        if(locations[pos[0]][pos[1]].equals("")) {
	        	locations[pos[0]][pos[1]] = name;
			}
			else {
				locations[pos[0]][pos[1]] = locations[pos[0]][pos[1]] + " " + name;
			}
		}
		printGrid();
    }
    
    
    /************************************************************************  METHOD: MAKE MOVE  ********************************************************************************/
	private void makeMove(int id, String content) {
		
		String agId = null;  //e.x: A12 is the name of the agent
		String clId = null;  //e.x: C5 is the name of the client
		agentFound = false;
		int rId,cId;  // current rows and columns & new rows and columns (after the move)
		rId=cId=0;
		boolean possible = false;
		String type = "agent";
		
    	for(int row = 0; row < locations.length; row++){
    		
    		// if the agent's coordiantes are already found
    		if(agentFound) {
    			break;
    		}
    		// otherwise finds the block where agent Aid stands on, e.x: agent A14 stands on {row,column} = {2,3}
    		for(int col = 0; col < locations[row].length; col++){
                agId = "A"+String.valueOf(id);
    			if(locations[row][col].contains(agId)) {
    				rId=row;
    				cId=col;
    				agentFound = true;
    				break;
    			}
    		}
    	}
    	// current and next position coordinates
    	int [] currentPosition = {rId,cId};
    	int [] nextPosition = directionsToCoordinates(content, rId, cId);  // calculates the new rows & columns according to the move type (left, right, up and down)
    	
    	possible = changePositions(type, currentPosition, nextPosition, agId, id);
    	
    	if(!possible) {  //if the movement is not possible
			System.out.println("GRID: A" + id + ", you are not allowed to make this move. Check for obstructions!");
			rewards[id] = rewards[id] - 100;  // gives a bad reward to the agent for bumping into the wall
			
    		nextPosition[0] = rId;
    		nextPosition[1] = cId;
    		
    		//AGENT LEARNS WHICH (POSITION, ACTION) LED HIM TO BUMP INTO A WALL
    		ArrayList<Object> forbiddenMove = new ArrayList<Object>();  //each forbidden move looks like e.x: ["left",4,0,4,1,3]
    		forbiddenMove.add(content);
    		forbiddenMove.add(rId);
    		forbiddenMove.add(cId);
    		forbiddenMove.add(id);
    		forbiddenMoves.add(forbiddenMove);
    		
    	}
    	
    	//CHECKS IF AGENT IS CARRYING HIS CLIENT WITH HIM AND IF SO, MOVES THE CLIENT THE SAME WAY THE AGENT MOVES
		if (availability[id] == false && possible) {
			for (int cl = 0; cl < clients.size(); cl++) {
				
				if (agentsXclients[id][cl] == true) {
					type = "client";
					clId = "C"+String.valueOf(cl);
					possible = changePositions(type, currentPosition, nextPosition, clId, id);
					break;
				}
			}
		}
		
		// IS THE AGENT AVAILABLE AFTER THE MOVE?
		for (int cl = 0; cl < clients.size(); cl++) {
			if ((agentsXclients[id][cl] == true) && (locations[nextPosition[0]][nextPosition[1]].contains("C" + String.valueOf(cl)))) {  //if his client lies on the same position as the agent does
				availability[id] = false;  //since the agent and his client are on the same position, he has to carry him with him
				break;
			}
			else {
				availability[id] = true;  // the agent is still available
				continue;
			}
		}
	}

   
	/*******************************************************************  METHOD: CALCULATE DISTANCE  ****************************************************************************/
	private int calculateDistance(String agA, String agB) {
		int rA,cA,rB,cB;
		rA=cA=rB=cB=0;
	    
        //finds the position of agent a on the grid: (rA, cA)
    	for (int row = 0; row < locations.length; row++){
    		for (int col = 0; col < locations[row].length; col++){
    			if(locations[row][col].contains(agA)) {
    				rA=row;
    				cA=col;
    			}
    			//finds the position of agent b on the grid: (rB, cB)
    			else if(locations[row][col].contains(agB)) {
    				rB=row;
    				cB=col;
    			}
    		}
    	}
    	
		int dist = Math.abs(rA-rB)+Math.abs(cA-cB);  // calculates the Manhattan distance between agent a & agent b
		
		return dist;   //converts string into double
	}
	

	/*********************************************************************  METHOD: CENTER STRING  *******************************************************************************/
    public static String centerString (int width, String s) {
        return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
    }
    
    
    /***************************************************************  METHOD: DIRECTIONS TO COORDINATES  *************************************************************************/
    public static int[] directionsToCoordinates(String dir, int row, int col) {  // calculates the new rows & columns according to the move type (left, right, up and down)
		int[] coordinates = new int[2];
		
    	if(dir.equals("left")) {
    		coordinates[0] = row;
    		coordinates[1] = col-1;
    	}
    	else if(dir.equals("right")) {
    		coordinates[0] = row;
    		coordinates[1] = col+1;  		
    	}
        else if(dir.equals("up")) {
        	coordinates[0] = row-1;
        	coordinates[1] = col;  		
    	}
        else if(dir.equals("down")) {
        	coordinates[0] = row+1;
        	coordinates[1] = col;	
        }
        else if(dir.equals("here")) {
        	coordinates[0] = row;
        	coordinates[1] = col;	
        }
    	
		return coordinates;
	}
    
   
    /***************************************************************  METHOD: COORDINATES TO DIRECTIONS  *************************************************************************/
    public static String coordinatesToDirections(int[] curr, int[] next) {  // returns the direction according to the current position & the next position
		
    	if(curr[1] < next[1]) {
    		return "right";
    	}
    	else if(curr[1] > next[1]) {
    		return "left";
    	}
        else if(curr[0] < next[0]) {
    		return "down";
    	}
        else if(curr[0] > next[0]) {
    		return "up";
    	}
		return "here";  // if none of the above is true, then curr == next
	}
    
    
    /**************************************************************************  METHOD: CHOPPED  ********************************************************************************/
    static <T> ArrayList<ArrayList<T>> chopped(ArrayList<T> list, final int L) {  // chops an ArrayList into non-view sublists of length L
    	ArrayList<ArrayList<T>> parts = new ArrayList<ArrayList<T>>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<T>(
                list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }
    
    
    /*********************************************************************  METHOD: CHANGE POSITIONS  ****************************************************************************/
    public boolean changePositions (String t, int [] currentPos, int [] nextPos, String name, int agId) {
    	
    	int rId = currentPos[0];
    	int cId = currentPos[1];
    	int new_rId = nextPos[0];
    	int new_cId = nextPos[1];
    	
    	// if not allowed to move from current position to next position, keep the current position
    	if (!isAllowed(currentPos, nextPos)) {
			wasSuccessful=false;
			
			if(t.equals("agent")) {  //if this method is called for some AGENT'S move
				heuristicUpdate(currentPos, nextPos, agId);
			}
		}
    	
    	else {
    		// REMOVES AGENT FROM HIS OLD POSITION ON THE GRID
    		if(locations[rId][cId].contains(" " + name)) {  // agent is after another element on the cell
    			locations[rId][cId] = locations[rId][cId].replace(" " + name, "");
    		}
    		else if(locations[rId][cId].contains(name + " ")) {  // agent is the first element before another element on the cell
    			locations[rId][cId] = locations[rId][cId].replace(name + " ", "");
    		}
    		else {
    			locations[rId][cId] = locations[rId][cId].replace(name, "");  // agent is alone on the cell
    		}
    		// ADDS AGENT ON HIS NEW POSITION OF THE GRID
    		if(locations[new_rId][new_cId].equals("")) {
    			locations[new_rId][new_cId] = name;
			}
			else {
				locations[new_rId][new_cId] = locations[new_rId][new_cId] + " " + name;
			}

    		wasSuccessful=true;
    	}
    	return wasSuccessful;
    }
    
    
    /**********************************************************************  METHOD: IS ALLOWED  *********************************************************************************/
    public static boolean isAllowed(int [] current_pos, int [] new_pos) {  // checks if the agent can make the move
		
    	boolean isObstructed = false;
    	int leftSideWall [][]= {{4,0},{3,0},{0,1},{1,1},{4,2},{3,2}};  /* blocks on the left side of each wall - formating
    	coordinates as {X,Y} = {row,column} */
    	
    	// walls logic - is this block accessible from my current position?
    	for (int i = 0; i < leftSideWall.length; i++) {  // for each left block of each wall
    		
    		// store its own coordinates & the coordinates (leftBlock) of the block behind this wall (rightBlock)
			int[] leftBlock = {leftSideWall[i][0], leftSideWall[i][1]};
			int[] rightBlock = {leftBlock[0], leftBlock[1] + 1};
			
			// if the agent wants to move from his current block, to the one behind the wall
			if ((Arrays.equals(current_pos, leftBlock) && Arrays.equals(new_pos, rightBlock)) || (Arrays.equals(current_pos, rightBlock) && Arrays.equals(new_pos, leftBlock))) {
				isObstructed = true;
			}
		}
    	
    	// boundaries logic - does the block I want to move to even exist on the grid?
    	if (new_pos[0]>4 || new_pos[1]>4 || new_pos[0]<0 || new_pos[1]<0) {
    		isObstructed = true;
		}
    	
    	return !isObstructed;  // e.x: !isObstracted being true = the move is allowed
	}
    
    
    /******************************************************************  METHOD: HEURISTIC UPDATE  *******************************************************************************/
    private void heuristicUpdate(int [] current, int [] next, int id) {  // checks if the agent can make the move
    	int stability = 0;
    	int dirId = 0;
    	int[] thisPos = new int[2];
    	int[] adjacentPos = new int[2];
    	int[] neighbors = new int[4]; 
    	
    	while (stability < x*y) {
			
			// PAIR NO.1: basic cell = (i1,j1)
			for (int i1 = 0; i1 < x; i1++) {
				for (int j1 = 0; j1 < y; j1++) {
					
					thisPos[0] = i1;
					thisPos[1] = j1;
					
					for (int n = 0; n < 4; n++) {  //-999999 means that this neighbors list is empty for the next cell of the grid to fill it
						neighbors[n] = -999999;
					}
					
					//CASE: index out of bounds when nextPos = out of bounds | gets a very big heuristic value
					if (i1 == current[0] && j1 == current[1] && (next[0] > 4 || next[0] < 0 || next[1] > 4 || next[1] < 0)) { 
						dirId = Arrays.asList(MyAgent.directions).indexOf(coordinatesToDirections(current, next));
						neighbors[dirId] = 999999;
					}
					
					// PAIR NO.2: neighbor cell = (i2,j2) | where x,y are the coordinates of every possible neighbor
					for (int i2 = 0; i2 < x; i2++) {
						for (int j2 = 0; j2 < y; j2++) {
							
							adjacentPos[0] = i2;
							adjacentPos[1] = j2;
							String drct = coordinatesToDirections(thisPos, adjacentPos);
							int dst = Math.abs(i1-i2)+Math.abs(j1-j2);  // calculates the Manhattan distance between points (i1,j1) & (i2,j2)
							
							if (i1 == current[0] && j1 == current[1] && i2 == next[0] && j2 == next[1]) { /* the cell behind the wall is no
								more optimal for the agent */
								dirId = Arrays.asList(MyAgent.directions).indexOf(coordinatesToDirections(current, next));
								neighbors[dirId] = 999999;
							}
							else if (dst == 1) {
								dirId = Arrays.asList(MyAgent.directions).indexOf(coordinatesToDirections(thisPos, adjacentPos)); 
								if (!AStar.isForbidden(id, drct, thisPos)) {
									neighbors[dirId] = heuristics[id][AStar.getArrayIndex(colours, MyAgent.endBlock[id])][i2][j2];
								}
								else {  //any forbidden cell is no more optimal for the agent
									neighbors[dirId] = 999999;
								}
							}
							else {  //ignores current cell and any non-neighbor cell
								continue;
							}
						}
					}
					
					for (int n = 0; n < 4; n++) {  //CASE: index out of bounds when adjacentPos =! nextPos | gets heuristic value of this position plus ONE
						if (neighbors[n] == -999999) {
							neighbors[n] = heuristics[id][AStar.getArrayIndex(colours, MyAgent.endBlock[id])][thisPos[0]][thisPos[1]] + 1;
						}
					}
					
					// changes the heuristic value of cell (currentPos[0], currentPos[1]) due to his bumping into the wall
					int nearestNeighborValue = AStar.getMin(neighbors);
					int oldH = heuristics[id][AStar.getArrayIndex(colours, MyAgent.endBlock[id])][thisPos[0]][thisPos[1]];  //the old heuristic value
					
					if (!(MyAgent.endBlock[id][0] == thisPos[0] && MyAgent.endBlock[id][1] == thisPos[1])) {
						heuristics[id][AStar.getArrayIndex(colours, MyAgent.endBlock[id])][thisPos[0]][thisPos[1]] = nearestNeighborValue + 1;
					}
					int newH = heuristics[id][AStar.getArrayIndex(colours, MyAgent.endBlock[id])][thisPos[0]][thisPos[1]]; //the new heuristic value
					
					//in case there is no actual change | when stability = x*y the heuristic array update is over
					if (oldH == newH) {
						stability = stability + 1;
					}
					else {  // in case the heuristic value is changed
						stability = 0;
					}
				}
			}
		}
	}
    
    
    /**************************************************************  METHOD: CLIENT CONFLICT EXISTS  **************************************************************************/
    private boolean clientConflictExists(int agId) {
    	boolean multiple = false;  //if true, it indicates the existence of multiple agents, who desire to offer their services to some client
    	int clId = -1;
    	
    	//finds the client he wants to serve (the one that is responsible for the conflict)
    	for(int cl = 0; cl < clients.size(); cl++) {
    		if(agentsXclients[agId][cl]) {
    			clId = cl;
    			break;
    		}
    	}
    	//returns false if there is no other client waiting (if it's impossible for agentsXclients[agId] to contain any true values)
    	if(clId == -1) {
    		return multiple;
    	}
    	//finds how many other agents claim client clId
    	List<Integer> list = new ArrayList<Integer>();  //list looks like e.x.: [1,3,5,6] where 1,3,5,6 refer to agents 1,3,5 and 6
		for(int ag = 0; ag < numberOfAgents; ag++) {
			if(agentsXclients[ag][clId]) {
				list.add(ag);
			}
		}
		if(list.size() > 1) {  //more than 1 agent claims client cl
			multiple = true;
		}
		return multiple;  //returns true if multiple == true (meaning that multiple agents claim some client/s)
    }
    
    
    /****************************************************************  METHOD: CELL CONFLICT EXISTS  **************************************************************************/
    private boolean cellConflictExists(int agId, String[] move) {
    	boolean multiple = false;  //if true, it indicates the existence of multiple agents, who desire to visit altogether some cell of the grid
    	
    	//finds the cell he wants to visit (the one that is responsible for the conflict)
    	int[] cell = directionsToCoordinates(move[agId], MyAgent.currBlock[agId][0], MyAgent.currBlock[agId][1]);
    	
    	//finds how many other agents claim this cell
    	List<Integer> list = new ArrayList<Integer>();  //list looks like e.x.: [1,3,5,6] where 1,3,5,6 refer to agents 1,3,5 and 6
		for(int ag = 0; ag < numberOfAgents; ag++) {
			if(cell[0] < 5 && cell[0] > -1 && cell[1] < 5 && cell[1] > -1 && agentsXcells[ag][cell[0]][cell[1]]) {  /*if cell is not out of bounds
				(because there can't be any conflict when out of bounds) AND if this agent ag want to visit it as well*/
				list.add(ag);
			}
		}
		if(list.size() > 1) {  //more than 1 agent claims client cl
			multiple = true;
		}
    	return multiple;  //returns true if multiple == true (meaning that multiple agents claim some client/s)
    }
    
    
    /****************************************************************  METHOD: GET AUCTION WINNER  **************************************************************************/
    private String[] getAuctionWinner(int agId, String[] move) {
    	int[] data = new int[numberOfAgents];
    	String[] move_draft = new String[numberOfAgents];
    	int distFirst, distLast;
    	int clId = -1;
    	
    	//finds the client he wants to serve (the one that is responsible for the conflict)
    	for(int cl = 0; cl < clients.size(); cl++) { 
    		if(agentsXclients[agId][cl] == true) {
    			clId = cl;
    			break;
    		}
    	}
    	for (int ag = 0; ag < numberOfAgents; ag++) {
    		data[ag] = 9999;  //this changes only when the if statement below, is true!
    		
    		if(agentsXclients[ag][clId] == true) {
    			
    			move_draft[ag] = move[ag];
    			MyAgent.reverse_update_agentsXcells(ag, move_draft[ag]);
    			move[ag] = "here";
    			agentsXclients[ag][clId] = false;
    			
    			
    			int[] first = {Integer.parseInt((Grid.clients.get(clId).get(1)).toString()), Integer.parseInt((Grid.clients.get(clId).get(2)).toString())};  /* the initial
    			position of client clId*/
    			int[] last = {Integer.parseInt((Grid.clients.get(clId).get(3)).toString()), Integer.parseInt((Grid.clients.get(clId).get(4)).toString())};  /* the goal
    			position of client clId*/
    			
    			//distFirst = distance between currentPos of the agent and initialPos of the client && distLast = distance between currentPos of the agent and goalPos of the client
    			distFirst = heuristics[ag][AStar.getArrayIndex(colours, first)][MyAgent.currBlock[ag][0]][MyAgent.currBlock[ag][1]];
    			distLast = heuristics[ag][AStar.getArrayIndex(colours, last)][MyAgent.currBlock[ag][0]][MyAgent.currBlock[ag][1]];
    			data[ag] = 2*distFirst + 1*distLast;
    		}
    	}
    	int winner = AStar.getElementIndex(data, AStar.getMin(data));
    	move[winner] = move_draft[winner];
    	agentsXclients[winner][clId] = true;
    	MyAgent.isWaiting[clId] = false;  //the client has decided to get into the winner taxi-agent (he stops waiting for other agents)
    	MyAgent.update_agentsXcells(winner, move[winner]);
    	
    	if(!DEBUG) {
    		System.out.println("GRID: The winner of this auction is A" + winner + ". Congrats, you claimed C" + clId +"!"); 
    	}
    	return move;
    }
    
    
    /****************************************************************  METHOD: GET AUCTION SEQUENCE  **************************************************************************/
    private String[] getAuctionSequence(int agId, String[] move) {
    	int[] data = new int[numberOfAgents];
    	String[] move_draft = new String[numberOfAgents];
    	int distInit, distGoal, carrying, here, solver = 0;
    	String returns = null;
    	List<Integer> ids = new ArrayList<Integer>();
    	
    	//finds the cell he wants to visit (the one that is responsible for the conflict)
    	int[] cell = directionsToCoordinates(move[agId], MyAgent.currBlock[agId][0], MyAgent.currBlock[agId][1]);
    	
    	for (int ag = 0; ag < numberOfAgents; ag++) {
    		data[ag] = 999999;  //this changes only when the if statement below, is true!
    		String agName = "A"+String.valueOf(ag);  //e.x: if i=4 then agIA=A4
    		
    		if(agentsXcells[ag][cell[0]][cell[1]]) {
    			
    			//CRITERION #1 - IS THE AGENT ALREADY LYING IN THIS POSITION?
    			if(locations[cell[0]][cell[1]].contains(agName)) {
    				here = -999999;
    			}
    			else {
    				here = 0;
    			}
    			
    			//CRITERION #2 - IS THE AGENT FREE?
    			if(MyAgent.allFalse(agentsXclients[ag])) {  //if this agent does not carry any client with him
    				carrying = 1;
    			}
    			else {
    				carrying = 0;
    			}
    		
    			//CRITERION #3 - HOW FAR IS THE AGENT'S INITIAL POSITION? 
    			int[] init = initialPosition[ag];
    			int[] goal = MyAgent.endBlock[ag];
    			distInit = Math.abs(heuristics[ag][AStar.getArrayIndex(colours, goal)][init[0]][init[1]] - heuristics[ag][AStar.getArrayIndex(colours, goal)][MyAgent.currBlock[ag][0]][MyAgent.currBlock[ag][1]]);
    			
    			//CRITERION #4 - HOW CLOSE IS THE AGENT TO HIS GOAL POSITION?
    			distGoal = heuristics[ag][AStar.getArrayIndex(colours, goal)][MyAgent.currBlock[ag][0]][MyAgent.currBlock[ag][1]];
    			
    			//CRITERION #5 - CAN THE AGENT SOLVE THE CONFLICT?
   			    for(int other = 0; other < numberOfAgents; other++) {
   			    	
   			    	int[] other_cell = directionsToCoordinates(move[other], MyAgent.currBlock[other][0], MyAgent.currBlock[other][1]);
   			    	
   			    	if(here == 0 && other != ag && MyAgent.currBlock[ag][0] == other_cell[0] && MyAgent.currBlock[ag][1] == other_cell[1] && isAllowed(MyAgent.currBlock[other], other_cell)) {
   			    		solver = -9999;
   			    		break;
   			    	}
   			    	else {
   	    				solver = 0;
   	    			}
   			    }
    			
    			data[ag] = 1*solver + 1*here + 200*carrying - 15*distInit + 5*distGoal;
    			move_draft[ag] = move[ag];
    			MyAgent.reverse_update_agentsXcells(ag, move_draft[ag]);
    			move[ag] = "here";
    		}
    	}
    	//MINIMIZATION POLICY | SEQUENCE ANNOUNCEMENT
    	returns = "id";
    	int [] sortedIds = sort(data, returns);
    	returns = "data";
    	int [] sortedData = sort(data, returns);
    	
    	int winner = sortedIds[0];
    	move[winner] = move_draft[winner];
    	ids.add(winner);  //only for printing purposes
    	MyAgent.update_agentsXcells(winner, move[winner]);
    	
    	if(!roundsBlocked.containsKey(winner)) {
    		roundsBlocked.put(winner, 0);
    	}
    	for (int i = 1; i < sortedIds.length; i++) {
    		if(sortedData[i] != 999999) {
    			roundsBlocked.put(sortedIds[i], roundsBlocked.get(winner) + i);
    			ids.add(sortedIds[i]);  //only for printing purposes
    		}
    	}
    	if(!DEBUG) {
    		System.out.println("GRID: The auction sequence is: " + ids.toString() + " so A" + winner + ", you can visit cell: " + Arrays.toString(cell) + " first!"); 
    	}
    	return move;
    }
    
    
    public static int[] sort (int[] arr, String type) {
    	int tempValue = 0;
    	int tempIndex = 0;
    	int[] indexes = new int[arr.length];
    	
    	//creates the initial indexes array
    	for (int id = 0; id < arr.length; id++) {
    		indexes[id] = id;
    	}
    	
    	//sorts the arr & indexes arrays in ascending order using two for loops    
        for (int i = 0; i < arr.length; i++) {     
          for (int j = i+1; j < arr.length; j++) {     
              if(arr[i] > arr[j]) {  
            	 //swap values if not in order 
            	 tempValue = arr[i];   
                 arr[i] = arr[j];    
                 arr[j] = tempValue;
                 //swap indexes if not in order
                 tempIndex = indexes[i];
                 indexes[i] = indexes[j];    
                 indexes[j] = tempIndex;
               }     
            }     
        }
        if (type.equals("id")) {
        	return indexes;
        }
        else {
        	return arr;
        }
    }
}
