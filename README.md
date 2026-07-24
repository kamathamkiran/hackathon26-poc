# Credit Agreement AI - Application Documentation

This repository is a Spring Boot proof of concept for processing credit agreement PDFs. The application takes a local PDF path, sends the PDF through Google Document AI, extracts loan agreement fields with a Google ADK LLM agent, validates the extracted values, and returns a workflow response.

## Tech Stack

- Java 17
- Spring Boot 3.1.0
- Spring Web for the REST API
- Spring Data JPA for persistence
- H2 in-memory database
- Google Document AI for PDF parsing/OCR
- Google ADK for LLM-based extraction
- Jackson for JSON serialization
- Lombok for boilerplate reduction

## Repository Layout

```text
.
|-- pom.xml
|-- mvnw / mvnw.cmd
|-- src/main/java/com/db/hackathon
|   |-- CreditAgreementAiApplication.java
|   |-- controller/
|   |-- service/
|   |-- workflow/
|   |-- adk/
|   |-- entity/
|   |-- repository/
|   |-- dto/
|   |-- model/
|   |-- enums/
|   |-- config/
|   `-- exception/
|-- src/main/resources
|   |-- application.yaml
|   |-- sample.txt
|   `-- prompts/
`-- src/test/java/
```

## High-Level Flow

```text
Client
  |
  | POST /workflow/process with filePath
  v
WorkflowController
  |
  v
WorkflowService
  |
  | creates WorkflowEntity with status UPLOADED
  | stores filePath as JSON metadata
  v
WorkflowEngine
  |
  | builds WorkflowContext
  v
WorkflowExecutor -> DocumentParserAgent
  |
  | Google Document AI parses PDF
  v
WorkflowExecutor -> ExtractionAgent
  |
  | ADK LLM extracts agreement fields
  v
WorkflowExecutor -> ValidationAgent
  |
  | mandatory-field and confidence validation
  v
WorkflowResponse
```

The intended workflow status progression is:

```text
UPLOADED -> PARSED -> EXTRACTED -> VALIDATED -> COMPLETED
```

The enum also contains intermediate statuses:

```text
PARSING, EXTRACTING, VALIDATING
```

Those intermediate statuses are exposed by the agents as input statuses, but the current executor does not set them before each agent starts.

## API Layer

### `WorkflowController`

Path: `src/main/java/com/db/hackathon/controller/WorkflowController.java`

The controller exposes one endpoint:

```http
POST /workflow/process
Content-Type: multipart/form-data
```

Request parameter:

```text
filePath=<absolute-or-relative-path-to-pdf>
```

Example:

```bash
curl -X POST http://localhost:8080/workflow/process \
  -F "filePath=/path/to/agreement.pdf"
```

Even though the endpoint consumes `multipart/form-data`, it does not currently upload a file. It only accepts a string path to a PDF that must already exist on the machine where the Spring Boot app is running.

Response body:

```json
{
  "workflowId": "...",
  "status": "COMPLETED",
  "agreement": {
    "dealName": {
      "value": "...",
      "pageNumber": 1,
      "confidence": 0.95,
      "sourceText": "..."
    }
  },
  "validationResult": {
    "valid": true,
    "errors": [],
    "warnings": []
  }
}
```

## Service Layer

### `WorkflowService`

Path: `src/main/java/com/db/hackathon/service/WorkflowService.java`

Responsibilities:

- Creates a new `WorkflowEntity`.
- Generates a UUID workflow ID.
- Sets username to `OPS_USER`.
- Sets initial status to `UPLOADED`.
- Sets next agent to `DOCUMENT_PARSER`.
- Stores `filePath` as serialized JSON metadata.
- Saves the workflow through `WorkflowRepository`.
- Calls `WorkflowEngine.execute(...)`.
- Converts the final context into `WorkflowResponse`.

### `JsonSerializerService`

Path: `src/main/java/com/db/hackathon/service/JsonSerializerService.java`

Thin wrapper around Jackson `ObjectMapper`.

Provides:

- `serialize(Object)`
- `deserialize(String, Class<T>)`
- `readTree(String)`

### `JacksonConfig`

Path: `src/main/java/com/db/hackathon/config/JacksonConfig.java`

Creates the application-wide `ObjectMapper`.

Configuration:

- Registers available Jackson modules.
- Excludes `null` values from serialized JSON.

## Workflow Orchestration

### `WorkflowEngine`

Path: `src/main/java/com/db/hackathon/workflow/WorkflowEngine.java`

The engine decides which agents to run based on the current workflow status.

Current behavior:

- `UPLOADED`: run document parsing, extraction, validation.
- `PARSED`: run extraction and validation.
- `EXTRACTED`: run validation.

This design allows a workflow to resume from a saved checkpoint, as long as the persisted checkpoint data can be deserialized back into the context.

### `WorkflowContextBuilder`

Path: `src/main/java/com/db/hackathon/workflow/WorkflowContextBuilder.java`

Builds a `WorkflowContext` from the persisted `WorkflowEntity`.

Intended mapping:

- `UPLOADED`: read `filePath`.
- `PARSED`: deserialize `DocumentAnalysis`.
- `EXTRACTED`: deserialize `Agreement`.

### `WorkflowExecutor`

Path: `src/main/java/com/db/hackathon/workflow/WorkflowExecutor.java`

Runs a single `WorkflowAgent`.

For each agent:

1. Create or load a `WorkflowEventEntity`.
2. Run `agent.process(context)`.
3. Save a workflow checkpoint with the agent output.
4. Mark the event `SUCCESS`.
5. If status reaches `VALIDATED`, mark the workflow `COMPLETED`.

On failure:

1. Mark the event `FAILED`.
2. Increment retry count.
3. Mark the workflow `FAILED` once retry count reaches `MAX_RETRY`, currently `3`.
4. Rethrow the exception to the engine.

The engine catches and logs exceptions, then returns the current context.

### `WorkflowManager`

Path: `src/main/java/com/db/hackathon/workflow/WorkflowManager.java`

Centralizes persistence updates for:

- Workflow checkpoints.
- Workflow completion.
- Workflow failure.
- Workflow event creation.
- Workflow event success/failure.
- Retry counting.

## Agent Contract

### `WorkflowAgent`

Path: `src/main/java/com/db/hackathon/adk/agent/WorkflowAgent.java`

Every agent implements:

- `getAgentType()`
- `getInputStatus()`
- `getOutputStatus()`
- `process(WorkflowContext)`
- `getOutput(WorkflowContext)`

The executor is generic and works with this interface.

## Agent 1: Document Parsing

### `DocumentParserAgent`

Path: `src/main/java/com/db/hackathon/adk/agent/document/DocumentParserAgent.java`

Input:

- `WorkflowContext.filePath`

Output:

- `WorkflowContext.documentAnalysis`

Status after success:

- `PARSED`

It delegates the actual PDF processing to `GoogleDocumentAiProcessor`.

### `GoogleDocumentAiProcessor`

Path: `src/main/java/com/db/hackathon/adk/agent/document/GoogleDocumentAiProcessor.java`

Responsibilities:

- Reads a local PDF from `Path`.
- Creates a Google Document AI `RawDocument`.
- Uses MIME type `application/pdf`.
- Builds a processor name from configured project, location, and processor ID.
- Calls `DocumentProcessorServiceClient.processDocument(...)`.
- Maps the returned Google `Document` into the local `DocumentAnalysis` model.

### `DocumentAiMapper`

Path: `src/main/java/com/db/hackathon/adk/agent/document/DocumentAiMapper.java`

Converts a Google Document AI `Document` into:

- `totalPages`
- `fullText`
- `pages`

For each page it extracts text from:

1. Blocks, when available.
2. Paragraphs, as a fallback.

It uses Google text-anchor offsets to slice page/block text from the full document text.

### `DocumentAiConfiguration`

Path: `src/main/java/com/db/hackathon/adk/agent/document/DocumentAiConfiguration.java`

Creates the Google `DocumentProcessorServiceClient`.

It:

- Loads credentials from the classpath.
- Applies the `cloud-platform` scope.
- Builds a regional endpoint like `asia-south1-documentai.googleapis.com:443`.
- Registers the client as a Spring bean with `close` as the destroy method.

### `DocumentAiProperties`

Path: `src/main/java/com/db/hackathon/adk/agent/document/DocumentAiProperties.java`

Maps `google.document-ai.*` properties from `application.yaml`.

## Agent 2: Field Extraction

### `ExtractionAgent`

Path: `src/main/java/com/db/hackathon/adk/agent/extraction/ExtractionAgent.java`

Input:

- `WorkflowContext.documentAnalysis`

Output:

- `WorkflowContext.agreement`

Status after success:

- `EXTRACTED`

It delegates extraction to `AdkRunnerService`.

### `AdkRunnerService`

Path: `src/main/java/com/db/hackathon/adk/agent/extraction/AdkRunnerService.java`

Responsibilities:

- Initializes an ADK `Runner`.
- Creates in-memory ADK services for sessions, artifacts, and memory.
- Creates a new ADK session per extraction.
- Builds an extraction prompt using `PromptBuilder`.
- Sends the prompt to the configured LLM agent.
- Collects streamed response events into a single string.
- Removes Markdown code fences if present.
- Extracts the JSON object substring.
- Deserializes the JSON into `Agreement`.

### `AdkConfiguration`

Path: `src/main/java/com/db/hackathon/adk/agent/extraction/AdkConfiguration.java`

Creates an ADK `LlmAgent` named `ExtractionAgent`.

The model comes from:

```yaml
open-router.ai.model
```

Current configured value:

```yaml
gemini-3.1-flash-lite
```

### Extraction Prompt

Path: `src/main/resources/prompts/extraction.md`

The extraction prompt instructs the LLM to return only valid JSON for these fields:

- `dealName`
- `borrower`
- `facilityAmount`
- `currency`
- `interestRate`
- `effectiveDate`
- `maturityDate`
- `facilityType`
- `arranger`
- `governingLaw`

Each field should be an object containing:

- `value`
- `pageNumber`
- `confidence`
- `sourceText`

## Agent 3: Validation

### `ValidationAgent`

Path: `src/main/java/com/db/hackathon/adk/agent/validation/ValidationAgent.java`

Input:

- `WorkflowContext.agreement`

Output:

- `WorkflowContext.validationResult`

Status after success:

- `VALIDATED`, then the executor marks the workflow `COMPLETED`.

### `ValidationService`

Path: `src/main/java/com/db/hackathon/adk/agent/validation/ValidationService.java`

Validates:

- Mandatory fields are present and non-blank.
- Field confidence is at least `0.80`.

Missing mandatory fields become errors.

Low-confidence fields become warnings.

### `AgreementField`

Path: `src/main/java/com/db/hackathon/adk/agent/validation/AgreementField.java`

Defines all supported agreement fields and whether each field is mandatory.

Mandatory:

- `DEAL_NAME`
- `BORROWER`
- `FACILITY_AMOUNT`
- `CURRENCY`
- `EFFECTIVE_DATE`
- `FACILITY_TYPE`

Optional:

- `MATURITY_DATE`
- `INTEREST_RATE`
- `ARRANGER`
- `GOVERNING_LAW`

### `ValidationCode`

Path: `src/main/java/com/db/hackathon/adk/agent/validation/ValidationCode.java`

Supported validation codes:

- `MISSING_FIELD`
- `LOW_CONFIDENCE`
- `INVALID_DATE`
- `INVALID_AMOUNT`
- `INVALID_CURRENCY`

Only `MISSING_FIELD` and `LOW_CONFIDENCE` are currently used.

## Data Models

### `WorkflowContext`

Path: `src/main/java/com/db/hackathon/dto/WorkflowContext.java`

In-memory object passed through the workflow.

Contains:

- `workflow`
- `filePath`
- `documentAnalysis`
- `agreement`
- `validationResult`

### `WorkflowResponse`

Path: `src/main/java/com/db/hackathon/dto/WorkflowResponse.java`

Returned from the API.

Contains:

- `workflowId`
- `status`
- `agreement`
- `validationResult`

### `DocumentAnalysis`

Path: `src/main/java/com/db/hackathon/model/document/DocumentAnalysis.java`

Contains:

- `totalPages`
- `fullText`
- `pages`

### `DocumentPage`

Path: `src/main/java/com/db/hackathon/model/document/DocumentPage.java`

Contains:

- `pageNumber`
- `text`
- `blankPage`

### `Agreement`

Path: `src/main/java/com/db/hackathon/model/extraction/Agreement.java`

Contains one `ExtractedField` per agreement field.

Also provides `getField(AgreementField)` so validation can loop over enum values instead of hard-coding each property.

### `ExtractedField`

Path: `src/main/java/com/db/hackathon/model/extraction/ExtractedField.java`

Contains:

- `value`
- `pageNumber`
- `confidence`
- `sourceText`

### `ValidationResult`

Path: `src/main/java/com/db/hackathon/model/validation/ValidationResult.java`

Contains:

- `valid`
- `errors`
- `warnings`

### `ValidationMessage`

Path: `src/main/java/com/db/hackathon/model/validation/ValidationMessage.java`

Contains:

- `field`
- `code`
- `message`
- `pageNumber`

## Persistence

The app uses H2 in-memory database configuration:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:hackathon26
```

The H2 console is enabled at:

```text
/h2-console
```

### `WorkflowEntity`

Path: `src/main/java/com/db/hackathon/entity/WorkflowEntity.java`

Table:

```text
workflow
```

Stores:

- `workflowId`
- `username`
- `status`
- `nextAgent`
- `metadata`
- `humanJson`
- `failureReason`
- `createdAt`
- `updatedAt`
- `completedAt`

### `WorkflowEventEntity`

Path: `src/main/java/com/db/hackathon/entity/WorkflowEventEntity.java`

Table:

```text
workflow_event
```

Stores one event per workflow agent execution.

Fields:

- `id`
- `workflowId`
- `agent`
- `status`
- `retryCount`
- `durationMs`
- `failureReason`
- `createdAt`
- `updatedAt`

### Repositories

`WorkflowRepository`:

- Basic CRUD for workflows.
- `findPendingWorkflows()` returns workflows that are not `COMPLETED` or `FAILED`.

`WorkflowEventRepository`:

- Basic CRUD for workflow events.
- Finds the latest event for a workflow and agent.

## Configuration

Path: `src/main/resources/application.yaml`

Main settings:

```yaml
server:
  port: 8080

spring:
  application:
    name: credit-agreement-ai

google:
  ai:
    appName: credit-agreement-ai
    userId: ops-user
    model: gemini-3.5-flash
  document-ai:
    project-id: 277914962735
    location: asia-south1
    processor-id: bbe75eb9ad1a653c
    credentials: hackathon-document-ai.json

open-router:
  ai:
    appName: credit-agreement-ai
    userId: ops-user
    model: gemini-3.1-flash-lite
```

Important runtime requirements:

- A Google Document AI credentials file named `hackathon-document-ai.json` must be available on the application classpath.
- The configured processor must exist in the configured region.
- The app process must be able to read the PDF path passed to `/workflow/process`.

## Error Handling

Path: `src/main/java/com/db/hackathon/exception/GlobalExceptionHandler.java`

Handles:

- `IllegalArgumentException` as `400 Bad Request`.
- Google Document AI `InvalidArgumentException` as `400 Bad Request`.
- gRPC `StatusRuntimeException` as either `400` for invalid arguments or `500` for other processing failures.
- Generic `Exception` as `500 Internal Server Error`.

The handler includes user-oriented messages for invalid PDF, empty file, processor configuration, deployment location, and permissions problems.

## Tests

Path: `src/test/java/com/db/hackathon26_poc/CreditAgreementAiApplicationTests.java`

Current test coverage:

- One Spring context-load test.

There are no unit tests yet for:

- Workflow state transitions.
- Document AI mapping.
- Prompt building.
- ADK response cleanup.
- Agreement validation.
- Failure and retry behavior.

## How to Run

The intended command is:

```bash
./mvnw spring-boot:run
```

If the wrapper script is not executable:

```bash
bash mvnw spring-boot:run
```

Then call:

```bash
curl -X POST http://localhost:8080/workflow/process \
  -F "filePath=/path/to/agreement.pdf"
```

## Current Implementation Caveats

These are important if you are trying to run the app as-is.

1. `WorkflowEntity` does not currently define `jsonOutput`, but `WorkflowManager` and `WorkflowContextBuilder` call `setJsonOutput(...)` and `getJsonOutput()`. The entity has `metadata` and `humanJson` fields instead. This will likely prevent compilation until the field naming is aligned.

2. `WorkflowService` stores the initial `filePath` in `metadata`, but `WorkflowContextBuilder` tries to read it from `jsonOutput`. These should use the same field.

3. `ExtractionAgent.getOutput(...)` currently returns `context.getDocumentAnalysis()`. It should probably return `context.getAgreement()` so the extraction checkpoint stores the extracted agreement rather than the parsed document.

4. `getInputStatus()` values from the agents are not used by `WorkflowExecutor`, so statuses like `PARSING`, `EXTRACTING`, and `VALIDATING` are never persisted during active execution.

5. The retry logic increments retry count and can mark a workflow failed, but the current synchronous `WorkflowEngine.execute(...)` does not loop and retry the failed agent within the same API call.

6. `document.md` and `validation.md` exist under `src/main/resources/prompts`, but they are empty. Only `extraction.md` currently drives prompt behavior.

7. The endpoint imports multipart upload types and consumes `multipart/form-data`, but it does not accept an uploaded PDF file. It accepts only `filePath`.

8. `WorkflowRepository.findPendingWorkflows()` exists, but there is no scheduler or worker in this repo currently using it.

9. The Maven wrapper script may need executable permission before `./mvnw` works directly.

## Mental Model

Think of this app as a synchronous workflow runner around three steps:

1. Convert a credit agreement PDF into structured text.
2. Ask an LLM to extract business fields from that text.
3. Validate required fields and confidence scores.

The workflow tables are designed to support checkpointing and resume behavior, but the current API path runs the full pipeline immediately in one request.
