# Taxi Management System using JADE
This project is a Java-based taxi management system developed using [JADE](https://en.wikipedia.org/wiki/Java_Agent_Development_Framework) (Java Agent Development Framework).

## Introduction

The Taxi Management System is designed to simulate the operation of taxi drivers in a grid world. The project includes two versions: a single-agent package and a multi-agent package. In the single-agent system, you'll find a standalone taxi agent that operates independently to serve clients and maximize profits. This single-agent setup serves as the basis for the multi-agent variant, which introduces multiple taxi agents. In that system, the agents collaborate and communicate to efficiently serve clients, resolve conflicts, and optimize their payoffs. Coordination, planning, and negotiation become crucial aspects of the system, enabling a more sophisticated and realistic approach to taxi management.

## Features
- Single-agent and multi-agent taxi management systems.
- Grid-based world with 4 starting/ending points (e.g., Red, Blue, Green, Yellow).
- Taxi agents capable of picking up & dropping off clients, navigating the grid world and collaborating with other agents.
- Collision resolution through auctions.
- Real-time A* (RTA*) algorithm for route planning.
- Detailed logging and debugging options.

## Setup
1. **Clone the Repository:** Clone this GitHub repository to your local machine
2. **Navigate to the Appropriate Directory:** Open a terminal and navigate to either the `single-agent-system` or `multi-agent-system` directory within the cloned repository, depending on which system you want to run.
4. **Compile the Java Scripts:** Use the `javac` command to do so.
5. **Run the System:** Run the `Grid` class, which serves as the entry point.

### Additional Notes:

- Ensure you have Java Development Kit (JDK) installed on your system.
- Depending on your system and IDE, you may use an integrated development environment (IDE) like Eclipse or IntelliJ IDEA to import and run the project.
- For detailed information on how to configure and customize the behavior of the agents or grid, please refer to the relevant sections in the code or documentation.

## Usage
- Upon running the project, the taxi agents will start serving clients in the grid world.
- You can modify agent behaviors, grid configurations, and other parameters in the code to experiment with different scenarios.
- Detailed documentation and examples of agent interactions are available in the code comments.

## Authors
[Natalia Koliou](https://www.linkedin.com/in/natalia-koliou-b37b01197/) & Dimitris Lazarakis
