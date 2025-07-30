# CineScope Backend

Java Spring Boot backend powering the CineScope movie recommendation platform.

---

## Tech Stack & Architecture

### Backend
- **Language:** Java 17
- **Framework:** Spring Boot
- **Authentication:** JWT stored in secure `HttpOnly` cookies
- **Filter:** `JwtAuthFilter` parses JWT from cookie and sets Spring Security context
- **Endpoints:** REST APIs for auth, movie search, watchlist management, and enrichment

---

## AWS CLOUD

The CineScope backend is tightly integrated with several AWS services to ensure scalability, reliability, and performance.

### Services Used

- **AWS Lambda** – Python-based serverless enrichment of movie data
- **Amazon DynamoDB** – NoSQL database to persist enriched movie and user watchlist data
- **Amazon S3** – Stores high-resolution movie posters and thumbnails
- **Amazon EC2** – Hosts the Spring Boot backend within a Docker container
- **IAM** – Role-based access control for secure inter-service communication
- **AWS SDK (Java)** – Used to invoke Lambda and access DynamoDB from Spring Boot backend
- **AWS Certificate Manager + Certbot** – Used with Nginx on EC2 to enable HTTPS

---

## Movie Enrichment with AWS Lambda
- **Language:** Python (in Lambda)
- **Purpose:** Analyze movie data (e.g., actor, genre, year, rating) and enrich watchlist items
- **Features:**
    - Computes a `popularity_score`
    - Updates enriched data back into DynamoDB
- **Trigger:** Spring Boot backend via AWS SDK using `lambda:InvokeFunction`

---

## Database: DynamoDB

**Table:** `CineScopeWatchlist`  
**Stored Fields:**
- `user_id` (String)
- `movie_id` (String)
- `title`
- `genres`
- `actors`
- `overview`
- `release_date`
- `poster_url`
- `thumbnail_url`
- `popularity_score` (Decimal)
- `actor_classification` (String)

---

## Poster Storage: AWS S3
- High-res movie posters stored in an S3 bucket
- Example URL:  
  `https://cinescope-posters.s3.us-east-2.amazonaws.com/originals/120.jpg`

---

## Deployment: EC2 + Docker
- Backend is Dockerized and deployed to an **Amazon EC2 instance**
- **Port:** 8080
- **HTTPS:** Enabled with Nginx + Certbot (Let's Encrypt)
- `.tar` built locally and uploaded via SCP

---

## Security

### IAM Roles (AWS)
- **Role:** `EC2ECRReadOnlyRole`
- Attatched to the EC2 instance to securely enable backend access to AWS services (Lambda, DynamoDB, S3)
- Includes both mangaged policies (like `AmazonS3FullAccess`) and custom inline policies for invoking Lambda functions and others
