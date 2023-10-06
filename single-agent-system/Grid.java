package single-agent-system;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class Grid extends Agent {
	
	private static final long serialVersionUID = 3L; /*SerialVersionUID is used to ensure that during deserialization
	the same class that was used during serialize process is loaded. Serialization is a mechanism of converting the state
	of an object into a byte stream & deserialization is the reverse process*/
	
    private int tries=0;
    private int maxTries=60;  //number of actions in an episode | each agent makes a total of maxTries actions
    private String[] agentArray;
    public static int numberOfAgents = 1;
    public static int x = 5;
    public static int y = 5;
    public static String[][] locations = new String[x][y];
    //public static int[][] utility = new int[x+2][y+2];  //stores the utility of each cell of the grid (learns how good or how bad it is) TODO: PLEASE IGNORE FOR NOW
    public static ArrayList<ArrayList<Object>> clients = new ArrayList<ArrayList<Object>>();  //list of all the clients data
    private int visibility = 3;  //how far it is possible for an agent to detect another agent
    public static int rate = 2;  // number of clients for each round
    private int[][] colours = {{0,0},{4,0},{4,3},{0,4}};
    public static int[][] initialPosition = new int[numberOfAgents][2];  // an array with the agents' initial coordinates (x,y) on the grid
    public static int leftovers = 0;  // we add clients on the grid when leftovers is zero, so we start by setting leftovers to zero
    private boolean agentFound = false;
    public static boolean[] availability = new boolean[numberOfAgents]; // defines which agent is available (doesn't carry any client)
    public static boolean[][] agentsXclients = new boolean[numberOfAgents][rate];  // links each agent with the client he decided to serve
    public static boolean wasSuccessful;  // boolean variable to define whether the move took place
    public static ArrayList<Integer> servedClients = new ArrayList<Integer>();  //contains the  ids' of the served clients
    public static ArrayList<ArrayList<Object>> forbiddenMoves = new ArrayList<ArrayList<Object>>();  //list of all forbidden moves
    public static int[] rewards = new int[numberOfAgents];
    private boolean newRound;

    Random rand = new Random();  // creates instance of Random class
    
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
      
        System.out.print("Grid: I found these agents on me > ");
        for (int i = 0; i < numberOfAgents; ++i) System.out.println(agentArray[i] + "  ");
        System.out.println("Grid: The total number of them is > "+ numberOfAgents);
        
      //System.exit(2); //IDK WHAT THAT IS YET :)

        //runs until the grid shuts down | loops for maxTries times (tries=0,1,2,3,4)
        addBehaviour(new CyclicBehaviour(this) {
        	
        	private static final long serialVersionUID = 4L;
        	
            public void action(){

                try {Thread.sleep(50);} catch (InterruptedException ie) {}  //waits for the agents to get ready
                
                //print();  //IDK WHAT THAT IS YET :)
                newRound = false;
                if(tries<maxTries) {
                	if (leftovers == 0) {
                		newRound = true;
                		// CREATES NEW CLIENTS DATA
                		clients.clear();
                		servedClients.clear();
                		leftovers = rate;  //there are rate new clients on the grid e.x: if rate = 2, then we add 2 more clients
                    	ArrayList<Object> client = new ArrayList<Object>();  //each client looks like e.x: ["C12",0,0,4,0] where start = {0,0} and goal = {4,0}
                    	int client_counter = -1;  //first client is named C0 (and not C1)
                    	
                    	for (int cl = 0; cl < rate; cl++) {  //for each client
                    		client_counter = client_counter + 1;
                    		
                    		//declare that in the beginning every agent is available, meaning that no client is currently being served by any agent
                    		for (int ag = 0; ag < numberOfAgents; ag++) {
                    			availability[ag] = true;
                    			agentsXclients[ag][cl] = false;
                    		}
                    		
                    		//define his name, his starting block and his goal block
                    		String clientName = "C"+String.valueOf(client_counter);
                    		int[] startingBlock = colours[rand.nextInt(4)];
                    		int[] goalBlock = colours[rand.nextInt(4)];
                    		
                    		//creates client's data list (string values)
                    		client.add(clientName);
                    		client.add(startingBlock[0]);
                    		client.add(startingBlock[1]);
                    		client.add(goalBlock[0]);
                    		client.add(goalBlock[1]);
                    	}
                    	
                    clients = chopped(client, 5);  //creates clients list, which contains all clients' lists
            		System.out.println("Grid: These are the clients you need to serve > " + Grid.clients);
                    System.out.println("Grid: Initial state of the environment ...");
                    findStartingLocations(locations, clients);  //prints the initial state of the grid (initial positions of the agents & clients)
                	}
                	
                	//the grid creates a message to "ask" each agent on it to pick a direction
                    for (String agentName: agentArray) {  //for each agent on the grid
                    	
                    	agentFound = false;
                    	int id = Integer.parseInt(agentName.replaceAll("[\\D]", ""));  /*stores the digit included
                    	in the agentName e.x: id=4 if agentName="Agent-4"*/
                    	String agId = "A"+String.valueOf(id);  //e.x: if i=4 then agIA=A4
                    	
                    	if (newRound == true) {
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
                        System.out.println("Agent-" + id + ": This is try number > " + tries);
                        message.setContent("Pick a Direction");  //asks the question
                        
                        send(message);  //the message now looks like this e.x: {Receiver: "Agent-6", Content: "Pick a Direction"}
                        
                    }
                    
                    //the grid says outloud that some agent just moved in a particular direction
                    for (int i = 0; i < numberOfAgents; i++) {
                      
                    	agentFound = false;
                    	
                    	//waits until message appears in queue (waits for message)
                        ACLMessage msg = null;
                        msg = blockingReceive();  ///using a blocking receive causes the block of all the behaviours 
                        
                        String senderName = msg.getSender().getLocalName();
                        
                        int id = Integer.parseInt(senderName.replaceAll("[\\D]", ""));  /*stores the digit included
                        in the senderName e.x: id=6 if senderName="Agent-6"*/
                        
                        if (msg.getContent().equals("no need to")) {
                        	System.out.println(senderName + ": There's no need to. My client was already at his destination :/");
                        }
                        else {
                        	System.out.println(senderName + ": I'm moving " + msg.getContent());
                            //the output looks like this e.x.: >> Agent-6 moving: up
                        	makeMove(id, msg.getContent(), locations);
                        }
                        
                        rewards[id] = rewards[id] - 1;  // gives a relatively bad reward to the agent, according to how fast he reaches the client's destination
                       
                        //PRINTS THE GRID
                        try {
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                        printGrid(locations);
                        
                        // IF THIS AGENT CARRIED HIS CLIENT TO HIS GOAL DESTINATION SUCCESSFULLY
                		if (availability[id] == false) {
                			for (int cl = 0; cl < rate; cl++) {
                				
                				if (agentsXclients[id][cl] == true) {
                					int[] goalPosition = {Integer.parseInt((clients.get(cl).get(3)).toString()), Integer.parseInt((clients.get(cl).get(4)).toString())};
                					
                					String clId = "C"+String.valueOf(cl);
                					if (locations[goalPosition[0]][goalPosition[1]].contains(clId)) {
                						
                    					int clRow = goalPosition[0];
                    					int clCol = goalPosition[1];
                    					String clName = "C"+String.valueOf(cl);
                    					
                    					//MAKES ALL THE CHANGES AFTER THE FINAL MOVE THAT LEAD TO THE DESTINATION OF THE CLIENT
                    					locations[clRow][clCol] = locations[clRow][clCol].replace(" " + clName, "");  // removes client from grid
                    					servedClients.add(cl);
                    					leftovers = leftovers - 1;
                    					agentsXclients[id][cl] = false;
                    					rewards[id] = rewards[id] + 20;
                    					//utility[clRow + 1][clCol + 1] = 0; TODO: PLEASE IGNORE FOR NOW
                    					
                    					// if some other client is on this position, take him to his destination :)
                    					for (int othercl = 0; othercl < leftovers; othercl++) {
                    						if (locations[clRow][clCol].contains("C" + String.valueOf(othercl))) {
                    							availability[id] = false;
                    							agentsXclients[id][othercl] = true;
                    							break;
                    						}
                    						else {
                    							availability[id] = true;
                    						}
                    					}
                    					printGrid(locations);
                					}
                					break; 
                				}
                				else {
                					continue;
                				}
                			}
                		}
                		System.out.println(senderName + ": My total reward is > " + rewards[id]);
                    }
                    
                    //double for-loop to get the combinations of each pair of agents (agi,agj)
                    for (int i = 0; i < numberOfAgents; i++) {
                        for (int j = 0; j < numberOfAgents; j++) {
                            
                            if(i==j) continue;  //there is no point in case of (agi,agi), so avoid it
                            
                            String agI = "A"+String.valueOf(i);  //e.x: if i=4 then agIA=A4 (it indicates that the first agent is agent 4)
                            String agJ = "A"+String.valueOf(j);  //e.x: if j=17 then agJ=A17 (it indicates that the second agent is agent 17)
                            
                            int distance = calculateDistance(agI, agJ, locations);  //distance of agi and agj in steps
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
                    tries++;
                }
                
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
                    System.out.println(getLocalName()+" terminating");
                    doDelete();  //terminates
//                    System.exit(0);
                }

            }
        });
    }
    
    
    private void printGrid(String[][] gr) {
    	
		//prints the initial state of the grid (environment, agents & clients)
		System.out.println("\n"+"\n".replaceAll("\n", "-".repeat(66)));
    	for (int row = 0; row < gr.length; row++){
    		for (int col = 0; col < gr[row].length; col++){
    			System.out.print("|"+centerString(12, gr[row][col]));
    		}
    		System.out.println("|"+"\n"+"-".repeat(66));
    	}
    }
    
    
    private void findStartingLocations(String[][] grid, ArrayList<ArrayList<Object>> tasks) {
    	
    	String agAg = null;
    	
    	
		for(int ag = 0; ag < numberOfAgents; ag++) {
			boolean stayHere = false;
			
			for(int row = 0; row < grid.length; row++){
        		// if the agent is already found
        		if(stayHere) {
        			break;
        		}
        		// otherwise searches for the agent inside the grid
        		for(int col = 0; col < grid[row].length; col++){
                    agAg = "A" + String.valueOf(ag);
        			if(grid[row][col].contains(agAg)) {
        				stayHere = true;
        				break;
        			}
        		}
        	}
			if(!stayHere) {
				
				//define the 4 discrete positions of the grid (possible clients positions): R,Y,B and G
		    	grid[0][0]="R";
		    	grid[4][0]="Y";
		    	grid[4][3]="B";
		    	grid[0][4]="G";
		    	
				String[] splitArray = agentArray[ag].split("-");  // splitArray looks like e.x: ["Agent", "17"]
				int randRow = rand.nextInt(5);
				int randCol = rand.nextInt(5);
				
				if(grid[randRow][randCol].equals("")) {
					grid[randRow][randCol] = "A"+splitArray[1];  // A1 represents the presence of Agent-1 on the grid
				}
				else {
					grid[randRow][randCol] = grid[randRow][randCol] + " " + "A"+splitArray[1];  /* A1 A2 represents the
					presence of Agent-1 & Agent-2 on the same position on the grid */
				}
			}
		}
		
		for(int cl = 0; cl < tasks.size(); cl++) {
			// client's name and it's starting position
			String name = (clients.get(cl).get(0)).toString();
			int[] pos = {Integer.parseInt((clients.get(cl).get(1)).toString()), Integer.parseInt((clients.get(cl).get(2)).toString())};
	        
	        if(grid[pos[0]][pos[1]].equals("")) {
	        	grid[pos[0]][pos[1]] = name;
			}
			else {
				grid[pos[0]][pos[1]] = grid[pos[0]][pos[1]] + " " + name;
			}
		}
		
		printGrid(grid);
    }
    
    
	private void makeMove(int id, String content, String[][] grid) {
		
		String agId = null;  //e.x: A12 is the name of the agent
		String clId = null;  //e.x: C5 is the name of the client
		agentFound = false;
		int rId,cId;  // current rows and columns & new rows and columns (after the move)
		rId=cId=0;
		boolean possible = false;
		
    	for(int row = 0; row < grid.length; row++){
    		
    		// if the agent's coordiantes are already found
    		if(agentFound) {
    			break;
    		}
    		// otherwise finds the block where agent Aid stands on, e.x: agent A14 stands on {row,column} = {2,3}
    		for(int col = 0; col < grid[row].length; col++){
                agId = "A"+String.valueOf(id);
    			if(grid[row][col].contains(agId)) {
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
    	
    	possible = changePositions(currentPosition, nextPosition, agId, grid);
    	
    	if(!possible) {  //if the movement is not possible
			System.err.println("Grid: Agent-" + id + ", you are not allowed to make this move. Check for obstructions!");
			rewards[id] = rewards[id] - 100;  // gives a bad reward to the agent for bumping into the wall
			//utility[rId + 1][cId + 1] = utility[rId + 1][cId + 1] + 10;  TODO: PLEASE IGNORE FOR NOW
			
    		nextPosition[0] = rId;
    		nextPosition[1] = cId;
    		
    		//AGENT LEARNS WHICH (POSITION, ACTION) LED HIM TO BUMP INTO A WALL
    		ArrayList<Object> forbiddenMove = new ArrayList<Object>();  //each forbidden move looks like e.x: ["left",4,0,4,1]
    		forbiddenMove.add(content);
    		forbiddenMove.add(rId);
    		forbiddenMove.add(cId);
    		forbiddenMoves.add(forbiddenMove);
    	}
    	/*else {
    		utility[rId + 1][cId + 1] = utility[rId + 1][cId + 1] + 3; 
    	} TODO: PLEASE IGNORE FOR NOW*/
    	
    	//CHECKS IF AGENT IS CARRYING HIS CLIENT WITH HIM AND IF SO, MOVES THE CLIENT THE SAME WAY THE AGENT MOVES
		if (availability[id] == false && possible) {
			for (int cl = 0; cl < clients.size(); cl++) {
				
				if (agentsXclients[id][cl] == true) {
					clId = "C"+String.valueOf(cl);
					possible = changePositions(currentPosition, nextPosition, clId, grid);
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

   
	private int calculateDistance(String agA, String agB, String[][] grid) {
		int rA,cA,rB,cB;
		rA=cA=rB=cB=0;
	    
        //finds the position of agent a on the grid: (rA, cA)
    	for (int row = 0; row < grid.length; row++){
    		for (int col = 0; col < grid[row].length; col++){
    			if(grid[row][col].contains(agA)) {
    				rA=row;
    				cA=col;
    			}
    			//finds the position of agent b on the grid: (rB, cB)
    			else if(grid[row][col].contains(agB)) {
    				rB=row;
    				cB=col;
    			}
    		}
    	}
    	
		int dist = Math.abs(rA-rB)+Math.abs(cA-cB);  // calculates the Manhattan distance between agent a & agent b
		
		return dist;   //converts string into double
	}
	
	
    public static String centerString (int width, String s) {
        return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
    }
    
    public int[] directionsToCoordinates(String dir, int row, int col) {  // calculates the new rows & columns according to the move type (left, right, up and down)
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
    	//System.out.println(Arrays.toString(coordinates));
		return coordinates;
	}
    
    // chops an ArrayList into non-view sublists of length L
    static <T> ArrayList<ArrayList<T>> chopped(ArrayList<T> list, final int L) {
    	ArrayList<ArrayList<T>> parts = new ArrayList<ArrayList<T>>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<T>(
                list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }
    
    public boolean changePositions (int [] currentPos, int [] nextPos, String name, String[][] gr) {
    	
    	int rId = currentPos[0];
    	int cId = currentPos[1];
    	int new_rId = nextPos[0];
    	int new_cId = nextPos[1];
    	
    	// if not allowed to move from current position to next position, keep the current position
    	if (!isAllowed(currentPos ,nextPos)) {
			// TODO: give bad reward to the agent
			wasSuccessful=false;
		}
    	else {
    		// REMOVES AGENT FROM HIS OLD POSITION ON THE GRID
    		if(gr[rId][cId].contains(" " + name)) {  // agent is after another element on the cell
    			gr[rId][cId] = gr[rId][cId].replace(" " + name, "");
    		}
    		else if(gr[rId][cId].contains(name + " ")) {  // agent is the first element before another element on the cell
    			gr[rId][cId] = gr[rId][cId].replace(name + " ", "");
    		}
    		else {
    			gr[rId][cId] = gr[rId][cId].replace(name, "");  // agent is alone on the cell
    		}
    		// ADDS AGENT ON HIS NEW POSITION OF THE GRID
    		if(gr[new_rId][new_cId].equals("")) {
    			gr[new_rId][new_cId] = name;
			}
			else {
				gr[new_rId][new_cId] = gr[new_rId][new_cId] + " " + name;
			}
    		// TODO: give good reward to the agent
    		wasSuccessful=true;
    	}
    	return wasSuccessful;
    }
    
    private boolean isAllowed(int [] current_pos, int [] new_pos) {  // checks if the agent can make the move
		
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
}
