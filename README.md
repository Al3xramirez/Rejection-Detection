# Rejection Detection

Rejection Detection is a full-stack app made for the purpose of showing you rejections from jobs you've applied to using the proccess of connecting to your Gmail, scaning incoming email messages, and classifing them as job-rejection related or not. The goal is to help a user track hiring outcomes.

## What This App Does

- Connects a Google account through OAuth2.
- Pulls Gmail messages for authenticated users.
- Parses each message into structured fields like sender, subject, body, timestamp, and company guess.
- Shows a dashboard with rejection counts, history, and a funny message letting you knowe you got rejected.

## Why did I make this?

- Initially, the purpose for this website was to keep track of the future rejection emails that I'll be receiving in the upcoming weeks, so I decided why not continue practicing coding, learning new ideas, and at the same time, cope by making an application turning hummiliation of job rejections into a funny and organized dashboard.
## Tech-Stack

<div>
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/docker/docker-original.svg" width="45" alt="Docker" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/vuejs/vuejs-original.svg" width="45" alt="Vue" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original.svg" width="45" alt="Spring Boot" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/google/google-original.svg" width="45" alt="Google OAuth" />
  <img <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/maven/maven-original.svg"  width="45" alt="Maven" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/mysql/mysql-original-wordmark.svg" width="45" alt="MySQL" />
</div>

## Handoff Setup Guide (For Friends / Other Users)

This section explains exactly how someone else can run this app after you hand it off.

### 1. What NOT to share

Do not share these items when downloading and testing this repository:

- Real `.env` file
- Real Google OAuth client secret
- Any personal database passwords

### 2. Prerequisites

The person running your project needs:

- Docker Desktop installed and running
- A Google Cloud project with OAuth credentials
- Access to edit environment variables in a local `.env` file

### 3. Environment variables

From the repo root:

```bash
cp .env.example .env
```

Then edit `.env` and provide real values:

```env
REJECTION_DETECTION_DB_USERNAME=local_app_user
REJECTION_DETECTION_DB_PASSWORD=local_app_password

DOCKER_MYSQL_ROOT_PASSWORD=changeme_root
DOCKER_DB_USERNAME=appuser
DOCKER_DB_PASSWORD=changeme_app

GOOGLE_OAUTH_CLIENT_ID=replace_me
GOOGLE_OAUTH_CLIENT_SECRET=replace_me
```

Important:

- `REJECTION_DETECTION_DB_USERNAME`, `REJECTION_DETECTION_DB_PASSWORD` are you're env variables for one of two db's you will need to set-up.
- `DOCKER_DB_USERNAME` and `DOCKER_DB_PASSWORD` are used by both MySQL and backend in `docker-compose.yml`. These are the second db env. variables you'll need to set up.
- `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET` must come from Google Cloud OAuth credentials.

### 4. Google OAuth configuration (A MUST)

In Google Cloud Console:

1. OAuth consent screen:
- User type should be External if non-organization users will test.
- Add app name, support email, and developer contact.
- Add required scopes used by app:
  - `openid`
  - `email`
  - `profile`
  - `https://www.googleapis.com/auth/gmail.readonly`

2. OAuth client credentials:
- Create OAuth client ID (Web application)
- Add Authorized JavaScript origin:
  - `http://localhost:5173`
- Add Authorized redirect URI:
  - `http://localhost:8080/login/oauth2/code/google`


3. Allow other users:
- If app is in Testing mode, only listed test users can log in.
- Add each friend email under OAuth consent screen -> Test users.
- If you want broader access, publish the OAuth consent screen to Production.

Without this step, friends will see "access blocked" or unauthorized login errors as well as yourself.

### 5. Run the app with Docker

From repo root:

```bash
docker compose up --build -d
```

Services:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- MySQL mapped to host port: `3307`

To stop:

```bash
docker compose down
```

To view logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

### 6. First login flow

1. Open `http://localhost:5173`
2. Click Google login
3. Approve permissions
4. Return to dashboard
5. Press sync to scan Gmail messages

### 7. You made it this far?

- If you made it this far into this nightmare of env varaibles and installations, you're probably bored out of your mind or really intrigued by this, if so thanks. I have no plans to actually put this program onto a website so keeping it local for now :) I don't want to get more pissed off at deployment then I already have.

