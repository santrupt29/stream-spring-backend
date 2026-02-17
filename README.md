# 🌀 Vortex - Video Streaming Backend

The **Vortex Backend** is a robust Spring Boot application designed to power a modern video streaming platform. It handles video uploads, processing (transcoding to HLS), and streaming, leveraging AWS S3 (via DigitalOcean Spaces) for storage and PostgreSQL for metadata management.
Frontend Repo - 

## 🔗 Repository Links
* **Backend:** [github.com/santrupt29/stream-spring-backend](https://github.com/santrupt29/stream-spring-backend)
* **Frontend:** [github.com/santrupt29/vortex-frontend](https://github.com/santrupt29/stream-frontend)

## 🚀 Key Features

*   **Video Upload & Processing**: Uploads raw MP4 files and transcodes them into HLS (HTTP Live Streaming) format using **FFmpeg**.
*   **Adaptive Bitrate Streaming**: Serves HLS playlists (`.m3u8`) and segments (`.ts`) for smooth playback.
*   **Partial Content Streaming**: Supports HTTP Range requests for efficient seeking and playback of raw files.
*   **Secure Storage**: Integrates with S3-compatible object storage (DigitalOcean Spaces) for scalable video hosting.
*   **User Management**: Basic user authentication and video ownership (linked to `Users`).
*   **Database Integration**: Uses JPA/Hibernate with PostgreSQL for persistent data storage.

## 🛠 Technology Stack

*   **Java**: 21
*   **Framework**: Spring Boot 3.5.10
*   **Database**: PostgreSQL
*   **Build Tool**: Maven
*   **Video Processing**: FFmpeg
*   **Cloud Storage**: AWS SDK (S3 / DigitalOcean Spaces)
*   **Security**: Spring Security & JWT

## ⚙️ Prerequisites

Ensure you have the following installed:

*   **Java 21 JDK**
*   **Maven**
*   **PostgreSQL**
*   **FFmpeg** (Must be available in system PATH)

## 🔧 Configuration

### Environment Variables

The application requires the following environment variables for AWS/S3 access:

```bash
export CLOUD_AWS_CREDENTIALS_ACCESS_KEY="your_access_key"
export CLOUD_AWS_CREDENTIALS_SECRET_KEY="your_secret_key"
```

### Application Properties (`src/main/resources/application.properties`)

Key configurations include:

*   **Database URL**: `jdbc:postgresql://localhost:5433/videodb` (Note: Default port is set to `5433`)
*   **S3/Spaces Endpoint**: `https://sgp1.digitaloceanspaces.com`
*   **Bucket Name**: `stream-app-storage`
*   **Region**: `sgp1`

## 📥 Installation & Running

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/santrupt29/stream-spring-backend.git
    cd stream-spring-backend
    ```

2.  **Set up the Database:**
    Ensure PostgreSQL is running on port `5433` (or update `application.properties`).
    Create the database:
    ```sql
    CREATE DATABASE videodb;
    ```

3.  **Build the project:**
    ```bash
    mvn clean package -DskipTests
    ```

4.  **Run the application:**
    ```bash
    java -jar target/spring-stream-backend-0.0.1-SNAPSHOT.jar
    ```
    Alternatively, using Maven:
    ```bash
    mvn spring-boot:run
    ```

The server will start on port `8080`.

## 🔌 API Endpoints

### Videos

*   `POST /api/v1/videos`: Upload a new video (Form data: `file`, `title`, `description`).
*   `GET /api/v1/videos`: List all videos.
*   `GET /api/v1/videos/stream/{videoId}`: Stream raw video file.
*   `GET /api/v1/videos/stream/range/{videoId}`: Stream raw video with range support.
*   `GET /api/v1/videos/{videoId}/master.m3u8`: Get HLS master playlist.
*   `GET /api/v1/videos/{videoId}/{segment}.ts`: Get HLS video segment.
*   `GET /api/v1/videos/my-videos`: Get videos uploaded by the authenticated user.

## 🚀 Deployment

This project is deployed on a **DigitalOcean Droplet**.
For detailed deployment instructions, including server setup, FFmpeg installation, and systemd configuration, please refer to [deploy.md](deploy.md).

## 📄 License

This project is licensed under the MIT License.
