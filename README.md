# starjamz
Cloud-based music platform
Note: still under development. email brxstarjam@gmail.com for questions

## Not Just Music Orientation
Starjamz is designed to go far beyond a typical music streaming service. 

### Independent Artists and Labels
- **Streaming Music:** Music artists can stream music, like a traditional music app on the web, Android, or iOS.
- **Streaming Video:** Concerts, and other music events can be streamed via the web with Starjamz! 
- **Video Sharing:** Engage fans with music videos, shorts on your own site and across social media!
- **Monetization:** By charging subscription fees, artists can create a steady revenue stream independent of traditional music sales and streaming royalties.
- **Exclusive Content:** Artists can offer exclusive content or early access to new releases for subscribers, enhancing fan loyalty.

### Community Radio Stations and Podcasters
- **Live Broadcasting:** Community radio stations and podcasters can live stream audio content, allowing for real-time interaction with listeners.
- **Archiving Shows:** The ability to host audio and video content enables these creators to archive their shows for on-demand access.

### Educational Institutions
- **Online Learning:** Schools and universities can use the platform to host educational content, live lectures, and interactive sessions with students.
- **Subscription Access:** Institutions can charge subscription fees for access to specialized courses or content, creating a new revenue stream.

### Event Organizers
- **Virtual Events:** With the capability to live stream audio and video, event organizers can host virtual concerts, festivals, and conferences, reaching a global audience.
- **Merchandise Sales:** Selling event-related apparel and merchandise directly through the app can add an additional revenue source.

### Fitness and Wellness Industry
- **Workout and Yoga Classes:** Instructors can live stream classes, allowing them to reach a broader audience beyond geographical limitations.
- **Subscription Models:** Monthly subscription models can be employed for access to exclusive workout routines, diet plans, or wellness content.


### Charities and Non-profits
- **Fundraising Events:** Live stream concerts, talks, or other events can be organized as fundraisers, with options for viewers to donate directly through the app.
- **Awareness Campaigns:** The platform can be used to host informational videos and podcasts to raise awareness about various causes.

### Corporate Sector
- **Product Launches:** Companies can use the platform to live stream product launches or press conferences.
- **Training and Development:** Corporations can host training materials for employees, using subscription models for specialized content.

### Gaming Community 
- **Live Game Streaming:** Gamers can live stream their gameplay, host tournaments, and interact with viewers in real-time.
- **Merchandise:** Selling gaming-related apparel and merchandise directly to fans.

### Niche Communities
- **Special Interest Groups:** Communities centered around niche interests (e.g., astronomy, bird watching) can live stream events, host informative content, and create a subscription-based model for specialized access.

*** 

## Architecture
Starjamz employs an architecture based on microservices that run, and can be scaled, independently of each other. 

## Getting the app running locally

Right now, the repo is **not a one-command startup**. To run it successfully, you need to provide missing infrastructure and a few config fixes.

### 1) Use Java 17 for all Gradle builds
All services target Java 17, so use JDK 17 when running Gradle.

### 2) Bring up required infrastructure first
The services are configured to expect these dependencies:
- **Eureka server** at `http://localhost:8761/eureka/`
- **Kafka** broker at `127.0.0.1:9092`
- **Redis** at `localhost:6379`

### 3) Set real secrets (or disable integrations while developing)
Default properties use placeholder values for Stripe and SendGrid. Replace them before testing payment/email flows.

### 4) Start backend services
You can start each service with Gradle from its folder (`GatewayService`, `PaymentService`, `MusicService`, etc.).

### 5) Start frontend
From `Frontend/starjamz`, install dependencies and run the Next.js app.

### Known repository gaps
- No Eureka server service/module is included in this repository.
- The docker compose file is for backend services only and does not include Eureka/Kafka/Redis.
- Gateway routes are discovery-driven, so service registration needs to be working before gateway endpoints resolve.
