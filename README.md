# Taxi-Management-System-using-JADE
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
To set up and run the Taxi Management System, follow these steps:
