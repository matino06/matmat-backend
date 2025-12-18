# MatMat Backend

MatMat Backend is the server-side application for **MatMat**, an interactive learning platform designed to help Croatian high school students prepare for the national mathematics exam (State Matura).

The backend is responsible for business logic, user progress tracking, spaced repetition scheduling and task management. It exposes a REST API consumed by the MatMat frontend.

---

## 🌐 Live Services

* **API Base URL:** [https://api.matmat.online](https://api.matmat.online)
* **Frontend Application:** [https://matmat.online](https://matmat.online)
* **Frontend Repository:** [https://github.com/matino06/matmat-frontend](https://github.com/matino06/matmat-frontend)

---

## 🧠 Project Overview

MatMat focuses on *learning with understanding*, not memorization. The backend supports:

* Adaptive learning through a spaced repetition algorithm
* Detailed tracking of user knowledge and progress
* Structured math tasks with step-by-step solutions
* Scalable cloud deployment

The system is designed so that consistent usage leads users toward **100% mastery** of the exam material.

---

## 🛠 Tech Stack

* **Language:** Java
* **Framework:** Spring Boot
* **API Type:** REST
* **Database:** PostgreSQL
* **Authentication:** Firebase
* **Hosting:** AWS Elastic Beanstalk
* **Cloud Storage:** AWS S3 (images & static assets)
* **Bot Protection:** Cloudflare Turnstile
* **Build Tool:** Maven

---

## ☁️ Storage & Security

### AWS S3 Integration
MatMat backend uses **Amazon AWS S3** for storing and serving static assets:

* Task-related images

The backend handles:
* Secure upload of images to S3
* Generation of public or signed URLs
* Retrieval of image metadata for frontend consumption

### Cloudflare Turnstile Verification
To protect the platform from bots and abuse, **Cloudflare Turnstile** is verified on the backend:

* Turnstile tokens are validated server-side

This ensures security without degrading user experience.

---

## 🚀 Features (Backend)

* **User Management**

  * Registration & authentication
  * Secure Firebase authorization

* **Spaced Repetition Engine**

  * Tracks task performance
  * Schedules optimal review times based on forgetting curves

* **Task & Content Management**

  * Math problems structured by topic and difficulty
  * Detailed step-by-step solutions stored and served via API

* **Progress Tracking**

  * Chapter unlocking logic
  * Learning statistics per user

* **Media Storage**

  * Image upload and retrieval via AWS S3

* **Security & Abuse Protection**

  * Server-side Cloudflare Turnstile validation
  * Protection against automated abuse

---

## 📦 Repository Structure (Simplified)

```
matmat-backend/
├── src/
│   ├── main/
│   │   ├── java/com/matmat/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── storage/
│   │   │   └── security/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── README.md
```

---

## ⚙️ Local Development

### Prerequisites

* Java 17+
* Maven
* Running database instance

### Setup

```bash
git clone https://github.com/matino06/matmat-backend.git
cd matmat-backend
mvn spring-boot:run
```

Configure environment variables or `application.properties` for:

* Database connection
* AWS S3 Storage
* Cloudflare Turnstile

---

## ☁️ Deployment

The backend is deployed on **AWS Elastic Beanstalk**, providing:

* Automatic scaling
* Load balancing
* Environment-based configuration

Deployment is handled via packaged Spring Boot JAR.
