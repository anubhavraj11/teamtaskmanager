# Team Task Manager

Full-stack Spring Boot application for project management, task assignment, and progress tracking with `ADMIN` and `MEMBER` roles.

## Features

- JWT-based signup and login
- Role-based access control with Spring Security
- Admin project CRUD
- Admin team membership management
- Task creation and reassignment
- Member-only task status updates
- Admin and member dashboards
- Search, filtering, overdue views, pagination, and task priority
- Static frontend served directly by Spring Boot
- PostgreSQL persistence
- Railway-ready Docker deployment

## Roles

### ADMIN

- Create, update, and delete projects
- Add and remove project members
- Create tasks
- Assign and reassign tasks to project members
- View all tasks in all projects
- View dashboard metrics across the entire system

### MEMBER

- Sign up and log in
- View only assigned tasks
- Update task status only for assigned tasks
- View personal dashboard metrics and overdue tasks

## Authentication and security

- `POST /auth/signup` creates a `MEMBER` account
- `POST /auth/login` returns a JWT token and current user data
- Passwords are stored with BCrypt
- JWT contains the authenticated user role
- All endpoints except `/auth/*` and static frontend assets require authentication
- Role checks are enforced with Spring Security and service-level validation

## Database model

### User

- `id`
- `fullName`
- `email`
- `password`
- `role`

### Project

- `id`
- `name`
- `description`
- `createdBy`
- `members`

### Task

- `id`
- `title`
- `description`
- `status` (`TODO`, `IN_PROGRESS`, `DONE`)
- `priority` (`LOW`, `MEDIUM`, `HIGH`)
- `dueDate`
- `assignedTo`
- `project`

### Relationships

- `Project -> Task`: one-to-many
- `Project <-> User`: many-to-many
- `Task -> User`: many-to-one for assignee
- `Project -> User`: many-to-one for creator
- `Task -> User`: many-to-one for creator

## API endpoints

### Auth

- `POST /auth/signup`
- `POST /auth/login`

### Users

- `GET /users/me`
- `GET /users` (`ADMIN` only)

### Projects

- `POST /projects` (`ADMIN` only)
- `GET /projects`
- `PUT /projects/{id}` (`ADMIN` only)
- `DELETE /projects/{id}` (`ADMIN` only)
- `PUT /projects/{id}/add-member` (`ADMIN` only)
- `DELETE /projects/{id}/members/{memberId}` (`ADMIN` only)

### Tasks

- `POST /tasks` (`ADMIN` only)
- `PUT /tasks/{id}/assign` (`ADMIN` only)
- `PUT /tasks/{id}/status`
- `GET /tasks` (`ADMIN` only)
- `GET /tasks/my-tasks`
- `GET /tasks/overdue`

### Dashboard

- `GET /dashboard`

## Task query support

The task listing endpoints support query parameters:

- `search`
- `status`
- `priority`
- `projectId`
- `page`
- `size`

Example:

```text
GET /tasks?search=api&status=TODO&priority=HIGH&page=0&size=10
```

## Validation rules

- Email must be unique
- Name, email, password, title, status, priority, and due date are validated with Bean Validation
- Project membership is validated before task assignment
- Members can update only their own tasks
- Allowed task status flow:
  - `TODO -> IN_PROGRESS`
  - `IN_PROGRESS -> DONE`

## Frontend

Static frontend files live in:

- `src/main/resources/static/index.html`
- `src/main/resources/static/app.css`
- `src/main/resources/static/app.js`

The frontend includes:

- login and signup pages
- admin dashboard
- member dashboard
- project management UI
- team member management UI
- task filtering and pagination UI
- task priority and status controls
- project progress indicators

## Project structure

```text
.
|-- Dockerfile
|-- mvnw
|-- mvnw.cmd
|-- pom.xml
|-- README.md
`-- src
    |-- main
    |   |-- java/com/ethara/taskmanager
    |   |   |-- config
    |   |   |-- controller
    |   |   |-- dto
    |   |   |-- entity
    |   |   |-- exception
    |   |   |-- repository
    |   |   |-- security
    |   |   `-- service
    |   `-- resources
    |       |-- application.properties
    |       `-- static
    `-- test
```

## Environment variables

Required:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/taskdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
JWT_SECRET=use-a-secret-with-at-least-32-characters
```

Optional:

```text
JWT_EXPIRATION_MS=86400000
ADMIN_FULL_NAME=System Admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=SecurePass123
```

If no `ADMIN` exists and the optional admin bootstrap variables are provided, the application seeds an initial admin account at startup.

## Local run

1. Create the PostgreSQL database.
2. Set the environment variables.
3. Build the application:

```bash
./mvnw clean package
```

4. Start the application:

```bash
./mvnw spring-boot:run
```

5. Open:

```text
http://localhost:8080
```

If port `8080` is already in use, set another port:

```bash
PORT=8082 ./mvnw spring-boot:run
```

## Railway deployment

The repository includes a `Dockerfile`, so Railway can build and run the application directly.

1. Push the repository to GitHub.
2. Create a new Railway project from the GitHub repository.
3. Add a PostgreSQL service or external PostgreSQL connection.
4. Set these environment variables in Railway:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<at-least-32-characters>
JWT_EXPIRATION_MS=86400000
ADMIN_FULL_NAME=<initial admin name>
ADMIN_EMAIL=<initial admin email>
ADMIN_PASSWORD=<initial admin password>
```

5. Deploy. Railway will provide `PORT`, which Spring Boot already reads.

## Verified locally

The current implementation was verified for:

- application compile and package
- static frontend served at `/`
- admin login
- member signup
- project create, update, add member, and delete
- task create
- task assign
- `GET /tasks`
- `GET /tasks/my-tasks`
- `GET /tasks/overdue`
- member status transition from `TODO` to `IN_PROGRESS` to `DONE`
