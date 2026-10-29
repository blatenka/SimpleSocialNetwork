# ALOUTE Social Network

ALOUTE is a social networking application built with Java 17 and Spring Boot. It uses Thymeleaf and Bootstrap for the web interface, MySQL for relational data, MongoDB for messaging data, Cloudinary for media uploads, and WebSocket for real-time features.

## Features

- **Accounts and security:** register, log in, log out, and recover/reset a password by email.
- **Newsfeed and posts:** browse a paginated feed; create posts with text, photos, and videos; select post visibility; search posts; and view post details.
- **Social connections:** search for people, send and manage friend requests, view friends and mutual friends, and remove a friendship.
- **Profiles:** view and update profile information, profile avatar, and cover image.
- **Chat:** view conversations, send and receive messages and image attachments, and use real-time messaging.
- **Engagement:** comment on posts, reply to comments, react to posts/comments, and receive and manage notifications.
- **Administration:** administrators can manage user accounts, review and remove posts, and view system statistics. Admin pages and APIs are restricted to users with the administrator role.

## Requirements

- Java 17 or newer
- MySQL 8 or compatible
- MongoDB
- Internet access for the first Maven Wrapper build
- Cloudinary account (needed for image/video uploads)
- Gmail account with a Google App Password (needed for real email)

Maven is included through the Maven Wrapper, so a separate Maven installation is not required.

## 1. Get the project

Clone or download the repository, then open a terminal in the project root (the directory containing `mvnw.cmd` and `pom.xml`).

## 2. Prepare the databases

Start MySQL and create the database:

```sql
CREATE DATABASE aloute_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Start MongoDB locally on its default port (`27017`). The current configuration connects to the `aloute_db` MongoDB database at `mongodb://localhost:27017/aloute_db`.

The application uses Hibernate `ddl-auto: update` to update the MySQL schema during startup. Back up important data before running schema changes against an existing database.

## 3. Configure `.env`

Spring Boot loads `.env` from the project root using `spring.config.import` in `src/main/resources/application.yml`. Create a file named `.env` beside `pom.xml` and add the settings below:

```properties
# MySQL
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Gmail SMTP (optional; required for real password-reset emails)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_gmail_address@gmail.com
MAIL_PASSWORD=your_google_app_password
MAIL_FROM=your_gmail_address@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true

# Cloudinary (required for image/video uploads)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Public application URL used in links in emails
APP_BASE_URL=http://localhost:8080
```

Replace the example values with your own. For Gmail, use a Google **App Password**, not your regular account password. Keep the password secret; do not paste it into source code, commit it, or share it in chat. The `.env` file is listed in `.gitignore` and should remain local.

`DB_USERNAME` defaults to `root`; `DB_PASSWORD` defaults to blank. The database name and local MongoDB URL are currently configured in `application.yml`, not through `.env`. Optional settings can be omitted if the corresponding feature is not being used, but configure Cloudinary before testing uploads and SMTP before testing email delivery.

## 4. Build and run

From the project root, use PowerShell:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

The first command compiles the project and runs its tests. The second starts the application. Open [http://localhost:8080](http://localhost:8080) in a browser.

To run tests without packaging:

```powershell
.\mvnw.cmd test
```

## Troubleshooting

- **MySQL connection fails:** confirm the MySQL service is running, `aloute_db` exists, and the `.env` credentials are correct.
- **MongoDB connection fails:** confirm MongoDB is running and accepting connections on `localhost:27017`.
- **Email is not sent:** verify the Gmail address and App Password, ensure two-step verification and App Passwords are enabled for the Google account, and check that SMTP access is not blocked by the network.
- **Media upload fails:** check the Cloudinary credentials and the selected file type/size.
- **`.env` changes are not picked up:** ensure the file is in the project root and restart the application after editing it.
