<h4># Bank-Managment-System</h4>
A secure Bank Management System developed using Spring Boot, Thymeleaf, MySQL, and Spring Security.  
The application allows users to register, login, deposit money, withdraw money, and manage banking transactions through a user-friendly web interface.

FEATURES
## Features

- User Registration and Login Authentication
- Secure Password Encryption using Spring Security
- Deposit and Withdrawal Operations
- Downloadable CSV Account Statements with Optional Date Filters
- Real-time Balance Management
- Transaction History Tracking
- Separate Admin Login Portal
- Responsive Banking Dashboard
- MySQL Database Integration
- MVC Architecture using Spring Boot
- Form Validation and Error Handling

- ## Technologies Used

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL
- HTML
- CSS
- Maven

- ## Key Highlights

- Implemented secure authentication and authorization
- Used JPA repositories for database operations
- Applied MVC architecture for clean code structure
- Integrated MySQL database for persistent storage
- Developed transaction management functionality

- ## Project Structure

- Controller Layer
- Service Layer
- Repository Layer
- Model Layer
- Security Configuration
- Thymeleaf Templates

- ## Future Enhancements

- Fund Transfer Between Accounts
- Email Notifications
- OTP Verification
- JWT Authentication

- ## Admin Portal

Set your admin credentials once in a local `.env` file:

```powershell
Copy-Item .env.example .env
notepad .env
```

Replace the example values with your own admin username and a strong, unique password. The `.env` file is ignored by Git, so credentials are not committed. After saving it, start the application normally:

```powershell
.\mvnw.cmd spring-boot:run
```

The app loads `.env` on startup, so you do not need to set the credentials again when restarting it from the project folder. Existing PowerShell environment variables still take precedence over `.env`. Open `/admin` to sign in. Administrators are directed to the admin workspace, where they can view customer account information.

- ## Screenshots

- Login Page
- <img width="381" height="363" alt="image" src="https://github.com/user-attachments/assets/e210147b-5217-4408-b0d4-c3ecb1dcda91" />

- Registration Page
- <img width="1343" height="637" alt="image" src="https://github.com/user-attachments/assets/c946c30f-a493-42c6-bb27-381e29d72982" />

- Dashboard
- <img width="1309" height="630" alt="image" src="https://github.com/user-attachments/assets/3159cb3f-be13-4df2-b690-474d920cec0e" />
<img width="1270" height="529" alt="image" src="https://github.com/user-attachments/assets/9afeb5b6-bff6-44ad-85f6-5e24e90498ff" />


- Deposit, Withdraw and Transfer module 
- <img width="346" height="517" alt="image" src="https://github.com/user-attachments/assets/d9974b02-b662-487f-b2cd-a4a0f1d245a3" />  -<img width="346" height="516" alt="image" src="https://github.com/user-attachments/assets/2bdec9fe-6e02-49c0-b313-b7edb69bef92" /> <img width="344" height="582" alt="image" src="https://github.com/user-attachments/assets/7c3634c4-fbf4-4738-9d9d-28b4835977be" />



- Transaction history
- <img width="1329" height="605" alt="image" src="https://github.com/user-attachments/assets/082cf560-4c75-4f3e-a24a-07466440526e" />
