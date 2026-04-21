# HRMS Attrition Analysis Project Documentation

## 1. Project Overview
The HRMS (Human Resource Management System) Attrition Analysis Subsystem is a Java-based desktop application designed to manage employee data, track exit interviews, evaluate employee attrition risk, calculate attrition trends, and provide an executive analytics dashboard. It uses a SQLite database for persistence and a Java Swing GUI (`MainDashboard.java`) for user interaction.

## 2. Core Concepts and Domain Models
The project is built around several core domain models, organized in the `com.hrms.model` package:
- **`Employee`**: Represents an employee in the system with attributes like ID, name, department, role, joining date, attendance, performance score, etc.
- **`ExitInterview`**: Stores information about an employee's exit, including the primary reason, feedback, and exit date.
- **`AttritionRecord`**: Represents historical attrition data for a specific period (e.g., Monthly, Quarterly, Annual), tracking totals, exits, and calculated attrition rates.
- **`RiskAssessment`**: Stores the evaluated attrition risk for an employee, including the risk level (LOW, MEDIUM, HIGH) and the reasoning behind it.
- **`Analytics Models` (`FilterSpec`, `SegmentComparison`, `CorrelationReport`, `RootCauseFinding`, `DashboardSnapshot`)**: Specialized models used to encapsulate data for the analytics and segmentation engines.

## 3. Design Patterns Used
The project heavily utilizes Object-Oriented Analysis and Design (OOAD) principles and established design patterns to ensure scalability, maintainability, and loose coupling.

### 3.1. Model-View-Controller (MVC) Pattern
The entire application follows the MVC architectural pattern:
- **Models**: Data representation (e.g., `Employee`, `RiskAssessment`).
- **Views**: UI components found in `com.hrms.ui` (e.g., `MainDashboard`).
- **Controllers**: Act as intermediaries between the View and Services (e.g., `EmployeeController`, `RiskController`, `DashboardController`).

### 3.2. Strategy Pattern
Used in the Risk Evaluation subsystem to define different algorithms for assessing employee risk based on varying criteria.
- **Interface**: `RiskStrategy` defines the `evaluate(Employee)` method.
- **Implementations**:
  - `LowRiskStrategy`: Applied when attendance >= 85% and promotions > 0.
  - `MediumRiskStrategy`: Applied when attendance 65-84% or zero promotions.
  - `HighRiskStrategy`: Applied when attendance < 65% (critically low).

### 3.3. Factory Pattern
Used to instantiate the correct Strategy implementation without exposing the instantiation logic to the client.
- **`RiskFactory`**: Contains the `getStrategy(Employee)` method which returns the appropriate `RiskStrategy` implementation based on the employee's specific attributes.

### 3.4. Observer Pattern
Implemented to build a reactive UI that automatically updates when backend data changes.
- **`DataChangeSubject`** / **`DataChangeObserver`**: Interfaces defining the publisher-subscriber contract.
- **`DataEventBus`**: A Singleton thread-safe central event hub. Services (like `AttritionService` or `RiskService`) notify the event bus when calculations are completed. The `DashboardController` listens to these events and triggers a Swing UI refresh callback.

### 3.5. Facade Pattern
Used to simplify the interactions with multiple complex analytics services.
- **`DashboardService`**: Aggregates calls to `SegmentationService`, `AnalyticsService`, and other sub-services to build a comprehensive `DashboardSnapshot` for the UI, hiding the underlying complexity.

### 3.6. Chain of Responsibility Pattern
Implemented to handle validation of `Employee` objects flexibly, ensuring that validation logic is decoupled from the main `EmployeeService`.
- **`EmployeeValidator`**: Base abstract class forming the chain.
- **Implementations**: `BasicInfoValidator`, `MetricsValidator`, and `StatusValidator` handle specific validation responsibilities in sequence.

### 3.7. Integration Interfaces
The system provides clean contracts to integrate with third-party or external HR systems:
- **`IHRAnalyticsReportingService`**: Interface contract for external analytics and reporting.
- **`IPayrollService`**: Interface contract for interacting with external payroll management systems.

## 4. SOLID Principles
- **Single Responsibility Principle (SRP)**: Services like `EmployeeService`, `AttritionService`, and `RiskService` have one clear area of responsibility. `DBConnection` only manages the database connection.
- **Open/Closed Principle (OCP)**: The Risk Evaluation engine (`RiskStrategy`) is open for extension (e.g., adding a new risk level) but closed for modification.
- **Liskov Substitution Principle (LSP)**: Any specific `RiskStrategy` (e.g., `HighRiskStrategy`) can substitute the base `RiskStrategy` interface seamlessly without breaking `RiskFactory`.
- **Interface Segregation Principle (ISP)**: Large interfaces were avoided in favor of focused ones like `IDashboardService` and `IAttritionRate`.
- **Dependency Inversion Principle (DIP)**: `DashboardService` interacts with subsystems via abstractions where possible, and services depend on the `DBConnection` abstraction instead of concrete data access logic.

## 5. GRASP Principles
- **Creator**: `DashboardService` is responsible for creating the `DashboardSnapshot` since it aggregates all the necessary data.
- **Information Expert**: `AttritionService` calculates attrition rates because it possesses (or coordinates with the DB to get) all the historical employee data.
- **Controller**: `DashboardController` and `EmployeeController` handle system events from the Swing UI and delegate to the appropriate Services.
- **Low Coupling**: Utilizing the **Observer** (`DataEventBus`) pattern decouples the UI from the background services. The UI simply listens for data updates without tightly coupling to service implementations.
- **High Cohesion**: Classes are highly focused on their core domain (e.g., `EmployeeService` handles only CRUD operations for employees).
- **Polymorphism**: Used in the risk calculation engine where `RiskStrategy.evaluate()` is implemented differently depending on the concrete strategy.

## 6. Project Structure
- `com.hrms.controller`: Request routing, input validation, and delegation to services.
- `com.hrms.db`: SQLite database connection management (`DBConnection`).
- `com.hrms.exception`: Custom exceptions (e.g., `HRMSException`, `DivideByZeroException`, `InvalidDateRangeException`).
- `com.hrms.model`: Domain classes and Enums.
- `com.hrms.service`: Core business logic, interfaces, and patterns implementation.
- `com.hrms.ui`: Java Swing-based user interface.

## 7. Commit History & Project Evolution

The development of the project was carried out iteratively. Below is the commit history explaining the progression:

### **Initial Commits & Base Setup**
*(Commits: 7807c4b, 69ac466, b9a2fa1, 3ae7842)*
- **What happened**: The base project structure was established. The core `Employee` and `ExitInterview` models were created, along with their respective Services (`EmployeeService`, `ExitInterviewService`) and Controllers. The initial SQLite database schema (`DBConnection`), custom exceptions (`HRMSException`), and the base `MainDashboard` UI were introduced. Documentation and diagrams were also uploaded.

### **Step 2: Attrition Calculation + Trend Analysis**
*(Commit: dfa5195)*
- **What happened**: Added functionality to calculate historical attrition rates and identify trends.
- **Key Additions**: `AttritionRecord` model, `PeriodType` enum, and the `IAttritionRate` interface. Implemented `AttritionService` to handle calculations.
- **UI & DB Updates**: Updated the UI with an "Attrition Analysis" tab featuring trend tables and Java2D line charts. The database schema was updated to handle hire/termination dates. Added specialized exceptions like `DivideByZeroException` and `InvalidDateRangeException`.

### **Step 3: Risk Evaluation System — Strategy + Factory patterns**
*(Commit: eaafb75)*
- **What happened**: Introduced the predictive risk evaluation module to flag employees likely to leave.
- **Key Additions**: Implemented the **Strategy Pattern** (`LowRiskStrategy`, `MediumRiskStrategy`, `HighRiskStrategy`) and **Factory Pattern** (`RiskFactory`) to determine an employee's attrition risk. Created the `IRiskClassification` interface and `RiskService`.
- **UI Updates**: Added a "Risk Evaluation" tab with single/bulk evaluate options, color-coded results, and summary KPI chips.

### **Step 4: Segmentation, Analytics & Executive Dashboard**
*(Commit: 7bd8ef9)*
- **What happened**: Implemented advanced analytics, data segmentation, and an executive dashboard using the **Observer** and **Facade** patterns.
- **Key Additions**: Added models like `FilterSpec`, `CorrelationReport`, and `RootCauseFinding`. Created the `DataEventBus` (Observer pattern) to notify the UI of data changes. Created the `DashboardService` (Facade pattern) to aggregate analytics into a `DashboardSnapshot`.
- **UI Updates**: Added an "Analytics Dashboard" tab with KPI bars, correlation progress bars, and department-wise attrition charts.

### **Fixing small bugs**
*(Commit: 9d8779c)*
- **What happened**: Addressed minor bugs and stabilized the application flow following the integration of the complex Analytics Dashboard.

### **DB Integration - Initial commit**
*(Commit: 91f83e2)*
- **What happened**: Further enhancements to the database layer, integrating the external database driver (`hrms-database-1.0-SNAPSHOT.jar`) and standardizing the database file (`hrms.db`) across the project.
