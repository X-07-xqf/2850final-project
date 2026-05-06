# Sage - COMP2850 Group 50 Project (Good Food and Healthy Eating)

https://two850final-project.onrender.com


## Users
- Subscribers can log meals, set macro goals, browse healthy recipes, save favourite recipes, and message a health professional.
- Professionals can see every client's day at a glance, check if they are on track with their set goals, and message clients.

## Stack
Ktor and Kotlin, Thymeleaf, CSS, JS. 
Database: H2 in dev and Postgres on Render via Exposed ORM.
JDK 17

```
src/main/
├── kotlin/com/goodfood/
│   ├── Application.kt    
│   ├── auth/             
│   ├── diary/            
│   ├── recipes/          
│   ├── goals/            
│   ├── messages/         
│   ├── professional/     
│   ├── profile/          
│   ├── seed/             
│   └── config/           
└── resources/
    ├── templates/        
    └── static/           
```

