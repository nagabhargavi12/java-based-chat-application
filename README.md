# Java Client-Server Chat Application

This is a console-based multi-client chat application developed using
Java socket programming. The project follows a client–server architecture
and allows multiple users to communicate in real time.

## Features
- Client-server communication using TCP sockets
- Supports multiple clients using multithreading
- User signup and login functionality
- Password security using SHA-256 hashing
- Real-time message broadcasting to all connected clients
- Server-side message storage using MySQL database
- Admin can send messages directly from the server
- Graceful client exit using /exit command

## Technologies Used
- Java
- Socket Programming
- Multithreading
- JDBC
- MySQL Database
- SHA-256 Password Hashing

## Files Included
- Client.java : Handles client-side communication
- Server.java : Handles server logic, authentication, and broadcasting

## How to Run
1. Ensure Java and MySQL are installed
2. Create a MySQL database named `chatdb`
3. Create required tables for users and messages
4. Start the server:
   java Server
5. Run the client in multiple terminals:
   java Client

## Database Note
Update the database username and password in Server.java
before running the application.

## Note
This project demonstrates networking, multithreading, database
integration, and basic security concepts using Java.
