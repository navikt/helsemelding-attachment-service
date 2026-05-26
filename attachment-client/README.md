# attachment-client

Kotlin client library for interacting with the Attachment Service.

## Purpose

Simplify integration with `helsemelding-attachment-service` by hiding HTTP, serialization and error handling from consumers

## Usage

Import `attachment-client` library:

```
dependencies {
    implementation("no.nav.helsemelding:attachment-client:0.0.1")
    ...
}
```

Saving attachments:

```
val attachmentClient = HttpAttachmentClient()

val messageId = Uuid.random()

val fileBytes = Files.readAllBytes(Paths.get("..."))
val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)

val testAttachments = listOf(
    Attachment(
        description = "PDF file",
        contentType = "application/pdf",
        contentBase64 = fileBase64
    ),
    ...
)

val savingResult = attachmentClient.saveAttachments(
    messageId = messageId,
    attachments = testAttachments
)

if (savingResult.isRight()) {
    // Attachments saved successfully
} else {
    val savingError = savingResult.leftOrNull()
    // Handle error
}
```

Get attachments:

```
val attachmentClient = HttpAttachmentClient()

val readingResult = attachmentClient.getAttachments(messageId)

if (readingResult.isRight()) {
    val attachments = readingResult.getOrNull()!!

    val fileBase64 = attachments[0].contentBase64
    val fileBytes = Base64.getDecoder().decode(fileBase64)

    Files.write(Paths.get("..."), fileBytes)
} else {
    val readingError = readingResult.leftOrNull()
    // Handle error
}
```

## Dependencies

* Uses shared models from `attachment-model`
* Calls the HTTP API exposed by `helsemelding-attachment-service`
