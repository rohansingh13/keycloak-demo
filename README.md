
# Keycloak Demo

A Spring Boot application that integrates with Keycloak for user management, group creation, and token handling. This project demonstrates how to interact with Keycloak's Admin API to create users, assign them to groups, and manage authentication.

## Features

- Obtain a Keycloak token using the master realm.
- Create a user if they do not exist.
- Create a group if it does not exist.
- Assign users to groups.
- Handle exceptions and log errors.

## Technologies Used

- Spring Boot
- Keycloak
- Java 21 or 23
- Maven
- Docker

## Requirements

- Java 21 or 23
- Maven
- Docker (for local Keycloak server)

## Setup Instructions

### Clone the Repository

```bash
git clone https://github.com/yourusername/keycloak-demo.git
cd keycloak-demo
```


## Configure Application Properties

Update the src/main/resources/application.properties file with your Keycloak server details:

properties

spring.application.name=keycloak-demo
server.port=8081
keycloak.url=http://localhost:8080
keycloak.realm=master
keycloak.client-id=admin-cli
keycloak.admin-username=admin
keycloak.admin-password=admin


## Running Keycloak Locally
Start Keycloak using Docker:


```bash
docker run -d -p 8080:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin jboss/keycloak
```

Access Keycloak Admin Console at http://localhost:8080/auth/admin using the credentials:

Username: admin
Password: admin

## Build and Run the Application
Build the project using Maven:

```bash
mvn clean install
```

Run the application:

```bash
mvn spring-boot:run
```


## API Endpoint

POST /api/keycloak/v1/users/create-and-assign-group

### This endpoint performs multiple operations in a single request:

1. Obtain a Keycloak token.
2. Create a user and assign them to a group.
3. Retrieve groups.
4. Retrieve user details.
5. Create User and Assign to Group

### To create a user and assign them to a group, send a POST request to the following endpoint:

Request URL:

```bash
http://localhost:8081/api/keycloak/v1/users/create-and-assign-group
```


### Request Body:

```json
{
    "username": "sachintest",
    "email": "sachintest@example.com",
    "groupName": "sachintestgroup"
}


