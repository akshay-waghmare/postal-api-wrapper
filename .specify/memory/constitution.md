<!--
Sync Impact Report:
- Version change: 1.0.0 -> 1.1.0
- Modified principles: Adjusted for Java/Spring Boot context (Type Safety, Documentation).
- Modified sections: Technology Stack, Development Workflow.
- Templates requiring updates: None.
- Follow-up TODOs: None.
-->
# MailIt Postal Wrapper API Constitution

## Core Principles

### I. Robust Error Handling
The wrapper service MUST normalize errors from the upstream TrackingMore API into a consistent, typed exception hierarchy. Consumers of this service should never have to parse raw upstream HTTP error responses. All network and API errors must be caught, logged, and mapped to appropriate HTTP status codes and structured error responses (e.g., \TrackingMoreException\, \UpstreamServiceUnavailable\).

### II. Type Safety & Validation
All inputs and outputs MUST be strongly typed. The application shall use Java Classes or Records with Jakarta Bean Validation to validate data *before* processing or forwarding to the upstream API. Return values from the upstream API must be deserialized into structured objects to ensure type safety within the application logic.

### III. Abstraction & Usability
The internal logic MUST NOT be tightly coupled to the implementation details of the underlying HTTP requests to TrackingMore. The application exposes clean Service interfaces (e.g., \TrackingService\) that map directly to business operations. Configuration (API keys, base URLs) must be managed via Spring Boot externalized configuration (\pplication.properties\ or \pplication.yml\).

### IV. Testability
The application MUST be designed to allow easy mocking of the underlying network layer. It should be possible to test business logic using \@MockBean\ or similar mechanisms without making actual network calls to TrackingMore.

### V. Documentation First
Every public Service method and Controller endpoint MUST have Javadoc explaining its purpose, parameters, and return values. API endpoints should be documented using OpenAPI/Swagger annotations.

## Technology Stack

**Language**: Java 17+ (LTS)
**Framework**: Spring Boot 3+
**HTTP Client**: Spring \RestClient\ or \WebClient\
**Validation**: Jakarta Bean Validation (Hibernate Validator)
**Testing**: JUnit 5, Mockito, AssertJ
**Documentation**: SpringDoc OpenAPI (Swagger)

## Development Workflow

1.  **Spec-Driven**: All changes start with a specification update.
2.  **Test-Driven**: Write tests (Unit or Integration) before implementing the logic.
3.  **Style & Quality**: Code must adhere to standard Java conventions (e.g., Google Java Style) and pass static analysis (e.g., Checkstyle, SonarLint).

## Governance

This constitution governs the development of the MailIt Postal Wrapper API.
-   **Amendments**: Changes to these principles require a Pull Request and approval from the project maintainers.
-   **Versioning**: The project follows Semantic Versioning (MAJOR.MINOR.PATCH).
-   **Compliance**: All code reviews must verify compliance with these principles.

**Version**: 1.1.0 | **Ratified**: 2025-12-18 | **Last Amended**: 2025-12-18
