# GCP Free-Tier Setup

This guide provisions the Google Cloud resources required by the Credit Agreement AI backend.

> Important: Google Cloud requires a billing account for the Free Trial and many APIs. New accounts may receive promotional credits for a limited period, but Document AI, Gemini/Vertex AI, storage, and network usage can create charges after credits or free quotas are exhausted. Set a budget alert before testing.

## Architecture

![Credit Agreement AI event flow](docs/architecture-flow.svg)

The setup creates this event path:

```mermaid
flowchart LR
    App[Spring Boot upload API] --> Bucket[GCS bucket]
    Bucket --> Notify[GCS notification]
    Notify --> Topic[Pub/Sub topic]
    Topic --> Sub[Pub/Sub subscription]
    Sub --> Listener[AgreementSubscriber]
    Listener --> DB[(Workflow table)]
    Listener --> AI[Document AI and LLM]
    AI --> DB
    UI[External UI] --> App
    UI --> DB
```

## Values Used by This Project

Use your own project and resource names where possible. The application currently expects these values:

| Resource | Current value |
| --- | --- |
| Spring Cloud project | `gen-lang-client-0146388054` |
| Document AI project | `277914962735` |
| GCS bucket | `loaniq-agreement` |
| Pub/Sub subscription | `agreement-upload-sub` |
| Document AI location | `asia-south1` |
| Document AI processor ID | `bbe75eb9ad1a653c` |
| Credential file | `src/main/resources/hackathon26-credentials.json` |

For a new setup, prefer one project for all resources. If the existing Document AI processor belongs to a different project, either keep the separate project and grant access across projects or create a new processor in the application project.

## 1. Create a Free-Tier Account and Project

1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a Google account.
3. Start the Google Cloud Free Trial if it is offered for the account.
4. Create a new project, for example `credit-agreement-ai-dev`.
5. Open **Billing** and link the project to the billing account.

The Free Trial is not the same as a permanently free account. Stop services and delete test resources when finished.

## 2. Enable Required APIs

In **APIs & Services** -> **Library**, enable:

- Cloud Storage
- Google Cloud Storage JSON API
- Cloud Pub/Sub API
- Cloud Document AI API

## 3. Create the GCS Bucket

1. Open **Cloud Storage** -> **Buckets** -> **Create**.
2. Use a globally unique bucket name.
3. Choose **Uniform access control**.
4. Choose a location, such as `US`, if it is appropriate for your data.
5. Do not make the bucket public.
6. Create the bucket.

The upload code adds `uuid` and `username` as object metadata. Do not expose the bucket publicly; the backend service account should be the only reader/writer.

## 4. Create Pub/Sub Topic and Subscription

The topic receives object-created notifications from GCS. The subscription is consumed by this application.

1. Open **Pub/Sub** -> **Topics** -> **Create topic**.
2. Create `agreement-upload-topic`.
3. Create a subscription named `agreement-upload-sub` on that topic.
4. Keep message retention and acknowledgement defaults for development.

## 5. Allow GCS to Publish Notifications

GCS uses its project service agent to publish to the Pub/Sub topic. Configure this entirely in the Google Cloud Console:

1. Open **IAM & Admin** -> **IAM** and select the application project.
2. Find the Google-managed service account with this format:
   `service-PROJECT_NUMBER@gs-project-accounts.iam.gserviceaccount.com`.
3. If it is not listed, open **IAM & Admin** -> **Service Accounts**, select **Google-managed service accounts**, and locate the **Cloud Storage service agent**. Google creates it after the Cloud Storage API is enabled.
4. Open **Pub/Sub** -> **Topics** -> `agreement-upload-topic`.
5. Open the **Permissions** or **Grant access** panel.
6. Add the Cloud Storage service agent as a principal.
7. Select the role **Pub/Sub Publisher** and save.

The project number is shown in **IAM & Admin** -> **Settings**. Use the project number, not the project ID, in the service-agent email address.

## 6. Create the GCS Notification

Create a notification for new object events from the Google Cloud Console:

1. Open **Cloud Storage** -> **Buckets** and select `YOUR_BUCKET_NAME`.
2. Open the **Notifications** tab.
3. Select **Create notification**.
4. Enter a notification name, such as `agreement-upload-notification`.
5. For the destination, select **Cloud Pub/Sub topic**.
6. Select the application project and the `agreement-upload-topic` topic.
7. Select the object event **Object finalized**. This fires when an upload completes.
8. Select **JSON** as the payload format and leave the optional prefix/suffix filters empty unless the application only processes a specific path.
9. Select **Create**.
10. Return to the bucket's **Notifications** tab and confirm the notification is listed and points to `agreement-upload-topic`.

This notification is the **publisher**. The Spring Boot application does not publish the event; it only subscribes to `agreement-upload-sub`.

## 7. Create a Document AI Processor

1. Open **Document AI** in the Google Cloud Console.
2. Select the same project used by the application, unless you intentionally use a separate Document AI project.
3. Choose **Processor Gallery** -> select the appropriate document/OCR processor.
4. Create the processor in the desired location.
5. Copy the processor ID from the processor details page.
6. Record the processor location and project ID.

Update these values in `src/main/resources/application.yaml`:

```yaml
google:
  document-ai:
    project-id: YOUR_DOCUMENT_AI_PROJECT_ID
    location: YOUR_PROCESSOR_LOCATION
    processor-id: YOUR_PROCESSOR_ID
```

Document AI is not generally covered by an unlimited free tier. Test with small documents and monitor billing.

## 8. Create Runtime Permissions

For local development, create a dedicated service account rather than using a personal account:

1. Open **IAM & Admin** -> **Service Accounts** and select the application project.
2. Select **Create service account**.
3. Enter `credit-agreement-ai` as the service-account name and `Credit Agreement AI local runtime` as the description.
4. Select **Create and continue**.
5. In **Grant this service account access to project**, add **Pub/Sub Subscriber** and **Document AI API User**.
6. Select **Continue**.
7. For bucket access, open **Cloud Storage** -> **Buckets** -> `YOUR_BUCKET_NAME` -> **Permissions** -> **Grant access**. Add the service account email and select **Storage Object Admin**.
8. If the configured LLM uses Vertex AI, open **IAM & Admin** -> **IAM**, find the service account, select **Edit principal**, and add **Vertex AI User**.
9. Save each change.

For production, replace broad development roles with narrower custom roles and use Workload Identity or the hosting platform's attached service account instead of a JSON key.

## 9. Configure Local Credentials

The current application loads the credential file configured here:

```yaml
google:
  document-ai:
    credentials: hackathon26-credentials.json
```

For local-only development, create and download a key from the Google Cloud Console:

1. Open **IAM & Admin** -> **Service Accounts** and select the application project.
2. Select `credit-agreement-ai`.
3. Open the **Keys** tab and select **Add key** -> **Create new key**.
4. Select **JSON** and select **Create**. The browser downloads the key once; Google does not provide the same private key again.
5. Rename the downloaded file to `hackathon26-credentials.json` and place it at `src/main/resources/hackathon26-credentials.json`.

Place the file where the application can load it from the classpath, or change the application configuration to use an external path.

**Never commit the JSON key.** Add the credential filename to `.gitignore`, rotate it immediately if it is exposed, and prefer Application Default Credentials or Workload Identity outside local development.

## 10. Update Application Configuration

Update `src/main/resources/application.yaml`:

```yaml
spring:
  cloud:
    gcp:
      project-id: YOUR_PROJECT_ID

google:
  document-ai:
    project-id: YOUR_DOCUMENT_AI_PROJECT_ID
    location: YOUR_PROCESSOR_LOCATION
    processor-id: YOUR_PROCESSOR_ID
    credentials: hackathon26-credentials.json
  pubsub:
    subscription: agreement-upload-sub
  bucket:
    name: YOUR_BUCKET_NAME
```

The application currently uses an H2 in-memory database, so no Cloud SQL instance is required for local development.

## 11. Verify the Complete Flow

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Upload a small test PDF using the application's web UI or an HTTP client such as Postman. Send a `POST` request to `http://localhost:8080/workflow/upload` as `multipart/form-data` with these fields:

- `uuid`: `workflow-123`
- `username`: `ops-user`
- `file`: the test PDF

Verify each boundary:

1. Confirm the PDF exists in the GCS bucket.
2. Open **Pub/Sub** -> `agreement-upload-sub` and check that messages are being acknowledged.
3. Check application logs for `Received Pub/Sub payload`.
4. Check the `workflow` table for the workflow UUID.
5. Confirm the status progresses through `UPLOADED`, `PARSED`, `EXTRACTED`, `VALIDATED`, `REVIEWED`, and `HUMAN_REVIEW_PENDING`.
6. Inspect `metadata` for the latest serialized result.

## Cleanup

Delete development resources from the Google Cloud Console when finished:

1. Open **Cloud Storage** -> **Buckets**, select `YOUR_BUCKET_NAME`, open **Objects**, select all objects, choose **Delete**, then return to the bucket list and delete the bucket.
2. Open **Pub/Sub** -> **Subscriptions**, select `agreement-upload-sub`, choose **Delete**, and confirm.
3. Open **Pub/Sub** -> **Topics**, select `agreement-upload-topic`, choose **Delete**, and confirm.
4. Open **IAM & Admin** -> **Service Accounts**, select `credit-agreement-ai`, choose **Delete**, and confirm.
5. Delete the project from **IAM & Admin** -> **Settings** only if it contains no resources you need.
